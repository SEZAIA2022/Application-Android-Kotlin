package com.houssein.sezaia.ui.screen

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.houssein.sezaia.R
import com.houssein.sezaia.ui.utils.UIUtils

class AboutUsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_about_us)
        // Appliquer les insets des barres système
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))
        // Configuration de la toolbar
        UIUtils.initToolbar(
            this,getString(R.string.about_us),actionIconRes = R.drawable.baseline_info_outline_24, onBackClick = {finish()},
            onActionClick = { recreate() }
        )
    }
}