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
import com.houssein.sezaia.model.request.DeleteAccountRequest
import com.houssein.sezaia.model.response.DeleteAccountResponse
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.utils.UIUtils
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DeleteAccountActivity : BaseActivity() {

    private lateinit var emailEditText: TextInputEditText
    private lateinit var confirmPasswordEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var emailLayout: TextInputLayout
    private lateinit var confirmPasswordLayout: TextInputLayout
    private lateinit var btnDeleteAccount: Button
    private lateinit var inputFields: List<Pair<TextInputEditText, TextInputLayout>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_delete_account)
        // Appliquer les insets des barres système
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))
        initViews()
        // Configuration de la toolbar
        UIUtils.initToolbar(
            this,getString(R.string.delete_account), actionIconRes = R.drawable.baseline_delete_24, onBackClick = {finish()},
            onActionClick = {recreate() }
        )
        setupListeners()
    }

    private fun initViews() {
        emailEditText = findViewById(R.id.email)
        confirmPasswordEditText = findViewById(R.id.confirmPassword)
        passwordEditText = findViewById(R.id.password)
        emailLayout = findViewById(R.id.emailLayout)
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout)
        passwordLayout = findViewById(R.id.passwordLayout)
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount)
        inputFields = listOf(
            emailEditText to emailLayout,
            confirmPasswordEditText to confirmPasswordLayout,
            passwordEditText to passwordLayout
        )
        UIUtils.hideShowPassword(this, passwordEditText)
        UIUtils.hideShowPassword(this, confirmPasswordEditText)
    }

    private fun setupListeners() {
        btnDeleteAccount.setOnClickListener {
            deleteAccount()
        }
        inputFields.forEach { (editText, layout) ->
            editText.addTextChangedListener(UIUtils.inputWatcher(editText, layout))
        }
    }

    private fun deleteAccount() {
        val email = emailEditText.text.toString()
        val password = passwordEditText.text.toString()
        val deleteAccountRequest = DeleteAccountRequest(email, password)

        RetrofitClient.instance.deleteAccount(deleteAccountRequest).enqueue(object :
            Callback<DeleteAccountResponse> {
            override fun onResponse(
                call: Call<DeleteAccountResponse>,
                response: Response<DeleteAccountResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val token = response.body()?.token
                    val message = response.body()?.message
                    Toast.makeText(this@DeleteAccountActivity, message, Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@DeleteAccountActivity, VerifyOtpActivity::class.java)
                    intent.putExtra("token", token)
                    intent.putExtra("previousPage", "DeleteAccountActivity")
                    intent.putExtra("email", deleteAccountRequest.email)
                    startActivity(intent)
                    finish()
                } else {
                    resetInputStyles(R.color.red, clear = true, inputFields)
                    emailLayout.error = " "
                    confirmPasswordLayout.error = " "
                    passwordLayout.error = " "

                    val errorMessage = try {
                        response.errorBody()?.string()?.let {
                            JSONObject(it).getString("message")
                        } ?: "Unknown error"
                    } catch (e: Exception) {
                        "Network Error : ${response.code()}"
                    }
                    showDialog(
                        "Account not Deleted",
                        errorMessage,
                        positiveButtonText = null, // pas de bouton positif
                        onPositiveClick = null,
                        negativeButtonText = "OK",
                        onNegativeClick = { /* rien */ },
                        cancelable = true
                    )
                }
            }

            override fun onFailure(call: Call<DeleteAccountResponse>, t: Throwable) {
                showDialog(
                    "Connection failure",
                    t.localizedMessage ?: "Unknown error",
                    positiveButtonText = null, // pas de bouton positif
                    onPositiveClick = null,
                    negativeButtonText = "OK",
                    onNegativeClick = { /* rien */ },
                    cancelable = true
                )
            }
        })
    }
}
