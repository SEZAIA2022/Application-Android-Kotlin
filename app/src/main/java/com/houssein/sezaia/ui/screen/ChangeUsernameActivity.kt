package com.houssein.sezaia.ui.screen

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.houssein.sezaia.R
import com.houssein.sezaia.model.data.MyApp
import com.houssein.sezaia.model.request.ChangeUsernameRequest
import com.houssein.sezaia.model.request.LoginRequest
import com.houssein.sezaia.model.response.ChangeUsernameResponse
import com.houssein.sezaia.model.response.LoginResponse
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.utils.UIUtils
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChangeUsernameActivity : BaseActivity() {

    private lateinit var usernameEditText: TextInputEditText
    private lateinit var newUsernameEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var usernameLayout: TextInputLayout
    private lateinit var newUsernameLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var btnChangeUsername: Button
    private lateinit var inputFields: List<Pair<TextInputEditText, TextInputLayout>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_username)
        enableEdgeToEdge()
        // Appliquer les insets des barres système
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))

        initViews()
        // Configuration de la toolbar
        UIUtils.initToolbar(
            this,getString(R.string.change_username), actionIconRes = R.drawable.baseline_account_circle_24, onBackClick = {finish()},
            onActionClick = {recreate() }
        )
        setupListeners()
    }

    private fun initViews() {
        usernameEditText = findViewById(R.id.username)
        newUsernameEditText = findViewById(R.id.newUsername)
        passwordEditText = findViewById(R.id.password)
        usernameLayout = findViewById(R.id.usernameLayout)
        newUsernameLayout = findViewById(R.id.newUsernameLayout)
        passwordLayout = findViewById(R.id.passwordLayout)

        btnChangeUsername = findViewById(R.id.btnChangeUsername)
        inputFields = listOf(
            usernameEditText to usernameLayout,
            newUsernameEditText to newUsernameLayout,
            passwordEditText to passwordLayout
        )

        inputFields.firstOrNull()?.first?.requestFocus()

        UIUtils.hideShowPassword(this, passwordEditText)
    }

    private fun setupListeners() {
        btnChangeUsername.setOnClickListener {
            changeUser()
        }
        inputFields.forEach { (editText, layout) ->
            editText.addTextChangedListener(UIUtils.inputWatcher(editText, layout))
        }
    }

    private fun changeUser() {
        val username = usernameEditText.text.toString()
        val newUsername = newUsernameEditText.text.toString()
        val password = passwordEditText.text.toString()
        val app = application as MyApp
        val applicationName = app.application_name
        val changeUsernameRequest = ChangeUsernameRequest(username, password, newUsername, applicationName)

        RetrofitClient.instance.changeUsername(changeUsernameRequest).enqueue(object : Callback<ChangeUsernameResponse>{
            override fun onResponse(
                call: Call<ChangeUsernameResponse>,
                response: Response<ChangeUsernameResponse>
            ) {
                if (response.isSuccessful) {
                    val body = response.body()!!
                    val message = body.message
                    Toast.makeText(this@ChangeUsernameActivity, message, Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@ChangeUsernameActivity, ProfileActivity::class.java))
                    finish()
                }
                else {
                    resetInputStyles(R.color.red, clear = true, inputFields)
                    usernameLayout.error = " "
                    newUsernameLayout.error = " "
                    passwordLayout.error = " "

                    val errorMessage = try {
                        response.errorBody()?.string()?.let {
                            JSONObject(it).getString("message")
                        } ?: "Unknown error"
                    } catch (e: Exception) {
                        "Network Error : ${response.code()}"
                    }
                    showDialog("Username not changed", errorMessage,positiveButtonText = null, // pas de bouton positif
                        onPositiveClick = null,
                        negativeButtonText = "OK",
                        onNegativeClick = { /* rien */ },
                        cancelable = true)
                }
            }

            override fun onFailure(call: Call<ChangeUsernameResponse>, t: Throwable) {
                showDialog("Connection failure", t.localizedMessage ?: "Unknown error", positiveButtonText = null, // pas de bouton positif
                    onPositiveClick = null,
                    negativeButtonText = "OK",
                    onNegativeClick = { /* rien */ },
                    cancelable = true)
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