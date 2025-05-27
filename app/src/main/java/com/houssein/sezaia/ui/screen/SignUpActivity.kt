package com.houssein.sezaia.ui.screen

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import com.google.android.material.textfield.TextInputEditText
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

    private lateinit var username: TextInputEditText
    private lateinit var email: TextInputEditText
    private lateinit var countryCode: CountryCodePicker
    private lateinit var number: TextInputEditText
    private lateinit var address: TextInputEditText
    private lateinit var city: TextInputEditText
    private lateinit var postalCode: TextInputEditText
    private lateinit var password: TextInputEditText
    private lateinit var confirmPassword: TextInputEditText
    private lateinit var signUpButton: Button
    private lateinit var loginLink: TextView

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

        UIUtils.hideShowPassword(this, password)
        UIUtils.hideShowPassword(this, confirmPassword)

        handleSignUpButtonClick()

        UIUtils.setupClickableText(
            context = this,
            textView = loginLink,
            fullText = loginLink.text.toString(),
            clickableText = getString(R.string.login),
            clickableColorRes = R.color.light_blue,
            targetActivity = LoginActivity::class.java
        )
    }

    private fun initViews() {
        username = findViewById(R.id.username)
        email = findViewById(R.id.email)
        countryCode = findViewById(R.id.countryCodePicker)
        number = findViewById(R.id.phone)
        address = findViewById(R.id.address)
        city = findViewById(R.id.city)
        postalCode = findViewById(R.id.postalCode)
        password = findViewById(R.id.password)
        confirmPassword = findViewById(R.id.confirm_password)
        signUpButton = findViewById(R.id.sign_up_button)
        loginLink = findViewById(R.id.loginLink)
    }

    private fun handleSignUpButtonClick() {
        signUpButton.setOnClickListener {
            val user = username.text.toString().trim()
            val mail = email.text.toString().trim()
            val pass = password.text.toString()
            val confirm = confirmPassword.text.toString()

            if (user.isEmpty() || mail.isEmpty() || pass.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
            } else if (pass != confirm) {
                Toast.makeText(this, "Les mots de passe ne correspondent pas", Toast.LENGTH_SHORT).show()
            } else {
                registerUser()
            }
        }
    }

    private fun registerUser() {
        val request = SignUpRequest(
            username = username.text.toString().trim(),
            email = email.text.toString().trim(),
            password = password.text.toString(),
            confirm_password = confirmPassword.text.toString(),
            number = number.text.toString().trim(),
            address = address.text.toString().trim(),
            country_code = countryCode.selectedCountryCodeWithPlus,
            city = city.text.toString().trim(),
            postal_code = postalCode.text.toString().trim()
        )

        RetrofitClient.instance.signUp(request).enqueue(object : Callback<SignUpResponse> {
            override fun onResponse(call: Call<SignUpResponse>, response: Response<SignUpResponse>) {
                if (response.isSuccessful  && response.body() != null) {
                    val token = response.body()?.token
                    val message = response.body()?.message ?: "OTP envoyé"
                    Toast.makeText(this@SignUpActivity, message, Toast.LENGTH_LONG).show()

                    val intent = Intent(this@SignUpActivity, VerifyOtpActivity::class.java)
                    intent.putExtra("token", token)
                    intent.putExtra("previousPage", "SignUpActivity")
                    intent.putExtra("email", request.email)
                    startActivity(intent)
                } else {
                    val errorMessage = try {
                        val errorBody = response.errorBody()?.string()
                        val json = JSONObject(errorBody ?: "")
                        json.optString("message", "Erreur inconnue")
                    } catch (e: Exception) {
                        "Erreur serveur"
                    }
                    showDialog("Échec de l'inscription", errorMessage,positiveButtonText = null, // pas de bouton positif
                        onPositiveClick = null,
                        negativeButtonText = "OK",
                        onNegativeClick = { /* rien */ },
                        cancelable = true)
                }
            }

            override fun onFailure(call: Call<SignUpResponse>, t: Throwable) {
                showDialog("Erreur réseau", t.localizedMessage ?: "Erreur inconnue",positiveButtonText = null, // pas de bouton positif
                    onPositiveClick = null,
                    negativeButtonText = "OK",
                    onNegativeClick = { /* rien */ },
                    cancelable = true)
            }
        })
    }
}


