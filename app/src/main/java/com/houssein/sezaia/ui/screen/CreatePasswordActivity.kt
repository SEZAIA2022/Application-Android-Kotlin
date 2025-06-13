package com.houssein.sezaia.ui.screen

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import com.google.android.material.textfield.TextInputEditText
import com.houssein.sezaia.R
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
        btnResetPass = findViewById(R.id.btnResetPassword)
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
            val pass = passwordInput.text.toString()
            val confirm = confirmPasswordInput.text.toString()

            when {
                pass.isEmpty() || confirm.isEmpty() -> {
                    Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show()
                }
                pass != confirm -> {
                    Toast.makeText(this, getString(R.string.passwords_do_not_match), Toast.LENGTH_SHORT).show()
                }
                else -> {
                    resetPassword()
                }
            }
        }
    }

    private fun resetPassword() {
        val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val email =  sharedPref.getString("email", null)
        if (email.isNullOrEmpty()) {
            showDialog(getString(R.string.error), getString(R.string.email_missing), positiveButtonText = null, // pas de bouton positif
                onPositiveClick = null,
                negativeButtonText = "OK",
                onNegativeClick = { /* rien */ },
                cancelable = true)
            return
        }

        val request = CreateNewPasswordRequest(
            email = email,
            new_password = passwordInput.text.toString(),
            confirm_password = confirmPasswordInput.text.toString()
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

                    startActivity(intent)
                } else {
                    val errorMessage = try {
                        val errorBody = response.errorBody()?.string()
                        val json = JSONObject(errorBody ?: "")
                        json.optString("message", getString(R.string.unknown_error))
                    } catch (e: Exception) {
                        getString(R.string.server_error)
                    }
                    showDialog(getString(R.string.registration_failed), errorMessage, positiveButtonText = null, // pas de bouton positif
                        onPositiveClick = null,
                        negativeButtonText = "OK",
                        onNegativeClick = { /* rien */ },
                        cancelable = true)
                }
            }

            override fun onFailure(call: Call<CreateNewPasswordResponse>, t: Throwable) {
                showDialog(getString(R.string.network_error), t.localizedMessage ?: getString(R.string.unknown_error), positiveButtonText = null, // pas de bouton positif
                    onPositiveClick = null,
                    negativeButtonText = "OK",
                    onNegativeClick = { /* rien */ },
                    cancelable = true)
            }
        })
    }
}
