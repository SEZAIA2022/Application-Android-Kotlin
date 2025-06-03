package com.houssein.sezaia.ui.screen

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.houssein.sezaia.R
import com.houssein.sezaia.model.request.ChangeUsernameRequest
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.utils.UIUtils
import retrofit2.Callback

class ChangeUsernameActivity : AppCompatActivity() {
    private lateinit var currentUsername: TextInputEditText
    private lateinit var newUsername: TextInputEditText
    private lateinit var password: TextInputEditText
    private lateinit var btnChangeUsername: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_change_username)
        // Appliquer les insets des barres système
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))

        initViews()
        // Configuration de la toolbar
        UIUtils.initToolbar(
            this,getString(R.string.change_username), actionIconRes = R.drawable.baseline_account_circle_24, onBackClick = {finish()},
            onActionClick = {recreate() }
        )
        setupListeners()
    }

    private fun setupListeners() {
        password.setOnClickListener(
            changeUsername()
        )
    }

    private fun changeUsername(){
        val currentUser = currentUsername.text.toString()
        val newuser = newUsername.text.toString()
        val pass = password.text.toString()
        val changeUserRequest = ChangeUsernameRequest(currentUser,newuser,pass)

    }


    private fun initViews() {
        currentUsername = findViewById(R.id.username)
        newUsername = findViewById(R.id.newUsername)
        password = findViewById(R.id.password)
        btnChangeUsername = findViewById(R.id.btnChangeUsername)
        UIUtils.hideShowPassword(this, password)
    }


}