package com.houssein.sezaia.ui.screen

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.houssein.sezaia.R
import com.houssein.sezaia.model.data.MyApp
import com.houssein.sezaia.model.request.CreateNewPasswordRequest
import com.houssein.sezaia.model.response.CreateNewPasswordResponse
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.utils.UIUtils
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CreatePasswordActivity : BaseActivity() {

    private lateinit var passwordInput: TextInputEditText
    private lateinit var confirmPasswordInput: TextInputEditText
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var confirmPasswordLayout: TextInputLayout
    private lateinit var inputFields: List<Pair<TextInputEditText, TextInputLayout>>
    private lateinit var btnResetPass: Button
    private lateinit var applicationName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_password)
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))
        setupToolbar()
        initViews()
        handleResetButtonClick()
        setupClickableLoginText()
    }

    private fun initViews() {
        passwordInput = findViewById(R.id.password)
        confirmPasswordInput = findViewById(R.id.confirm_password)
        passwordLayout = findViewById(R.id.passwordInputLayout)
        confirmPasswordLayout = findViewById(R.id.confirmPasswordInputLayout)
        btnResetPass = findViewById(R.id.btnResetPassword)
        inputFields = listOf(
            passwordInput to passwordLayout,
            confirmPasswordInput to confirmPasswordLayout
        )
        inputFields.firstOrNull()?.first?.requestFocus()
    }

    private fun setupToolbar() {
        UIUtils.initToolbar(
            this,
            getString(R.string.create_new_password),
            actionIconRes = R.drawable.baseline_density_medium_24,
            onBackClick = { finish() },
            onActionClick = {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
        )
    }

    private fun setupClickableLoginText() {
        val textView: TextView = findViewById(R.id.remember)
        val fullText = textView.text.toString()

        UIUtils.makeTextClickable(
            context = this,
            textView = textView,
            fullText = fullText,
            clickableText = getString(R.string.login),
            clickableColorRes = R.color.light_blue
        ) {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun handleResetButtonClick() {
        btnResetPass.setOnClickListener {

            resetPassword()
        }
        inputFields.forEach { (editText, layout) ->
            editText.addTextChangedListener(UIUtils.inputWatcher(editText, layout))
        }
    }

    private fun resetPassword() {
        val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val email = sharedPref.getString("email", null)

        if (email.isNullOrEmpty()) {
            passwordLayout.error = getString(R.string.email_missing)
            confirmPasswordLayout.error = getString(R.string.email_missing)
            Toast.makeText(this, getString(R.string.email_missing), Toast.LENGTH_SHORT).show()
            return
        }

        val app = application as MyApp
        applicationName = app.application_name

        val request = CreateNewPasswordRequest(
            email = email,
            new_password = passwordInput.text.toString(),
            confirm_password = confirmPasswordInput.text.toString(),
            applicationName
        )

        RetrofitClient.instance.createNewPassword(request).enqueue(object : Callback<CreateNewPasswordResponse> {
            override fun onResponse(
                call: Call<CreateNewPasswordResponse>,
                response: Response<CreateNewPasswordResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val message = response.body()?.message ?: getString(R.string.password_changed)
                    Toast.makeText(this@CreatePasswordActivity, message, Toast.LENGTH_LONG).show()

                    val prefs = getSharedPreferences("MySuccessPrefs", MODE_PRIVATE)
                    prefs.edit().apply {
                        putString("title", getString(R.string.password_changed))
                        putString("content", getString(R.string.password_changed_successfully))
                        putString("button", getString(R.string.return_to_login))
                        apply()
                    }

                    val intent = Intent(this@CreatePasswordActivity, SuccessActivity::class.java)
                    startActivity(intent)

                } else {
                    resetInputStyles(R.color.red, clear = true, inputFields)

                    val errorBody = response.errorBody()
                    val errorMsg = if (errorBody != null) {
                        try {
                            val errorJson = JSONObject(errorBody.charStream().readText())
                            errorJson.optString("message", "Unknown error")
                        } catch (e: Exception) {
                            errorBody.string()
                        }
                    } else {
                        "Unknown error"
                    }

                    passwordLayout.error = errorMsg
                    confirmPasswordLayout.error = errorMsg

                    Toast.makeText(this@CreatePasswordActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<CreateNewPasswordResponse>, t: Throwable) {
                resetInputStyles(R.color.red, clear = true, inputFields)

                val errorMsg = t.localizedMessage ?: getString(R.string.unknown_error)
                passwordLayout.error = errorMsg
                confirmPasswordLayout.error = errorMsg

                Toast.makeText(this@CreatePasswordActivity, errorMsg, Toast.LENGTH_SHORT).show()
            }
        })
    }
}
