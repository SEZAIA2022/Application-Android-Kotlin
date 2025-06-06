package com.houssein.sezaia.ui.screen

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.houssein.sezaia.R
import com.houssein.sezaia.model.request.ForgotPasswordRequest
import com.houssein.sezaia.model.response.ForgotPasswordResponse
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.utils.UIUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import org.json.JSONObject
import android.content.Context


class ForgetActivity : BaseActivity() {

    private lateinit var emailInput: TextInputEditText
    private lateinit var sendOtpBtn: Button
    private lateinit var usernameLayout: TextInputLayout
    private lateinit var inputFields: List<Pair<TextInputEditText, TextInputLayout>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forget)

        UIUtils.applySystemBarsInsets(findViewById(R.id.main))
        setupToolbar()
        initViews()
        setupListeners()
        setupClickableLoginText()
    }

    private fun setupToolbar() {
        UIUtils.initToolbar(
            this,
            getString(R.string.forgot_password),
            actionIconRes = R.drawable.baseline_density_medium_24,
            onBackClick = { finish() },
            onActionClick = {  startActivity(Intent(this, SettingsActivity::class.java))}
        )
    }

    private fun initViews() {
        emailInput = findViewById(R.id.username)
        sendOtpBtn = findViewById(R.id.btnForget)
        usernameLayout = findViewById(R.id.usernameLayout)
        inputFields = listOf(emailInput to usernameLayout)
    }

    private fun setupListeners() {
        sendOtpBtn.setOnClickListener {
            val email = emailInput.text?.toString()?.trim().orEmpty()

            // Appel réseau avec modèle typé
            RetrofitClient.instance.forgotPassword(ForgotPasswordRequest(email))
                .enqueue(object : Callback<ForgotPasswordResponse> {
                    override fun onResponse(
                        call: Call<ForgotPasswordResponse>,
                        response: Response<ForgotPasswordResponse>
                    ) {
                        if (response.isSuccessful && response.body() != null) {
                            val body = response.body()!!
                            val token = body.token
                            val message = body.message
                            val emailResp = body.email

                            Toast.makeText(this@ForgetActivity, message, Toast.LENGTH_LONG).show()

                            val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
                            with (sharedPref.edit()) {
                                putString("token", token)
                                putString("previousPage", "ForgetActivity")
                                putString("email", emailResp)
                                apply()
                            }

                            val intent = Intent(this@ForgetActivity, VerifyOtpActivity::class.java)
                            startActivity(intent)

                        } else {
                            val errorMessage = try {
                                val json = JSONObject(response.errorBody()?.string() ?: "")
                                json.getString("message")
                            } catch (e: Exception) {
                                "Erreur du serveur"
                            }
                            resetInputStyles(R.color.red, clear = true, inputFields)
                            usernameLayout.error = errorMessage
                        }
                    }

                    override fun onFailure(call: Call<ForgotPasswordResponse>, t: Throwable) {
                        usernameLayout.error = "Erreur réseau : ${t.localizedMessage}"
                    }
                })
        }

        inputFields.forEach { (editText, layout) ->
            editText.addTextChangedListener(UIUtils.inputWatcher(editText, layout))
        }
    }

    private fun setupClickableLoginText() {
        val textView: TextView = findViewById(R.id.remember)
        val fullText = textView.text.toString()

        UIUtils.makeTextClickable(
            context = this,
            textView = textView,
            fullText = fullText,
            clickableText = getString(R.string.login),
            clickableColorRes = R.color.light_blue
        ) {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()
        resetInputStyles(R.color.gray, clear = true, inputFields)
    }

    override fun onResume() {
        super.onResume()
        resetInputStyles(R.color.gray, clear = false, inputFields)
    }
}