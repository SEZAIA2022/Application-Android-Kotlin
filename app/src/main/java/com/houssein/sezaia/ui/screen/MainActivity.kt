package com.houssein.sezaia.ui.screen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.houssein.sezaia.R
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.utils.UIUtils
import androidx.core.content.edit
import com.houssein.sezaia.model.data.MyApp
import java.util.Locale

class MainActivity : BaseActivity() {

    private lateinit var btnLogin: Button
    private lateinit var btnSignUp: Button
    private lateinit var backButton: ImageView
    private lateinit var actionButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)


        val prefs = getSharedPreferences("LoginData", MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("isLoggedIn", false)
        val userRole = prefs.getString("userRole", null)
        val app = application as MyApp
        val applicationName = app.application_name

        if (isLoggedIn && userRole != null) {
            val targetActivity = when (userRole) {
                "user", "admin" -> {
                    CameraActivity::class.java
                }
                else -> {
                    MainActivity::class.java
                }
            }

            startActivity(Intent(this, targetActivity))
        }

        // Appliquer les insets des barres système
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))

        initViews()

        UIUtils.initToolbar(
            this,
            applicationName,
            actionIconRes = R.drawable.baseline_density_medium_24,
            onBackClick = backButtonClickListener,
            onActionClick = actionButtonClickListener
        )


        requestCameraPermissionIfNeeded()
        setupListeners()
    }

    private val backButtonClickListener = {
        showDialog("Are you sure?",
            "press leave to quit",
            positiveButtonText = "Leave",
            onPositiveClick = { finish() },
            negativeButtonText = "Cancel",
            onNegativeClick = {},
            cancelable = false)
    }

    private val actionButtonClickListener = {
        startActivity(Intent(this, SettingsActivity::class.java))
    }


    private fun requestCameraPermissionIfNeeded() {
        val permission = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 1001)
        }
    }

    private fun initViews() {
        btnLogin = findViewById(R.id.btnlogin)
        btnSignUp = findViewById(R.id.btnsignup)
    }

    private fun setupListeners() {

        btnLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
        btnSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

}
