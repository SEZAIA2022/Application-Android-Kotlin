package com.houssein.sezaia.ui.screen

import android.content.Intent
import android.os.Bundle
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
            this,getString(R.string.welcome_admin), onBackClick = {finish()},
            onActionClick = { startActivity(Intent(this, SettingsActivity::class.java)) }
        )
    }
}