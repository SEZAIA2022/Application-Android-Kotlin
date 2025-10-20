package com.houssein.sezaia.ui.screen

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.houssein.sezaia.R
import com.houssein.sezaia.model.data.MyApp
import com.houssein.sezaia.model.request.DescriptionRequest
import com.houssein.sezaia.model.response.ProblemTypesResponse
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.utils.UIUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReportCreationActivity : AppCompatActivity() {

    private lateinit var summary: TextView
    private lateinit var typeLayout: TextInputLayout
    private lateinit var typeInput: MaterialAutoCompleteTextView
    private lateinit var problemLayout: TextInputLayout
    private lateinit var problemEditText: TextInputEditText
    private lateinit var btnSave: MaterialButton

    private var repairId: String? = null
    private var qrCode: String? = null

    // sélection courante
    private var selectedType: String? = null
    private var typesFromApi: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        setContentView(R.layout.activity_report_creation)

        UIUtils.applySystemBarsInsets(findViewById(R.id.rootReportCreation))
        UIUtils.initToolbar(
            this,
            getString(R.string.create_report),
            actionIconRes = R.drawable.baseline_density_medium_24,
            onBackClick = { finish() },
            onActionClick = { startActivity(Intent(this, SettingsActivity::class.java)) }
        )

        summary = findViewById(R.id.summary)
        typeLayout = findViewById(R.id.typeLayout)
        typeInput = findViewById(R.id.typeInput)
        problemLayout = findViewById(R.id.problemLayout)
        problemEditText = findViewById(R.id.problem)
        btnSave = findViewById(R.id.btnSaveReport)

        // Récup extras
        val sp = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        repairId = intent.getStringExtra("repair_id") ?: sp.getString("id", null)
        qrCode = intent.getStringExtra("qr_code")

        summary.text = getString(R.string.report_for_id, repairId ?: "—")

        // Charger les types pour l'application courante
        val applicationName = (application as MyApp).application_name
        loadProblemTypes(applicationName.lowercase())

        // validation champ description
        btnSave.isEnabled = false
        problemEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateSaveEnabled()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // écoute la sélection de type
        typeInput.setOnItemClickListener { _, _, position, _ ->
            selectedType = typesFromApi.getOrNull(position)
            typeLayout.error = null
            updateSaveEnabled()
        }

        btnSave.setOnClickListener {
            val id = repairId
            val description = problemEditText.text?.toString()?.trim().orEmpty()
            if (id.isNullOrBlank()) {
                Toast.makeText(this, "Missing repair ID", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }
            if (selectedType.isNullOrBlank()) {
                typeLayout.error = getString(R.string.fill_required_fields); return@setOnClickListener
            }
            if (description.isBlank()) {
                problemLayout.error = getString(R.string.fill_required_fields); return@setOnClickListener
            } else {
                problemLayout.error = null
            }

            // 👉 Envoi: ajoute 'type_name' si ton backend l’accepte dans DescriptionRequest
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    // Si ton DTO accepte un champ type_name, utilise-le :
                    // data class DescriptionRequest(val id: String, val description_probleme: String, val type_name: String?)
                    val req = DescriptionRequest(
                        id = id,
                        description_probleme = description,
                        type_name = selectedType
                    )
                    val res = RetrofitClient.instance.addDescription(req)
                    if (res.status == "success") {
                        Toast.makeText(this@ReportCreationActivity, res.message, Toast.LENGTH_LONG).show()
                        startActivity(Intent(this@ReportCreationActivity, CameraActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@ReportCreationActivity, "Error : ${res.message}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@ReportCreationActivity, "Network error : ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateSaveEnabled() {
        val descOk = !problemEditText.text.isNullOrBlank()
        val typeOk = !selectedType.isNullOrBlank()
        btnSave.isEnabled = descOk && typeOk
        btnSave.alpha = if (btnSave.isEnabled) 1f else 0.5f
    }

    @SuppressLint("StringFormatInvalid")
    private fun loadProblemTypes(application: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val res: ProblemTypesResponse = RetrofitClient.instance.getProblemTypes(application)
                if (res.status == "success") {
                    typesFromApi = res.types
                    val adapter = ArrayAdapter(
                        this@ReportCreationActivity,
                        android.R.layout.simple_list_item_1,
                        typesFromApi
                    )
                    typeInput.setAdapter(adapter)
                    // Optionnel: sélectionner d’office le 1er
                    // if (typesFromApi.isNotEmpty()) {
                    //     selectedType = typesFromApi.first()
                    //     typeInput.setText(selectedType, false)
                    //     updateSaveEnabled()
                    // }
                } else {
                    typesFromApi = emptyList()
                    typeLayout.error = res.message ?: getString(R.string.loading_error)
                }
            } catch (e: Exception) {
                typesFromApi = emptyList()
                typeLayout.error = getString(R.string.network_error, e.localizedMessage ?: "—")
            }
        }
    }
}
