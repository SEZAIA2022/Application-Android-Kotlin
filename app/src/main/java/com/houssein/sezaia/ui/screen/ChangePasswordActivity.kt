package com.houssein.sezaia.ui.screen

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.houssein.sezaia.R
import com.houssein.sezaia.model.request.ChangeNumberRequest
import com.houssein.sezaia.model.request.ChangePasswordRequest
import com.houssein.sezaia.model.response.ChangeNumberResponse
import com.houssein.sezaia.model.response.ChangePasswordResponse
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.utils.UIUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChangePasswordActivity : BaseActivity() {

    private lateinit var emailEditText: TextInputEditText
    private lateinit var oldPasswordEditText: TextInputEditText
    private lateinit var newPasswordEditText: TextInputEditText
    private lateinit var confirmNewPasswordEditText: TextInputEditText
    private lateinit var emailLayout: TextInputLayout
    private lateinit var oldPasswordLayout: TextInputLayout
    private lateinit var newPasswordLayout: TextInputLayout
    private lateinit var confirmNewPasswordLayout: TextInputLayout
    private lateinit var btnChangePassword: Button
    private lateinit var inputFields: List<Pair<TextInputEditText, TextInputLayout>>



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_change_password)
        // Appliquer les insets des barres système
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))

        initViews()
        // Configuration de la toolbar
        UIUtils.initToolbar(
            this,getString(R.string.change_password), actionIconRes = R.drawable.outline_lock_24, onBackClick = {finish()},
            onActionClick = { recreate() }
        )
        setupListeners()
    }

    private fun setupListeners() {
        btnChangePassword.setOnClickListener {
            if (UIUtils.validateInputs(inputFields)) {
                changePassword()
            }
        }
    }

    private fun initViews() {
        emailEditText = findViewById(R.id.email)
        emailLayout = findViewById(R.id.emailLayout)
        oldPasswordEditText = findViewById(R.id.oldPassword)
        oldPasswordLayout = findViewById(R.id.OldPasswordLayout)
        newPasswordEditText = findViewById(R.id.newPassword)
        newPasswordLayout = findViewById(R.id.newPasswordLayout)
        confirmNewPasswordEditText = findViewById(R.id.confirmNewPassword)
        confirmNewPasswordLayout = findViewById(R.id.confirmNewPasswordLayout)
        btnChangePassword = findViewById(R.id.btnChangePassword)

        inputFields = listOf(
            emailEditText to emailLayout,
            oldPasswordEditText to oldPasswordLayout,
            newPasswordEditText to newPasswordLayout,
            confirmNewPasswordEditText to confirmNewPasswordLayout
        )

        UIUtils.hideShowPassword(this, oldPasswordEditText)
        UIUtils.hideShowPassword(this, newPasswordEditText)
        UIUtils.hideShowPassword(this, confirmNewPasswordEditText)
    }

    private fun changePassword() {
        val email = emailEditText.text.toString()
        val oldPassword = oldPasswordEditText.text.toString()
        val newPassword = newPasswordEditText.text.toString()
        val confirmNewPassword = confirmNewPasswordEditText.text.toString()


        val request = ChangePasswordRequest(email, oldPassword, newPassword, confirmNewPassword)

        RetrofitClient.instance.changePassword(request).enqueue(object :
            Callback<ChangePasswordResponse> {
            override fun onResponse(
                call: Call<ChangePasswordResponse>,
                response: Response<ChangePasswordResponse>
            ) {
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        Toast.makeText(this@ChangePasswordActivity, body.message, Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@ChangePasswordActivity, ProfileActivity::class.java))
                        finish()
                    } ?: run {
                        showDialog(
                            "Error", "Empty response from server",
                            positiveButtonText = null,
                            negativeButtonText = "OK"
                        )
                    }
                } else {
                    resetInputStyles(R.color.red, clear = true, inputFields)
                    emailLayout.error = " "
                    oldPasswordLayout.error = " "
                    newPasswordLayout.error = " "
                    confirmNewPasswordLayout.error = " "

                    val errorMessage = UIUtils.parseErrorMessage(response)
                    showDialog(
                        title = "Password not changed",
                        message = errorMessage,
                        positiveButtonText = null,
                        negativeButtonText = "OK",
                        cancelable = true
                    )
                }
            }

            override fun onFailure(call: Call<ChangePasswordResponse>, t: Throwable) {
                showDialog(
                    title = "Connection failure",
                    message = t.localizedMessage ?: "Unknown error",
                    positiveButtonText = null,
                    negativeButtonText = "OK",
                    cancelable = true
                )
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