package com.houssein.sezaia.ui.screen

import RepairResponse
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.houssein.sezaia.R
import com.houssein.sezaia.model.request.DescriptionRequest
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.adapter.ResponseAdapter
import com.houssein.sezaia.ui.utils.UIUtils
import kotlinx.coroutines.launch

@Suppress("NAME_SHADOWING")
class RepairActivity : AppCompatActivity() {

    private lateinit var infoTextView: TextView
    private lateinit var problemEditText: TextInputEditText
    private lateinit var btnRepair: Button
    private lateinit var btnHistory: Button            // <--- Nouveau bouton
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ResponseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_repair)

        UIUtils.applySystemBarsInsets(findViewById(R.id.main))
        UIUtils.initToolbar(
            this, getString(R.string.repair), actionIconRes = R.drawable.baseline_density_medium_24,
            onBackClick = { finish() },
            onActionClick = { startActivity(Intent(this, SettingsActivity::class.java)) }
        )

        infoTextView = findViewById(R.id.repairInfo)
        problemEditText = findViewById(R.id.problem)
        btnRepair = findViewById(R.id.btnRepair)
        btnHistory = findViewById(R.id.btnHistory)             // <-- Initialisation btnHistory
        recyclerView = findViewById(R.id.responseList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val sharedPref = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val repairId = sharedPref.getString("id", null)

        btnRepair.isEnabled = false
        problemEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                btnRepair.isEnabled = !s.isNullOrBlank()
            }

            override fun afterTextChanged(s: Editable?) { }
        })

        // Listener pour ouvrir l'activité historique
        btnHistory.setOnClickListener {
            val qrCode = intent.getStringExtra("qr_code")
            val intent = Intent(this, QrCodeDetailActivity::class.java)
            intent.putExtra("qr_code", qrCode)
            startActivity(intent)
        }

        btnRepair.setOnClickListener {
            val description = problemEditText.text.toString()
            val repairId = sharedPref.getString("id", null)

            if (repairId == null) {
                Toast.makeText(this, "ID de réparation manquant", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val request = DescriptionRequest(id = repairId, description_probleme = description)
                    val response = RetrofitClient.instance.addDescription(request)

                    if (response.status == "success") {
                        Toast.makeText(this@RepairActivity, response.message, Toast.LENGTH_LONG).show()
                        val intent = Intent(this@RepairActivity, CameraActivity::class.java)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@RepairActivity, "Erreur : ${response.message}", Toast.LENGTH_LONG).show()
                    }

                } catch (e: Exception) {
                    Log.e("RepairActivity", "Erreur réseau : ${e.localizedMessage}", e)
                    Toast.makeText(this@RepairActivity, "Erreur réseau : ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }




        lifecycleScope.launch {
            try {
                val data = RetrofitClient.instance.getRepairDetails(repairId)
                displayRepairData(data)
            } catch (e: Exception) {
                Log.e("RepairActivity", "Erreur réseau : ${e.localizedMessage}", e)
                Toast.makeText(
                    this@RepairActivity,
                    "Erreur de chargement : ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun displayRepairData(data: RepairResponse) {
        val builder = StringBuilder().apply {
            append("👤 Utilisateur : ${data.repair.username}\n")
            append("📅 Date : ${data.repair.date ?: "N/A"}\n")
            append("💬 Commentaire : ${data.repair.comment ?: "—"}\n")
            append("📌 Statut : ${data.repair.status ?: "Non défini"}")
        }

        infoTextView.text = builder.toString()
        adapter = ResponseAdapter(data.responses)
        recyclerView.adapter = adapter
    }
}
