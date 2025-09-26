package com.houssein.sezaia.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.houssein.sezaia.R
import com.houssein.sezaia.model.request.VerifyTokenRequest
import com.houssein.sezaia.model.response.ApiResponse
import com.houssein.sezaia.model.response.VerifyResponse
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.utils.UIUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class VerifyEmailActivity : BaseActivity() {

    private lateinit var tvEmail: TextView
    private lateinit var btnOpenMail: MaterialButton

    // Garde-fou pour éviter les doubles traitements/navigation
    @Volatile
    private var isHandlingDeepLink = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_email)

        UIUtils.applySystemBarsInsets(findViewById(R.id.main))
        UIUtils.initToolbar(
            this,
            getString(R.string.verify_email_title),
            actionIconRes = R.drawable.baseline_verified_user_24,
            onBackClick = { finish() },
            onActionClick = { /* no-op */ }
        )

        tvEmail = findViewById(R.id.tvEmail)
        btnOpenMail = findViewById(R.id.btnOpenMail)

        val savedEmail = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            .getString("email", null)
        if (!savedEmail.isNullOrBlank()) {
            tvEmail.text = maskEmail(savedEmail)
            tvEmail.visibility = View.VISIBLE
        } else {
            tvEmail.visibility = View.GONE
        }

        btnOpenMail.setOnClickListener { openEmailApp() }

        // Traiter le deeplink si présent
        handleDeepLink(intent?.data)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent.data)
    }

    /**
     * Reconnaît:
     *  - /verify?token=...                 -> vérification d'inscription (register)
     *  - /create-new-password?token=...    -> vérification reset (forgot)
     * Tolère le slash final et les paramètres additionnels (UTM).
     */
    private fun handleDeepLink(data: Uri?) {
        if (data == null) return
        if (isHandlingDeepLink) return

        val token = data.getQueryParameter("token")?.trim().orEmpty()
        if (token.isEmpty()) {
            // Lien sans token → on ignore silencieusement (ou affiche un message)
            return
        }

        val path = (data.path ?: "").lowercase()

        isHandlingDeepLink = true
        when {
            path.contains("create-new-password") -> verifyForget(token)
            path.contains("verify") -> verifyRegister(token)
            else -> {
                isHandlingDeepLink = false
                // Chemin inattendu mais token présent : message utile pour debug
                Toast.makeText(this, getString(R.string.unknown_deeplink_path), Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Vérifie le token d'inscription et active le compte. */
    private fun verifyRegister(token: String) {
        val req = VerifyTokenRequest(token)
        RetrofitClient.instance.verifyRegister(req)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    isHandlingDeepLink = false
                    if (response.isSuccessful) {
                        // Succès → écran de succès / retour login
                        val prefs = getSharedPreferences("MySuccessPrefs", MODE_PRIVATE)
                        prefs.edit().apply {
                            putString("title", getStringSafe(R.string.email_verified_title, "Email vérifié"))
                            putString("content", getStringSafe(R.string.email_verified_content, "Votre e-mail a été vérifié. Vous pouvez vous connecter."))
                            putString("button", getString(R.string.return_to_login))
                            apply()
                        }
                        startActivity(Intent(this@VerifyEmailActivity, SuccessActivity::class.java))
                        finish()
                    } else {
                        showHttpErrorToast(response)
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    isHandlingDeepLink = false
                    Toast.makeText(this@VerifyEmailActivity, "Erreur réseau : ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    /** Vérifie le token de reset; si OK, stocke le token et passe à la création du nouveau MDP. */
    private fun verifyForget(token: String) {
        val req = VerifyTokenRequest(token)
        RetrofitClient.instance.verifyForget(req)
            .enqueue(object : Callback<VerifyResponse> {
                override fun onResponse(call: Call<VerifyResponse>, response: Response<VerifyResponse>) {
                    isHandlingDeepLink = false
                    if (response.isSuccessful && response.body()?.ok == true) {
                        // Stocker le token pour CreatePasswordActivity
                        getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
                            .edit().putString("reset_token", token).apply()

                        startActivity(Intent(this@VerifyEmailActivity, CreatePasswordActivity::class.java))
                        finish()
                    } else {
                        val msg = response.body()?.error ?: "Invalid or used token"
                        Toast.makeText(this@VerifyEmailActivity, msg, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<VerifyResponse>, t: Throwable) {
                    isHandlingDeepLink = false
                    Toast.makeText(this@VerifyEmailActivity, "Erreur réseau : ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // ----- Utilitaires UI -----

    private fun openEmailApp() {
        val gmail = packageManager.getLaunchIntentForPackage("com.google.android.gm")
        if (gmail != null) {
            startActivity(gmail); return
        }
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_EMAIL)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent); return
        } catch (_: Exception) { /* ignore */ }

        val chooser = Intent.createChooser(
            Intent(Intent.ACTION_VIEW, Uri.parse("mailto:")),
            getString(R.string.open_email_app)
        )
        startActivity(chooser)
    }

    private fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return email
        val name = parts[0]; val domain = parts[1]
        val maskedName = when {
            name.isEmpty() -> ""
            name.length == 1 -> "*"
            name.length == 2 -> "${name.first()}*"
            else -> name.first() + "*".repeat(name.length - 2) + name.last()
        }
        return "$maskedName@$domain"
    }

    private fun showHttpErrorToast(resp: Response<*>) {
        val msg = try {
            val raw = resp.errorBody()?.string()
            if (raw.isNullOrBlank()) "Erreur ${resp.code()}" else raw
        } catch (_: Exception) {
            "Erreur ${resp.code()}"
        }
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    /** Renvoie une valeur de repli si la ressource string manque. */
    private fun getStringSafe(resId: Int, fallback: String): String = try {
        getString(resId)
    } catch (_: Exception) { fallback }
}
