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

    fun showDialog(
        title: String,
        message: String,
        positiveButtonText: String? = null,
        onPositiveClick: (() -> Unit)? = null,
        negativeButtonText: String? = "Annuler",
        onNegativeClick: (() -> Unit)? = null,
        cancelable: Boolean = false,
    ) {
        val positiveButtonTextColorResId: Int = R.color.blue
        val negativeButtonTextColorResId: Int = R.color.blue
        val builder = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(cancelable)

        if (positiveButtonText != null && onPositiveClick != null) {
            builder.setPositiveButton(positiveButtonText) { _, _ -> onPositiveClick() }
        }

        if (negativeButtonText != null && onNegativeClick != null) {
            builder.setNegativeButton(negativeButtonText) { dialog, _ ->
                dialog.dismiss()
                onNegativeClick()
            }
        }

        val dialog = builder.show()

        // Appliquer la couleur du bouton positif si demandé
        if (positiveButtonTextColorResId != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(
                ContextCompat.getColor(this, positiveButtonTextColorResId)
            )
        }

        // Appliquer la couleur du bouton négatif si demandé
        if (negativeButtonTextColorResId != null) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(
                ContextCompat.getColor(this, negativeButtonTextColorResId)
            )
        }
    }



}
