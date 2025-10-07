package com.houssein.sezaia.ui.screen

import RepairResponse
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.houssein.sezaia.R
import com.houssein.sezaia.model.request.DescriptionRequest
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.adapter.ResponseAdapter
import com.houssein.sezaia.ui.utils.UIUtils
import kotlinx.coroutines.launch

class RepairActivity : AppCompatActivity() {

    private lateinit var infoTextView: TextView
    private lateinit var problemEditText: TextInputEditText
    private lateinit var btnRepair: Button
    private lateinit var btnHistory: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ResponseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Redimensionne la surface utile quand le clavier apparaît
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        setContentView(R.layout.activity_repair)

        val root = findViewById<View>(R.id.main)
        val bottomPanel = findViewById<View>(R.id.bottomPanel) // défini dans le XML corrigé

        // Insets système pour le conteneur racine (status/navigation bars)
        UIUtils.applySystemBarsInsets(root)
        // Padding dynamique (clavier ou barres système) appliqué au panneau bas
        applyImeAndSystemBarsPadding(bottomPanel)

        UIUtils.initToolbar(
            this,
            getString(R.string.repair),
            actionIconRes = R.drawable.baseline_density_medium_24,
            onBackClick = { finish() },
            onActionClick = { startActivity(Intent(this, SettingsActivity::class.java)) }
        )

        infoTextView     = findViewById(R.id.repairInfo)
        problemEditText  = findViewById(R.id.problem)
        btnRepair        = findViewById(R.id.btnRepair)
        btnHistory       = findViewById(R.id.btnHistory)
        recyclerView     = findViewById(R.id.responseList)

        recyclerView.layoutManager = LinearLayoutManager(this)

        val sharedPref = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val repairId   = sharedPref.getString("id", null)

        // Bouton Save actif seulement s'il y a du texte
        btnRepair.isEnabled = false
        problemEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                btnRepair.isEnabled = !s.isNullOrBlank()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Historique
        btnHistory.setOnClickListener {
            val qrCode = intent.getStringExtra("qr_code")
            startActivity(Intent(this, QrCodeDetailActivity::class.java).putExtra("qr_code", qrCode))
        }

        // Enregistrer / envoyer le rapport
        btnRepair.setOnClickListener {
            val description = problemEditText.text.toString()
            val repairIdNow = sharedPref.getString("id", null)
            if (repairIdNow == null) {
                Toast.makeText(this, "ID de réparation manquant", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val request  = DescriptionRequest(id = repairIdNow, description_probleme = description)
                    val response = RetrofitClient.instance.addDescription(request)

                    if (response.status == "success") {
                        Toast.makeText(this@RepairActivity, response.message, Toast.LENGTH_LONG).show()
                        startActivity(Intent(this@RepairActivity, CameraActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@RepairActivity, "Erreur : ${response.message}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Log.e("RepairActivity", "Erreur réseau : ${e.localizedMessage}", e)
                    Toast.makeText(this@RepairActivity, "Erreur réseau : ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Charger les données
        lifecycleScope.launch {
            try {
                val data = RetrofitClient.instance.getRepairDetails(repairId)
                displayRepairData(data)
            } catch (e: Exception) {
                Log.e("RepairActivity", "Load error: ${e.localizedMessage}", e)
                Toast.makeText(
                    this@RepairActivity,
                    "Erreur de chargement : ${e.localizedMessage ?: "inconnue"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        // Confort : s'assure que le champ reste visible quand il prend le focus
        problemEditText.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) v.post { ensureVisible(v) }
        }
    }

    private fun displayRepairData(data: RepairResponse) {
        val builder = StringBuilder().apply {
            append("👤 Utilisateur : ${data.repair.username}\n")
            append("📅 Date : ${data.repair.date ?: "N/A"}\n")
            append("💬 Commentaire : ${data.repair.comment ?: "—"}\n")
            append("📌 Statut : ${data.repair.status ?: "—"}")
        }
        infoTextView.text = builder.toString()

        adapter = ResponseAdapter(data.responses)
        recyclerView.adapter = adapter
    }

    // ---------- Helpers IME / Insets ----------

    /** Applique un padding bas = max(hauteur clavier, barres système) au [target]. */
    private fun applyImeAndSystemBarsPadding(target: View) {
        val start = target.paddingLeft
        val top = target.paddingTop
        val end = target.paddingRight
        val baseBottom = target.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(target) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.updatePadding(
                left = start,
                top = top,
                right = end,
                bottom = baseBottom + maxOf(sys.bottom, ime.bottom)
            )
            WindowInsetsCompat.CONSUMED
        }
    }

    /** Si le champ est encore trop bas (proche du clavier), on remonte un peu la liste. */
    private fun ensureVisible(view: View) {
        val r = Rect()
        view.getWindowVisibleDisplayFrame(r)
        val loc = IntArray(2)
        view.getLocationOnScreen(loc)
        val viewBottom = loc[1] + view.height
        val margin = dp(12f)
        if (viewBottom + margin > r.bottom) {
            recyclerView.smoothScrollBy(0, (viewBottom + margin) - r.bottom)
        }
    }

    private fun dp(v: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics).toInt()
}
