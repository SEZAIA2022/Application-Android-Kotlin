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
import com.houssein.sezaia.model.request.ChangePasswordRequest
import com.houssein.sezaia.model.request.CreateNewPasswordRequest
import com.houssein.sezaia.model.response.BaseResponse
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
        val token = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            .getString("reset_token", null)

        if (token.isNullOrEmpty()) {
            val msg = getString(R.string.token_missing)
            passwordLayout.error = msg
            confirmPasswordLayout.error = msg
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            return
        }

        val newPass = passwordInput.text?.toString()?.trim().orEmpty()
        val confirm = confirmPasswordInput.text?.toString()?.trim().orEmpty()

        val req = CreateNewPasswordRequest(
            token = token,
            new_password = newPass,
            confirm_password = confirm
        )

        RetrofitClient.instance.createNewPassword(req).enqueue(object : Callback<BaseResponse> {
            override fun onResponse(call: Call<BaseResponse>, resp: Response<BaseResponse>) {
                if (resp.isSuccessful) {
                    val message = resp.body()?.message ?: getString(R.string.password_changed)
                    Toast.makeText(this@CreatePasswordActivity, message, Toast.LENGTH_LONG).show()

                    // nettoyage du token
                    getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                        .edit().remove("reset_token").apply()

                    // Aller sur un écran de succès
                    val prefs = getSharedPreferences("MySuccessPrefs", MODE_PRIVATE)
                    prefs.edit().apply {
                        putString("title", getString(R.string.password_changed))
                        putString("content", getString(R.string.password_changed_successfully))
                        putString("button", getString(R.string.return_to_login))
                        apply()
                    }
                    startActivity(Intent(this@CreatePasswordActivity, SuccessActivity::class.java))
                    finish()

                } else {
                    resetInputStyles(R.color.red, clear = true, inputFields)
                    val errorBody = resp.errorBody()
                    val msg = if (errorBody != null) {
                        try {
                            val j = JSONObject(errorBody.charStream().readText())
                            j.optString("error", j.optString("message", "Unknown error"))
                        } catch (e: Exception) {
                            errorBody.string()
                        }
                    } else "Unknown error"
                    passwordLayout.error = msg
                    confirmPasswordLayout.error = msg
                    Toast.makeText(this@CreatePasswordActivity, msg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                resetInputStyles(R.color.red, clear = true, inputFields)
                val msg = t.localizedMessage ?: getString(R.string.unknown_error)
                passwordLayout.error = msg
                confirmPasswordLayout.error = msg
                Toast.makeText(this@CreatePasswordActivity, msg, Toast.LENGTH_SHORT).show()
            }
        })
    }

}
