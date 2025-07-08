package com.houssein.sezaia.ui.screen

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.cardview.widget.CardView
import com.google.android.material.card.MaterialCardView
import com.houssein.sezaia.R
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.utils.UIUtils
import androidx.core.content.edit

class SettingsActivity : BaseActivity() {

    private lateinit var cardAboutUs: MaterialCardView
    private lateinit var cardPrivacyPolicy: MaterialCardView
    private lateinit var cardTermsOfUse: MaterialCardView
    private lateinit var cardLanguage: MaterialCardView
    private lateinit var cardProfile: MaterialCardView
    private lateinit var cardHelp: MaterialCardView
    private lateinit var cardLogout: MaterialCardView
    private lateinit var cardHistory: MaterialCardView

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
        cardHelp = findViewById(R.id.cardHelp)

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

            }
        }

        // Appliquer le même listener sur toutes les cartes
        cardAboutUs.setOnClickListener(cardClickListener)
        cardPrivacyPolicy.setOnClickListener(cardClickListener)
        cardTermsOfUse.setOnClickListener(cardClickListener)
        cardLanguage.setOnClickListener(cardClickListener)
        cardProfile.setOnClickListener(cardClickListener)
        cardLogout.setOnClickListener(cardClickListener)
        cardHistory.setOnClickListener(cardClickListener)
        cardHelp.setOnClickListener(cardClickListener)
    }

    private fun HelpPage() {
        startActivity((Intent(this, HelpActivity::class.java)))
    }

    private fun historyPage() {
        startActivity(Intent(this, HistoryActivity::class.java))
    }

    private fun logoutAction() {
        val prefs = getSharedPreferences("LoginData", MODE_PRIVATE)
        prefs.edit {
            putBoolean("isLoggedIn", false)
                .remove("userRole")  // Supprime le rôle
        }

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()

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
        if (role == "admin"){
            repairCard.visibility = if (isLoggged) View.VISIBLE else View.GONE
            helpCard.visibility = if (isLoggged) View.VISIBLE else View.GONE
        }
        profileCard.visibility = if (isLoggged) View.VISIBLE else View.GONE
        logoutCard.visibility = if (isLoggged) View.VISIBLE else View.GONE
        historyCard.visibility = if (isLoggged) View.VISIBLE else View.GONE
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
