package com.houssein.sezaia.ui.screen

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.houssein.sezaia.R
import com.houssein.sezaia.model.request.AddQrRequest
import com.houssein.sezaia.model.response.BaseResponse
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.utils.UIUtils
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ActivateQrCodeActivity : BaseActivity() {

    private lateinit var scroll: NestedScrollView

    private lateinit var usernameEditText: TextInputEditText
    private lateinit var qrIdEditText: TextInputEditText
    private lateinit var countryEditText: TextInputEditText
    private lateinit var cityEditText: TextInputEditText
    private lateinit var zoneEditText: TextInputEditText
    private lateinit var streetEditText: TextInputEditText
    private lateinit var exactLocationEditText: TextInputEditText
    private lateinit var btnAddQrCode: Button

    private lateinit var userLayout: TextInputLayout
    private lateinit var qrIdLayout: TextInputLayout
    private lateinit var countryLayout: TextInputLayout
    private lateinit var cityLayout: TextInputLayout
    private lateinit var zoneLayout: TextInputLayout
    private lateinit var streetLayout: TextInputLayout
    private lateinit var exactLocationLayout: TextInputLayout

    private lateinit var inputFields: List<Pair<TextInputEditText, TextInputLayout>>

    // mise en cache des infos de session pour éviter des getSharedPreferences répétés
    private var userRole: String? = null
    private var loggedUsername: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ajuste le contenu quand le clavier apparaît
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        setContentView(R.layout.activity_activate_qr_code)
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))

        scroll = findViewById(R.id.scroll)
        applyImeAndSystemBarsPadding(scroll)

        // charge les préférences une seule fois
        getSharedPreferences("LoginData", MODE_PRIVATE).apply {
            userRole = getString("userRole", null)
            loggedUsername = getString("loggedUsername", null)
        }

        initViews()
        attachFocusAutoScroll()
        applyUserRoleBehavior() // pré-remplir/verrouiller si nécessaire

        UIUtils.initToolbar(
            this,
            getString(R.string.activate_qr_code),
            actionIconRes = R.drawable.baseline_qr_code_24,
            onBackClick = { finish() },
            onActionClick = { recreate() }
        )

        btnAddQrCode.setOnClickListener { addQrCode() }
    }

    private fun initViews() {
        usernameEditText = findViewById(R.id.user)
        qrIdEditText = findViewById(R.id.idQr)
        countryEditText = findViewById(R.id.country)
        cityEditText = findViewById(R.id.city)
        zoneEditText = findViewById(R.id.zone)
        streetEditText = findViewById(R.id.street)
        exactLocationEditText = findViewById(R.id.exactLocation)

        userLayout = findViewById(R.id.userLayout)
        qrIdLayout = findViewById(R.id.idQrLayout)
        countryLayout = findViewById(R.id.countryLayout)
        cityLayout = findViewById(R.id.cityLayout)
        zoneLayout = findViewById(R.id.zoneLayout)
        streetLayout = findViewById(R.id.streetLayout)
        exactLocationLayout = findViewById(R.id.exactLocationLayout)

        btnAddQrCode = findViewById(R.id.btnAddQrcode)

        inputFields = listOf(
            usernameEditText to userLayout,
            qrIdEditText to qrIdLayout,
            countryEditText to countryLayout,
            cityEditText to cityLayout,
            zoneEditText to zoneLayout,
            streetEditText to streetLayout,
            exactLocationEditText to exactLocationLayout
        )

        inputFields.firstOrNull()?.first?.requestFocus()
        inputFields.forEach { (et, layout) ->
            et.addTextChangedListener(UIUtils.inputWatcher(et, layout))
        }
    }

    /** Applique l’affichage voulu selon le rôle :
     *  - user : champ visible, pré-rempli avec loggedUsername, non modifiable
     *  - autres : champ éditable
     */
    private fun applyUserRoleBehavior() {
        if (userRole == "user") {
            val value = loggedUsername.orEmpty()
            usernameEditText.setText(value)
            usernameEditText.isEnabled = false
            usernameEditText.isFocusable = false
            usernameEditText.isClickable = false
            userLayout.isHintEnabled = true
            // Optionnel: teinte pour montrer "lecture seule"
            usernameEditText.setTextColor(ContextCompat.getColor(this, R.color.gray))
            qrIdEditText.requestFocus()
        } else {
            usernameEditText.isEnabled = true
            usernameEditText.isFocusableInTouchMode = true
            usernameEditText.requestFocus()
        }
    }

    // ===== clavier & scroll identiques à SignUp =====

    private fun applyImeAndSystemBarsPadding(target: View) {
        val start = target.paddingLeft
        val top = target.paddingTop
        val end = target.paddingRight
        val baseBottom = target.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(target) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.updatePadding(
                left = start,
                top = top,
                right = end,
                bottom = baseBottom + maxOf(sys.bottom, ime.bottom)
            )
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun attachFocusAutoScroll() {
        val margin = dp(12f)
        inputFields.map { it.first }.forEach { edit ->
            edit.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) v.post { ensureVisible(scroll, v, margin) }
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

    // ===== API =====

    private fun addQrCode() {
        // ⚠️ On garde exactement ta logique demandée pour username :
        val username: String = if (userRole == "user") {
            getSharedPreferences("LoginData", MODE_PRIVATE)
                .getString("loggedUsername", null) ?: ""
        } else {
            usernameEditText.text.toString().trim()
        }

        val idQr = qrIdEditText.text.toString().trim()
        val country = countryEditText.text.toString().trim()
        val city = cityEditText.text.toString().trim()
        val zone = zoneEditText.text.toString().trim()
        val street = streetEditText.text.toString().trim()
        val exactLocation = exactLocationEditText.text.toString().trim()

        val qrCode = getSharedPreferences("MyPrefs", MODE_PRIVATE)
            .getString("qrData", null)
            .orEmpty()

        // Validation simple
        if (username.isEmpty() || idQr.isEmpty() || country.isEmpty() || city.isEmpty()
            || zone.isEmpty() || street.isEmpty() || exactLocation.isEmpty() || qrCode.isEmpty()
        ) {
            Toast.makeText(this, R.string.all_fields_required, Toast.LENGTH_SHORT).show()
            return
        }

        val request = AddQrRequest(
            username = username,
            qr_id = idQr,
            country = country,
            city = city,
            zone = zone,
            street = street,
            exact_location = exactLocation,
            qr_code = qrCode
        )

        RetrofitClient.instance.addQr(request).enqueue(object : Callback<BaseResponse> {
            override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.status == "success") {
                        Toast.makeText(this@ActivateQrCodeActivity, body.message, Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@ActivateQrCodeActivity, CameraActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(
                            this@ActivateQrCodeActivity,
                            body?.message ?: getString(R.string.unknown_error),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    val msg = try {
                        JSONObject(response.errorBody()?.string() ?: "")
                            .optString("message", getString(R.string.server_error))
                    } catch (_: Exception) {
                        getString(R.string.server_error)
                    }
                    Toast.makeText(this@ActivateQrCodeActivity, msg, Toast.LENGTH_SHORT).show()
                }
            }

            @SuppressLint("StringFormatInvalid")
            override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                Toast.makeText(
                    this@ActivateQrCodeActivity,
                    getString(R.string.network_error, t.localizedMessage ?: "Unknown"),
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }
}
