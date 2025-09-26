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
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var btnDeleteAccount: Button
    private lateinit var inputFields: List<Pair<TextInputEditText, TextInputLayout>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_delete_account)

        UIUtils.applySystemBarsInsets(findViewById(R.id.main))
        UIUtils.initToolbar(
            this,
            getString(R.string.delete_account),
            actionIconRes = R.drawable.baseline_delete_24,
            onBackClick = { finish() },
            onActionClick = { recreate() }
        )

        initViews()
        setupListeners()
    }

    private fun initViews() {
        emailEditText = findViewById(R.id.email)
        passwordEditText = findViewById(R.id.password)

        emailLayout = findViewById(R.id.emailLayout)
        passwordLayout = findViewById(R.id.passwordLayout)

        btnDeleteAccount = findViewById(R.id.btnDeleteAccount)

        inputFields = listOf(
            emailEditText to emailLayout,
            passwordEditText to passwordLayout
        )
        inputFields.firstOrNull()?.first?.requestFocus()

        UIUtils.hideShowPassword(this, passwordEditText)
    }

    private fun setupListeners() {
        btnDeleteAccount.setOnClickListener { deleteAccount() }
        inputFields.forEach { (editText, layout) ->
            editText.addTextChangedListener(UIUtils.inputWatcher(editText, layout))
        }
    }

    private fun deleteAccount() {
        val email = emailEditText.text?.toString()?.trim().orEmpty()
        val password = passwordEditText.text?.toString()?.trim().orEmpty()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, getString(R.string.fill_required_fields), Toast.LENGTH_SHORT).show()
            return
        }

        val app = application as MyApp
        val applicationName = app.application_name

        btnDeleteAccount.isEnabled = false

        val request = DeleteAccountRequest(
            email = email,
            password = password,
            application_name = applicationName
        )

        RetrofitClient.instance.deleteAccount(request).enqueue(object : Callback<BaseResponse> {
            override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                btnDeleteAccount.isEnabled = true

                if (response.isSuccessful) {
                    val body = response.body()
                    Toast.makeText(this@DeleteAccountActivity, body?.message ?: "OK", Toast.LENGTH_SHORT).show()

                    // Sauvegarde pour VerifyEmailActivity
                    getSharedPreferences("MyAppPrefs", MODE_PRIVATE).edit().apply {
                        putString("previous_page", "DeleteAccountActivity")
                        putString("email", email)         // email qui va recevoir le lien
                        putString("flow", "delete_account")
                        apply()
                    }

                    startActivity(Intent(this@DeleteAccountActivity, VerifyEmailActivity::class.java))
                    finish()
                } else {
                    val msg = try {
                        val raw = response.errorBody()?.string()
                        if (raw.isNullOrEmpty()) "Unknown error"
                        else JSONObject(raw).optString("message", raw)
                    } catch (_: Exception) {
                        "Unknown error (${response.code()})"
                    }

                    passwordLayout.error = " "
                    Toast.makeText(this@DeleteAccountActivity, msg, Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                btnDeleteAccount.isEnabled = true
                Toast.makeText(this@DeleteAccountActivity, t.localizedMessage ?: "Network error", Toast.LENGTH_LONG).show()
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
