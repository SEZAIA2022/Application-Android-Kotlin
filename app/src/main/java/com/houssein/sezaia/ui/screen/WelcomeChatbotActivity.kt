package com.houssein.sezaia.ui.screen

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import com.houssein.sezaia.R
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.utils.UIUtils

class WelcomeChatbotActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_welcome_chatbot)

        // Appliquer les insets des barres système
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))

        UIUtils.initToolbar(
            this,getString(R.string.chatbot), actionIconRes = R.drawable.baseline_density_medium_24, onBackClick = {finish()},
            onActionClick = { startActivity(Intent(this, SettingsActivity::class.java)) }
        )

        val btnContinueToChatbot: Button = findViewById(R.id.btnContinueToChat)
        btnContinueToChatbot.setOnClickListener {
            navigateToChatbot()
        }
    }

    // Navigation vers l'activité Settings
    private fun navigateToSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    // Navigation vers l'activité Chatbot
    private fun navigateToChatbot() {
        val intent = Intent(this, ChatbotActivity::class.java)
        startActivity(intent)
    }
}
