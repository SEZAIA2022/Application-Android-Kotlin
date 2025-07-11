package com.houssein.sezaia.ui.screen

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge

import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.houssein.sezaia.R
import com.houssein.sezaia.model.request.AddQrRequest
import com.houssein.sezaia.model.response.BaseResponse
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.utils.UIUtils
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ActivateQrCodeActivity : BaseActivity() {

    private lateinit var usernameEditText: TextInputEditText
    private lateinit var locationEditText: TextInputEditText
    private lateinit var usernameLayout: TextInputLayout
    private lateinit var locationLayout: TextInputLayout
    private lateinit var btnAddQrCode: Button
    private lateinit var inputFields: List<Pair<TextInputEditText, TextInputLayout>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_activate_qr_code)
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))
        initViews()
        UIUtils.initToolbar(
            this,getString(R.string.change_email),actionIconRes = R.drawable.baseline_qr_code_24, onBackClick = {finish()},
            onActionClick = { recreate() }
        )
        setupListeners()
    }

    private fun setupListeners() {
        btnAddQrCode.setOnClickListener{
            addQrCode()
        }
    }

    private fun addQrCode() {
        val username = usernameEditText.text.toString()
        val location = locationEditText.text.toString()
        val sharedPrefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val qrCode = sharedPrefs.getString("qrData", null).toString()
        val request = AddQrRequest(
            username, location, qrCode
        )

        RetrofitClient.instance.addQr(request).enqueue(object : Callback<BaseResponse> {
            override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                if (response.isSuccessful){
                    val body = response.body()
                    if (body != null && body.status == "success"){
                        Toast.makeText(this@ActivateQrCodeActivity, body.message, Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@ActivateQrCodeActivity, CameraActivity::class.java)
                        startActivity(intent)
                        finish()
                    }else {
                        Toast.makeText(this@ActivateQrCodeActivity, body?.message ?: "Unknown Error", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = try {
                        errorBody?.let { JSONObject(it).getString("message") }
                    } catch (e: Exception) {
                        "Unknown Error"
                    }
                    Toast.makeText(this@ActivateQrCodeActivity, errorMessage, Toast.LENGTH_SHORT).show()
                }


            }

            override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                Toast.makeText(this@ActivateQrCodeActivity, "Network Error : ${t.localizedMessage}", Toast.LENGTH_LONG).show()            }

        })

    }

    private fun initViews() {
        usernameEditText = findViewById(R.id.user)
        usernameLayout = findViewById(R.id.userLayout)
        locationEditText = findViewById(R.id.location)
        locationLayout = findViewById(R.id.locationLayout)
        btnAddQrCode = findViewById(R.id.btnAddQrcode)
        inputFields = listOf(
            usernameEditText to usernameLayout,
            locationEditText to locationLayout
        )
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