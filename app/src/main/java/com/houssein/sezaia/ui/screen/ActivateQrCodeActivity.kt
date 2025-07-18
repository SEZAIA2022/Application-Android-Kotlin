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
    private lateinit var countryEditText: TextInputEditText
    private lateinit var cityEditText: TextInputEditText
    private lateinit var zoneEditText: TextInputEditText
    private lateinit var streetEditText: TextInputEditText
    private lateinit var exactLocationEditText: TextInputEditText
    private lateinit var btnAddQrCode: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_activate_qr_code)
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))

        initViews()

        UIUtils.initToolbar(
            this,
            getString(R.string.change_email),
            actionIconRes = R.drawable.baseline_qr_code_24,
            onBackClick = { finish() },
            onActionClick = { recreate() }
        )

        btnAddQrCode.setOnClickListener {
            addQrCode()
        }
    }

    private fun addQrCode() {
        val username = usernameEditText.text.toString().trim()
        val country = countryEditText.text.toString().trim()
        val city = cityEditText.text.toString().trim()
        val zone = zoneEditText.text.toString().trim()
        val street = streetEditText.text.toString().trim()
        val exactLocation = exactLocationEditText.text.toString().trim()

        val sharedPrefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val qrCode = sharedPrefs.getString("qrData", null).orEmpty()

        // Check for empty fields
        if (username.isEmpty() || country.isEmpty() || city.isEmpty()
            || zone.isEmpty() || street.isEmpty() || exactLocation.isEmpty()
            || qrCode.isEmpty()
        ) {
            Toast.makeText(this, "Tous les champs sont requis.", Toast.LENGTH_SHORT).show()
            return
        }

        val request = AddQrRequest(
            username = username,
            country = country,
            city = city,
            zone = zone,
            street = street,
            exact_location = exactLocation,
            qr_code = qrCode
        )

        RetrofitClient.instance.addQr(request).enqueue(object : Callback<BaseResponse> {
            override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.status == "success") {
                        Toast.makeText(this@ActivateQrCodeActivity, body.message, Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@ActivateQrCodeActivity, CameraActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@ActivateQrCodeActivity, body?.message ?: "Erreur inconnue", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = try {
                        JSONObject(errorBody ?: "").getString("message")
                    } catch (e: Exception) {
                        "Erreur serveur"
                    }
                    Toast.makeText(this@ActivateQrCodeActivity, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                Toast.makeText(this@ActivateQrCodeActivity, "Erreur réseau : ${t.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun initViews() {
        usernameEditText = findViewById(R.id.user)
        countryEditText = findViewById(R.id.country)
        cityEditText = findViewById(R.id.city)
        zoneEditText = findViewById(R.id.zone)
        streetEditText = findViewById(R.id.street)
        exactLocationEditText = findViewById(R.id.exactLocation)
        btnAddQrCode = findViewById(R.id.btnAddQrcode)
    }
}
