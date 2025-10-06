package com.houssein.sezaia.ui.screen

import android.content.Intent
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
import com.hbb20.CountryCodePicker
import com.houssein.sezaia.R
import com.houssein.sezaia.model.data.MyApp
import com.houssein.sezaia.model.request.ChangeNumberRequest
import com.houssein.sezaia.model.response.ChangeNumberResponse
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.utils.UIUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChangeNumberActivity : BaseActivity() {

    private lateinit var scroll: NestedScrollView

    private lateinit var oldNumberEditText: TextInputEditText
    private lateinit var newNumberEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText

    private lateinit var oldCcPicker: CountryCodePicker
    private lateinit var newCcPicker: CountryCodePicker

    private lateinit var oldNumberLayout: TextInputLayout
    private lateinit var newNumberLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout

    private lateinit var btnChangeNumber: Button
    private lateinit var inputFields: List<Pair<TextInputEditText, TextInputLayout>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Redimensionner avec le clavier
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        setContentView(R.layout.activity_change_number)
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))

        // Toolbar
        UIUtils.initToolbar(
            this,
            getString(R.string.change_number),
            actionIconRes = R.drawable.baseline_local_phone_24,
            onBackClick = { finish() },
            onActionClick = { recreate() }
        )

        scroll = findViewById(R.id.scroll)
        applyImeAndSystemBarsPadding(scroll)

        initViews()
        attachFocusAutoScroll()
        setupListeners()
    }

    private fun initViews() {
        oldNumberEditText = findViewById(R.id.oldPhoneNumber)
        newNumberEditText = findViewById(R.id.newPhoneNumber)
        passwordEditText = findViewById(R.id.password)

        oldCcPicker = findViewById(R.id.oldCountryCodePicker)
        newCcPicker = findViewById(R.id.newCountryCodePicker)

        oldNumberLayout = findViewById(R.id.oldPhoneLayout)
        newNumberLayout = findViewById(R.id.newPhoneLayout)
        passwordLayout = findViewById(R.id.passwordLayout)

        btnChangeNumber = findViewById(R.id.btnChangeNumber)

        inputFields = listOf(
            oldNumberEditText to oldNumberLayout,
            newNumberEditText to newNumberLayout,
            passwordEditText to passwordLayout
        )
        inputFields.firstOrNull()?.first?.requestFocus()

        UIUtils.hideShowPassword(this, passwordEditText)
        inputFields.forEach { (et, layout) ->
            et.addTextChangedListener(UIUtils.inputWatcher(et, layout))
        }
    }

    private fun setupListeners() {
        btnChangeNumber.setOnClickListener {
            if (UIUtils.validateInputs(inputFields)) changeNumber()
        }
    }

    // ===== clavier & scroll =====

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

    // ===== API =====

    private fun changeNumber() {
        val oldNumber = oldNumberEditText.text.toString().trim()
        val newNumber = newNumberEditText.text.toString().trim()
        val oldPostalCode = oldCcPicker.selectedCountryCodeWithPlus
        val newPostalCode = newCcPicker.selectedCountryCodeWithPlus
        val password = passwordEditText.text.toString()
        val applicationName = (application as MyApp).application_name

        val req = ChangeNumberRequest(
            oldPostalCode, oldNumber, newPostalCode, newNumber, password, applicationName
        )

        RetrofitClient.instance.changeNumber(req).enqueue(object : Callback<ChangeNumberResponse> {
            override fun onResponse(
                call: Call<ChangeNumberResponse>,
                response: Response<ChangeNumberResponse>
            ) {
                if (response.isSuccessful) {
                    val msg = response.body()?.message ?: getString(R.string.done)
                    Toast.makeText(this@ChangeNumberActivity, msg, Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@ChangeNumberActivity, ProfileActivity::class.java))
                    finish()
                } else {
                    resetInputStyles(R.color.red, clear = true, inputFields)
                    oldNumberLayout.error = " "
                    newNumberLayout.error = " "
                    passwordLayout.error = " "
                    val msg = UIUtils.parseErrorMessage(response)
                    showDialog("Number not changed", msg, negativeButtonText = "OK", cancelable = true)
                }
            }

            override fun onFailure(call: Call<ChangeNumberResponse>, t: Throwable) {
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
