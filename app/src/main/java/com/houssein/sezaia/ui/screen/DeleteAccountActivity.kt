package com.houssein.sezaia.ui.screen

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.houssein.sezaia.R
import com.houssein.sezaia.model.data.MyApp
import com.houssein.sezaia.model.request.ChangeEmailRequest
import com.houssein.sezaia.model.request.DeleteAccountRequest
import com.houssein.sezaia.model.response.BaseResponse
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
        inputFields.firstOrNull()?.first?.requestFocus()
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
        val app = application as MyApp
        val applicationName = app.application_name


        val deleteAccountRequest = DeleteAccountRequest(email, password, applicationName)
        Log.d("DeleteAccount", "Tentative de suppression avec email: $email") // Log de débogage
        RetrofitClient.instance.deleteAccount(deleteAccountRequest).enqueue(object : Callback<BaseResponse> {
            override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                Log.d("DeleteAccount", "Réponse reçue: ${response.code()}")

                if (response.isSuccessful) {
                    val body = response.body()
                    Log.d("DeleteAccount", "Body reçu: ${body?.toString()}")

                    if (body != null && body.status == "success") {
                        // Sauvegarder les informations avant la redirection
                        getSharedPreferences("MyAppPrefs", MODE_PRIVATE).edit().apply {
                            putString("email", email)
                            putString("previous_page", "DeleteAccountActivity") // Correction: devrait être DeleteAccountActivity
                            apply()
                        }
                        Log.d("DeleteAccount", "Redirection vers VerifyOtpActivity") // Log de débog

                        // Redirection vers VerifyOtpActivity
                        val intent = Intent(this@DeleteAccountActivity, VerifyOtpActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(intent)
                        finish() // Ferme l'activité actuelle

                    } else {
                        Log.e("DeleteAccount", "Erreur dans la réponse: ${body?.message}")
                        Toast.makeText(
                            this@DeleteAccountActivity,
                            body?.message ?: "Erreur inconnue",
                            Toast.LENGTH_LONG // Changé à LONG pour mieux voir le message
                        ).show()
                    }
                } else {
                    Log.e("DeleteAccount", "Réponse non successful: ${response.errorBody()?.string()}") // Log de débogage
                    handleErrorResponse(response)
                }
            }

            override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                handleNetworkFailure(t)
            }
        })
    }

    private fun handleErrorResponse(response: Response<BaseResponse>) {
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
            positiveButtonText = null,
            onPositiveClick = null,
            negativeButtonText = "OK",
            onNegativeClick = { /* rien */ },
            cancelable = true
        )
    }

    private fun handleNetworkFailure(t: Throwable) {
        showDialog(
            "Connection failure",
            t.localizedMessage ?: "Unknown error",
            positiveButtonText = null,
            onPositiveClick = null,
            negativeButtonText = "OK",
            onNegativeClick = { /* rien */ },
            cancelable = true
        )
    }
}
