package com.houssein.sezaia.ui.screen

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.houssein.sezaia.R
import com.houssein.sezaia.model.data.MyApp
import com.houssein.sezaia.model.request.ChangeEmailRequest
import com.houssein.sezaia.model.request.ChangeUsernameRequest
import com.houssein.sezaia.model.response.BaseResponse
import com.houssein.sezaia.model.response.ChangeUsernameResponse
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.utils.UIUtils
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChangeEmailActivity : BaseActivity() {

    private lateinit var emailEditText: TextInputEditText
    private lateinit var newEmailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var emailLayout: TextInputLayout
    private lateinit var newEmailLayout: TextInputLayout
    private lateinit var btnChangeEmail: Button
    private lateinit var inputFields: List<Pair<TextInputEditText, TextInputLayout>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_change_email)
        // Appliquer les insets des barres système
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))

        initViews()
        // Configuration de la toolbar
        UIUtils.initToolbar(
            this,getString(R.string.change_email),actionIconRes = R.drawable.baseline_mail_24, onBackClick = {finish()},
            onActionClick = { recreate() }
        )
        setupListeners()


    }
    private fun initViews() {
        emailEditText = findViewById(R.id.email)
        newEmailEditText = findViewById(R.id.newEmail)
        passwordEditText = findViewById(R.id.password)
        emailLayout = findViewById(R.id.emailLayout)
        newEmailLayout = findViewById(R.id.newEmailLayout)
        passwordLayout = findViewById(R.id.passwordLayout)
        btnChangeEmail = findViewById(R.id.btnChangeEmail)
        inputFields = listOf(
            emailEditText to emailLayout,
            newEmailEditText to newEmailLayout,
            passwordEditText to passwordLayout
        )
        inputFields.firstOrNull()?.first?.requestFocus()

        UIUtils.hideShowPassword(this, passwordEditText)

    }

    private fun setupListeners() {
        btnChangeEmail.setOnClickListener {
            changeEmail()
        }
        inputFields.forEach { (editText, layout) ->
            editText.addTextChangedListener(UIUtils.inputWatcher(editText, layout))
        }
    }

    private fun changeEmail() {
        val email = emailEditText.text.toString()
        val newEmail = newEmailEditText.text.toString()
        val password = passwordEditText.text.toString()
        val app = application as MyApp
        val applicationName = app.application_name

        btnChangeEmail.isEnabled = false

        val request = ChangeEmailRequest(
            email = email,
            new_email = newEmail,
            password = password,
            applicationName
        )

        RetrofitClient.instance.changeEmail(request).enqueue(object : Callback<BaseResponse> {
            override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                btnChangeEmail.isEnabled = true
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.status == "success") {
                        Toast.makeText(this@ChangeEmailActivity, body.message, Toast.LENGTH_SHORT).show()

                        // Stocker uniquement l'email actuel (clé de stockage OTP) dans SharedPreferences
                        val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                        prefs.edit().apply {
                            putString("previous_page", "ChangeEmailActivity")
                            putString("email", email)
                            apply()
                        }

                        // Rediriger vers écran de vérification OTP (pas besoin de new_email ici)
//                        val intent = Intent(this@ChangeEmailActivity, VerifyOtpActivity::class.java)
//                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@ChangeEmailActivity, body?.message ?: "Unknown Error", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Unknown Error"
                    Toast.makeText(this@ChangeEmailActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                btnChangeEmail.isEnabled = true
                Toast.makeText(this@ChangeEmailActivity, "Network error : ${t.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        })
    }



    override fun onResume() {
        super.onResume()
        resetInputStyles(R.color.gray, clear = false, inputFields)
    }

    override fun onStart() {
        super.onStart()
        resetInputStyles(R.color.gray, clear = true, inputFields)
    }


}