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
import com.houssein.sezaia.model.response.BaseResponse
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
            showBackButton = false,
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
    override fun onBackPressed() {
        super.onBackPressed()
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent.data)
    }

    /**
     * Reconnaît:
     *  - /verify?token=...&flow=register_user|change_email|delete_account
     *  - /create-new-password?token=... (reset password)
     */
    private fun handleDeepLink(data: Uri?) {
        if (data == null || isHandlingDeepLink) return

        val token = data.getQueryParameter("token")?.trim().orEmpty()
        if (token.isEmpty()) return

        val path = (data.path ?: "").lowercase()
        val flow = data.getQueryParameter("flow")?.lowercase()

        isHandlingDeepLink = true
        when {
            // Reset password (mobile → CreatePasswordActivity)
            path.contains("create-new-password") -> verifyForget(token)

            // Flows pilotés explicitement par 'flow'
            flow == "change_email"   -> verifyChangeEmail(token)
            flow == "delete_account" -> verifyDeleteAccount(token)

            // Inscription (accepte aussi flow=register_user ou verify_register)
            (path.contains("verify") && flow.isNullOrEmpty()) ||
            (path.contains("verify") && flow in setOf("register_user", "verify_register")) -> verifyRegister(token)


            else -> {
                isHandlingDeepLink = false
                Toast.makeText(this, getString(R.string.unknown_deeplink_path), Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Confirme le changement d’email (POST /api/verify_change_email) */
    private fun verifyChangeEmail(token: String) {
        val req = VerifyTokenRequest(token)
        RetrofitClient.instance.verifyChangeEmail(req)
            .enqueue(object : Callback<BaseResponse> {
                override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                    isHandlingDeepLink = false
                    if (response.isSuccessful && response.body()?.status == "success") {
                        // Préparer l’écran de succès générique
                        getSharedPreferences("MySuccessPrefs", MODE_PRIVATE)
                            .edit()
                            .putString("title", getString(R.string.email_changed_title))
                            .putString("content", getString(R.string.email_changed_success))
                            .putString("button", getString(R.string.return_to_login))
                            .apply()

                        startActivity(Intent(this@VerifyEmailActivity, SuccessActivity::class.java))
                        finish()
                    } else {
                        showHttpErrorToast(response)
                    }
                }
                override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                    isHandlingDeepLink = false
                    Toast.makeText(
                        this@VerifyEmailActivity,
                        "Erreur réseau : ${t.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    /** Confirme la suppression de compte (POST /api/verify_delete_account) */
    private fun verifyDeleteAccount(token: String) {
        val req = VerifyTokenRequest(token)
        RetrofitClient.instance.verifyDeleteAccount(req)
            .enqueue(object : Callback<BaseResponse> {
                override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                    isHandlingDeepLink = false
                    if (response.isSuccessful && response.body()?.status == "success") {
                        getSharedPreferences("MySuccessPrefs", MODE_PRIVATE)
                            .edit()
                            .putString("title", getString(R.string.account_deleted_title))
                            .putString("content", getString(R.string.account_deleted_success))
                            .putString("button", getString(R.string.return_to_login))
                            .apply()

                        startActivity(Intent(this@VerifyEmailActivity, SuccessActivity::class.java))
                        finish()
                    } else {
                        showHttpErrorToast(response)
                    }
                }
                override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                    isHandlingDeepLink = false
                    Toast.makeText(
                        this@VerifyEmailActivity,
                        "Erreur réseau : ${t.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    /** Vérifie le token d'inscription et active le compte (POST /api/email/verify) */
    private fun verifyRegister(token: String) {
        val req = VerifyTokenRequest(token)
        RetrofitClient.instance.verifyRegister(req) // Doit retourner Call<BaseResponse>
            .enqueue(object : Callback<BaseResponse> {
                override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                    isHandlingDeepLink = false
                    if (response.isSuccessful && response.body()?.status == "success") {
                        getSharedPreferences("MySuccessPrefs", MODE_PRIVATE)
                            .edit()
                            .putString("title", getStringSafe(R.string.email_verified_title, "Email vérifié"))
                            .putString("content", getStringSafe(R.string.email_verified_content, "Votre e-mail a été vérifié. Vous pouvez vous connecter."))
                            .putString("button", getString(R.string.return_to_login))
                            .apply()

                        startActivity(Intent(this@VerifyEmailActivity, SuccessActivity::class.java))
                        finish()
                    } else {
                        showHttpErrorToast(response)
                    }
                }
                override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                    isHandlingDeepLink = false
                    Toast.makeText(
                        this@VerifyEmailActivity,
                        "Erreur réseau : ${t.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    /** Vérifie le token du reset; si OK → CreatePasswordActivity (POST /api/verify_forget) */
    private fun verifyForget(token: String) {
        val req = VerifyTokenRequest(token)
        RetrofitClient.instance.verifyForget(req) // Call<VerifyResponse>
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
                    Toast.makeText(
                        this@VerifyEmailActivity,
                        "Erreur réseau : ${t.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
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
