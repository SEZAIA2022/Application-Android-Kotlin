package com.houssein.sezaia.ui.screen

import RepairResponse
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.houssein.sezaia.R
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.utils.UIUtils
import kotlinx.coroutines.launch

class RepairActivity : AppCompatActivity() {

    private lateinit var infoTextView: TextView
    private lateinit var problemEditText: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_repair)

        UIUtils.applySystemBarsInsets(findViewById(R.id.main))
        UIUtils.initToolbar(
            this, getString(R.string.repair), actionIconRes = R.drawable.baseline_density_medium_24,
            onBackClick = { finish() },
            onActionClick = { startActivity(Intent(this, SettingsActivity::class.java)) }
        )
        infoTextView = findViewById(R.id.info)
        problemEditText = findViewById(R.id.problem)

        // Récupère l'ID passé via Intent (ou par défaut 1)
        val sharedPref = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val repairId = sharedPref.getString("id", null)
        val statusRepair = sharedPref.getString("status_repair", null)

        // Appel réseau en coroutine
        lifecycleScope.launch {
            try {
                val data: RepairResponse = RetrofitClient.instance.getRepairDetails(repairId)
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
            append("User: ${data.repair.username}\n")
            append("Date: ${data.repair.date ?: "N/A"}\n")
            append("Comment: ${data.repair.comment ?: "—"}\n\n")
            append("Status: ${data.repair.status }" )
            append("Responses:\n")
            data.responses.forEach { r ->
                append("- Q: ${r.question_text ?: "Unknown"}\n")
                append("  R: ${r.response ?: "Response not found"}\n\n")
            }
        }

        infoTextView.text = builder.toString()
        problemEditText.setText(data.repair.comment.orEmpty())
    }
}
