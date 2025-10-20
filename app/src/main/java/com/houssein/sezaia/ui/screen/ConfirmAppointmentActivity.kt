package com.houssein.sezaia.ui.screen

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.houssein.sezaia.R
import com.houssein.sezaia.model.data.MyApp
import com.houssein.sezaia.model.data.QuestionAnswer
import com.houssein.sezaia.model.request.AskRepairWithResponsesRequest
import com.houssein.sezaia.model.request.NotificationRequest
import com.houssein.sezaia.model.request.ResponseItem
import com.houssein.sezaia.model.request.TechnicianRequest
import com.houssein.sezaia.model.response.BaseResponse
import com.houssein.sezaia.model.response.TechnicianResponse
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.utils.UIUtils
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume

class ConfirmAppointmentActivity : BaseActivity() {

    private lateinit var scroll: NestedScrollView
    private lateinit var summary: TextView
    private lateinit var confirmButton: MaterialButton
    private lateinit var commentEditText: TextInputEditText
    private lateinit var commentLayout: TextInputLayout

    private lateinit var applicationName: String
    private lateinit var loggedUser: String
    private lateinit var loggedEmail: String
    private lateinit var qrData: String
    private lateinit var selectedDateIso: String
    private lateinit var selectedSlot: String
    private lateinit var formattedDate: String
    private lateinit var responseList: ArrayList<QuestionAnswer>

    private var commentText: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        setContentView(R.layout.activity_confirm_appointment)
        UIUtils.applySystemBarsInsets(findViewById(R.id.rootConfirm))
        UIUtils.initToolbar(
            this, getString(R.string.confirm_appointment), R.drawable.baseline_density_medium_24,
            onBackClick = { finish() },
            onActionClick = { startActivity(Intent(this, SettingsActivity::class.java)) }
        )

        // Views
        scroll = findViewById(R.id.scroll)
        summary = findViewById(R.id.summary)
        confirmButton = findViewById(R.id.confirmButton)
        commentEditText = findViewById(R.id.comment)
        commentLayout = findViewById(R.id.commentLayout)
        confirmButton.isEnabled = false
        confirmButton.alpha = 0.5f

        // Extras
        loggedUser = intent.getStringExtra("loggedUser") ?: ""
        loggedEmail = intent.getStringExtra("loggedEmail") ?: ""
        qrData = intent.getStringExtra("qrData") ?: ""
        applicationName = intent.getStringExtra("applicationName") ?: (application as MyApp).application_name
        selectedDateIso = intent.getStringExtra("selectedDate") ?: ""
        selectedSlot = intent.getStringExtra("selectedSlot") ?: ""
        formattedDate = intent.getStringExtra("formattedDate") ?: ""
        @Suppress("UNCHECKED_CAST")
        responseList = (intent.getSerializableExtra("responses") as? ArrayList<QuestionAnswer>) ?: arrayListOf()

        summary.text = formattedDate

        // validation commentaire
        commentEditText.addTextChangedListener(UIUtils.inputWatcher(commentEditText, commentLayout))
        commentEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                commentText = s?.toString().orEmpty()
                updateConfirmButtonState()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // gestion clavier pour garder le champ visible
        applyImeAndSystemBarsPadding(scroll)
        hookImeAnimationForAutoScroll(scroll)

        confirmButton.setOnClickListener { onConfirmClick() }
    }

    private fun updateConfirmButtonState() {
        val enabled = commentText.isNotBlank()
        confirmButton.isEnabled = enabled
        confirmButton.alpha = if (enabled) 1f else 0.5f
    }

    private fun onConfirmClick() {
        if (commentText.isBlank()) {
            Toast.makeText(this, R.string.fill_required_fields, Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val dateKey = selectedDateIso // yyyy-MM-dd
            val technicianEmail = fetchNearestTechnicianEmailSuspend(
                loggedEmail, applicationName, dateKey, selectedSlot
            )
            if (technicianEmail == null) {
                Toast.makeText(this@ConfirmAppointmentActivity, R.string.no_tech_this_slot, Toast.LENGTH_LONG).show()
                return@launch
            }

            val responsesPayload = responseList.map { ResponseItem(it.id, it.answer) }
            val req = AskRepairWithResponsesRequest(
                loggedUser,
                formattedDate,               // "EEEE, dd MMMM HH:mm"
                commentText,
                qrData,
                responsesPayload,
                applicationName,
                technicianEmail
            )

            RetrofitClient.instance.sendAskRepairWithResponses(req)
                .enqueue(object : Callback<BaseResponse> {
                    override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                        if (response.isSuccessful && response.body()?.status == "success") {
                            Toast.makeText(this@ConfirmAppointmentActivity, response.body()?.message, Toast.LENGTH_SHORT).show()
                            sendNotificationToAdmin(technicianEmail, applicationName)
                            sendNotificationToUser(loggedEmail, applicationName)
                            val prefs = getSharedPreferences("MySuccessPrefs", MODE_PRIVATE)
                            prefs.edit().apply {
                                putString("title", getString(R.string.request_sent))
                                putString("content", getString(R.string.request_message_sent))
                                putString("button", getString(R.string.show_history))
                                apply()
                            }
                            startActivity(Intent(this@ConfirmAppointmentActivity, SuccessActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this@ConfirmAppointmentActivity, R.string.booking_error, Toast.LENGTH_SHORT).show()
                        }
                    }
                    @SuppressLint("StringFormatInvalid")
                    override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                        Toast.makeText(
                            this@ConfirmAppointmentActivity,
                            getString(R.string.network_error, t.localizedMessage),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        }
    }

    // ==== utils clavier (reprend ta logique) ====
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
    private fun hookImeAnimationForAutoScroll(target: View) {
        ViewCompat.setWindowInsetsAnimationCallback(
            target,
            object : WindowInsetsAnimationCompat.Callback(
                WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE
            ) {
                override fun onProgress(insets: WindowInsetsCompat, runningAnimations: MutableList<WindowInsetsAnimationCompat>): WindowInsetsCompat = insets
                override fun onEnd(animation: WindowInsetsAnimationCompat) {
                    if (animation.typeMask and WindowInsetsCompat.Type.ime() != 0) {
                        currentFocus?.post { currentFocus?.let { scrollToViewOnScreen(it) } }
                    }
                }
            }
        )
    }
    private fun scrollToViewOnScreen(target: View) {
        if (target.width == 0 || target.height == 0) return
        val r = android.graphics.Rect(0, 0, target.width, target.height)
        target.requestRectangleOnScreen(r, true)
    }

    private fun sendNotificationToAdmin(email: String, appName: String) {
        RetrofitClient.instance.notifyAdmin(
            NotificationRequest("New request received", "admin", email, appName)
        ).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {}
            override fun onFailure(call: Call<Void>, t: Throwable) {}
        })
    }
    private fun sendNotificationToUser(email: String, appName: String) {
        RetrofitClient.instance.notifyAdmin(
            NotificationRequest("New request sent successfully", "user", email, appName)
        ).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {}
            override fun onFailure(call: Call<Void>, t: Throwable) {}
        })
    }

    private suspend fun fetchNearestTechnicianEmailSuspend(
        email: String, application: String, date: String, hourSlot: String
    ): String? = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        RetrofitClient.instance.getNearestAdminEmail(
            TechnicianRequest(email, application, date, hourSlot)
        ).enqueue(object : Callback<TechnicianResponse> {
            override fun onResponse(call: Call<TechnicianResponse>, response: Response<TechnicianResponse>) {
                cont.resume(if (response.isSuccessful) response.body()?.email else null)
            }
            override fun onFailure(call: Call<TechnicianResponse>, t: Throwable) {
                cont.resume(null)
            }
        })
    }
}
