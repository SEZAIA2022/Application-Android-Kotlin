package com.houssein.sezaia.ui.screen

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.houssein.sezaia.R
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.model.request.*
import com.houssein.sezaia.model.response.*
import com.houssein.sezaia.ui.*
import com.houssein.sezaia.ui.utils.SimpleTextWatcher
import com.houssein.sezaia.ui.utils.UIUtils
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class VerifyOtpActivity : BaseActivity() {

    private lateinit var otpFields: List<Pair<TextInputEditText, TextInputLayout>>
    private lateinit var otp1Input: TextInputEditText
    private lateinit var otp1Layout: TextInputLayout
    private lateinit var otp2Input: TextInputEditText
    private lateinit var otp2Layout: TextInputLayout
    private lateinit var otp3Input: TextInputEditText
    private lateinit var otp3Layout: TextInputLayout
    private lateinit var otp4Input: TextInputEditText
    private lateinit var otp4Layout: TextInputLayout
    private lateinit var verifyButton: Button
    private lateinit var resendOtpButton: TextView
    private lateinit var otpLayout: LinearLayout
    private var token: String? = null
    private var source: String? = null
    private var email: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_otp)
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))
        extractIntentExtras()
        initViews()
        UIUtils.initToolbar(
            this, getString(R.string.otp_verification), actionIconRes = R.drawable.baseline_verified_user_24, onBackClick = { finish() },
            onActionClick = { recreate() }
        )
        setupListeners()
        setupClickableResend()
    }

    private fun extractIntentExtras() {
        token = intent.getStringExtra("token")
        email = intent.getStringExtra("email")
        source = intent.getStringExtra("previousPage")
    }

    private fun initViews() {
        otpLayout = findViewById(R.id.otpLayout)
        otp1Input = findViewById(R.id.otpInput1)
        otp1Layout = findViewById(R.id.otpLayout1)
        otp2Input = findViewById(R.id.otpInput2)
        otp2Layout = findViewById(R.id.otpLayout2)
        otp3Input = findViewById(R.id.otpInput3)
        otp3Layout = findViewById(R.id.otpLayout3)
        otp4Input = findViewById(R.id.otpInput4)
        otp4Layout = findViewById(R.id.otpLayout4)

        otpFields = listOf(
            otp1Input to otp1Layout,
            otp2Input to otp2Layout,
            otp3Input to otp3Layout,
            otp4Input to otp4Layout
        )

        verifyButton = findViewById(R.id.btnContinue)
        resendOtpButton = findViewById(R.id.resend)

        otp1Input.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                moveFocus(otp1Input, otp2Input, null)
            }
        })
        otp2Input.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                moveFocus(otp2Input, otp3Input, otp1Input)
            }
        })
        otp3Input.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                moveFocus(otp3Input, otp4Input, otp2Input)
            }
        })
        otp4Input.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                moveFocus(otp4Input, null, otp3Input)
            }
        })
    }

    private fun setupListeners() {
        verifyButton.setOnClickListener { verifyOtp() }
        otpFields.forEach { (editText, layout) ->
            editText.addTextChangedListener(UIUtils.inputWatcher(editText, layout))
        }
    }

    private fun setupClickableResend() {
        val fullText = resendOtpButton.text.toString()
        UIUtils.makeTextClickable(
            context = this,
            textView = resendOtpButton,
            fullText = fullText,
            clickableText = "Resend",
            clickableColorRes = R.color.light_blue
        ) {
            resendOtp()
        }
    }

    private fun getOtp(): String = otpFields.joinToString("") { it.first.text.toString().trim() }

    private fun verifyOtp() {
        val otp = getOtp()

        when (source) {
            "ForgetActivity" -> {
                val request = VerifyForgetRequest(otp = otp, token = token ?: "", email = email ?: "")
                RetrofitClient.instance.verifyForget(request).enqueue(object : Callback<VerifyForgetResponse> {
                    override fun onResponse(call: Call<VerifyForgetResponse>, response: Response<VerifyForgetResponse>) {
                        if (response.isSuccessful && response.body() != null) {
                            handleSuccessResponse(response.body()!!)
                        } else {
                            showErrorResponse(response)
                        }
                    }

                    override fun onFailure(call: Call<VerifyForgetResponse>, t: Throwable) {
                        showNetworkError(t)
                    }
                })
            }
            "SignUpActivity" -> {
                val request = VerifyRegisterRequest(otp = otp, token = token ?: "", email = email ?: "")
                RetrofitClient.instance.verifyRegister(request).enqueue(object : Callback<VerifyRegisterResponse> {
                    override fun onResponse(call: Call<VerifyRegisterResponse>, response: Response<VerifyRegisterResponse>) {
                        if (response.isSuccessful && response.body() != null) {
                            handleSuccessResponse(response.body()!!)
                        } else {
                            showErrorResponse(response)
                        }
                    }

                    override fun onFailure(call: Call<VerifyRegisterResponse>, t: Throwable) {
                        showNetworkError(t)
                    }
                })
            }
            "ChangeEmailActivity" -> {
                val request = VerifyChangeEmailRequest(otp = otp, token = token ?: "", email = email ?: "")
                RetrofitClient.instance.verifyChangeEmail(request).enqueue(object : Callback<VerifyChangeEmailResponse> {
                    override fun onResponse(call: Call<VerifyChangeEmailResponse>, response: Response<VerifyChangeEmailResponse>) {
                        if (response.isSuccessful && response.body() != null) {
                            handleSuccessResponse(response.body()!!)
                        } else {
                            showErrorResponse(response)
                        }
                    }

                    override fun onFailure(call: Call<VerifyChangeEmailResponse>, t: Throwable) {
                        showNetworkError(t)
                    }
                })
            }
            "DeleteAccountActivity" -> {
                val request = VerifyDeleteAccountRequest(otp = otp, token = token ?: "", email = email ?: "")
                RetrofitClient.instance.verifyDeleteAccount(request).enqueue(object : Callback<VerifyDeleteAccountResponse> {
                    override fun onResponse(call: Call<VerifyDeleteAccountResponse>, response: Response<VerifyDeleteAccountResponse>) {
                        if (response.isSuccessful && response.body() != null) {
                            handleSuccessResponse(response.body()!!)
                        } else {
                            showErrorResponse(response)
                        }
                    }

                    override fun onFailure(call: Call<VerifyDeleteAccountResponse>, t: Throwable) {
                        showNetworkError(t)
                    }
                })
            }
            else -> {
                Toast.makeText(this, "Unknown Source", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resendOtp() {
        val request = ResendOtpRequest(email = email ?: "", token = token ?: "")
        RetrofitClient.instance.resendOtp(request).enqueue(object : Callback<ResendOtpResponse> {
            override fun onResponse(call: Call<ResendOtpResponse>, response: Response<ResendOtpResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    token = data.token // Met à jour le token
                    Toast.makeText(this@VerifyOtpActivity, data.message ?: "OTP sent.", Toast.LENGTH_SHORT).show()
                } else {
                    showErrorResponse(response)
                }
            }

            override fun onFailure(call: Call<ResendOtpResponse>, t: Throwable) {
                showNetworkError(t)
            }
        })
    }

    private fun handleSuccessResponse(data: Any) {
        val message = when (data) {
            is VerifyForgetResponse -> data.message ?: "Successfully verified"
            is VerifyRegisterResponse -> data.message ?: "Successfully verified"
            is VerifyChangeEmailResponse -> data.message ?: "Successfully verified"
            is VerifyDeleteAccountResponse -> data.message ?: "Successfully verified"
            else -> "Successfully verified"
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

        when (source) {
            "ForgetActivity" -> {
                val intent = Intent(this, CreatePasswordActivity::class.java)
                intent.putExtra("email", email)
                startActivity(intent)
            }
            "SignUpActivity", "ChangeEmailActivity", "DeleteAccountActivity" -> {
                val intent = Intent(this, SuccessActivity::class.java)
                intent.putExtra("title", getSuccessTitle(source))
                intent.putExtra("content", getSuccessMessage(source))
                intent.putExtra("button", getSuccessButton(source))
                startActivity(intent)
            }
        }
    }

    private fun showErrorResponse(response: Response<*>) {
        val errorMsg = try {
            val errorJson = JSONObject(response.errorBody()?.string() ?: "")
            errorJson.getString("message")
        } catch (e: Exception) {
            "Unexpected error"
        }
        Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
        indicateOtpError()
    }

    private fun showNetworkError(t: Throwable) {
        Toast.makeText(this, "Network error : ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
    }

    private fun indicateOtpError() {
        otpFields.forEach { (editText, layout) ->
            editText.text?.clear()
            layout.boxStrokeColor = ContextCompat.getColor(this, R.color.red)
        }
        otp1Input.requestFocus()
    }

    private fun getSuccessTitle(source: String?): String = when (source) {
        "SignUpActivity" -> "Created account"
        "ChangeEmailActivity" -> "Email modified"
        "DeleteAccountActivity" -> "Account deleted"
        else -> "Success"
    }

    private fun getSuccessMessage(source: String?): String = when (source) {
        "SignUpActivity" -> "Your account has been successfully created."
        "ChangeEmailActivity" -> "Your email has been successfully modified, and you should be able to access it now."
        "DeleteAccountActivity" -> "Your account has been successfully deleted, and you will no longer be able to access it."
        else -> "Operation successfully completed."
    }

    private fun getSuccessButton(source: String?): String = when (source) {
        "SignUpActivity", "ChangeEmailActivity", "DeleteAccountActivity" -> getString(R.string.return_to_login)
        else -> "Continue"
    }

    fun moveFocus(currentField: TextInputEditText, nextField: TextInputEditText?, prevField: TextInputEditText?) {
        val text = currentField.text.toString()
        currentField.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                resetInputStyles(R.color.blue, false, otpFields)
            }
        }

        if (text.length > 1) {
            currentField.setText(text[0].toString())
            currentField.setSelection(1)
        }

        if (text.length == 1 && nextField != null) {
            nextField.requestFocus()
        }

        if (text.isEmpty() && prevField != null) {
            prevField.requestFocus()
        }
    }
}
