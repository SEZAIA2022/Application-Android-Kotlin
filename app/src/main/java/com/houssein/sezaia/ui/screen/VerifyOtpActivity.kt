package com.houssein.sezaia.ui.screen

import android.content.Context
import android.content.Intent
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
import com.houssein.sezaia.model.request.ResendOtpRequest
import com.houssein.sezaia.model.request.VerifyChangeEmailRequest
import com.houssein.sezaia.model.request.VerifyForgetRequest
import com.houssein.sezaia.model.response.BaseResponse
import com.houssein.sezaia.model.response.VerifyForgetResponse
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
    private var email: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_otp)
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))
        val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val previousPage = prefs.getString("previous_page", null)
        val email = prefs.getString("email", null)
        val newEmail = prefs.getString("new_email", null)
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
                    moveFocus(
                        currentField = editText,
                        nextField = otpFields.getOrNull(index + 1)?.first,
                        prevField = otpFields.getOrNull(index - 1)?.first
                    )
                }
            })
        }
    }

    private fun setupListeners() {
        val previous_page = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            .getString("previous_page", null) ?: return
        if (previous_page == "ForgetActivity"){
            verifyButton.setOnClickListener { verifyOtp() }
        }
        else {
            verifyButton.setOnClickListener { verifyOtpEmail() }
        }

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

    private fun resendOtp() {
        val email = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            .getString("email", null) ?: return

        resendOtpButton.isEnabled = false

        val request = ResendOtpRequest(email)

        RetrofitClient.instance.resendOtp(request).enqueue(object : Callback<BaseResponse> {
            override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                resendOtpButton.isEnabled = true
                if (response.isSuccessful) {
                    Toast.makeText(this@VerifyOtpActivity, response.body()?.message, Toast.LENGTH_SHORT).show()
                    otpFields.forEach { it.first.text?.clear() }
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Erreur inconnue"
                    Toast.makeText(this@VerifyOtpActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                resendOtpButton.isEnabled = true
                Toast.makeText(this@VerifyOtpActivity, "Erreur réseau : ${t.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun verifyOtp() {
        val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        email = sharedPref.getString("email", null)

        if (email.isNullOrEmpty()) {
            Toast.makeText(this, "Aucun email trouvé. Veuillez réessayer.", Toast.LENGTH_SHORT).show()
            return
        }
        val otp = getOtp()
        if (otp.length != 4) {
            Toast.makeText(this, "Veuillez entrer le code complet", Toast.LENGTH_SHORT).show()
            return
        }

        val email = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            .getString("email", null) ?: return

        val request = VerifyForgetRequest(email = email, otp = otp)

        RetrofitClient.instance.verifyForget(request).enqueue(object : Callback<BaseResponse> {
            override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                if (response.isSuccessful) {
                    startActivity(Intent(this@VerifyOtpActivity, CreatePasswordActivity::class.java))
                    finish()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Erreur inconnue"
                    Toast.makeText(this@VerifyOtpActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                Toast.makeText(this@VerifyOtpActivity, "Erreur réseau : ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun verifyOtpEmail() {
        val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val oldEmail = prefs.getString("email", "") ?: ""
        val newEmail = prefs.getString("new_email", "") ?: ""
        val otp= getOtp()
        val request = VerifyChangeEmailRequest(
            email = oldEmail,           // Ancien email
            new_email = newEmail,       // Nouveau email
            otp = otp
        )

        RetrofitClient.instance.verifyChangeEmail(request).enqueue(object : Callback<BaseResponse> {
            override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.status == "success") {
                        Toast.makeText(this@VerifyOtpActivity, body.message, Toast.LENGTH_SHORT).show()
                        // ✅ Fermer ou rediriger après succès
                        finish()
                    } else {
                        Toast.makeText(this@VerifyOtpActivity, body?.message ?: "Erreur inconnue", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorMsg = try {
                        response.errorBody()?.string() ?: "Erreur inconnue"
                    } catch (e: Exception) {
                        "Erreur lors du traitement de la réponse"
                    }
                    Toast.makeText(this@VerifyOtpActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                Toast.makeText(this@VerifyOtpActivity, "Erreur réseau : ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }






    private fun getOtp(): String =
        otpFields.joinToString("") { it.first.text?.toString()?.trim().orEmpty() }

    fun moveFocus(currentField: TextInputEditText, nextField: TextInputEditText?, prevField: TextInputEditText?) {
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
}
