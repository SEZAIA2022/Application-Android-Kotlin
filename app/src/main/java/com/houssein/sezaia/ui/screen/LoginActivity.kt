package com.houssein.sezaia.ui.screen

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.houssein.sezaia.R
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.utils.UIUtils
import com.houssein.sezaia.model.request.LoginRequest
import com.houssein.sezaia.model.response.LoginResponse
import com.houssein.sezaia.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import org.json.JSONObject
import androidx.core.content.edit

class LoginActivity : BaseActivity() {

    private lateinit var usernameEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var usernameLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var btnLogin: Button
    private lateinit var btnSignUp: Button
    private lateinit var inputFields: List<Pair<TextInputEditText, TextInputLayout>>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        enableEdgeToEdge()
        // Appliquer les insets des barres système
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))

        initViews()
        UIUtils.initToolbar(
            this,getString(R.string.login),actionIconRes = R.drawable.baseline_density_medium_24, onBackClick = {finish()},
            onActionClick = { startActivity(Intent(this, SettingsActivity::class.java)) }
        )
        setupListeners()
    }

    private fun initViews() {
        usernameEditText = findViewById(R.id.username)
        passwordEditText = findViewById(R.id.password)
        usernameLayout = findViewById(R.id.usernameLayout)
        passwordLayout = findViewById(R.id.passwordLayout)
        btnLogin = findViewById(R.id.btnlogin)
        btnSignUp = findViewById(R.id.btnsign_up)

        inputFields = listOf(
            usernameEditText to usernameLayout,
            passwordEditText to passwordLayout
        )

        UIUtils.hideShowPassword(this, passwordEditText)
    }

    private fun setupListeners() {
        findViewById<TextView>(R.id.forgotPassword).apply {
            paint.isUnderlineText = true
            setOnClickListener {
                startActivity(Intent(context, ForgetActivity::class.java))
            }
        }

        btnSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        btnLogin.setOnClickListener {
            loginCheck()
        }

        inputFields.forEach { (editText, layout) ->
            editText.addTextChangedListener(UIUtils.inputWatcher(editText, layout))
        }
    }

    override fun onStart() {
        super.onStart()
        getSharedPreferences("MyPrefs", MODE_PRIVATE)
            .edit {
                putBoolean("showCardsInSettings", false)
            }
        resetInputStyles(R.color.gray, clear = true, inputFields)
    }

    override fun onResume() {
        super.onResume()
        resetInputStyles(R.color.gray, clear = false, inputFields)
    }

    private fun loginCheck() {
        val username = usernameEditText.text.toString()
        val password = passwordEditText.text.toString()
        val loginRequest = LoginRequest(username, password)

        RetrofitClient.instance.login(loginRequest).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(
                call: Call<LoginResponse>,
                response: Response<LoginResponse>
            ) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        val role = responseBody.role.lowercase()
                        val user = responseBody.user
                        val email = responseBody.email
                        val targetActivity = when (role) {
                            "user" -> CameraActivity::class.java
                            "admin" -> WelcomeAdminActivity::class.java
                            else -> {
                                showDialog("Error", "Role unknown : $role",positiveButtonText = null, // pas de bouton positif
                                    onPositiveClick = null,
                                    negativeButtonText = "OK",
                                    onNegativeClick = { /* rien */ },
                                    cancelable = true)
                                return
                            }
                        }
                        getSharedPreferences("LoginData", MODE_PRIVATE)
                            .edit()
                            .putString("loggedUsername", user)
                            .putString("LoggedEmail", email)
                            .putBoolean("isLoggedIn", true)
                            .putString("userRole", role)
                            .apply()
                        getSharedPreferences("MyPrefs", MODE_PRIVATE)
                            .edit()
                            .putBoolean("showCardsInSettings", true)
                            .apply()

                        startActivity(Intent(this@LoginActivity, targetActivity))
                    } ?: showDialog("Error", "Empty response from the server.", positiveButtonText = null, // pas de bouton positif
                        onPositiveClick = null,
                        negativeButtonText = "OK",
                        onNegativeClick = { /* rien */ },
                        cancelable = true)
                } else {
                    resetInputStyles(R.color.red, clear = true, inputFields)
                    usernameLayout.error = " "
                    passwordLayout.error = " "

                    val errorMessage = try {
                        response.errorBody()?.string()?.let {
                            JSONObject(it).getString("message")
                        } ?: "Unknown error"
                    } catch (e: Exception) {
                        "Network Error : ${response.code()}"
                    }
                    showDialog("Connection failure", errorMessage,positiveButtonText = null, // pas de bouton positif
                        onPositiveClick = null,
                        negativeButtonText = "OK",
                        onNegativeClick = { /* rien */ },
                        cancelable = true)
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                showDialog("Connection failure", t.localizedMessage ?: "Unknown error", positiveButtonText = null, // pas de bouton positif
                    onPositiveClick = null,
                    negativeButtonText = "OK",
                    onNegativeClick = { /* rien */ },
                    cancelable = true)
            }
        })
    }

}
