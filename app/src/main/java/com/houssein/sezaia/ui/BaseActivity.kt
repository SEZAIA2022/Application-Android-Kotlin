package com.houssein.sezaia.ui

import android.content.res.ColorStateList
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.houssein.sezaia.R

open class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Appliquer une couleur blanche à la barre de statut avec texte sombre
        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
    }

    fun resetInputStyles(
        colorRes: Int,
        clear: Boolean,
        fields: List<Pair<TextInputEditText, TextInputLayout>>
    ) {
        val color = ContextCompat.getColor(this, colorRes)
        for ((editText, layout) in fields) {
            layout.error = null
            editText.setTextColor(color)
            layout.setStartIconTintList(ColorStateList.valueOf(color))
            if (clear) {
                editText.text = null
            }
        }

    }
    fun showDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("OK", null)
            .show()
    }



}
