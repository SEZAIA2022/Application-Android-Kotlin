package com.houssein.sezaia.ui.screen

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.houssein.sezaia.R
import com.houssein.sezaia.model.response.Repair
import com.houssein.sezaia.model.response.RepairApiResponse
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.adapter.RepairAdapter
import com.houssein.sezaia.ui.utils.UIUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class QrCodeDetailActivity : BaseActivity() {

    private lateinit var repairRecyclerView: RecyclerView
    private lateinit var spinnerSortDate: Spinner
    private lateinit var spinnerFilterStatus: Spinner

    private var allRepairs: List<Repair> = emptyList()
    private lateinit var adapter: RepairAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_code_detail)
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))

        UIUtils.initToolbar(
            this,
            getString(R.string.qr_code_details),
            actionIconRes = R.drawable.baseline_density_medium_24,
            onBackClick = { finish() },
            onActionClick = { startActivity(Intent(this, SettingsActivity::class.java)) }
        )

        repairRecyclerView = findViewById(R.id.repairRecyclerView)
        repairRecyclerView.layoutManager = LinearLayoutManager(this)

        spinnerSortDate = findViewById(R.id.spinnerSortDate)
        spinnerFilterStatus = findViewById(R.id.spinnerFilterStatus)

        setupSpinners()

        val qrCode = intent.getStringExtra("qr_code")
        val sharedPref = getSharedPreferences("LoginData", MODE_PRIVATE)
        val techUser = sharedPref.getString("loggedUsername", "") ?: ""
        if (qrCode != null) {
            loadRepairData(qrCode, techUser)
        } else {
            Toast.makeText(this, "QR code not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSpinners() {
        val sortOptions = listOf("Date Ascending", "Date Descending")
        spinnerSortDate.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sortOptions)

        val statusOptions = listOf("All", "Processing", "Repaired")
        spinnerFilterStatus.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, statusOptions)

        val listener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                updateFilteredList()
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        spinnerSortDate.onItemSelectedListener = listener
        spinnerFilterStatus.onItemSelectedListener = listener
    }

    private fun loadRepairData(qrCode: String, techUser: String) {
        RetrofitClient.instance.fetchRepairByQrCode(qrCode, techUser)
            .enqueue(object : Callback<RepairApiResponse> {
                override fun onResponse(call: Call<RepairApiResponse>, response: Response<RepairApiResponse>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body?.status == "success" && !body.data.isNullOrEmpty()) {
                            allRepairs = body.data
                            updateFilteredList()
                        } else {
                            Toast.makeText(this@QrCodeDetailActivity, body?.message ?: "History not found", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@QrCodeDetailActivity, "Qr has not been repaired before", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<RepairApiResponse>, t: Throwable) {
                    Toast.makeText(this@QrCodeDetailActivity, "Please connect to the internet and try again.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateFilteredList() {
        val sortAsc = spinnerSortDate.selectedItemPosition == 0
        val selectedStatus = spinnerFilterStatus.selectedItem.toString()

        val filteredList = allRepairs
            .filter { selectedStatus == "All" || it.status.equals(selectedStatus, ignoreCase = true) }
            .sortedWith(compareBy {
                parseDate(it.date) ?: Date(0)
            })
            .let { if (sortAsc) it else it.reversed() }

        adapter = RepairAdapter(filteredList) { repair ->
            val message = buildString {
                appendLine("🗓️ Date : ${repair.date}")
                repair.hour_slot?.let { appendLine("⏰ Hour : $it") }
                appendLine("🔳 QR Code : ${repair.qr_code}")
                appendLine("🛠️ Problem : ${repair.description_probleme}")
                repair.comment?.let { appendLine("💬 Comment : $it") }
                appendLine("📌 Status : ${repair.status}")
                if (repair.status == "repaired") {
                    appendLine("👷 Technician : ${repair.user_tech ?: "N/A"}")
                }
                repair.address?.let {
                    appendLine("📍 Address : $it")
                }
            }

            val context = repairRecyclerView.context

            val encodedAddress = repair.address?.let { Uri.encode(it) }
            val mapsUri = encodedAddress?.let {
                Uri.parse("https://www.google.com/maps/search/?api=1&query=$it")
            }

            showDialog(
                title = "Repair details",
                message = message,
                positiveButtonText = "OK",
                onPositiveClick = {},
                negativeButtonText = if (mapsUri != null) "Ouvrir dans Google Maps" else null,
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


        repairRecyclerView.adapter = adapter
    }

    private fun parseDate(dateString: String?): Date? {
        return try {
            dateString?.let {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it)
            }
        } catch (e: Exception) {
            null
        }
    }


}
