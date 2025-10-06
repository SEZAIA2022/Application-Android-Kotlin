package com.houssein.sezaia.ui.screen

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.houssein.sezaia.R
import com.houssein.sezaia.model.data.MyApp
import com.houssein.sezaia.model.request.ChangePasswordRequest
import com.houssein.sezaia.model.response.ChangePasswordResponse
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.utils.UIUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChangePasswordActivity : BaseActivity() {

    private lateinit var scroll: NestedScrollView

    private lateinit var emailEditText: TextInputEditText
    private lateinit var oldPasswordEditText: TextInputEditText
    private lateinit var newPasswordEditText: TextInputEditText
    private lateinit var confirmNewPasswordEditText: TextInputEditText

    private lateinit var emailLayout: TextInputLayout
    private lateinit var oldPasswordLayout: TextInputLayout
    private lateinit var newPasswordLayout: TextInputLayout
    private lateinit var confirmNewPasswordLayout: TextInputLayout

    private lateinit var btnChangePassword: Button
    private lateinit var inputFields: List<Pair<TextInputEditText, TextInputLayout>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // redimensionner l'UI quand le clavier apparaît
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        setContentView(R.layout.activity_change_password)
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))

        // Toolbar
        UIUtils.initToolbar(
            this,
            getString(R.string.change_password),
            actionIconRes = R.drawable.outline_lock_24,
            onBackClick = { finish() },
            onActionClick = { recreate() }
        )

        scroll = findViewById(R.id.scroll)
        applyImeAndSystemBarsPadding(scroll)

        initViews()
        attachFocusAutoScroll() // <- auto scroll ciblé quand un champ reçoit le focus
        setupListeners()
    }

    private fun initViews() {
        emailEditText = findViewById(R.id.email)
        emailLayout = findViewById(R.id.emailLayout)

        oldPasswordEditText = findViewById(R.id.oldPassword)
        oldPasswordLayout = findViewById(R.id.OldPasswordLayout)

        newPasswordEditText = findViewById(R.id.newPassword)
        newPasswordLayout = findViewById(R.id.newPasswordLayout)

        confirmNewPasswordEditText = findViewById(R.id.confirmNewPassword)
        confirmNewPasswordLayout = findViewById(R.id.confirmNewPasswordLayout)

        btnChangePassword = findViewById(R.id.btnChangePassword)

        inputFields = listOf(
            emailEditText to emailLayout,
            oldPasswordEditText to oldPasswordLayout,
            newPasswordEditText to newPasswordLayout,
            confirmNewPasswordEditText to confirmNewPasswordLayout
        )
        inputFields.firstOrNull()?.first?.requestFocus()

        UIUtils.hideShowPassword(this, oldPasswordEditText)
        UIUtils.hideShowPassword(this, newPasswordEditText)
        UIUtils.hideShowPassword(this, confirmNewPasswordEditText)
    }

    private fun setupListeners() {
        btnChangePassword.setOnClickListener {
            if (UIUtils.validateInputs(inputFields)) changePassword()
        }
        inputFields.forEach { (et, layout) ->
            et.addTextChangedListener(UIUtils.inputWatcher(et, layout))
        }
    }

    // ====== clavier & scroll ======

    private fun applyImeAndSystemBarsPadding(target: View) {
        val baseL = target.paddingLeft
        val baseT = target.paddingTop
        val baseR = target.paddingRight
        val baseB = target.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(target) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val extra = maxOf(sys.bottom, ime.bottom)
            v.updatePadding(left = baseL, top = baseT, right = baseR, bottom = baseB + extra)
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

    // ====== API ======

    private fun changePassword() {
        val email = emailEditText.text.toString().trim()
        val oldPassword = oldPasswordEditText.text.toString()
        val newPassword = newPasswordEditText.text.toString()
        val confirmNewPassword = confirmNewPasswordEditText.text.toString()
        val appName = (application as MyApp).application_name

        val req = ChangePasswordRequest(email, oldPassword, newPassword, confirmNewPassword, appName)

        RetrofitClient.instance.changePassword(req).enqueue(object : Callback<ChangePasswordResponse> {
            override fun onResponse(
                call: Call<ChangePasswordResponse>,
                response: Response<ChangePasswordResponse>
            ) {
                if (response.isSuccessful) {
                    val msg = response.body()?.message ?: getString(R.string.done)
                    Toast.makeText(this@ChangePasswordActivity, msg, Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@ChangePasswordActivity, ProfileActivity::class.java))
                    finish()
                } else {
                    resetInputStyles(R.color.red, clear = true, inputFields)
                    emailLayout.error = " "
                    oldPasswordLayout.error = " "
                    newPasswordLayout.error = " "
                    confirmNewPasswordLayout.error = " "
                    val msg = UIUtils.parseErrorMessage(response)
                    showDialog("Password not changed", msg, negativeButtonText = "OK", cancelable = true)
                }
            }

            override fun onFailure(call: Call<ChangePasswordResponse>, t: Throwable) {
                showDialog("Connection failure", t.localizedMessage ?: "Unknown error",
                    negativeButtonText = "OK", cancelable = true)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        resetInputStyles(R.color.gray, clear = false, inputFields)
    }

    override fun onStart() {
        super.onStart()
        resetInputStyles(R.color.gray, clear = true, inputFields)
    }
}
