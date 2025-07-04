package com.houssein.sezaia.ui.screen

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.houssein.sezaia.R
import com.houssein.sezaia.model.response.PrivacyPolicyResponse
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.utils.UIUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PrivacyPolicyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_privacy_policy)

        val privacyTextView = findViewById<TextView>(R.id.privacy_policy_text)

        // Appliquer les insets des barres système
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))

        // Configuration de la toolbar
        UIUtils.initToolbar(
            this,
            getString(R.string.privacy_policy),
            actionIconRes = R.drawable.outline_lock_24,
            onBackClick = { finish() },
            onActionClick = { recreate() }
        )

        // Appel Retrofit pour récupérer le contenu de la politique de confidentialité
        RetrofitClient.instance.getPrivacyPolicy().enqueue(object : Callback<PrivacyPolicyResponse> {
            override fun onResponse(
                call: Call<PrivacyPolicyResponse>,
                response: Response<PrivacyPolicyResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    privacyTextView.text = response.body()!!.privacy_policy
                } else {
                    privacyTextView.text = getString(R.string.data_not_found)
                }
            }

            override fun onFailure(call: Call<PrivacyPolicyResponse>, t: Throwable) {
                Log.e("PrivacyPolicyActivity", "Erreur réseau : ${t.message}")
                privacyTextView.text = getString(R.string.connection_error)
            }
        })
    }
}
