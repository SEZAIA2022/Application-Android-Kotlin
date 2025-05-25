package com.houssein.sezaia.ui.screen

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.houssein.sezaia.R
import com.houssein.sezaia.ui.utils.UIUtils

class ChangeNumberActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_change_number)
        // Appliquer les insets des barres système
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))
        // Configuration de la toolbar
        UIUtils.initToolbar(
            this,getString(R.string.change_number), actionIconRes = R.drawable.baseline_local_phone_24, onBackClick = {finish()},
            onActionClick = { recreate() }
        )
    }
}