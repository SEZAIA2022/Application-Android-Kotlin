package com.houssein.sezaia.ui.screen

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.houssein.sezaia.R
import com.houssein.sezaia.ui.adapter.HelpAdapter
import com.houssein.sezaia.model.HelpItem
import com.houssein.sezaia.ui.utils.UIUtils

class HelpActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HelpAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)
        // Appliquer les insets des barres système
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))

        // Initialisation de la toolbar
        UIUtils.initToolbar(
            this,getString(R.string.help_center), actionIconRes = R.drawable.baseline_help_outline_24, onBackClick = {finish()},
            onActionClick = {recreate() }
        )
        recyclerView = findViewById(R.id.helpRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val helpItems = listOf(
            HelpItem("Comment annuler un rendez-vous ?", "Allez dans l'historique et cliquez sur 'Annuler le rendez-vous'."),
            HelpItem("Comment contacter le support ?", "Envoyez un mail à support@sezaia.com."),
            HelpItem("Quelles sont les heures d'ouverture ?", "Du lundi au vendredi de 9h à 17h."),
            HelpItem("Comment scanner le QR code ?", "Utilisez l'appareil photo intégré à l'application.")
        )

        adapter = HelpAdapter(helpItems)
        recyclerView.adapter = adapter
    }
}
