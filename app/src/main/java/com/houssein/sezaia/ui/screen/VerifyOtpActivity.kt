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
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.houssein.sezaia.R
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.model.request.ResendOtpRequest
import com.houssein.sezaia.model.request.VerifyForgetRequest
import com.houssein.sezaia.model.response.ResendOtpResponse
import com.houssein.sezaia.model.response.VerifyForgetResponse
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
    private var token: String? = null
    private var previousPage: String? = null
    private var email: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_otp)
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))

        initViews()
        setupToolbar()
        setupListeners()
        setupClickableResend()
    }

//    private fun loadSharedPrefs() {
//        val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
//        token = sharedPref.getString("token", null)
//        previousPage = sharedPref.getString("previousPage", null)
//        email = sharedPref.getString("email", null)
//    }

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

    private fun resendOtp() {
        val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val savedEmail = sharedPref.getString("email", null) ?: email
        val savedToken = sharedPref.getString("token", null) ?: token

        if (savedEmail.isNullOrEmpty() || savedToken.isNullOrEmpty()) {
            Toast.makeText(this, "Token ou email introuvable", Toast.LENGTH_SHORT).show()
            return
        }

        resendOtpButton.isEnabled = false // Désactive pendant la requête

        val request = ResendOtpRequest(savedEmail, savedToken)

        RetrofitClient.instance.resendOtp(request)
            .enqueue(object : Callback<ResendOtpResponse> {
                override fun onResponse(call: Call<ResendOtpResponse>, response: Response<ResendOtpResponse>) {
                    resendOtpButton.isEnabled = true
                    if (response.isSuccessful && response.body() != null) {
                        val responseBody = response.body()!!
                        val newToken = responseBody.new_token
                        val message = responseBody.message ?: "Code renvoyé"

                        if (!newToken.isNullOrEmpty()) {
                            token = newToken
                            Log.d("VerifyOtpActivity", "token:$token and email:$savedEmail")
                            with(sharedPref.edit()) {
                                putString("token", newToken)
                                putString("email", savedEmail)
                                apply()
                            }
                        }

                        Toast.makeText(this@VerifyOtpActivity, message, Toast.LENGTH_SHORT).show()
                        otpFields.forEach { it.first.text?.clear() }
                    } else {
                        val errorMessage = try {
                            JSONObject(response.errorBody()?.string() ?: "{}").optString("message", "Erreur inconnue")
                        } catch (e: Exception) {
                            "Erreur inconnue"
                        }
                        Toast.makeText(this@VerifyOtpActivity, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResendOtpResponse>, t: Throwable) {
                    resendOtpButton.isEnabled = true
                    Toast.makeText(this@VerifyOtpActivity, "Erreur réseau : ${t.localizedMessage}", Toast.LENGTH_LONG).show()
                    Log.e("ResendOtp", "Erreur : ", t)
                }
            })
    }

    private fun verifyOtp() {
        val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        token = sharedPref.getString("token", null)
        email = sharedPref.getString("email", null)
        val otp = getOtp()
        if (otp.length != 4) {
            Toast.makeText(this, "Veuillez entrer le code complet", Toast.LENGTH_SHORT).show()
            return
        }

        if (token.isNullOrEmpty() || email.isNullOrEmpty()) {
            Toast.makeText(this, "Token ou email manquant, veuillez renvoyer le code.", Toast.LENGTH_SHORT).show()
            Log.d("VerifyOtpActivity", "token:$token and email:$email")
            return
        }

        Log.d("VerifyOtpActivity", "Utilisation token pour vérification : $token")

        val request = VerifyForgetRequest(otp, token!!, email!!)

        RetrofitClient.instance.verifyForget(request)
            .enqueue(object : Callback<VerifyForgetResponse> {
                override fun onResponse(call: Call<VerifyForgetResponse>, response: Response<VerifyForgetResponse>) {
                    if (response.isSuccessful) {
                        startActivity(Intent(this@VerifyOtpActivity, CreatePasswordActivity::class.java))
                        finish()
                    } else {
                        val errorMessage = try {
                            JSONObject(response.errorBody()?.string() ?: "{}").optString("message", "Erreur inconnue")
                        } catch (e: Exception) {
                            "Erreur inconnue"
                        }
                        Toast.makeText(this@VerifyOtpActivity, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<VerifyForgetResponse>, t: Throwable) {
                    Toast.makeText(this@VerifyOtpActivity, "Erreur : ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun getOtp(): String = otpFields.joinToString("") { it.first.text.toString().trim() }

    fun moveFocus(currentField: TextInputEditText, nextField: TextInputEditText?, prevField: TextInputEditText?) {
        val text = currentField.text.toString()

        if (text.length > 1) {
            currentField.setText(text[0].toString())
            currentField.setSelection(1)
        }

        when {
            text.length == 1 && nextField != null -> nextField.requestFocus()
            text.isEmpty() && prevField != null -> prevField.requestFocus()
        }
    }


}