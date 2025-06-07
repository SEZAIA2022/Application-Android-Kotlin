package com.houssein.sezaia.ui.screen

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.edit
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.houssein.sezaia.R
import com.houssein.sezaia.model.request.ForgotPasswordRequest
import com.houssein.sezaia.model.request.ResendOtpRequest
import com.houssein.sezaia.model.response.BaseResponse
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.utils.UIUtils
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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
            onActionClick = { startActivity(Intent(this, SettingsActivity::class.java)) }
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
            println(email)
            val request = ForgotPasswordRequest(email)

            RetrofitClient.instance.forgotPassword(request)
                .enqueue(object : Callback<BaseResponse> {
                    override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                        if (response.isSuccessful) {
                            val body = response.body()
                            Toast.makeText(this@ForgetActivity, body?.message, Toast.LENGTH_SHORT).show()

                            // Sauvegarder l'email pour l'étape suivante
                            val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
                            sharedPref.edit().apply {
                                putString("previous_page", "ForgetActivity")
                                putString("email", email)
                                apply()
                            }
                            // Aller à VerifyOtpActivity
                            startActivity(Intent(this@ForgetActivity, VerifyOtpActivity::class.java))
                        } else {
                            val errorMsg = response.errorBody()?.string() ?: "Erreur inconnue"
                            Toast.makeText(this@ForgetActivity, errorMsg, Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                        Toast.makeText(this@ForgetActivity, "Erreur réseau : ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
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
