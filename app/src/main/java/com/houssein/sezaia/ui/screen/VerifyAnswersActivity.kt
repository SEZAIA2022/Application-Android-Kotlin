package com.houssein.sezaia.ui.screen

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.houssein.sezaia.R
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.network.SubmitRepportRequest
import com.houssein.sezaia.network.SubmitResponse
import com.houssein.sezaia.ui.adapter.VerifyReportAdapter
import com.houssein.sezaia.ui.adapter.VerifyReportUiItem
import com.houssein.sezaia.ui.utils.UIUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class VerifyAnswersActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnEdit: Button
    private lateinit var btnConfirm: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvProgress: TextView
    private lateinit var adapter: VerifyReportAdapter

    private var structures = listOf<ReportStructureWithAnswers>()
    private lateinit var applicationName: String
    private var username: String = ""
    private var qrCode: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_answers)

        UIUtils.applySystemBarsInsets(findViewById(R.id.main))
        UIUtils.initToolbar(
            this,
            "Check the answers",
            onBackClick = { finish() }
        )

        recyclerView = findViewById(R.id.answersRecyclerView)
        btnEdit = findViewById(R.id.btnEdit)
        btnConfirm = findViewById(R.id.btnConfirm)
        progressBar = findViewById(R.id.progressBar)
        tvProgress = findViewById(R.id.tvProgress)

        recyclerView.layoutManager = LinearLayoutManager(this)

        // Récupérer les données
        @Suppress("UNCHECKED_CAST")
        structures = intent.getSerializableExtra("structures") as? List<ReportStructureWithAnswers> ?: emptyList()
        applicationName = intent.getStringExtra("application") ?: ""
        username = intent.getStringExtra("username") ?: ""
        qrCode = intent.getStringExtra("qr_code") ?: ""

        // Afficher les réponses
        displayAnswers()

        btnEdit.setOnClickListener {
            finish()  // Retour à l'écran précédent
        }

        btnConfirm.setOnClickListener {
            submitAnswers()
        }
    }

    private fun displayAnswers() {
        // Construire les items pour l'adapter
        val uiItems = mutableListOf<VerifyReportUiItem>()

        var lastTitle: String? = null

        structures
            .sortedWith(compareBy({ it.title.lowercase() }, { it.subtitle.lowercase() }))
            .forEach { structure ->

                // ✅ ajouter le titre UNE SEULE FOIS
                if (lastTitle != structure.title) {
                    uiItems.add(VerifyReportUiItem.TitleItem(structure.title))
                    lastTitle = structure.title
                }

                // ✅ ajouter chaque sous-titre
                uiItems.add(VerifyReportUiItem.SubtitleItem(structure.subtitle))

                // ✅ ajouter les questions
                structure.questions.forEach { question ->
                    uiItems.add(VerifyReportUiItem.QuestionItem(question))
                }
            }

        adapter = VerifyReportAdapter(uiItems)
        recyclerView.adapter = adapter


        updateProgress()
    }

    @SuppressLint("SetTextI18n")
    private fun updateProgress() {
        // Compter les réponses fournies
        val allQuestions = structures.flatMap { it.questions }
        val answeredCount = allQuestions.count { it.answer.trim().isNotEmpty() }
        val total = allQuestions.size
        val progressPercent = if (total > 0) (100 * answeredCount / total) else 0

        progressBar.progress = progressPercent
        tvProgress.text = "$answeredCount/$total questions answered"
    }

    private fun submitAnswers() {
        // Vérifier qu'aucune réponse n'est vide pour les questions obligatoires
        val allQuestions = structures.flatMap { it.questions }
        val emptyRequired = allQuestions.filter {
            it.is_required && it.answer.trim().isEmpty()
        }

        if (emptyRequired.isNotEmpty()) {
            Toast.makeText(
                this,
                "Some mandatory questions are empty!",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        btnConfirm.isEnabled = false
        btnConfirm.alpha = 0.5f

        // Créer la map des réponses {question_id: answer}
        val answersMap = allQuestions.associate { q ->
            q.id.toString() to q.answer
        }

        // Utiliser la première structure pour title/subtitle
        val firstStructure = structures.firstOrNull()
        val title = firstStructure?.title ?: "Report"
        val subtitle = firstStructure?.subtitle ?: "Submission"

        val request = SubmitRepportRequest(
            application = applicationName,
            title = title,
            subtitle = subtitle,
            username = username,
            qr_code = qrCode,
            answers = answersMap
        )


        RetrofitClient.instance
            .submitRepport(request)
            .enqueue(object : Callback<SubmitResponse> {
                override fun onResponse(
                    call: Call<SubmitResponse>,
                    response: Response<SubmitResponse>
                ) {
                    if (response.isSuccessful && response.body()?.status == "success") {
                        Toast.makeText(
                            this@VerifyAnswersActivity,
                            "Report successfully submitted!",
                            Toast.LENGTH_LONG
                        ).show()

                        // Retour à l'écran principal
                        startActivity(Intent(this@VerifyAnswersActivity, DailyInterventionActivity::class.java))
                    } else {
                        Toast.makeText(
                            this@VerifyAnswersActivity,
                            response.message(),
                            Toast.LENGTH_SHORT
                        ).show()

                        btnConfirm.isEnabled = true
                        btnConfirm.alpha = 1f
                    }
                }

                override fun onFailure(call: Call<SubmitResponse>, t: Throwable) {
                    Log.e("VerifyActivity", "submitRepport failed", t)
                    Toast.makeText(
                        this@VerifyAnswersActivity,
                        "Network error: ${t.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()

                    btnConfirm.isEnabled = true
                    btnConfirm.alpha = 1f
                }
            })
    }
}
