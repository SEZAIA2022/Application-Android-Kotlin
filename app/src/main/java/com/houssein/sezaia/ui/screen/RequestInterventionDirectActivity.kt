package com.houssein.sezaia.ui.screen

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.houssein.sezaia.R
import com.houssein.sezaia.model.data.MyApp
import com.houssein.sezaia.model.request.NotificationRequest
import com.houssein.sezaia.model.request.SendAskDirectRequest
import com.houssein.sezaia.model.request.TechnicianRequest
import com.houssein.sezaia.model.response.SendAskDirectResponse
import com.houssein.sezaia.model.response.TechnicianResponse
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.utils.UIUtils
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RequestInterventionDirectActivity : BaseActivity() {

    private lateinit var scroll: NestedScrollView
    private lateinit var idQr: TextInputEditText
    private lateinit var idQrLayout: TextInputLayout
    private lateinit var comment: TextInputEditText
    private lateinit var commentLayout: TextInputLayout
    private lateinit var confirmButton: Button

    private lateinit var loggedEmail: String
    private lateinit var loggedUsername: String
    private lateinit var applicationName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Le clavier redimensionne la zone utile
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        setContentView(R.layout.activity_request_intervention_direct)

        UIUtils.applySystemBarsInsets(findViewById(R.id.main))

        // Toolbar
        UIUtils.initToolbar(
            this,
            getString(R.string.request_intervention),
            actionIconRes = R.drawable.baseline_density_medium_24,
            onBackClick = { finish() },
            onActionClick = { startActivity(Intent(this, SettingsActivity::class.java)) }
        )

        // Prefs & contexte
        val spLogin = getSharedPreferences("LoginData", MODE_PRIVATE)
        loggedEmail = spLogin.getString("LoggedEmail", "") ?: ""
        loggedUsername = spLogin.getString("loggedUsername", "") ?: ""
        applicationName = (application as MyApp).application_name

        // Views
        scroll = findViewById(R.id.scroll)
        idQr = findViewById(R.id.idQr)
        idQrLayout = findViewById(R.id.idQrLayout)
        comment = findViewById(R.id.comment)
        commentLayout = findViewById(R.id.commentLayout)
        confirmButton = findViewById(R.id.confirmButton)

        // Padding bas dynamique (barres système + IME)
        applyImeAndSystemBarsPadding(scroll)

        // Validation live (réutilise ton util)
        idQr.addTextChangedListener(UIUtils.inputWatcher(idQr, idQrLayout))
        comment.addTextChangedListener(UIUtils.inputWatcher(comment, commentLayout))

        // Auto-scroll si un champ prend le focus et est caché
        attachFocusAutoScroll(scroll, listOf(idQr, comment))

        confirmButton.setOnClickListener { onConfirmClick() }
    }

    private fun onConfirmClick() {
        val qrText = idQr.text?.toString()?.trim().orEmpty()
        val commentText = comment.text?.toString()?.trim().orEmpty()

        var ok = true
        if (qrText.isEmpty()) {
            idQrLayout.error = getString(R.string.all_fields_required)
            ok = false
        } else idQrLayout.error = null

        if (commentText.isEmpty()) {
            commentLayout.error = getString(R.string.all_fields_required)
            ok = false
        } else commentLayout.error = null

        if (!ok) return

        confirmButton.isEnabled = false

        // 1) Trouver un technicien — on n'envoie QUE l'email (autres champs vides)
        val reqTech = TechnicianRequest(
            email = loggedEmail,
            application_name = "", // backend peut déduire
            date = "",             // backend calcule (>= now + 2h)
            hour_slot = ""         // backend calcule
        )

        RetrofitClient.instance.getNearestAdminEmail(reqTech)
            .enqueue(object : Callback<TechnicianResponse> {
                override fun onResponse(
                    call: Call<TechnicianResponse>,
                    response: Response<TechnicianResponse>
                ) {
                    if (!response.isSuccessful) {
                        val serverMsg = parseServerMessage(response)
                        Toast.makeText(
                            this@RequestInterventionDirectActivity,
                            serverMsg ?: getString(R.string.no_tech_available),
                            Toast.LENGTH_LONG
                        ).show()
                        confirmButton.isEnabled = true
                        return
                    }

                    val techEmail = response.body()?.email
                    if (response.body()?.status != "success" || techEmail.isNullOrBlank()) {
                        val msg = response.body()?.message ?: getString(R.string.no_tech_available)
                        Toast.makeText(this@RequestInterventionDirectActivity, msg, Toast.LENGTH_LONG).show()
                        confirmButton.isEnabled = true
                        return
                    }

                    // 2) Créer la demande en base via /send_ask_direct
                    val createReq = SendAskDirectRequest(
                        username = loggedUsername.ifBlank { loggedEmail },
                        comment = commentText,
                        qr_id = qrText,
                        application_name = applicationName,
                        technician_email = techEmail
                    )

                    RetrofitClient.instance.sendAskDirect(createReq)
                        .enqueue(object : Callback<SendAskDirectResponse> {
                            override fun onResponse(
                                call: Call<SendAskDirectResponse>,
                                resp: Response<SendAskDirectResponse>
                            ) {
                                val okCreate = resp.isSuccessful && resp.body()?.status == "success"
                                if (!okCreate) {
                                    val msg = parseServerMessage(resp) ?: getString(R.string.booking_error)
                                    Toast.makeText(this@RequestInterventionDirectActivity, msg, Toast.LENGTH_LONG).show()
                                    confirmButton.isEnabled = true
                                    return
                                }

                                val askId = resp.body()?.ask_repair_id ?: "-"

                                // 3) Notifier le technicien
                                val msgAdmin =
                                    "Direct intervention request\nQR: $qrText\nComment: $commentText\nRef: #$askId"
                                sendNotification(
                                    role = "admin",
                                    email = techEmail,
                                    message = msgAdmin
                                ) {
                                    // 4) Notifier l’utilisateur
                                    val msgUser =
                                        "Your direct intervention request has been sent.\nReference: #$askId"
                                    sendNotification(
                                        role = "user",
                                        email = loggedEmail,
                                        message = msgUser
                                    ) {
                                        Toast.makeText(
                                            this@RequestInterventionDirectActivity,
                                            R.string.request_sent,
                                            Toast.LENGTH_LONG
                                        ).show()
                                        finish()
                                    }
                                }
                            }

                            override fun onFailure(call: Call<SendAskDirectResponse>, t: Throwable) {
                                Toast.makeText(
                                    this@RequestInterventionDirectActivity,
                                    getString(R.string.network_error, t.localizedMessage),
                                    Toast.LENGTH_LONG
                                ).show()
                                confirmButton.isEnabled = true
                            }
                        })
                }

                override fun onFailure(call: Call<TechnicianResponse>, t: Throwable) {
                    Toast.makeText(
                        this@RequestInterventionDirectActivity,
                        getString(R.string.network_error, t.localizedMessage),
                        Toast.LENGTH_LONG
                    ).show()
                    confirmButton.isEnabled = true
                }
            })
    }

    /** Envoie une notif via /notify_admin (utilisé pour admin & user). */
    private fun sendNotification(role: String, email: String, message: String, onDone: () -> Unit) {
        RetrofitClient.instance.notifyAdmin(
            NotificationRequest(
                message = message,
                role = role,
                email = email,
                application_name = applicationName
            )
        ).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                // on continue même si la notif échoue
                onDone()
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                onDone()
            }
        })
    }

    // ----------- Helpers UI : clavier & scroll -----------

    /** Padding bas = max(barres système, clavier) pour garder le bouton visible. */
    private fun applyImeAndSystemBarsPadding(target: View) {
        val l = target.paddingLeft
        val t = target.paddingTop
        val r = target.paddingRight
        val baseB = target.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(target) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.updatePadding(left = l, top = t, right = r, bottom = baseB + maxOf(sys.bottom, ime.bottom))
            WindowInsetsCompat.CONSUMED
        }
    }

    /** Auto-scroll seulement si l’input n’est pas entièrement visible. */
    private fun attachFocusAutoScroll(nsv: NestedScrollView, views: List<View>) {
        val margin = dp(12f)
        views.forEach { v ->
            v.setOnFocusChangeListener { fv, hasFocus ->
                if (hasFocus) fv.post { ensureVisible(nsv, fv, margin) }
            }
        }
    }

    private fun ensureVisible(nsv: NestedScrollView, child: View, margin: Int) {
        val r = Rect()
        child.getDrawingRect(r)
        nsv.offsetDescendantRectToMyCoords(child, r)

        val top = nsv.scrollY + nsv.paddingTop
        val bottom = nsv.scrollY + nsv.height - nsv.paddingBottom

        when {
            r.top - margin < top -> nsv.smoothScrollTo(0, r.top - nsv.paddingTop - margin)
            r.bottom + margin > bottom -> {
                val y = r.bottom - (nsv.height - nsv.paddingBottom) + margin
                nsv.smoothScrollTo(0, y)
            }
        }
    }

    private fun dp(v: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics).toInt()

    // ----------- Helpers parsing -----------

    private fun parseServerMessage(response: Response<*>): String? {
        return try {
            val raw = response.errorBody()?.string()
            if (raw.isNullOrBlank()) null else JSONObject(raw).optString("message").takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }
}
