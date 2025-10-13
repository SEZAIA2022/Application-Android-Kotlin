package com.houssein.sezaia.ui.screen

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.houssein.sezaia.R
import com.houssein.sezaia.model.data.MyApp
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.utils.UIUtils
import java.util.Locale

class MainActivity : BaseActivity() {

    private lateinit var btnLogin: Button
    private lateinit var btnSignUp: Button
    private var targetActivity: Class<out Activity>? = null

    companion object {
        private const val REQUEST_CODE_CAMERA = 1001
        private const val REQUEST_CODE_NOTIFICATIONS = 1002

        // Attente max pour que application_type soit prêt (2s total)
        private const val TYPE_MAX_TRIES = 10
        private const val TYPE_TRY_INTERVAL_MS = 200L
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

    // 1) Permission caméra
    private fun checkAndRequestCameraPermission() {
        val permission = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), REQUEST_CODE_CAMERA)
        } else {
            checkAndRequestNotificationPermission()
        }
    }

    // 2) Permission notifications (Android 13+)
    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(permission), REQUEST_CODE_NOTIFICATIONS)
            } else {
                continueToMain()
            }
        } else {
            continueToMain()
        }
    }

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
                    finishAffinity()
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

    // ===================== NAVIGATION ROBUSTE =====================

    private fun continueToMain() {
        val prefs = getSharedPreferences("LoginData", MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("isLoggedIn", false)
        val role = prefs.getString("userRole", null)

        setupListeners() // Toujours prêt pour login/signup

        if (!isLoggedIn || role == null) return

        // On attend que le type soit prêt (prefs -> MyApp -> retry court)
        waitUntilTypeReady(
            onReady = { typeFinal ->
                decideAndNavigate(role, typeFinal)
            },
            onTimeout = {
                // En dernier recours, on peut choisir un défaut "scan" pour éviter blocage
                Log.w("MyApp", "application_type not ready, using fallback 'scan'")
                decideAndNavigate(role, "scan")
            }
        )
    }

    /**
     * Attend jusqu'à TYPE_MAX_TRIES * TYPE_TRY_INTERVAL_MS que application_type soit dispo.
     * Source de vérité:
     * 1) SharedPreferences("LoginData").getString("application_type")
     * 2) (applicationContext as MyApp).application_type
     */
    private fun waitUntilTypeReady(
        tries: Int = 0,
        onReady: (String) -> Unit,
        onTimeout: () -> Unit
    ) {
        val prefs = getSharedPreferences("LoginData", MODE_PRIVATE)
        val fromPrefs = prefs.getString("application_type", null)
        val fromApp = (applicationContext as? MyApp)?.application_type

        val candidate = (fromPrefs ?: fromApp)?.trim()
        if (!candidate.isNullOrEmpty()) {
            onReady(candidate)
            return
        }

        if (tries >= TYPE_MAX_TRIES) {
            onTimeout()
            return
        }

        Handler(Looper.getMainLooper()).postDelayed({
            waitUntilTypeReady(tries + 1, onReady, onTimeout)
        }, TYPE_TRY_INTERVAL_MS)
    }

    private fun decideAndNavigate(roleRaw: String, typeRaw: String) {
        val roleNorm = roleRaw.trim().lowercase(Locale.ROOT)
        val typeNorm = typeRaw.trim().lowercase(Locale.ROOT)

        targetActivity = when (typeNorm) {
            "direct" -> if (roleNorm == "user") RequestInterventionDirectActivity::class.java else CameraActivity::class.java
            "scan"   -> if (roleNorm == "user" || roleNorm == "admin") CameraActivity::class.java else null
            "both"   -> if (roleNorm == "user") InterventionActivity::class.java else CameraActivity::class.java
            else     -> null
        }

        Log.d("MyApp", "DECIDE roleRaw=$roleRaw, role=$roleNorm, typeRaw=$typeRaw, type=$typeNorm, target=$targetActivity")

        targetActivity?.let { clazz ->
            // On passe le snapshot utilisé pour éviter toute future divergence
            val i = Intent(this@MainActivity, clazz).apply {
                putExtra("EXTRA_ROLE", roleNorm)
                putExtra("EXTRA_TYPE", typeNorm)
            }
            startActivity(i)
            finish()
        } ?: run {
            showDialog("Error", "Unknown role/type: role=$roleNorm, type=$typeNorm", negativeButtonText = "OK")
        }
    }

    // ===================== UI =====================

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
