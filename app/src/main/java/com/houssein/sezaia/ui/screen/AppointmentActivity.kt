package com.houssein.sezaia.ui.screen

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import com.houssein.sezaia.R
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.utils.UIUtils

class AppointmentActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_appointment)
        // Appliquer les insets des barres système
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))
        // Configuration de la toolbar
        UIUtils.initToolbar(
            this,getString(R.string.appointment), onBackClick = {finish()},
            onActionClick = { startActivity(Intent(this, SettingsActivity::class.java)) }
        )
        val messageTextView: TextView = findViewById(R.id.messageTextView)
        messageTextView.text = "Merci d'avoir répondu à toutes les questions !"
    }
}