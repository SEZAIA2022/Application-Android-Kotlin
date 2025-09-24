package com.houssein.sezaia.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.houssein.sezaia.R
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.utils.UIUtils

class VerifyEmailActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_email)

        UIUtils.applySystemBarsInsets(findViewById(R.id.main))
        UIUtils.initToolbar(
            this,
            getString(R.string.verify_email_title), // ex: "Email verification"
            actionIconRes = R.drawable.baseline_verified_user_24,
            onBackClick = { finish() },
            onActionClick = { /* no-op */ }
        )

        val tvMessage = findViewById<TextView>(R.id.tvMessage)
        val tvEmail   = findViewById<TextView>(R.id.tvEmail)
        val btnOpen   = findViewById<MaterialButton>(R.id.btnOpenMail)

        // Message par défaut (déjà défini dans le layout via @string/check_your_email_message)
        // Tu peux le forcer ici si besoin :
        // tvMessage.text = getString(R.string.check_your_email_message)

        // Afficher l'email masqué (si on l'a stocké après l'inscription)
        val savedEmail = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            .getString("email", null)

        if (!savedEmail.isNullOrBlank()) {
            tvEmail.text = maskEmail(savedEmail)
            tvEmail.visibility = View.VISIBLE
        } else {
            tvEmail.visibility = View.GONE
        }

        // Ouvrir l'application d'email (Gmail si dispo, sinon catégorie email, sinon chooser mailto)
        btnOpen.setOnClickListener { openEmailApp() }
    }

    private fun openEmailApp() {
        // 1) Tenter Gmail
        val gmail = packageManager.getLaunchIntentForPackage("com.google.android.gm")
        if (gmail != null) {
            startActivity(gmail)
            return
        }

        // 2) Fallback : app mail par défaut
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_EMAIL)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            return
        } catch (_: Exception) { }

        // 3) Dernier recours : chooser mailto:
        val chooser = Intent.createChooser(
            Intent(Intent.ACTION_VIEW, Uri.parse("mailto:")),
            getString(R.string.open_email_app)
        )
        startActivity(chooser)
    }

    private fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return email
        val name = parts[0]
        val domain = parts[1]
        val maskedName = when {
            name.length <= 2 -> name.first() + "*"
            else -> name.first() + "*".repeat(name.length - 2) + name.last()
        }
        return "$maskedName@$domain"
    }
}
