package com.houssein.sezaia.ui.screen

import android.content.Intent
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
import com.houssein.sezaia.ui.adapter.RepairAdapter
import com.houssein.sezaia.ui.utils.UIUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class QrCodeDetailActivity : AppCompatActivity() {

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
        if (qrCode != null) {
            loadRepairData(qrCode)
        } else {
            Toast.makeText(this, "Aucun QR code trouvé", Toast.LENGTH_SHORT).show()
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

    private fun loadRepairData(qrCode: String) {
        RetrofitClient.instance.fetchRepairByQrCode(qrCode)
            .enqueue(object : Callback<RepairApiResponse> {
                override fun onResponse(call: Call<RepairApiResponse>, response: Response<RepairApiResponse>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body?.status == "success" && !body.data.isNullOrEmpty()) {
                            allRepairs = body.data
                            updateFilteredList()
                        } else {
                            Toast.makeText(this@QrCodeDetailActivity, body?.message ?: "Aucune donnée trouvée", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@QrCodeDetailActivity, "Erreur serveur : ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<RepairApiResponse>, t: Throwable) {
                    Toast.makeText(this@QrCodeDetailActivity, "Erreur réseau : ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
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
                appendLine("🛠️ Problem : ${repair.description_problem}")
                repair.comment?.let { appendLine("💬 Comment : $it") }
                appendLine("📌 Status : ${repair.status}")
            }

            showDialog(
                title = "Repair details",
                message = message,
                positiveButtonText = "OK",
                onPositiveClick = {}
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

    private fun showDialog(
        title: String,
        message: String,
        positiveButtonText: String,
        onPositiveClick: () -> Unit
    ) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveButtonText) { _, _ -> onPositiveClick() }
            .show()
    }

}
