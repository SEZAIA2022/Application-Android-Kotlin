package com.houssein.sezaia.ui.screen

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.util.Log
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
    private lateinit var otpLayout: LinearLayout
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
        otpLayout = findViewById(R.id.otpLayout)

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
        when (previousPage) {
            "ForgetActivity" -> verifyButton.setOnClickListener { verifyForgetOtp() }
            "ChangeEmailActivity" -> verifyButton.setOnClickListener { verifyEmailChangeOtp() }
            "SignUpActivity" -> verifyButton.setOnClickListener { verifySignUpOtp() }
            "DeleteAccountActivity" -> verifyButton.setOnClickListener { verifyDeleteAccountOtp() }
        }

        otpFields.forEach { (editText, layout) ->
            editText.addTextChangedListener(UIUtils.inputWatcher(editText, layout))
        }
    }

    private fun handleOtpFocusNavigation(currentField: TextInputEditText, nextField: TextInputEditText?, prevField: TextInputEditText?) {
        val text = currentField.text.toString()
        if (text.length > 1) {
            currentField.setText(text[0].toString())
            currentField.setSelection(1)
        }

        when {
            text.length == 1 -> nextField?.requestFocus()
            text.isEmpty() -> prevField?.requestFocus()
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

    private fun resendOtp() {
        resendOtpButton.isEnabled = false
        val request = ResendOtpRequest(email.toString(), previousPage.toString(), applicationName)

        RetrofitClient.instance.resendOtp(request).enqueue(object : Callback<BaseResponse> {
            override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                resendOtpButton.isEnabled = true
                if (response.isSuccessful) {
                    Toast.makeText(this@VerifyOtpActivity, response.body()?.message, Toast.LENGTH_SHORT).show()
                    otpFields.forEach { it.first.text?.clear() }
                } else UIUtils.showErrorResponse(this@VerifyOtpActivity, response)
            }

            override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                resendOtpButton.isEnabled = true
                Toast.makeText(this@VerifyOtpActivity, "Erreur réseau : ${t.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun verifyForgetOtp() {
        val otp = getOtp()

        verifyButton.isEnabled = false
        val request = VerifyForgetRequest(email!!, otp, applicationName)

        RetrofitClient.instance.verifyForget(request).enqueue(object : Callback<BaseResponse> {
            override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                verifyButton.isEnabled = true
                if (response.isSuccessful) {
                    startActivity(Intent(this@VerifyOtpActivity, CreatePasswordActivity::class.java))
                    finish()
                } else UIUtils.showErrorResponse(this@VerifyOtpActivity, response)
            }

            override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                verifyButton.isEnabled = true
                Toast.makeText(this@VerifyOtpActivity, "Erreur réseau : ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun verifyEmailChangeOtp() {
        val otp = getOtp()

        val request = VerifyChangeEmailRequest(email!!, otp)

        RetrofitClient.instance.verifyChangeEmail(request).enqueue(object : Callback<BaseResponse> {
            override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    Toast.makeText(this@VerifyOtpActivity, response.body()?.message, Toast.LENGTH_SHORT).show()

                    val sharedPref = getSharedPreferences("MySuccessPrefs", Context.MODE_PRIVATE)
                    sharedPref.edit().apply {
                        putString("title", "Email modified")
                        putString("content", "Your email has been successfully modified, and you should be able to access it now.")
                        putString("button", getString(R.string.return_to_login))
                        apply()
                    }
                    startActivity(Intent(this@VerifyOtpActivity, SuccessActivity::class.java))
                    finish()
                } else UIUtils.showErrorResponse(this@VerifyOtpActivity, response)
            }

            override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                Toast.makeText(this@VerifyOtpActivity, "Erreur réseau : ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun verifySignUpOtp() {
        val otp = getOtp()
        val request = VerifyRegisterRequest(email!!, otp)

        RetrofitClient.instance.verifyRegister(request).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    val sharedPref = getSharedPreferences("MySuccessPrefs", Context.MODE_PRIVATE)
                    sharedPref.edit().apply {
                        putString("title", "Created account")
                        putString("content", "Your account has been successfully created.")
                        putString("button", getString(R.string.return_to_login))
                        apply()
                    }
                    startActivity(Intent(this@VerifyOtpActivity, SuccessActivity::class.java))
                    finish()
                } else UIUtils.showErrorResponse(this@VerifyOtpActivity, response)

            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Toast.makeText(this@VerifyOtpActivity, "Erreur réseau : ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun verifyDeleteAccountOtp() {
        val otp = getOtp()
        val request = VerifyDeleteAccountRequest(otp, email!!)

        RetrofitClient.instance.verifyDeleteAccount(request).enqueue(object : Callback<BaseResponse> {
            override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    Toast.makeText(this@VerifyOtpActivity, response.body()?.message, Toast.LENGTH_SHORT).show()

                    val sharedPref = getSharedPreferences("MySuccessPrefs", Context.MODE_PRIVATE)
                    sharedPref.edit().apply {
                        putString("title", "Account deleted")
                        putString("content", "Your account has been successfully deleted, and you will no longer be able to access it.")
                        putString("button", getString(R.string.return_to_login))
                        apply()
                    }

                    getSharedPreferences("LoginData", MODE_PRIVATE).edit {
                        putBoolean("isLoggedIn", false)
                        remove("userRole")
                    }
                    startActivity(Intent(this@VerifyOtpActivity, SuccessActivity::class.java))
                    finish()
                } else UIUtils.showErrorResponse(this@VerifyOtpActivity, response)
            }

            override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                Toast.makeText(this@VerifyOtpActivity, "Erreur réseau : ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showErrorResponse(context: Context, response: Response<*>) {
        val errorMsg = try {
            val errorBody = response.errorBody()?.string()
            if (!errorBody.isNullOrEmpty()) {
                val json = JSONObject(errorBody)
                json.optString("message", "Erreur inconnue")
            } else {
                "Erreur inconnue"
            }
        } catch (e: Exception) {
            Log.e("VerifyOtpActivity", "Erreur parsing errorBody: ${e.localizedMessage}")
            "Erreur lors du traitement de la réponse"
        }

        Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
    }

    private fun getOtp(): String {
        val otp = otpFields.joinToString("") { it.first.text?.toString()?.trim().orEmpty() }
        return otp.takeIf { it.length == otpFields.size } ?: ""
    }
}
