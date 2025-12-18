package com.houssein.sezaia.ui.screen

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.cardview.widget.CardView
import androidx.core.content.edit
import com.google.android.material.card.MaterialCardView
import com.houssein.sezaia.R
import com.houssein.sezaia.model.data.MyApp
import com.houssein.sezaia.model.request.LogoutRequest
import com.houssein.sezaia.model.response.BaseResponse
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.utils.UIUtils
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class SettingsActivity : BaseActivity() {

    private lateinit var cardAboutUs: MaterialCardView
    private lateinit var cardPrivacyPolicy: MaterialCardView
    private lateinit var cardTermsOfUse: MaterialCardView
    private lateinit var cardLanguage: MaterialCardView
    private lateinit var cardProfile: MaterialCardView
    private lateinit var cardCamera: MaterialCardView
    private lateinit var cardReport: MaterialCardView
    private lateinit var cardHelp: MaterialCardView
    private lateinit var cardLogout: MaterialCardView
    private lateinit var cardHistory: MaterialCardView
    private lateinit var cardRepair: MaterialCardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)


        // Appliquer les insets des barres système
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))

        // Initialisation de la toolbar
        UIUtils.initToolbar(
            this,getString(R.string.settings), actionIconRes = R.drawable.baseline_settings_24, onBackClick = {finish()},
            onActionClick = {recreate() }
        )

        // Initialisation des cartes
        initializeCards()

        // Mettre à jour l'UI en fonction des préférences
        updateUIBasedOnPreferences()
    }

    // Méthode pour initialiser les cartes et appliquer les listeners
    private fun initializeCards() {
        cardAboutUs = findViewById(R.id.cardAboutUs)
        cardPrivacyPolicy = findViewById(R.id.cardPrivacyPolicy)
        cardTermsOfUse = findViewById(R.id.cardTermsOfUse)
        cardLanguage = findViewById(R.id.cardLanguage)
        cardProfile = findViewById(R.id.cardProfile)
        cardLogout = findViewById(R.id.cardLogout)
        cardHistory = findViewById(R.id.cardHistory)
        cardRepair = findViewById(R.id.cardRepair)
        cardHelp = findViewById(R.id.cardHelp)
        cardCamera = findViewById(R.id.cardCamera)
        cardReport = findViewById(R.id.cardReport)

        val cardClickListener = View.OnClickListener { view ->
            when (view.id) {
                R.id.cardAboutUs -> openAboutUsPage()
                R.id.cardPrivacyPolicy -> openPrivacyPolicyPage()
                R.id.cardTermsOfUse -> openTermsOfUsePage()
                R.id.cardLanguage -> openLanguageSelection()
                R.id.cardProfile -> openProfilePage()
                R.id.cardLogout -> logoutAction()
                R.id.cardHistory -> historyPage()
                R.id.cardHelp -> HelpPage()
                R.id.cardRepair -> QrCodePage()
                R.id.cardCamera -> cameraPage()
                R.id.cardReport -> reportPage()

            }
        }

        // Appliquer le même listener sur toutes les cartes
        cardAboutUs.setOnClickListener(cardClickListener)
        cardCamera.setOnClickListener(cardClickListener)
        cardReport.setOnClickListener (cardClickListener)
        cardPrivacyPolicy.setOnClickListener(cardClickListener)
        cardTermsOfUse.setOnClickListener(cardClickListener)
        cardLanguage.setOnClickListener(cardClickListener)
        cardProfile.setOnClickListener(cardClickListener)
        cardLogout.setOnClickListener(cardClickListener)
        cardHistory.setOnClickListener(cardClickListener)
        cardHelp.setOnClickListener(cardClickListener)
        cardRepair.setOnClickListener(cardClickListener)
    }

    private fun QrCodePage() {
        startActivity((Intent(this, QrCodeActivity::class.java)))
    }
    private fun cameraPage() {
        val prefs = getSharedPreferences("REPORT_PREFS", MODE_PRIVATE)
        prefs.edit {
            putString("reportPage", null)
        }
        startActivity((Intent(this, CameraActivity::class.java)))
    }
    @SuppressLint("CommitPrefEdits")
    private fun reportPage() {
        val prefs = getSharedPreferences("REPORT_PREFS", MODE_PRIVATE)
        prefs.edit {
            putString("reportPage", "reportPage")
        }
        startActivity(Intent(this, CameraActivity::class.java))
    }

    private fun HelpPage() {
        startActivity((Intent(this, HelpActivity::class.java)))
    }

    private fun historyPage() {
        startActivity(Intent(this, HistoryActivity::class.java))
    }

    private fun logoutAction() {
        val prefs = getSharedPreferences("LoginData", MODE_PRIVATE)
        val username = prefs.getString("loggedUsername", null)
        val app = application as MyApp
        val applicationName = app.application_name

        val logoutRequest = LogoutRequest(username.toString(), applicationName)

        RetrofitClient.instance.logout(logoutRequest).enqueue(object : Callback<BaseResponse> {
            override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                if (response.isSuccessful) {
                    // Nettoyer la session locale
                    clearSession()

                    // Message toast
                    val message = response.body()?.message ?: "Logout Successfully."
                    Toast.makeText(this@SettingsActivity, message, Toast.LENGTH_SHORT).show()
                    val prefs = getSharedPreferences("LoginData", MODE_PRIVATE)
                    val editor = prefs.edit()
                    editor.clear() // Vide tout
                    editor.putBoolean("isLoggedIn", false) // Garde isLoggedIn = false
                    editor.apply()

                    // Redirection vers MainActivity
                    val intent = Intent(this@SettingsActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    val errorMessage = try {
                        response.errorBody()?.string()?.let {
                            JSONObject(it).getString("message")
                        } ?: "Unknown Error"
                    } catch (e: Exception) {
                        "Network Error : ${response.code()}"
                    }

                    showDialog(
                        title = "Connection Error",
                        message = errorMessage,
                        positiveButtonText = null,
                        onPositiveClick = null,
                        negativeButtonText = "OK",
                        onNegativeClick = {},
                        cancelable = true
                    )
                }
            }

            override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                showDialog(
                    title = "Unknown Error",
                    message = t.localizedMessage ?: "Unknown Error",
                    positiveButtonText = null,
                    onPositiveClick = null,
                    negativeButtonText = "OK",
                    onNegativeClick = {},
                    cancelable = true
                )
            }
        })
    }

    // Fonction utilitaire pour vider SharedPreferences sans redirection
    private fun clearSession() {
        val prefs = getSharedPreferences("LoginData", MODE_PRIVATE)
        prefs.edit {
            putBoolean("isLoggedIn", false)
            remove("userRole")
            remove("loggedUsername")
            remove("loggedEmail")
        }
    }




    private fun openProfilePage() {
        startActivity(Intent(this, ProfileActivity::class.java))
    }

    // Méthode pour mettre à jour l'UI en fonction des préférences
    private fun updateUIBasedOnPreferences() {
        val prefs = getSharedPreferences("LoginData", MODE_PRIVATE)
        val isLoggged = prefs.getBoolean("isLoggedIn", false)
        val role = prefs.getString("userRole", null)

        val profileCard : CardView = findViewById(R.id.cardProfile)
        val logoutCard : CardView = findViewById(R.id.cardLogout)
        val historyCard : CardView = findViewById(R.id.cardHistory)
        val repairCard : CardView = findViewById(R.id.cardRepair)
        val helpCard : CardView = findViewById(R.id.cardHelp)
        val cameraCard : CardView = findViewById(R.id.cardCamera)
        val reportCard : CardView = findViewById(R.id.cardReport)
        if (role == "admin"){
            repairCard.visibility = if (isLoggged) View.VISIBLE else View.GONE
            cameraCard.visibility = if (isLoggged) View.VISIBLE else View.GONE
            reportCard.visibility = if (isLoggged) View.VISIBLE else View.GONE
            helpCard.visibility = if (isLoggged) View.VISIBLE else View.GONE
            profileCard.visibility = if (isLoggged) View.VISIBLE else View.GONE
            logoutCard.visibility = if (isLoggged) View.VISIBLE else View.GONE
        } else {
            profileCard.visibility = if (isLoggged) View.VISIBLE else View.GONE
            logoutCard.visibility = if (isLoggged) View.VISIBLE else View.GONE
            historyCard.visibility = if (isLoggged) View.VISIBLE else View.GONE
        }

    }

    // Méthodes pour ouvrir les pages respectives (à implémenter)
    private fun openAboutUsPage() {
        startActivity(Intent(this, AboutUsActivity::class.java))
    }

    private fun openPrivacyPolicyPage() {
        startActivity(Intent(this, PrivacyPolicyActivity::class.java))
    }

    private fun openTermsOfUsePage() {
        startActivity(Intent(this, TermsOfUseActivity::class.java))
    }

    private fun openLanguageSelection() {
        Toast.makeText(this, "Language clicked", Toast.LENGTH_SHORT).show()
        // TODO: Démarrer l'activité LanguageSelectionActivity
    }
}
