package com.houssein.sezaia.ui.screen

import RepairResponse
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.houssein.sezaia.R
import com.houssein.sezaia.model.data.MyApp
import com.houssein.sezaia.model.response.QrIdResponses
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.adapter.ResponseAdapter
import com.houssein.sezaia.ui.utils.UIUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RepairDetailsActivity : AppCompatActivity() {

    private lateinit var scroll: NestedScrollView
    private lateinit var infoTextView: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnHistory: MaterialButton
    private lateinit var btnCreateReport: MaterialButton
    private lateinit var adapter: ResponseAdapter

    private var repairId: String? = null
    private var qrCode: String? = null
    private var qrId: String? = null   // ✅ on stocke le qr_id ici

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_repair_details)

        UIUtils.applySystemBarsInsets(findViewById(R.id.rootRepairDetails))
        UIUtils.initToolbar(
            this,
            getString(R.string.repair),
            actionIconRes = R.drawable.baseline_density_medium_24,
            onBackClick = { finish() },
            onActionClick = { startActivity(Intent(this, SettingsActivity::class.java)) }
        )

        scroll = findViewById(R.id.scrollContent)
        infoTextView = findViewById(R.id.repairInfo)
        recyclerView = findViewById(R.id.responseList)
        btnHistory = findViewById(R.id.btnHistory)
        btnCreateReport = findViewById(R.id.btnCreateReport)

        recyclerView.layoutManager = LinearLayoutManager(this)

        val sp = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        repairId = intent.getStringExtra("repair_id") ?: sp.getString("id", null)
        qrCode = intent.getStringExtra("qr_code")

        // 🔹 Récupérer l'application courante
        val applicationName = (application as MyApp).application_name

        // 🔹 Aller chercher qr_id dès le démarrage (si on a qr_code)
        if (!qrCode.isNullOrBlank()) {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val res: QrIdResponses = RetrofitClient.instance.getQrId(
                        qrCode = qrCode!!,
                        application = applicationName.lowercase()
                    )
                    if (res.status == "success" && res.qr_id != null) {
                        qrId = res.qr_id
                    } else {
                        Toast.makeText(
                            this@RepairDetailsActivity,
                            res.message ?: "QR id introuvable.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    Log.e("RepairDetailsActivity", "get_qr_id error: ${e.localizedMessage}", e)
                    Toast.makeText(
                        this@RepairDetailsActivity,
                        "Erreur get_qr_id : ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        // Historique → maintenant on passe qr_id (si dispo), sinon on retombe sur qr_code (fallback)
        btnHistory.setOnClickListener {
            val intent = Intent(this, QrCodeDetailActivity::class.java)
            qrId?.let { intent.putExtra("qr_id", it) }
            qrCode?.let { intent.putExtra("qr_code", it) } // fallback si l’autre écran le supporte encore
            startActivity(intent)
        }

        // Aller à la création de rapport → on passe qr_id
        btnCreateReport.setOnClickListener {
            val id = repairId
            if (id.isNullOrBlank()) {
                Toast.makeText(this, "ID de réparation manquant", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, ReportCreationActivity::class.java)
                .putExtra("repair_id", id)
            qrId?.let { intent.putExtra("qr_id", it) }       // ✅ on envoie qr_id
            // (optionnel) garde aussi qr_code en secours si l’autre écran l’utilise encore
            qrCode?.let { intent.putExtra("qr_code", it) }
            startActivity(intent)
        }

        // Charger les données de réparation (inchangé)
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val data = RetrofitClient.instance.getRepairDetails(repairId)
                displayRepairData(data)
            } catch (e: Exception) {
                Log.e("RepairDetailsActivity", "Load error: ${e.localizedMessage}", e)
                Toast.makeText(
                    this@RepairDetailsActivity,
                    "Erreur de chargement : ${e.localizedMessage ?: "inconnue"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun displayRepairData(data: RepairResponse) {
        val b = StringBuilder().apply {
            append("👤 ${getString(R.string.user)} : ${data.repair.username}\n")
            append("📅 ${getString(R.string.date)} : ${data.repair.date ?: "N/A"}\n")
            append("💬 ${getString(R.string.comment)} : ${data.repair.comment ?: "—"}\n")
            append("📌 ${getString(R.string.status)} : ${data.repair.status ?: "—"}")
        }
        infoTextView.text = b.toString()

        adapter = ResponseAdapter(data.responses)
        recyclerView.adapter = adapter
    }
}
