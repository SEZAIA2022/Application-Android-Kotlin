package com.houssein.sezaia.ui.screen

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.houssein.sezaia.R
import com.houssein.sezaia.model.response.HelpItem
import com.houssein.sezaia.model.response.HelpResponse
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.adapter.HelpAdapter
import com.houssein.sezaia.ui.utils.UIUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HelpActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HelpAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        UIUtils.applySystemBarsInsets(findViewById(R.id.main))
        UIUtils.initToolbar(
            this,
            getString(R.string.help_center),
            actionIconRes = R.drawable.baseline_help_outline_24,
            onBackClick = { finish() },
            onActionClick = { recreate() }
        )

        recyclerView = findViewById(R.id.helpRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        fetchHelpTasks()
    }

    private fun fetchHelpTasks() {
        val call = RetrofitClient.instance.getHelpTasks()
        call.enqueue(object : Callback<HelpResponse> {
            override fun onResponse(call: Call<HelpResponse>, response: Response<HelpResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val helpItems: List<HelpItem> = response.body()!!.tasks
                    adapter = HelpAdapter(helpItems)
                    recyclerView.adapter = adapter
                } else {
                    Toast.makeText(this@HelpActivity, "Erreur de chargement des données", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<HelpResponse>, t: Throwable) {
                Log.e("HelpActivity", "API call failed: ${t.message}")
                Toast.makeText(this@HelpActivity, "Échec de la connexion", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
