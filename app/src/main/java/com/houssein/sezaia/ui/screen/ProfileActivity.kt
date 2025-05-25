package com.houssein.sezaia.ui.screen

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import com.google.android.material.card.MaterialCardView
import com.houssein.sezaia.R
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.utils.UIUtils

class ProfileActivity : BaseActivity() {

    private lateinit var cardUsername: MaterialCardView
    private lateinit var cardEmail: MaterialCardView
    private lateinit var cardPassword: MaterialCardView
    private lateinit var cardNumber: MaterialCardView
    private lateinit var cardDelete: MaterialCardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        // Appliquer les insets des barres système
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))

        // Initialisation de la toolbar
        UIUtils.initToolbar(
            this, getString(R.string.profile), actionIconRes = R.drawable.baseline_account_circle_24, onBackClick = {finish()},
            onActionClick = { recreate() }
        )

        // Initialisation des cartes
        initializeCards()
    }

    // Méthode pour initialiser les cartes et appliquer les listeners
    private fun initializeCards() {
        cardUsername = findViewById(R.id.cardUsername)
        cardEmail = findViewById(R.id.cardEmail)
        cardPassword = findViewById(R.id.cardPassword)
        cardNumber = findViewById(R.id.cardNumber)
        cardDelete = findViewById(R.id.cardDelete)

        val cardClickListener = View.OnClickListener { view ->
            when (view.id) {
                R.id.cardUsername -> openUsernamePage()
                R.id.cardEmail -> openEmailPage()
                R.id.cardPassword -> openPasswordPage()
                R.id.cardNumber-> openNumberPage()
                R.id.cardDelete-> DeleteAccount()
            }
        }

        // Appliquer le même listener sur toutes les cartes
        cardUsername.setOnClickListener(cardClickListener)
        cardEmail.setOnClickListener(cardClickListener)
        cardPassword.setOnClickListener(cardClickListener)
        cardNumber.setOnClickListener(cardClickListener)
        cardDelete.setOnClickListener(cardClickListener)
    }

    private fun openUsernamePage() {
        startActivity(Intent(this, ChangeUsernameActivity::class.java))
    }

    private fun openEmailPage() {
        startActivity(Intent(this, ChangeEmailActivity::class.java))
    }

    private fun openPasswordPage() {
        startActivity(Intent(this, ChangePasswordActivity::class.java))
    }

    private fun openNumberPage() {
        startActivity(Intent(this, ChangeNumberActivity::class.java))
    }

    private fun DeleteAccount() {
        startActivity(Intent(this, DeleteAccountActivity::class.java))
    }
}
