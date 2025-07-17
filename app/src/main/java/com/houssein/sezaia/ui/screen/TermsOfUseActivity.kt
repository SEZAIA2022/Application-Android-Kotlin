package com.houssein.sezaia.ui.screen

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.houssein.sezaia.R
import com.houssein.sezaia.model.data.MyApp
import com.houssein.sezaia.model.response.TermsOfUseResponse
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.utils.UIUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TermsOfUseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_terms_of_use)

        val textView = findViewById<TextView>(R.id.terms_of_use_text)

        // Appliquer les insets des barres système
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))

        // Configuration de la toolbar
        UIUtils.initToolbar(
            this,
            getString(R.string.terms_of_use),
            actionIconRes = R.drawable.outline_insert_drive_file_24,
            onBackClick = { finish() },
            onActionClick = { recreate() }
        )
        val app = application as MyApp
        val applicationName = app.application_name
        // Chargement du contenu depuis l’API
        RetrofitClient.instance.getTermsOfUse(applicationName).enqueue(object : Callback<TermsOfUseResponse> {
            override fun onResponse(
                call: Call<TermsOfUseResponse>,
                response: Response<TermsOfUseResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    textView.text = response.body()!!.term_of_use
                } else {
                    textView.text = getString(R.string.data_not_found)
                }
            }

            override fun onFailure(call: Call<TermsOfUseResponse>, t: Throwable) {
                Log.e("TermsOfUseActivity", "Erreur réseau : ${t.message}")
                textView.text = getString(R.string.connection_error)
            }
        })
    }
}
