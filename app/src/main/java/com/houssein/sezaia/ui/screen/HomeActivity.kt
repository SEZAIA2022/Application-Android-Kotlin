package com.houssein.sezaia.ui.screen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.houssein.sezaia.R
import com.houssein.sezaia.model.data.MyApp
import com.houssein.sezaia.model.response.AppNameTypeResponse
import com.houssein.sezaia.model.response.BaseResponse
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.utils.UIUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class HomeActivity : BaseActivity() {

    private lateinit var scroll: NestedScrollView
    private lateinit var companyLayout: TextInputLayout
    private lateinit var companyInput: TextInputEditText
    private lateinit var confirmBtn: MaterialButton

    private var noInternetDialog: AlertDialog? = null

    companion object {
        private const val REQUEST_CODE_CAMERA = 1001
        private const val REQUEST_CODE_NOTIFICATIONS = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)

        // S’adapte quand le clavier apparaît
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        setContentView(R.layout.activity_home)
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))

        UIUtils.initToolbar(
            this,
            getString(R.string.app_name),
            actionIconRes = R.drawable.baseline_density_medium_24,
            onBackClick = backButtonClickListener,
            onActionClick = { startActivity(Intent(this, SettingsActivity::class.java)) }
        )

        // Views
        scroll        = findViewById(R.id.scroll)
        companyLayout = findViewById(R.id.companyLayout)
        companyInput  = findViewById(R.id.company)
        confirmBtn    = findViewById(R.id.btnconfirmCompany)

        // Padding bas dynamique = max(system bars, clavier)
        applyImeAndSystemBarsPadding(scroll)

        // Scroll doux vers le champ quand il prend le focus
        attachFocusAutoScroll(companyInput)

        companyInput.setText("")

        checkAndRequestCameraPermission()

        if (!isOnline()) showNoInternetDialog()

        confirmBtn.setOnClickListener { onConfirmCompany() }
    }

    override fun onDestroy() {
        super.onDestroy()
        noInternetDialog?.dismiss()
        noInternetDialog = null
    }

    // ---------- Insets & scroll ----------
    private fun applyImeAndSystemBarsPadding(target: View) {
        val l = target.paddingLeft
        val t = target.paddingTop
        val r = target.paddingRight
        val baseB = target.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(target) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.updatePadding(left = l, top = t, right = r, bottom = baseB + maxOf(sys.bottom, ime.bottom))
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun attachFocusAutoScroll(editText: View) {
        val margin = dp(12f)
        editText.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) v.post { ensureVisible(scroll, v, margin) }
        }
    }

    private fun ensureVisible(nsv: NestedScrollView, child: View, margin: Int) {
        val rect = android.graphics.Rect()
        child.getDrawingRect(rect)
        nsv.offsetDescendantRectToMyCoords(child, rect)

        val top = nsv.scrollY + nsv.paddingTop
        val bottom = nsv.scrollY + nsv.height - nsv.paddingBottom

        when {
            rect.top - margin < top ->
                nsv.smoothScrollTo(0, rect.top - nsv.paddingTop - margin)
            rect.bottom + margin > bottom -> {
                val y = rect.bottom - (nsv.height - nsv.paddingBottom) + margin
                nsv.smoothScrollTo(0, y)
            }
        }
    }

    private fun dp(v: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics).toInt()

    // ---------- Actions ----------
    private fun onConfirmCompany() {
        companyLayout.error = null
        val newName = companyInput.text?.toString()?.trim().orEmpty()

        if (newName.isBlank()) {
            companyLayout.error = getString(R.string.all_fields_required)
            companyInput.requestFocus()
            return
        }
        if (!isOnline()) {
            showNoInternetDialog()
            return
        }

        hideKeyboard(companyInput)
        confirmBtn.isEnabled = false

        val body = mapOf("application_name" to newName.lowercase())
        RetrofitClient.instance.existCompany(body)
            .enqueue(object : Callback<BaseResponse> {
                override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                    if (!response.isSuccessful) {
                        companyInput.setText("")
                        if (response.code() == 404) {
                            companyLayout.error = "Unknown company"
                            companyInput.requestFocus()
                        } else {
                            Toast.makeText(this@HomeActivity, "Server error (${response.code()})", Toast.LENGTH_LONG).show()
                        }
                        confirmBtn.isEnabled = true
                        return
                    }

                    val br = response.body()
                    if (br?.status != "success") {
                        companyInput.setText("")
                        companyLayout.error = br?.message ?: "Unknown company"
                        companyInput.requestFocus()
                        confirmBtn.isEnabled = true
                        return
                    }

                    fetchAppNameTypeAndSave(newName)
                }

                override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                    confirmBtn.isEnabled = true
                    companyInput.setText("")
                    if (!isOnline()) showNoInternetDialog()
                    else Toast.makeText(this@HomeActivity, getString(R.string.network_error, t.localizedMessage), Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun fetchAppNameTypeAndSave(appName: String) {
        val app = application as MyApp

        RetrofitClient.instance.getAppNameType(appName)
            .enqueue(object : Callback<AppNameTypeResponse> {
                override fun onResponse(call: Call<AppNameTypeResponse>, response: Response<AppNameTypeResponse>) {
                    confirmBtn.isEnabled = true

                    if (!response.isSuccessful) {
                        companyInput.setText("")
                        Toast.makeText(this@HomeActivity, "Server error (${response.code()})", Toast.LENGTH_LONG).show()
                        return
                    }

                    val body = response.body()
                    if (body?.status == "success" && body.data != null) {
                        val finalName = body.data.application?.toString().orEmpty()
                        val finalType = body.data.type?.toString().orEmpty()

                        app.setApplicationInfo(finalName, finalType, from = "api")

                        val prefs = getSharedPreferences("LoginData", MODE_PRIVATE)
                        val isLoggedIn = prefs.getBoolean("isLoggedIn", false)
                        val role = prefs.getString("userRole", null)?.trim()?.lowercase(Locale.ROOT)

                        if (isLoggedIn && !role.isNullOrEmpty()) {
                            decideAndNavigate(role, app.application_type)
                        } else {
                            startActivity(Intent(this@HomeActivity, MainActivity::class.java))
                            companyInput.setText("")
                        }
                    } else {
                        companyInput.setText("")
                        Toast.makeText(this@HomeActivity, "Data not found for $appName", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<AppNameTypeResponse>, t: Throwable) {
                    confirmBtn.isEnabled = true
                    companyInput.setText("")
                    if (!isOnline()) showNoInternetDialog()
                    else Toast.makeText(this@HomeActivity, getString(R.string.network_error, t.localizedMessage), Toast.LENGTH_LONG).show()
                }
            })
    }

    // ---------- Routage ----------
    private fun decideAndNavigate(roleRaw: String, typeRaw: String) {
        val role = roleRaw.trim().lowercase(Locale.ROOT)
        val type = typeRaw.trim().lowercase(Locale.ROOT)

        val target = when (type) {
            "direct" -> if (role == "user") RequestInterventionDirectActivity::class.java else CameraActivity::class.java
            "scan"   -> if (role == "user" || role == "admin") CameraActivity::class.java else null
            "both"   -> if (role == "user") InterventionActivity::class.java else CameraActivity::class.java
            else     -> null
        }

        Log.d("MyApp", "DECIDE role=$role, type=$type, target=$target")

        if (target != null) {
            val i = Intent(this@HomeActivity, target).apply {
                putExtra("EXTRA_ROLE", role)
                putExtra("EXTRA_TYPE", type)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(i)
            finish()
        } else {
            showDialog("Error", "Unknown role/type: role=$role, type=$type", negativeButtonText = "OK")
        }
    }

    // ---------- Permissions ----------
    private fun checkAndRequestCameraPermission() {
        val permission = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), REQUEST_CODE_CAMERA)
        } else {
            checkAndRequestNotificationPermission()
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(permission), REQUEST_CODE_NOTIFICATIONS)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_CAMERA &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkAndRequestNotificationPermission()
        } else if (requestCode == REQUEST_CODE_CAMERA) {
            Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_LONG).show()
        }
    }

    // ---------- Divers ----------
    private fun isOnline(): Boolean {
        val cm = getSystemService<ConnectivityManager>() ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun showNoInternetDialog() {
        if (noInternetDialog?.isShowing == true) return

        noInternetDialog = MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.no_internet_title))
            .setMessage(getString(R.string.no_internet_message))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.retry)) { _, _ -> relaunchApp() }
            .create().also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        ?.setTextColor(ContextCompat.getColor(this, R.color.blue))
                }
                dialog.show()
            }
    }

    private fun relaunchApp() {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        if (launchIntent != null) {
            startActivity(launchIntent)
            finishAffinity()
            overridePendingTransition(0, 0)
        } else {
            recreate()
        }
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

    private fun hideKeyboard(view: View) {
        val imm = getSystemService<InputMethodManager>()
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
