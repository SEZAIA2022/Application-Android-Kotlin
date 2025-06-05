package com.houssein.sezaia.ui.screen

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.houssein.sezaia.R
import com.houssein.sezaia.model.request.ChangeEmailRequest
import com.houssein.sezaia.model.request.ChangeUsernameRequest
import com.houssein.sezaia.model.response.ChangeEmailResponse
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
        val changeEmailRequest = ChangeEmailRequest(email, newEmail, password)

        RetrofitClient.instance.changeEmail(changeEmailRequest).enqueue(object :
            Callback<ChangeEmailResponse> {
            override fun onResponse(
                call: Call<ChangeEmailResponse>,
                response: Response<ChangeEmailResponse>
            ) {
                if (response.isSuccessful&& response.body() != null) {
                    val token = response.body()?.token
                    val message = response.body()?.message
                    Toast.makeText(this@ChangeEmailActivity, message, Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@ChangeEmailActivity, VerifyOtpActivity::class.java)
                    intent.putExtra("token", token)
                    intent.putExtra("previousPage", "ChangeEmailActivity")
                    intent.putExtra("email", changeEmailRequest. new_email)
                    startActivity(intent)
                    finish()
                }
                else {
                    resetInputStyles(R.color.red, clear = true, inputFields)
                    emailLayout.error = " "
                    newEmailLayout.error = " "
                    passwordLayout.error = " "

                    val errorMessage = try {
                        response.errorBody()?.string()?.let {
                            JSONObject(it).getString("message")
                        } ?: "Unknown error"
                    } catch (e: Exception) {
                        "Network Error : ${response.code()}"
                    }
                    showDialog("Email not changed", errorMessage,positiveButtonText = null, // pas de bouton positif
                        onPositiveClick = null,
                        negativeButtonText = "OK",
                        onNegativeClick = { /* rien */ },
                        cancelable = true)
                }
            }

            override fun onFailure(call: Call<ChangeEmailResponse>, t: Throwable) {
                showDialog("Connection failure", t.localizedMessage ?: "Unknown error", positiveButtonText = null, // pas de bouton positif
                    onPositiveClick = null,
                    negativeButtonText = "OK",
                    onNegativeClick = { /* rien */ },
                    cancelable = true)
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