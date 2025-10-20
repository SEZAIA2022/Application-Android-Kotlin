package com.houssein.sezaia.ui.screen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.messaging.FirebaseMessaging
import com.houssein.sezaia.R
import com.houssein.sezaia.model.data.MyApp
import com.houssein.sezaia.model.request.LoginRequest
import com.houssein.sezaia.model.response.LoginResponse
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.utils.UIUtils
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : BaseActivity() {

    private lateinit var scroll: NestedScrollView

    private lateinit var usernameEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var usernameLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var btnLogin: Button
    private lateinit var btnSignUp: Button
    private lateinit var inputFields: List<Pair<TextInputEditText, TextInputLayout>>

    var targetActivity: Class<out Activity>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ajuster la zone utile quand le clavier apparaît
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        setContentView(R.layout.activity_login)
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))

        scroll = findViewById(R.id.scroll)
        applyImeAndSystemBarsPadding(scroll)   // padding bas dynamique

        initViews()
        attachFocusAutoScroll()                // auto-scroll sur focus

        UIUtils.initToolbar(
            this,
            getString(R.string.login),
            actionIconRes = R.drawable.baseline_density_medium_24,
            onBackClick = { finish() },
            onActionClick = { startActivity(Intent(this, SettingsActivity::class.java)) }
        )

        setupListeners()
    }

    private fun initViews() {
        usernameEditText = findViewById(R.id.username)
        passwordEditText = findViewById(R.id.password)
        usernameLayout = findViewById(R.id.usernameLayout)
        passwordLayout = findViewById(R.id.passwordLayout)
        btnLogin = findViewById(R.id.btnlogin)
        btnSignUp = findViewById(R.id.btnsign_up)

        inputFields = listOf(
            usernameEditText to usernameLayout,
            passwordEditText to passwordLayout
        )

        inputFields.firstOrNull()?.first?.requestFocus()
        UIUtils.hideShowPassword(this, passwordEditText)
    }

    private fun setupListeners() {
        findViewById<TextView>(R.id.forgotPassword).apply {
            paint.isUnderlineText = true
            setOnClickListener {
                showDialog(
                    title = "Forget password",
                    message = "Select the method to reset your password.",
                    positiveButtonText = "Email",
                    onPositiveClick = {
                        getSharedPreferences("MethodePrefs", Context.MODE_PRIVATE).edit {
                            putString("methode", "Email")
                        }
                        startActivity(Intent(context, ForgetActivity::class.java))
                    },
                    negativeButtonText = "Cancel",
                    onNegativeClick = { },
                    cancelable = true
                )
            }
        }

        btnSignUp.setOnClickListener { startActivity(Intent(this, SignUpActivity::class.java)) }
        btnLogin.setOnClickListener { loginCheck() }

        inputFields.forEach { (et, layout) ->
            et.addTextChangedListener(UIUtils.inputWatcher(et, layout))
        }
    }

    override fun onStart() {
        super.onStart()
        resetInputStyles(R.color.gray, clear = true, inputFields)
    }

    override fun onResume() {
        super.onResume()
        resetInputStyles(R.color.gray, clear = false, inputFields)
    }

    // ---------- Scroll & clavier (mêmes helpers que SignUp) ----------

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

    private fun attachFocusAutoScroll() {
        val margin = dp(12f)
        inputFields.map { it.first }.forEach { edit ->
            edit.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) v.post { ensureVisible(scroll, v, margin) }
            }
        }
    }

    private fun ensureVisible(nsv: NestedScrollView, child: View, margin: Int) {
        val rect = Rect()
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

    // ---------- Login ----------

    private fun loginCheck() {
        val username = usernameEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val app = applicationContext as MyApp
        val name = app.application_name
        val type = app.application_type

        if (username.isEmpty() || password.isEmpty()) {
            showDialog(
                title = "Error",
                message = "Username and password are required.",
                negativeButtonText = "OK",
                onNegativeClick = { },
                cancelable = true
            )
            usernameEditText.requestFocus()
            return
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                showDialog(
                    title = "Error",
                    message = "Unable to get device token.",
                    negativeButtonText = "OK",
                    onNegativeClick = { },
                    cancelable = true
                )
                return@addOnCompleteListener
            }

            val fcmToken = task.result ?: ""
            val req = LoginRequest(username, password, name, fcmToken)

            RetrofitClient.instance.login(req).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.let { body ->
                            val role = body.role.lowercase()
                            val typeApp = type

                            targetActivity =
                                when (typeApp) {
                                    "direct" -> if (role == "user") RequestInterventionDirectActivity::class.java else DailyInterventionActivity::class.java
                                    "scan"   -> if (role == "user") CameraActivity::class.java else DailyInterventionActivity::class.java
                                    "both"   -> if (role == "user") InterventionActivity::class.java else DailyInterventionActivity::class.java
                                    else     -> null
                                }

                            getSharedPreferences("LoginData", MODE_PRIVATE).edit {
                                putString("loggedUsername", body.user)
                                putString("LoggedEmail", body.email)
                                putBoolean("isLoggedIn", true)
                                putString("userRole", role)
                            }

                            if (targetActivity != null) {
                                startActivity(Intent(this@LoginActivity, targetActivity))
                            } else {
                                showDialog("Error", "Role unknown: $role", negativeButtonText = "OK")
                            }
                        } ?: showDialog("Error", "Empty response from the server.", negativeButtonText = "OK")
                    } else {
                        resetInputStyles(R.color.red, clear = true, inputFields)
                        usernameLayout.error = "\u00A0"
                        passwordLayout.error = "\u00A0"

                        val errorMessage = try {
                            response.errorBody()?.string()?.let { JSONObject(it).getString("message") }
                                ?: "Unknown error"
                        } catch (e: Exception) {
                            "Network Error : ${response.code()}"
                        }

                        showDialog(
                            title = "Connection failure",
                            message = errorMessage,
                            negativeButtonText = "OK",
                            onNegativeClick = { },
                            cancelable = true
                        )
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    showDialog("Connection failure", t.localizedMessage ?: "Unknown error", negativeButtonText = "OK")
                }
            })
        }
    }
}
