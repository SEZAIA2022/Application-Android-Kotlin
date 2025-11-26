package com.houssein.sezaia.ui.screen

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.houssein.sezaia.R
import com.houssein.sezaia.model.data.MyApp
import com.houssein.sezaia.model.response.Repair
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.adapter.RepairAdapterDaily
import com.houssein.sezaia.ui.utils.UIUtils
import kotlinx.coroutines.launch

class DailyInterventionActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private var adapter: RepairAdapterDaily? = null
    private var allRepairs: List<Repair> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily_intervention)

        UIUtils.applySystemBarsInsets(findViewById(R.id.main))
        UIUtils.initToolbar(
            this,
            getString(R.string.daily_intervention),
            actionIconRes = R.drawable.baseline_density_medium_24,
            showBackButton = false,
            onBackClick = { finish() },
            onActionClick = { startActivity(Intent(this, SettingsActivity::class.java)) }
        )

        recyclerView = findViewById(R.id.dailyRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadDailyRepairs()
    }
    override fun onBackPressed() {
        super.onBackPressed()
    }
    private fun loadDailyRepairs() {
        val app = application as MyApp
        val applicationName = app.application_name

        val sharedPref = getSharedPreferences("LoginData", MODE_PRIVATE)
        val loggedUsername = (sharedPref.getString("loggedUsername", "") ?: "").trim()

        if (loggedUsername.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_missing_username), Toast.LENGTH_LONG).show()
            return
        }

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getRepairsByDay(applicationName, loggedUsername)
                android.util.Log.d("Daily", "API status=${response.status}, size=${response.repairs?.size ?: 0}")

                if (response.status.equals("success", ignoreCase = true)) {
                    allRepairs = response.repairs ?: emptyList()

                    adapter = RepairAdapterDaily(allRepairs) { repair ->
                        val message = buildString {
                            appendLine("🗓️ Date : ${repair.date}")
                            repair.hour_slot?.let { appendLine("⏰ Hour : $it") }
                            appendLine("🔳 QR Id : ${repair.qr_code}")
                            appendLine("🛠️ Problem : ${repair.description_probleme}")
                            repair.comment?.let { appendLine("💬 Comment : $it") }
                            appendLine("📌 Status : ${repair.status}")
                            if (repair.status == "repaired") {
                                appendLine("👷 Technician : ${repair.user_tech ?: "N/A"}")
                            }
                            repair.address?.let { appendLine("📍 Address : $it") }
                        }

                        val context = recyclerView.context
                        val encodedAddress = repair.address?.let { Uri.encode(it) }
                        android.util.Log.d("Daily", "Repair id=${repair.id}, address='${repair.address}'")

                        val mapsUri = encodedAddress?.let {
                            Uri.parse("https://www.google.com/maps/search/?api=1&query=$it")
                        }

                        showDialog(
                            title = "Repair details",
                            message = message,
                            positiveButtonText = "OK",
                            onPositiveClick = {},
                            negativeButtonText = if (mapsUri != null) "Open in Google Maps" else null,
                            onNegativeClick =
                                if (mapsUri != null) {
                                    {
                                        val intent = Intent(Intent.ACTION_VIEW, mapsUri)
                                        intent.setPackage("com.google.android.apps.maps")
                                        context.startActivity(intent)
                                    }
                                } else null
                        )
                    }

                    recyclerView.adapter = adapter

                    if (allRepairs.isEmpty()) {
                        Toast.makeText(
                            this@DailyInterventionActivity,
                            getString(R.string.no_repairs_today),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    val msg = response.message ?: getString(R.string.error_generic)
                    Toast.makeText(this@DailyInterventionActivity, msg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DailyInterventionActivity, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
                android.util.Log.e("Daily", "API error", e)
            }
        }
    }
}
