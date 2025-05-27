package com.houssein.sezaia.ui.screen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.houssein.sezaia.R
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.utils.UIUtils

class MainActivity : BaseActivity() {

    private lateinit var btnLogin: Button
    private lateinit var btnSignUp: Button
    private lateinit var backButton: ImageView
    private lateinit var actionButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Appliquer les insets des barres système
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))
        requestCameraPermissionIfNeeded()
        initViews()
        setupListeners()
    }

    private fun requestCameraPermissionIfNeeded() {
        val permission = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 1001)
        }
    }

    private fun initViews() {
        backButton = findViewById(R.id.toolbar_back)
        actionButton = findViewById(R.id.toolbar_action)
        btnLogin = findViewById(R.id.btnlogin)
        btnSignUp = findViewById(R.id.btnsignup)
    }

    private fun setupListeners() {
        backButton.setOnClickListener {
            showDialog("Are you sure?",
                "press leave to quit",
                positiveButtonText = "Leave",
                onPositiveClick = { finish() },
                negativeButtonText = "Cancel",
                onNegativeClick = {},
                cancelable = false)
        }
        actionButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        btnLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
        btnSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()
        getSharedPreferences("MyPrefs", MODE_PRIVATE)
            .edit()
            .putBoolean("showCardsInSettings", false)
            .apply()
    }
}
