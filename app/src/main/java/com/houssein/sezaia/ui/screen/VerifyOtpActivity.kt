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
            onActionClick = { recreate()}
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

        val payload = mapOf(
            "otp" to otp,
            "token" to (token ?: ""),
            "email" to (email ?: "")
        )

        val call = when (source) {
            "ForgetActivity" -> RetrofitClient.instance.verifyForgetOtp(payload)
            "SignUpActivity" -> RetrofitClient.instance.verifyRegisterOtp(payload)
            "ChangeEmailActivity" -> RetrofitClient.instance.verifyChangeEmailOtp(payload)
            "DeleteAccountActivity" -> RetrofitClient.instance.deleteAccount(payload)
            else -> {
                Toast.makeText(this, "Source inconnue", Toast.LENGTH_SHORT).show()
                return
            }
        }

        call.enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful && response.body() != null) {
                    handleSuccess(response.body()!!)
                } else {
                    val errorMessage = try {
                        val json = JSONObject(response.errorBody()?.string() ?: "")
                        json.getString("message")
                    } catch (e: Exception) {
                        "Erreur du serveur"
                    }
                    indicateOtpError()
                    Toast.makeText(this@VerifyOtpActivity, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Toast.makeText(this@VerifyOtpActivity, "Erreur réseau : ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun indicateOtpError() {
        otpFields.forEach { (editText, layout) ->
            editText.text?.clear()
            layout.boxStrokeColor = ContextCompat.getColor(this, R.color.red)
        }
        otp1Input.requestFocus()
    }

    private fun resendOtp() {
        val payload = mapOf(
            "email" to (email ?: ""),
            "token" to (token ?: "")
        )

        RetrofitClient.instance.resendOtp(payload).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    token = data["token"] as? String
                    val msg = data["message"] as? String ?: "OTP envoyé."
                    Toast.makeText(this@VerifyOtpActivity, msg, Toast.LENGTH_SHORT).show()
                } else {
                    showError(response)
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Toast.makeText(this@VerifyOtpActivity, "Erreur réseau : ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun handleSuccess(data: Map<String, Any>) {
        val message = data["message"] as? String ?: "Vérifié avec succès"
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

        when (source) {
            "ForgetActivity" -> startActivity(Intent(this, CreatePasswordActivity::class.java))
            "SignUpActivity", "ChangeEmailActivity", "DeleteAccountActivity" -> {
                val intent = Intent(this, SuccessActivity::class.java)
                intent.putExtra("title", getSuccessTitle(source))
                intent.putExtra("content", getSuccessMessage(source))
                startActivity(intent)
            }
        }
    }

    private fun showError(response: Response<*>) {
        val errorMsg = try {
            val errorJson = JSONObject(response.errorBody()?.string() ?: "")
            errorJson.getString("message")
        } catch (e: Exception) {
            "Erreur inattendue"
        }
        Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
    }

    private fun getSuccessTitle(source: String?): String = when (source) {
        "SignUpActivity" -> "Compte créé"
        "ChangeEmailActivity" -> "Email modifié"
        "DeleteAccountActivity" -> "Compte supprimé"
        else -> "Succès"
    }

    private fun getSuccessMessage(source: String?): String = when (source) {
        "SignUpActivity" -> "Votre compte a été créé avec succès."
        "ChangeEmailActivity" -> "Votre email a été modifié avec succès."
        "DeleteAccountActivity" -> "Votre compte a été supprimé avec succès."
        else -> "Opération terminée avec succès."
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
