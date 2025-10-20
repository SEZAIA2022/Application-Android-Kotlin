package com.houssein.sezaia.ui.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.Editable
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.houssein.sezaia.R
import com.houssein.sezaia.ui.screen.SettingsActivity
import org.json.JSONObject
import retrofit2.Response

object UIUtils {
    @SuppressLint("ClickableViewAccessibility")
    fun hideShowPassword(context: Context, inputPassword: EditText) {
        var isPasswordVisible = false
        inputPassword.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = 2
                val drawable = inputPassword.compoundDrawables[drawableEnd]
                if (drawable != null && event.rawX >= (inputPassword.right - drawable.bounds.width())) {
                    isPasswordVisible = !isPasswordVisible
                    inputPassword.inputType = if (isPasswordVisible) {
                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    } else {
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    }
                    val visibilityIcon = if (isPasswordVisible) {
                        R.drawable.baseline_visibility_24
                    } else {
                        R.drawable.baseline_visibility_off_24
                    }
                    inputPassword.setCompoundDrawablesWithIntrinsicBounds(
                        null, null, ContextCompat.getDrawable(context, visibilityIcon), null
                    )
                    inputPassword.setSelection(inputPassword.text.length)
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    fun makeTextClickable(
        context: Context,
        textView: TextView,
        fullText: String,
        clickableText: String,
        clickableColorRes: Int,
        onClick: () -> Unit
    ) {
        val spannableString = SpannableString(fullText)
        val start = fullText.indexOf(clickableText)
        val end = start + clickableText.length

        if (start < 0) {
            textView.text = fullText
            return
        }

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                onClick()
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true
            }
        }

        val colorSpan = ForegroundColorSpan(ContextCompat.getColor(context, clickableColorRes))

        spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(colorSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        textView.text = spannableString
        textView.movementMethod = LinkMovementMethod.getInstance()
        textView.highlightColor = Color.TRANSPARENT
    }

    fun initToolbar(
        activity: AppCompatActivity,
        titleText: String,
        @DrawableRes actionIconRes: Int? = null,
        showBackButton: Boolean = true, // ✅ Nouveau paramètre avec valeur par défaut
        onBackClick: (() -> Unit)? = { activity.finish() },
        onActionClick: (() -> Unit)? = {
            val intent = Intent(activity, SettingsActivity::class.java)
            activity.startActivity(intent)
        }
    ) {
        val title = activity.findViewById<TextView>(R.id.toolbar_title)
        val back = activity.findViewById<ImageView>(R.id.toolbar_back)
        val action = activity.findViewById<ImageView>(R.id.toolbar_action)

        title.text = titleText

        // Couleur blanche forcée
        val whiteColor = ContextCompat.getColor(activity, R.color.white)
        back.setColorFilter(whiteColor)
        action.setColorFilter(whiteColor)

        // ✅ Gérer la visibilité du bouton back
        if (showBackButton) {
            back.visibility = View.VISIBLE
            back.setOnClickListener { onBackClick?.invoke() }
        } else {
            back.visibility = View.GONE
        }

        // Gérer le bouton action
        if (actionIconRes != null) {
            action.setImageResource(actionIconRes)
            action.visibility = View.VISIBLE
            action.setOnClickListener { onActionClick?.invoke() }
        } else {
            action.visibility = View.GONE
        }
    }


    // Méthode utilitaire pour gérer les insets des barres système
    fun applySystemBarsInsets(view: View) {

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    fun setupClickableText(
        context: Context,
        textView: TextView,
        fullText: String,
        clickableText: String,
        clickableColorRes: Int,
        targetActivity: Class<out AppCompatActivity>
    ) {
        makeTextClickable(
            context = context,
            textView = textView,
            fullText = fullText,
            clickableText = clickableText,
            clickableColorRes = clickableColorRes
        ) {
            context.startActivity(Intent(context, targetActivity))
        }
    }

    // Fonction pour le TextWatcher des champs de saisie
    fun inputWatcher(
        inputField: TextInputEditText,
        layout: TextInputLayout
    ): TextWatcher {
        return object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Supprime complètement l’erreur et désactive la zone d’erreur pour éviter l’espace
                layout.isErrorEnabled = false
                layout.error = null

                inputField.setTextColor(ContextCompat.getColor(inputField.context, R.color.blue))
                layout.setStartIconTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(inputField.context, R.color.gray))
                )
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
    }


    fun validateInputs(inputFields:List<Pair<TextInputEditText, TextInputLayout>>): Boolean {
        var isValid = true
        inputFields.forEach { (editText, layout) ->
            if (editText.text.isNullOrBlank()) {
                layout.error = "required fields"
                isValid = false
            } else {
                layout.error = null
            }
        }
        return isValid
    }


    fun parseErrorMessage(response: Response<*>): String {
        return try {
            val json = response.errorBody()?.string()
            JSONObject(json ?: "{}").optString("message", "Unknown error")
        } catch (e: Exception) {
            "Network error: ${response.code()}"
        }
    }
    fun showErrorResponse(context: Context, response: Response<*>) {
        val errorMsg = try {
            val errorBody = response.errorBody()?.string()
            if (!errorBody.isNullOrEmpty()) {
                val json = JSONObject(errorBody)
                json.optString("message", "Erreur inconnue")
            } else {
                "Erreur inconnue"
            }
        } catch (e: Exception) {
            Log.e("UIUtils", "Erreur parsing errorBody: ${e.localizedMessage}")
            "Erreur lors du traitement de la réponse"
        }

        Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
    }


}



