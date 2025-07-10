package com.houssein.sezaia.ui.screen

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.houssein.sezaia.R
import com.houssein.sezaia.model.response.RepairApiResponse
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.utils.UIUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class QrCodeDetailActivity : AppCompatActivity() {

    private lateinit var qrCodeTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_code_detail)
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))

        UIUtils.initToolbar(
            this,
            getString(R.string.qr_code_details),
            actionIconRes = R.drawable.baseline_settings_24,
            onBackClick = { finish() },
            onActionClick = { recreate() }
        )

        qrCodeTextView = findViewById(R.id.qrCodeDetailText)

        val qrCode = intent.getStringExtra("qr_code")

        if (qrCode != null) {
            loadRepairData(qrCode)
        } else {
            qrCodeTextView.text = "Aucun QR code trouvé"
        }
    }

    private fun loadRepairData(qrCode: String) {
        RetrofitClient.instance.fetchRepairByQrCode(qrCode)
            .enqueue(object : Callback<RepairApiResponse> {
                override fun onResponse(
                    call: Call<RepairApiResponse>,
                    response: Response<RepairApiResponse>
                ) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body?.status == "success" && !body.data.isNullOrEmpty()) {
                            val repair = body.data[0]
                            qrCodeTextView.text = """
                                Utilisateur : ${repair.username}
                                Date : ${repair.date}
                                Commentaire : ${repair.comment ?: "Aucun"}
                                Créneau horaire : ${repair.hour_slot ?: "Non spécifié"}
                                Statut : ${repair.status}
                                QR Code : ${repair.qr_code}
                            """.trimIndent()
                        } else {
                            qrCodeTextView.text = body?.message ?: "Aucune donnée trouvée"
                        }
                    } else {
                        qrCodeTextView.text = "Erreur serveur : ${response.code()}"
                    }
                }

                override fun onFailure(call: Call<RepairApiResponse>, t: Throwable) {
                    qrCodeTextView.text = "Erreur réseau : ${t.localizedMessage}"
                }
            })
    }
}

