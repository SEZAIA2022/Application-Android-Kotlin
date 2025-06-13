package com.houssein.sezaia.ui.screen

import android.content.Intent
import android.os.Bundle
import android.view.View
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
    private lateinit var btnHistory : Button
    private lateinit var btnClose : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_success)
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))
        initViews()
        setupListeners()
        UIUtils.initToolbar(
            this,getString(R.string.success),actionIconRes = R.drawable.outline_verified_24, onBackClick = {},
            onActionClick = {}
        )
    }

    private fun initViews(){
        val sharedPreferences = getSharedPreferences("MySuccessPrefs", MODE_PRIVATE)

        val newTitle = sharedPreferences.getString("title", null)
        val newMessage = sharedPreferences.getString("content", null)
        val newButton = sharedPreferences.getString("button", null)

        btnHistory = findViewById(R.id.btnHistory)
        title = findViewById(R.id.textView3)
        message = findViewById((R.id.textView2))
        btnClose = findViewById(R.id.btnClose)
        title.text = newTitle
        message.text = newMessage
        btnHistory.text = newButton
        if (btnHistory.text == getString(R.string.show_history)){
            btnClose.visibility = View.VISIBLE
        } else {
            btnClose.visibility = View.GONE
        }

    }

    private fun setupListeners() {
        if (btnHistory.text == getString(R.string.show_history)){
            btnHistory.setOnClickListener { startActivity( Intent(this@SuccessActivity, HistoryActivity::class.java)) }
            btnClose.setOnClickListener{
                // Fermer l'application
                finishAffinity()
            }
        } else {
            btnHistory.setOnClickListener { startActivity( Intent(this@SuccessActivity, LoginActivity::class.java)) }

        }
    }

}