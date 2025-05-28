package com.houssein.sezaia.ui.screen

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import com.houssein.sezaia.R
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.utils.UIUtils

class WelcomeAdminActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_welcome_admin)

        // Appliquer les insets des barres système
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))

        UIUtils.initToolbar(
            this, getString(R.string.welcome_admin),
            onBackClick = { finish() },
            onActionClick = { startActivity(Intent(this, SettingsActivity::class.java)) }
        )

        // 🔵 Récupération et affichage du username
        val username = getSharedPreferences("MyPrefs", MODE_PRIVATE)
            .getString("loggedUsername", "Username not found")//si existe pas retourne Username not found

        val email = getSharedPreferences("MyPrefs", MODE_PRIVATE)
            .getString("LoggedEmail", "Email not found")

        findViewById<TextView>(R.id.usernameText).text = "Bienvenue, $username, $email 👋"
    }
}
