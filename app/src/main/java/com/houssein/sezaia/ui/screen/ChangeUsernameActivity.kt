package com.houssein.sezaia.ui.screen

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.houssein.sezaia.R
import com.houssein.sezaia.ui.utils.UIUtils

class ChangeUsernameActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_change_username)
        // Appliquer les insets des barres système
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))
        // Configuration de la toolbar
        UIUtils.initToolbar(
            this,getString(R.string.change_username), actionIconRes = R.drawable.baseline_account_circle_24, onBackClick = {finish()},
            onActionClick = {recreate() }
        )
    }
}