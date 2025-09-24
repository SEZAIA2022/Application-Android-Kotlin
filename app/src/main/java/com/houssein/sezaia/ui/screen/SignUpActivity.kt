package com.houssein.sezaia.ui.screen

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
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
import androidx.core.content.edit

class SignUpActivity : BaseActivity() {

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
        setContentView(R.layout.activity_sign_up)
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))

        initViews()

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

        RetrofitClient.instance.registerUser(request).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    val message = response.body()?.message ?: getString(R.string.check_your_email)
                    Toast.makeText(this@SignUpActivity, message, Toast.LENGTH_LONG).show()

                    // Sauvegarde légère pour l’UX
                    getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE).edit {
                        putString("previous_page", "SignUpActivity")
                    }

                    // Aller vers l’écran “Vérifie ton e-mail” (coller token + App Link)
                    startActivity(Intent(this@SignUpActivity, VerifyEmailActivity::class.java))
                    finish()

                } else {
                    // Reset erreurs UI
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
