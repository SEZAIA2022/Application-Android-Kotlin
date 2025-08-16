package com.houssein.sezaia.ui.screen

import android.content.Context
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
import com.google.firebase.messaging.FirebaseMessaging
import com.houssein.sezaia.model.data.MyApp

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
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))
        initViews()

        UIUtils.initToolbar(
            this,
            getString(R.string.login),
            actionIconRes = R.drawable.baseline_density_medium_24,
            onBackClick = { finish() },
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

        inputFields.firstOrNull()?.first?.requestFocus()
        UIUtils.hideShowPassword(this, passwordEditText)
    }

    private fun setupListeners() {
        findViewById<TextView>(R.id.forgotPassword).apply {
            paint.isUnderlineText = true
            setOnClickListener {
                showDialog(
                    title = "Forget password",
                    message = "Select the method to reset your password.",
                    positiveButtonText = "Email",
                    onPositiveClick = {
                        val sharedPref = getSharedPreferences("MethodePrefs", Context.MODE_PRIVATE)
                        sharedPref.edit {
                            putString("methode", "Email")
                        }
                        startActivity(Intent(context, ForgetActivity::class.java))
                    },
                    negativeButtonText = "Cancel",
                    onNegativeClick = { },
                    cancelable = true
                )
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
        resetInputStyles(R.color.gray, clear = true, inputFields)
    }

    override fun onResume() {
        super.onResume()
        resetInputStyles(R.color.gray, clear = false, inputFields)
    }

    private fun loginCheck() {
        val username = usernameEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val app = application as MyApp
        val applicationName = app.application_name

        if (username.isEmpty() || password.isEmpty()) {
            showDialog(
                title = "Error",
                message = "Username and password are required.",
                positiveButtonText = null,
                onPositiveClick = null,
                negativeButtonText = "OK",
                onNegativeClick = { },
                cancelable = true
            )
            usernameEditText.requestFocus()
            return
        }

        // On récupère le token FCM et on l'envoie directement dans le login
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                showDialog(
                    title = "Error",
                    message = "Unable to get device token.",
                    positiveButtonText = null,
                    onPositiveClick = null,
                    negativeButtonText = "OK",
                    onNegativeClick = { },
                    cancelable = true
                )
                return@addOnCompleteListener
            }

            val fcmToken = task.result ?: ""
            val loginRequest = LoginRequest(username, password, applicationName, fcmToken)

            RetrofitClient.instance.login(loginRequest).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.let { body ->
                            val role = body.role.lowercase()
                            val user = body.user
                            val email = body.email

                            val targetActivity = when (role) {
                                "user", "admin" -> CameraActivity::class.java
                                else -> {
                                    showDialog(
                                        title = "Error",
                                        message = "Role unknown : $role",
                                        positiveButtonText = null,
                                        onPositiveClick = null,
                                        negativeButtonText = "OK",
                                        onNegativeClick = { },
                                        cancelable = true
                                    )
                                    return
                                }
                            }

                            getSharedPreferences("LoginData", MODE_PRIVATE).edit {
                                putString("loggedUsername", user)
                                putString("LoggedEmail", email)
                                putBoolean("isLoggedIn", true)
                                putString("userRole", role)
                            }

                            startActivity(Intent(this@LoginActivity, targetActivity))
                        } ?: showDialog(
                            title = "Error",
                            message = "Empty response from the server.",
                            positiveButtonText = null,
                            onPositiveClick = null,
                            negativeButtonText = "OK",
                            onNegativeClick = { },
                            cancelable = true
                        )
                    } else {
                        resetInputStyles(R.color.red, clear = true, inputFields)
                        usernameLayout.isErrorEnabled = true
                        usernameLayout.error = "\u00A0"
                        passwordLayout.isErrorEnabled = true
                        passwordLayout.error = "\u00A0"

                        val errorMessage = try {
                            response.errorBody()?.string()?.let {
                                JSONObject(it).getString("message")
                            } ?: "Unknown error"
                        } catch (e: Exception) {
                            "Network Error : ${response.code()}"
                        }

                        showDialog(
                            title = "Connection failure",
                            message = errorMessage,
                            positiveButtonText = null,
                            onPositiveClick = null,
                            negativeButtonText = "OK",
                            onNegativeClick = { },
                            cancelable = true
                        )
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    showDialog(
                        title = "Connection failure",
                        message = t.localizedMessage ?: "Unknown error",
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
}
