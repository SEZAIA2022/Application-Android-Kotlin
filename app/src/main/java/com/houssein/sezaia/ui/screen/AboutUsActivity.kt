package com.houssein.sezaia.ui.screen

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.houssein.sezaia.R
import com.houssein.sezaia.model.response.AboutUsResponse
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.utils.UIUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AboutUsActivity : AppCompatActivity() {

    private lateinit var aboutText: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Appeler l’API avant affichage
        fetchAboutUsData()
    }

    private fun fetchAboutUsData() {
        RetrofitClient.instance.getAboutUs().enqueue(object : Callback<AboutUsResponse> {
            override fun onResponse(
                call: Call<AboutUsResponse>,
                response: Response<AboutUsResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    aboutText = response.body()!!.about_us
                    initializeUI(aboutText)
                } else {
                    aboutText = "Erreur: contenu indisponible."
                    initializeUI(aboutText)
                }
            }

            override fun onFailure(call: Call<AboutUsResponse>, t: Throwable) {
                Log.e("AboutUs", "Erreur réseau: ${t.message}")
                aboutText = "Erreur de connexion."
                initializeUI(aboutText)
            }
        })
    }

    private fun initializeUI(content: String) {
        setContentView(R.layout.activity_about_us)
        enableEdgeToEdge()

        val aboutUsTextView = findViewById<TextView>(R.id.about_us_text)
        aboutUsTextView.text = content

        UIUtils.applySystemBarsInsets(findViewById(R.id.main))
        UIUtils.initToolbar(
            this,
            getString(R.string.about_us),
            actionIconRes = R.drawable.baseline_info_outline_24,
            onBackClick = { finish() },
            onActionClick = { recreate() }
        )
    }
}
