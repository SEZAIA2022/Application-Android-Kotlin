package com.houssein.sezaia.ui.screen

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.houssein.sezaia.R
import com.houssein.sezaia.model.data.MyApp
import com.houssein.sezaia.model.request.ForgotPasswordRequest
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
    private lateinit var textView: TextView
    private lateinit var applicationName: String

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
        emailInput = findViewById(R.id.username) // C'est un TextInputEditText
        sendOtpBtn = findViewById(R.id.btnForget)
        usernameLayout = findViewById(R.id.usernameLayout) // C'est un TextInputLayout
        textView = findViewById(R.id.textView2)
        inputFields = listOf(emailInput to usernameLayout)

        val sharedPref = getSharedPreferences("MethodePrefs", Context.MODE_PRIVATE)
        val methode = sharedPref.getString("methode", null)

//        if (methode == "Phone") {
//            usernameLayout.hint = getString(R.string.phone_number)
//            val drawableEnd = ContextCompat.getDrawable(this, R.drawable.baseline_local_phone_24)
//
//            emailInput.setCompoundDrawablesWithIntrinsicBounds(
//                null,      // drawableStart
//                null,      // drawableTop
//                drawableEnd, // drawableEnd (à droite)
//                null       // drawableBottom
//            )
//
//            textView.text = getString(R.string.forget_phone_message)
//
//        } else
        if (methode == "Email") {
            usernameLayout.hint = getString(R.string.email)

            val drawableEnd = ContextCompat.getDrawable(this, R.drawable.baseline_mail_24)
            emailInput.setCompoundDrawablesWithIntrinsicBounds(
                null, null, drawableEnd, null
            )

            textView.text = getString(R.string.forget_message)
        }
    }


    private fun setupListeners() {
        sendOtpBtn.setOnClickListener {
            val email = emailInput.text?.toString()?.trim().orEmpty()
            val app = application as MyApp
            applicationName = app.application_name
            val request = ForgotPasswordRequest(email, applicationName)

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
                            val errorBody = response.errorBody()
                            val errorMsg = if (errorBody != null) {
                                try {
                                    // Tente de lire le JSON d’erreur pour extraire un message spécifique
                                    val errorJson = JSONObject(errorBody.charStream().readText())
                                    errorJson.optString("message", "Erreur inconnue")
                                } catch (e: Exception) {
                                    // Si ce n’est pas du JSON, renvoie le texte brut
                                    errorBody.string()
                                }
                            } else {
                                "Erreur inconnue"
                            }
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
