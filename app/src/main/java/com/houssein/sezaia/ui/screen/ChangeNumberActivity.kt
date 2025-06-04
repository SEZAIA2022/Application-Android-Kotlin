package com.houssein.sezaia.ui.screen

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.hbb20.CountryCodePicker
import com.houssein.sezaia.R
import com.houssein.sezaia.model.request.ChangeNumberRequest
import com.houssein.sezaia.model.response.ChangeNumberResponse
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.utils.UIUtils

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class ChangeNumberActivity : BaseActivity() {

    private lateinit var oldNumberEditText: TextInputEditText
    private lateinit var newNumberEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var oldPostalCodeEditText: CountryCodePicker
    private lateinit var newPostalCodeEditText: CountryCodePicker
    private lateinit var oldNumberLayout: TextInputLayout
    private lateinit var newNumberLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var btnChangeNumber: Button
    private lateinit var inputFields: List<Pair<TextInputEditText, TextInputLayout>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_number)
        enableEdgeToEdge()
        // Appliquer les insets des barres système
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))
        initViews()

        UIUtils.initToolbar(
            this,
            getString(R.string.change_number),
            actionIconRes = R.drawable.baseline_local_phone_24,
            onBackClick = { finish() },
            onActionClick = { recreate() }
        )

        setupListeners()
    }

    private fun initViews() {
        oldNumberEditText = findViewById(R.id.oldPhoneNumber)
        newNumberEditText = findViewById(R.id.newPhoneNumber)
        passwordEditText = findViewById(R.id.password)
        oldPostalCodeEditText = findViewById(R.id.oldCountryCodePicker)
        newPostalCodeEditText = findViewById(R.id.newCountryCodePicker)

        oldNumberLayout = findViewById(R.id.oldPhoneLayout)
        newNumberLayout = findViewById(R.id.newPhoneLayout)
        passwordLayout = findViewById(R.id.passwordLayout)

        btnChangeNumber = findViewById(R.id.btnChangeNumber)

        inputFields = listOf(
            oldNumberEditText to oldNumberLayout,
            newNumberEditText to newNumberLayout,
            passwordEditText to passwordLayout
        )

        UIUtils.hideShowPassword(this, passwordEditText)
    }

    private fun setupListeners() {
        btnChangeNumber.setOnClickListener {
            if (UIUtils.validateInputs(inputFields)) {
                changeNumber()
            }
        }
    }



    private fun changeNumber() {
        val oldNumber = oldNumberEditText.text.toString()
        val newNumber = newNumberEditText.text.toString()
        val oldPostalCode = oldPostalCodeEditText.selectedCountryCodeWithPlus.toString()
        val newPostalCode = newPostalCodeEditText.selectedCountryCodeWithPlus.toString()
        val password = passwordEditText.text.toString()

        val request = ChangeNumberRequest(oldPostalCode, oldNumber, newPostalCode, newNumber, password)

        RetrofitClient.instance.changeNumber(request).enqueue(object : Callback<ChangeNumberResponse> {
            override fun onResponse(
                call: Call<ChangeNumberResponse>,
                response: Response<ChangeNumberResponse>
            ) {
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        Toast.makeText(this@ChangeNumberActivity, body.message, Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@ChangeNumberActivity, ProfileActivity::class.java))
                    } ?: run {
                        showDialog(
                            "Error", "Empty response from server",
                            positiveButtonText = null,
                            negativeButtonText = "OK"
                        )
                    }
                } else {
                    resetInputStyles(R.color.red, clear = true, inputFields)
                    oldNumberLayout.error = " "
                    newNumberLayout.error = " "
                    passwordLayout.error = " "

                    val errorMessage = UIUtils.parseErrorMessage(response)
                    showDialog(
                        title = "Number not changed",
                        message = errorMessage,
                        positiveButtonText = null,
                        negativeButtonText = "OK",
                        cancelable = true
                    )
                }
            }

            override fun onFailure(call: Call<ChangeNumberResponse>, t: Throwable) {
                showDialog(
                    title = "Connection failure",
                    message = t.localizedMessage ?: "Unknown error",
                    positiveButtonText = null,
                    negativeButtonText = "OK",
                    cancelable = true
                )
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
