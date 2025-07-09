package com.houssein.sezaia.ui.screen

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.houssein.sezaia.R

import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.houssein.sezaia.model.response.QrCodeResponse
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.adapter.QrCodeAdapter
import com.houssein.sezaia.ui.utils.UIUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class QrCodeActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: QrCodeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qrcode)
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))

        // Initialisation de la toolbar
        UIUtils.initToolbar(
            this,getString(R.string.qr_code_history), actionIconRes = R.drawable.baseline_settings_24, onBackClick = {finish()},
            onActionClick = {recreate() }
        )
        recyclerView = findViewById(R.id.repairRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        RetrofitClient.instance.getQRCodes().enqueue(object : Callback<QrCodeResponse> {
            override fun onResponse(call: Call<QrCodeResponse>, response: Response<QrCodeResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    val qrCodes = response.body()?.qrcodes ?: emptyList()
                    adapter = QrCodeAdapter(qrCodes) { selectedQrCode ->
                        val intent = Intent(this@QrCodeActivity, QrCodeDetailActivity::class.java)
                        intent.putExtra("qr_code", selectedQrCode)
                        startActivity(intent)
                    }
                    recyclerView.adapter = adapter

                } else {
                    Toast.makeText(this@QrCodeActivity, "Failed to load QR codes", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<QrCodeResponse>, t: Throwable) {
                Toast.makeText(this@QrCodeActivity, "Error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
}

