package com.houssein.sezaia.ui.screen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.houssein.sezaia.R
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.utils.UIUtils

class MainActivity : BaseActivity() {

    private lateinit var btnLogin: Button
    private lateinit var btnSignUp: Button

    companion object {
        private const val REQUEST_CODE_CAMERA = 1001
        private const val REQUEST_CODE_NOTIFICATIONS = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)



        UIUtils.applySystemBarsInsets(findViewById(R.id.main))
        initViews()
        UIUtils.initToolbar(
            this,
            "Assist By Scan",
            actionIconRes = R.drawable.baseline_density_medium_24,
            onBackClick = backButtonClickListener,
            onActionClick = actionButtonClickListener
        )

        checkAndRequestCameraPermission()
    }

    // Première vérification : Permission caméra
    private fun checkAndRequestCameraPermission() {
        val permission = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), REQUEST_CODE_CAMERA)
        } else {
            checkAndRequestNotificationPermission()
        }
    }

    // Enchaînement : Permission notification
    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(permission), REQUEST_CODE_NOTIFICATIONS)
            } else {
                // Toutes les permissions OK → continuer normalement
                continueToMain()
            }
        } else {
            continueToMain() // Pas besoin de permission notification < Android 13
        }
    }

    // Gère les réponses des permissions
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_CODE_CAMERA -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkAndRequestNotificationPermission()
                } else {
                    Toast.makeText(this, "Camera permission is required. Closing app.", Toast.LENGTH_LONG).show()
                    finishAffinity() // Ferme complètement l'app
                }
            }

            REQUEST_CODE_NOTIFICATIONS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    continueToMain()
                } else {
                    Toast.makeText(this, "Notification permission is required. Closing app.", Toast.LENGTH_LONG).show()
                    finishAffinity()
                }
            }
        }
    }

    // Si toutes les permissions sont OK → continuer
    private fun continueToMain() {
        val prefs = getSharedPreferences("LoginData", MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("isLoggedIn", false)
        val userRole = prefs.getString("userRole", null)

        if (isLoggedIn && userRole != null) {
            val targetActivity = when (userRole) {
                "user", "admin" -> CameraActivity::class.java
                else -> MainActivity::class.java
            }
            startActivity(Intent(this, targetActivity))
            finish()
        }

        setupListeners()
    }

    private val backButtonClickListener = {
        showDialog(
            "Are you sure?",
            "Press Leave to quit",
            positiveButtonText = "Leave",
            onPositiveClick = { finish() },
            negativeButtonText = "Cancel",
            onNegativeClick = {},
            cancelable = false
        )
    }

    private val actionButtonClickListener = {
        startActivity(Intent(this, SettingsActivity::class.java))
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
