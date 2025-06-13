package com.houssein.sezaia.ui.screen

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.houssein.sezaia.R
import com.houssein.sezaia.model.response.Repair
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.adapter.RepairAdapter
import com.houssein.sezaia.ui.utils.UIUtils
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : BaseActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RepairAdapter
    private lateinit var spinnerSortDate: Spinner
    private lateinit var spinnerFilterStatus: Spinner

    private var allRepairs: List<Repair> = emptyList()

    // Format de date + heure attendu
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        UIUtils.initToolbar(
            this,
            getString(R.string.history),
            actionIconRes = R.drawable.baseline_history_24,
            onBackClick = { finish() },
            onActionClick = { recreate() }
        )

        recyclerView = findViewById(R.id.repairRecyclerView)
        spinnerSortDate = findViewById(R.id.spinnerSortDate)
        spinnerFilterStatus = findViewById(R.id.spinnerFilterStatus)

        recyclerView.layoutManager = LinearLayoutManager(this)

        setupSpinners()

        lifecycleScope.launch {
            try {
                allRepairs = RetrofitClient.instance.getRepairs()
                updateFilteredList()
            } catch (e: Exception) {
                Toast.makeText(this@HistoryActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupSpinners() {
        val dateOptions = listOf("Date ↑", "Date ↓")
        val statusOptions = listOf("All", "processing", "repaired")

        val dateAdapter = ArrayAdapter(this, R.layout.spinner_item, dateOptions).apply {
            setDropDownViewResource(R.layout.spinner_dropdown_item)
        }

        val statusAdapter = ArrayAdapter(this, R.layout.spinner_item, statusOptions).apply {
            setDropDownViewResource(R.layout.spinner_dropdown_item)
        }

        spinnerSortDate.adapter = dateAdapter
        spinnerFilterStatus.adapter = statusAdapter

        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateFilteredList()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerSortDate.onItemSelectedListener = listener
        spinnerFilterStatus.onItemSelectedListener = listener
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
                append("Date: ${repair.date}\n")
                repair.hour_slot?.let { append("Hour: $it\n") }
                append("QR Code: ${repair.qr_code}\n")
                repair.comment?.let { append("Comment: $it\n") }
                append("Status: ${repair.status}\n")
            }

            showDialog(
                title = "Repair details",
                message = message,
                positiveButtonText = "OK",
                onPositiveClick = {}
            )
        }

        recyclerView.adapter = adapter
    }



    // Fonction pour parser date + heure en Date Java
    private fun parseDate(dateStr: String): Date? {
        return try {
            val format = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.ENGLISH)
            format.parse(dateStr)
        } catch (e: ParseException) {
            null
        }
    }

}
