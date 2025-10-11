package com.houssein.sezaia.ui.screen

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import com.houssein.sezaia.R
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.utils.UIUtils

class InterventionActivity : BaseActivity() {
    private lateinit var btnIntDirect: Button
    private lateinit var btnIntAppointment: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_intervention)
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))
        initViews()
        UIUtils.initToolbar(
            this,
            "Intervention",
            actionIconRes = R.drawable.baseline_density_medium_24,
            onBackClick = {},
            onActionClick = {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
        )
        setupListeners()
        }

    private fun initViews() {
        btnIntDirect = findViewById(R.id.btnIntDirect)
        btnIntAppointment = findViewById(R.id.btnIntAppointment)    }

    private fun setupListeners() {
        btnIntDirect.setOnClickListener {
            startActivity(Intent(this, RequestInterventionDirectActivity::class.java))
        }
        btnIntAppointment.setOnClickListener {
            startActivity(Intent(this, CameraActivity::class.java))
        }
    }
}
