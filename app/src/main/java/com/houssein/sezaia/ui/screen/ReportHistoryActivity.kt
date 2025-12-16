package com.houssein.sezaia.ui.screen

import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.houssein.sezaia.R
import com.houssein.sezaia.model.response.HistoryResponse
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.adapter.HistoryAdapter
import com.houssein.sezaia.ui.utils.UIUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ReportHistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var adapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_history)

        UIUtils.applySystemBarsInsets(findViewById(R.id.main))
        UIUtils.initToolbar(
            this,
            "Report history",
            onBackClick = { finish() }
        )

        recyclerView = findViewById(R.id.historyRecyclerView)
        emptyState = findViewById(R.id.emptyState)

        recyclerView.layoutManager = LinearLayoutManager(this)

        fetchHistory()
    }

    private fun fetchHistory() {
        val application = intent.getStringExtra("application") ?: ""
        val username = intent.getStringExtra("username")
        val qrCode = intent.getStringExtra("qr_code")

        RetrofitClient.instance
            .getRepportHistory(application, username, qrCode)
            .enqueue(object : Callback<HistoryResponse> {
                override fun onResponse(
                    call: Call<HistoryResponse>,
                    response: Response<HistoryResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val submissions = response.body()!!.data.submissions

                        if (submissions.isEmpty()) {
                            emptyState.visibility = android.view.View.VISIBLE
                            recyclerView.visibility = android.view.View.GONE
                        } else {
                            emptyState.visibility = android.view.View.GONE
                            recyclerView.visibility = android.view.View.VISIBLE

                            adapter = HistoryAdapter(submissions)
                            recyclerView.adapter = adapter
                        }
                    } else {
                        Toast.makeText(
                            this@ReportHistoryActivity,
                            "Error retrieving history",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<HistoryResponse>, t: Throwable) {
                    Log.e("HistoryActivity", "getRepportHistory failed", t)
                    Toast.makeText(
                        this@ReportHistoryActivity,
                        "Network error: ${t.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }
}
