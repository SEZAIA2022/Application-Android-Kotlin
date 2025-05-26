package com.houssein.sezaia.ui.screen

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.houssein.sezaia.R
import com.houssein.sezaia.ui.utils.UIUtils

class SuccessActivity : AppCompatActivity() {
    private lateinit var title : TextView
    private lateinit var message : TextView
    private lateinit var btnContinueLogin : Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_success)
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))
        initViews()
        setupListeners()
        UIUtils.initToolbar(
            this,getString(R.string.change_email),actionIconRes = R.drawable.outline_verified_24, onBackClick = {},
            onActionClick = { recreate() }
        )
    }

    private fun initViews(){
        val newTitle = intent.getStringExtra("title")
        val newMessage = intent.getStringExtra("content")
        btnContinueLogin = findViewById(R.id.btnContinueLogin)
        title = findViewById(R.id.textView3)
        message = findViewById((R.id.textView2))
        title.text = newTitle
        message.text = newMessage
    }

    private fun setupListeners() {
        btnContinueLogin.setOnClickListener { startActivity( Intent(this@SuccessActivity, LoginActivity::class.java)) }
    }

}