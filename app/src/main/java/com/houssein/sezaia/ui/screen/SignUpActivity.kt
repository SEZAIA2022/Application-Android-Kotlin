package com.houssein.sezaia.ui.screen

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.hbb20.CountryCodePicker
import com.houssein.sezaia.R
import com.houssein.sezaia.model.request.SignUpRequest
import com.houssein.sezaia.model.response.SignUpResponse
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.utils.UIUtils
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignUpActivity : BaseActivity() {

    private lateinit var usernameEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var countryCodeEditText: CountryCodePicker
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

    private lateinit var btnSignUp: Button
    private lateinit var loginLink: TextView
    private lateinit var inputFields: List<Pair<TextInputEditText, TextInputLayout>>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
        countryCodeEditText = findViewById(R.id.countryCodePicker)
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

        UIUtils.hideShowPassword(this, passwordEditText)
        UIUtils.hideShowPassword(this, confirmPasswordEditText)
    }

    private fun setupListeners() {
        btnSignUp.setOnClickListener {
            registerUser()
        }
        inputFields.forEach { (editText, layout) ->
            editText.addTextChangedListener(UIUtils.inputWatcher(editText, layout))
        }
    }
    private fun registerUser() {
        val request = SignUpRequest(
            username = usernameEditText.text.toString().trim(),
            email = emailEditText.text.toString().trim(),
            password = passwordEditText.text.toString(),
            confirm_password = confirmPasswordEditText.text.toString(),
            number = numberEditText.text.toString().trim(),
            address = addressEditText.text.toString().trim(),
            country_code = countryCodeEditText.selectedCountryCodeWithPlus,
            city = cityEditText.text.toString().trim(),
            postal_code = postalCodeEditText.text.toString().trim()
        )

        RetrofitClient.instance.signUp(request).enqueue(object : Callback<SignUpResponse> {
            override fun onResponse(call: Call<SignUpResponse>, response: Response<SignUpResponse>) {
                if (response.isSuccessful  && response.body() != null) {
                    val token = response.body()?.token
                    val message = response.body()?.message
                    Toast.makeText(this@SignUpActivity, message, Toast.LENGTH_LONG).show()

                    val intent = Intent(this@SignUpActivity, VerifyOtpActivity::class.java)
                    intent.putExtra("token", token)
                    intent.putExtra("previousPage", "SignUpActivity")
                    intent.putExtra("email", request.email)
                    startActivity(intent)
                } else {
                    resetInputStyles(R.color.red, clear = true, inputFields)
                    usernameLayout.error= ""
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
                    } catch (e: Exception) {
                        "Server error"
                    }
                    showDialog("Registration failure", errorMessage,positiveButtonText = null, // pas de bouton positif
                        onPositiveClick = null,
                        negativeButtonText = "OK",
                        onNegativeClick = { /* rien */ },
                        cancelable = true)
                }
            }

            override fun onFailure(call: Call<SignUpResponse>, t: Throwable) {
                showDialog("Network error", t.localizedMessage ?: "Unknown error",positiveButtonText = null, // pas de bouton positif
                    onPositiveClick = null,
                    negativeButtonText = "OK",
                    onNegativeClick = { /* rien */ },
                    cancelable = true)
            }
        })
    }
}


