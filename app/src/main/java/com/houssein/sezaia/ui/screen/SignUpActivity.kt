package com.houssein.sezaia.ui.screen

import android.content.Intent
import android.os.Bundle
import android.view.WindowInsetsAnimation
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
    private lateinit var ccp: CountryCodePicker
    private lateinit var phoneNumber: TextInputEditText
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

        // Appliquer les insets des barres système
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))

        // Initialisation des vues
        initViews()

        // Configuration de la Toolbar
        UIUtils.initToolbar(
            this,getString(R.string.sign_up),actionIconRes = R.drawable.baseline_density_medium_24 , onBackClick = {finish()},
            onActionClick = { startActivity(Intent(this, SettingsActivity::class.java)) }
        )
        // Gérer l'affichage des mots de passe
        UIUtils.hideShowPassword(this, password)
        UIUtils.hideShowPassword(this, confirmPassword)

        // Gestion du bouton d'inscription
        handleSignUpButtonClick()

        // Rendre le texte cliquable pour la connexion
        val textView: TextView = findViewById(R.id.loginLink)
        val fullText = textView.text.toString()
        UIUtils.setupClickableText(
            context = this,
            textView = textView,
            fullText = fullText,
            clickableText = getString(R.string.login),  // Assure-toi que cette chaîne existe
            clickableColorRes = R.color.light_blue,  // Assure-toi que cette couleur est définie
            targetActivity = LoginActivity::class.java
        )
    }

    // Initialiser les vues
    private fun initViews() {
        username = findViewById(R.id.username)
        email = findViewById(R.id.email)
        ccp = findViewById(R.id.countryCodePicker)
        phoneNumber = findViewById(R.id.phone)
        address = findViewById(R.id.address)
        city = findViewById(R.id.city)
        postalCode = findViewById(R.id.postalCode)
        password = findViewById(R.id.password)
        confirmPassword = findViewById(R.id.confirm_password)
        signUpButton = findViewById(R.id.sign_up_button)
        loginLink = findViewById(R.id.loginLink)
    }

    // Gérer l'événement du bouton d'inscription
    private fun handleSignUpButtonClick() {
        signUpButton.setOnClickListener {
            val user = username.text.toString()
            val mail = email.text.toString()
            val pass = password.text.toString()
            val confirm = confirmPassword.text.toString()

            // Validation des champs
            if (user.isEmpty() || mail.isEmpty() || pass.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            } else if (pass != confirm) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Creating account...", Toast.LENGTH_SHORT).show()
                // Logique d'inscription à implémenter ici (ex. Firebase)
            }
        }
    }

    private fun registerUser() {
        val request = SignUpRequest(
            username = username.text.toString(),
            email = email.text.toString(),
            password = password.text.toString(),
            confirmPassword = confirmPassword.text.toString(),
            number = phoneNumber.text.toString(),
            address = address.text.toString(),
            countryCode = ccp.selectedCountryCodeWithPlus,
            city = city.text.toString(),
            postalCode = postalCode.text.toString()
        )

        RetrofitClient.instance.signUp(request).enqueue(object : Callback<SignUpResponse> {
            override fun onResponse(call: Call<SignUpResponse>, response: Response<SignUpResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val token = response.body()?.token
                    val message = response.body()?.message ?: "Inscription réussie"

                    Toast.makeText(this@SignUpActivity, message, Toast.LENGTH_LONG).show()

                    // Rediriger vers l'activité de vérification OTP
                    val intent = Intent(this@SignUpActivity, VerifyOtpActivity::class.java)
                    intent.putExtra("token", token)
                    intent.putExtra("previousPage", "SignUpActivity")
                    intent.putExtra("email", request.email)
                    startActivity(intent)
                } else {
                    val errorMessage = try {
                        val errorBody = response.errorBody()?.string()
                        val json = JSONObject(errorBody ?: "")
                        json.getString("message")
                    } catch (e: Exception) {
                        "Erreur serveur"
                    }
                    showDialog("Échec de l'inscription", errorMessage)
                }
            }

            override fun onFailure(call: Call<SignUpResponse>, t: Throwable) {
                showDialog("Erreur réseau", t.localizedMessage ?: "Erreur inconnue")
            }
        })
    }


}