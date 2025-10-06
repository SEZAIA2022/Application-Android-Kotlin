package com.houssein.sezaia.ui.screen

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.hbb20.CountryCodePicker
import com.houssein.sezaia.R
import com.houssein.sezaia.model.data.MyApp
import com.houssein.sezaia.model.request.SignUpRequest
import com.houssein.sezaia.model.response.ApiResponse
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.utils.UIUtils
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignUpActivity : BaseActivity() {

    private lateinit var scroll: NestedScrollView

    private lateinit var usernameEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var countryCode: CountryCodePicker
    private lateinit var numberEditText: TextInputEditText
    private lateinit var addressEditText: TextInputEditText
    private lateinit var cityEditText: TextInputEditText
    private lateinit var postalCodeEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var confirmPasswordEditText: TextInputEditText

    private lateinit var usernameLayout: TextInputLayout
    private lateinit var emailLayout: TextInputLayout
    private lateinit var numberLayout: TextInputLayout
    private lateinit var addressLayout: TextInputLayout
    private lateinit var cityLayout: TextInputLayout
    private lateinit var postalCodeLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var confirmPasswordLayout: TextInputLayout

    private lateinit var applicationName: String
    private lateinit var btnSignUp: Button
    private lateinit var loginLink: TextView
    private lateinit var inputFields: List<Pair<TextInputEditText, TextInputLayout>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Redimensionner la zone utile quand le clavier apparaît
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        setContentView(R.layout.activity_sign_up)
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))

        scroll = findViewById(R.id.scroll)

        // Padding bas dynamique = max(system bars, clavier)
        applyImeAndSystemBarsPadding(scroll)

        initViews()
        attachFocusAutoScroll()   // <— nouveau : scroll seulement quand un champ prend le focus

        UIUtils.initToolbar(
            this,
            getString(R.string.sign_up),
            actionIconRes = R.drawable.baseline_density_medium_24,
            onBackClick = { finish() },
            onActionClick = { startActivity(Intent(this, SettingsActivity::class.java)) }
        )

        UIUtils.setupClickableText(
            context = this,
            textView = loginLink,
            fullText = loginLink.text.toString(),
            clickableText = getString(R.string.login),
            clickableColorRes = R.color.light_blue,
            targetActivity = LoginActivity::class.java
        )

        setupListeners()
    }

    private fun initViews() {
        usernameEditText = findViewById(R.id.username)
        emailEditText = findViewById(R.id.email)
        countryCode = findViewById(R.id.countryCodePicker)
        numberEditText = findViewById(R.id.phone)
        addressEditText = findViewById(R.id.address)
        cityEditText = findViewById(R.id.city)
        postalCodeEditText = findViewById(R.id.postalCode)
        passwordEditText = findViewById(R.id.password)
        confirmPasswordEditText = findViewById(R.id.confirm_password)

        usernameLayout = findViewById(R.id.usernameInputLayout)
        emailLayout = findViewById(R.id.emailInputLayout)
        numberLayout = findViewById(R.id.phoneInputLayout)
        addressLayout = findViewById(R.id.addressInputLayout)
        cityLayout = findViewById(R.id.cityInputLayout)
        postalCodeLayout = findViewById(R.id.postalCodeInputLayout)
        passwordLayout = findViewById(R.id.passwordInputLayout)
        confirmPasswordLayout = findViewById(R.id.confirmPasswordInputLayout)

        applicationName = (application as MyApp).application_name

        btnSignUp = findViewById(R.id.sign_up_button)
        loginLink = findViewById(R.id.loginLink)

        inputFields = listOf(
            usernameEditText to usernameLayout,
            emailEditText to emailLayout,
            numberEditText to numberLayout,
            addressEditText to addressLayout,
            cityEditText to cityLayout,
            postalCodeEditText to postalCodeLayout,
            passwordEditText to passwordLayout,
            confirmPasswordEditText to confirmPasswordLayout
        )
        inputFields.firstOrNull()?.first?.requestFocus()
        UIUtils.hideShowPassword(this, passwordEditText)
        UIUtils.hideShowPassword(this, confirmPasswordEditText)
    }

    private fun setupListeners() {
        btnSignUp.setOnClickListener { registerUser() }
        inputFields.forEach { (editText, layout) ->
            editText.addTextChangedListener(UIUtils.inputWatcher(editText, layout))
        }
    }

    // ====================  SCROLL / CLAVIER  ====================

    /** Ajoute du padding bas = hauteur clavier ou barres système pour garder le lien visible */
    private fun applyImeAndSystemBarsPadding(target: View) {
        val startPad = target.paddingLeft
        val topPad = target.paddingTop
        val endPad = target.paddingRight
        val baseBottomPad = target.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(target) { v, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val extraBottom = maxOf(sysBars.bottom, ime.bottom)
            v.updatePadding(
                left = startPad,
                top = topPad,
                right = endPad,
                bottom = baseBottomPad + extraBottom
            )
            WindowInsetsCompat.CONSUMED
        }
    }

    /** Attache un listener de focus sur chaque champ pour assurer sa visibilité sans “sauter en bas”. */
    private fun attachFocusAutoScroll() {
        val marginPx = dp(12f)
        inputFields.map { it.first }.forEach { edit ->
            edit.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) v.post { ensureVisible(scroll, v, marginPx) }
            }
        }
    }

    /** Scroll seulement si le View n’est pas entièrement visible dans le viewport. */
    private fun ensureVisible(nsv: NestedScrollView, child: View, margin: Int) {
        val rect = Rect()
        child.getDrawingRect(rect)
        nsv.offsetDescendantRectToMyCoords(child, rect)

        val viewportTop = nsv.scrollY + nsv.paddingTop
        val viewportBottom = nsv.scrollY + nsv.height - nsv.paddingBottom

        when {
            rect.top - margin < viewportTop -> {
                // trop haut
                nsv.smoothScrollTo(0, rect.top - nsv.paddingTop - margin)
            }
            rect.bottom + margin > viewportBottom -> {
                // trop bas
                val targetY = rect.bottom - (nsv.height - nsv.paddingBottom) + margin
                nsv.smoothScrollTo(0, targetY)
            }
            else -> {
                // déjà visible, ne rien faire
            }
        }
    }

    private fun dp(value: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics).toInt()

    // ====================  API  ====================

    private fun registerUser() {
        val request = SignUpRequest(
            usernameEditText.text.toString().trim(),
            emailEditText.text.toString().trim(),
            passwordEditText.text.toString(),
            confirmPasswordEditText.text.toString(),
            numberEditText.text.toString().trim(),
            addressEditText.text.toString().trim(),
            countryCode.selectedCountryCodeWithPlus,
            cityEditText.text.toString().trim(),
            postalCodeEditText.text.toString().trim(),
            applicationName
        )
        val email = emailEditText.text?.toString()?.trim().orEmpty()

        RetrofitClient.instance.registerUser(request).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    val message = response.body()?.message ?: getString(R.string.check_your_email)
                    Toast.makeText(this@SignUpActivity, message, Toast.LENGTH_LONG).show()

                    getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE).edit {
                        putString("previous_page", "SignUpActivity")
                    }
                    getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
                        .edit().putString("email", email).apply()

                    startActivity(Intent(this@SignUpActivity, VerifyEmailActivity::class.java))
                    finish()
                } else {
                    // reset erreurs UI
                    usernameLayout.error = ""
                    emailLayout.error = ""
                    numberLayout.error = ""
                    addressLayout.error = ""
                    cityLayout.error = ""
                    postalCodeLayout.error = ""
                    passwordLayout.error = ""
                    confirmPasswordLayout.error = ""

                    val errorMessage = try {
                        val errorBody = response.errorBody()?.string()
                        val json = JSONObject(errorBody ?: "")
                        json.optString("message", "Unknown error")
                    } catch (_: Exception) {
                        "Server error"
                    }
                    showDialog(
                        "Registration failure",
                        errorMessage,
                        positiveButtonText = null,
                        onPositiveClick = null,
                        negativeButtonText = "OK",
                        onNegativeClick = { },
                        cancelable = true
                    )
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                showDialog(
                    "Network error",
                    t.localizedMessage ?: "Unknown error",
                    positiveButtonText = null,
                    onPositiveClick = null,
                    negativeButtonText = "OK",
                    onNegativeClick = { },
                    cancelable = true
                )
            }
        })
    }
}
