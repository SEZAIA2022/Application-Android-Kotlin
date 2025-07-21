package com.houssein.sezaia.ui.screen

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.edit
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.houssein.sezaia.R
import com.houssein.sezaia.model.data.MyApp
import com.houssein.sezaia.model.request.*
import com.houssein.sezaia.model.response.ApiResponse
import com.houssein.sezaia.model.response.BaseResponse
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.utils.SimpleTextWatcher
import com.houssein.sezaia.ui.utils.UIUtils
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class VerifyOtpActivity : BaseActivity() {

    private lateinit var otpFields: List<Pair<TextInputEditText, TextInputLayout>>
    private lateinit var verifyButton: Button
    private lateinit var resendOtpButton: TextView
    private lateinit var applicationName: String
    private var email: String? = null
    private var previousPage: String? = null
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_otp)
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))

        sharedPrefs = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        email = sharedPrefs.getString("email", null)
        previousPage = sharedPrefs.getString("previous_page", null)
        applicationName = (application as MyApp).application_name

        if (email == null || previousPage == null) {
            Toast.makeText(this, "Erreur de récupération des données utilisateur.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupToolbar()
        setupListeners()
        setupClickableResend()
    }

    private fun setupToolbar() {
        UIUtils.initToolbar(
            this,
            getString(R.string.otp_verification),
            actionIconRes = R.drawable.baseline_verified_user_24,
            onBackClick = { finish() },
            onActionClick = { recreate() }
        )
    }

    private fun initViews() {
        val otpInputs = listOf(
            R.id.otpInput1 to R.id.otpLayout1,
            R.id.otpInput2 to R.id.otpLayout2,
            R.id.otpInput3 to R.id.otpLayout3,
            R.id.otpInput4 to R.id.otpLayout4
        )

        otpFields = otpInputs.map { (inputId, layoutId) ->
            findViewById<TextInputEditText>(inputId) to findViewById<TextInputLayout>(layoutId)
        }

        verifyButton = findViewById(R.id.btnContinue)
        resendOtpButton = findViewById(R.id.resend)
        otpFields.firstOrNull()?.first?.requestFocus()
        otpFields.forEachIndexed { index, (editText, _) ->
            editText.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    resetInputStyles(R.color.blue, false, otpFields)
                }
            }

            editText.addTextChangedListener(object : SimpleTextWatcher() {
                override fun afterTextChanged(s: Editable?) {
                    handleOtpFocusNavigation(
                        currentField = editText,
                        nextField = otpFields.getOrNull(index + 1)?.first,
                        prevField = otpFields.getOrNull(index - 1)?.first
                    )
                }
            })
        }
    }

    private fun setupListeners() {
        verifyButton.setOnClickListener {
            when (previousPage) {
                "ForgetActivity" -> verifyOtp(VerifyForgetRequest(email!!, getOtp(), applicationName)) {
                    startActivity(Intent(this, CreatePasswordActivity::class.java))
                    finish()
                }
                "ChangeEmailActivity" -> verifyOtp(VerifyChangeEmailRequest(email!!, getOtp(), applicationName)) {
                    navigateToSuccess(getString(R.string.return_to_login), "Email modified", "Your email has been successfully modified.")
                }
                "SignUpActivity" -> verifyRegisterOtp()
                "DeleteAccountActivity" -> verifyOtp(VerifyDeleteAccountRequest(getOtp(), email!!, applicationName)) {
                    getSharedPreferences("LoginData", MODE_PRIVATE).edit {
                        putBoolean("isLoggedIn", false)
                        remove("userRole")
                    }
                    navigateToSuccess(getString(R.string.return_to_login), "Account deleted", "Your account has been successfully deleted.")
                }
            }
        }

        otpFields.forEach { (editText, layout) ->
            editText.addTextChangedListener(UIUtils.inputWatcher(editText, layout))
        }
    }

    private fun verifyRegisterOtp() {
        val request = VerifyRegisterRequest(email!!, getOtp())
        RetrofitClient.instance.verifyRegister(request).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    navigateToSuccess(getString(R.string.return_to_login), "Account created", "Your account has been successfully created.")
                } else UIUtils.showErrorResponse(this@VerifyOtpActivity, response)
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Toast.makeText(this@VerifyOtpActivity, "Erreur réseau : ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun <T> verifyOtp(request: T, call: (T) -> Call<BaseResponse> = { r ->
        when (r) {
            is VerifyForgetRequest -> RetrofitClient.instance.verifyForget(r)
            is VerifyChangeEmailRequest -> RetrofitClient.instance.verifyChangeEmail(r)
            is VerifyDeleteAccountRequest -> RetrofitClient.instance.verifyDeleteAccount(r)
            else -> throw IllegalArgumentException("Unsupported request type")
        } as Call<BaseResponse>
    }, onSuccess: () -> Unit) {
        verifyButton.isEnabled = false
        call(request).enqueue(object : Callback<BaseResponse> {
            override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                verifyButton.isEnabled = true
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    resetInputStyles(R.color.red, true, otpFields)
                    otpFields.firstOrNull()?.first?.requestFocus()
                    UIUtils.showErrorResponse(this@VerifyOtpActivity, response)
                }
            }

            override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                verifyButton.isEnabled = true
                Toast.makeText(this@VerifyOtpActivity, "Network error : ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupClickableResend() {
        UIUtils.makeTextClickable(
            context = this,
            textView = resendOtpButton,
            fullText = resendOtpButton.text.toString(),
            clickableText = "Resend",
            clickableColorRes = R.color.light_blue
        ) {
            resendOtp()
        }
    }

    private fun resendOtp() {
        delayResendButton()
        val request = ResendOtpRequest(email.toString(), previousPage.toString(), applicationName)

        RetrofitClient.instance.resendOtp(request).enqueue(object : Callback<BaseResponse> {
            override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@VerifyOtpActivity, response.body()?.message, Toast.LENGTH_SHORT).show()
                    otpFields.forEach { it.first.text?.clear() }
                } else UIUtils.showErrorResponse(this@VerifyOtpActivity, response)
            }

            override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                Toast.makeText(this@VerifyOtpActivity, "Erreur réseau : ${t.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun delayResendButton(enableAfterMillis: Long = 30000) {
        resendOtpButton.isEnabled = false
        resendOtpButton.postDelayed({ resendOtpButton.isEnabled = true }, enableAfterMillis)
    }

    private fun handleOtpFocusNavigation(currentField: TextInputEditText, nextField: TextInputEditText?, prevField: TextInputEditText?) {
        currentField.text?.let { text ->
            if (text.length > 1) {
                currentField.setText(text[0].toString())
                currentField.setSelection(1)
            }
            when {
                text.length == 1 -> nextField?.requestFocus()
                text.isEmpty() -> prevField?.requestFocus()
                else -> {}
            }
        }
    }

    private fun getOtp(): String {
        return otpFields.joinToString("") { it.first.text?.toString()?.trim().orEmpty() }
            .takeIf { it.length == otpFields.size } ?: ""
    }

    private fun navigateToSuccess(buttonText: String, title: String, content: String) {
        val sharedPref = getSharedPreferences("MySuccessPrefs", Context.MODE_PRIVATE)
        sharedPref.edit().apply {
            putString("title", title)
            putString("content", content)
            putString("button", buttonText)
            apply()
        }
        startActivity(Intent(this, SuccessActivity::class.java))
        finish()
    }
}
