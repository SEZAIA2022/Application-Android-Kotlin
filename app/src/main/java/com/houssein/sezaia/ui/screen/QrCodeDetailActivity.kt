package com.houssein.sezaia.ui.screen

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.houssein.sezaia.R
import com.houssein.sezaia.ui.utils.UIUtils

class QrCodeDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_code_detail)
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))

        // Initialisation de la toolbar
        UIUtils.initToolbar(
            this,getString(R.string.qr_code_details), actionIconRes = R.drawable.baseline_settings_24, onBackClick = {finish()},
            onActionClick = {recreate() }
        )
        val qrCode = intent.getStringExtra("qr_code")
        val qrCodeTextView: TextView = findViewById(R.id.qrCodeDetailText)
        qrCodeTextView.text = qrCode ?: "Aucun QR code trouvé"
    }
}
