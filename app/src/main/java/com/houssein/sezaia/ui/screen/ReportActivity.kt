package com.houssein.sezaia.ui.screen

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.houssein.sezaia.R
import com.houssein.sezaia.model.data.MyApp
import com.houssein.sezaia.model.response.QuestionsResponse
import com.houssein.sezaia.model.response.SubtitlesResponse
import com.houssein.sezaia.model.response.TitlesResponse
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.adapter.ReportAdapter
import com.houssein.sezaia.ui.utils.UIUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.Serializable

data class ReportTitle(
    val title: String,
    val subtitles: List<ReportSubtitle>
)

data class ReportSubtitle(
    val subtitle: String,
    val questions: List<ReportQuestion>
)

data class ReportQuestion(
    val id: Int,
    val question_text: String,
    val question_type: String,
    val is_required: Boolean,
    val options: List<String>?
)

sealed class ReportUiItem {
    data class TitleItem(val title: String) : ReportUiItem()
    data class SubtitleItem(val subtitle: String) : ReportUiItem()
    data class QuestionItem(val question: ReportQuestion) : ReportUiItem()
}

data class ReportStructureWithAnswers(
    val title: String,
    val subtitle: String,
    val questions: MutableList<QuestionWithAnswer>  // MutableList pour pouvoir modifier
) : Serializable

data class QuestionWithAnswer(
    val id: Int,
    val question_text: String,
    val answer: String,
    val is_required: Boolean,
    val question_type: String,
    val options: List<String>?
) : Serializable


class ReportActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ReportAdapter
    private lateinit var btnHistory: Button
    private lateinit var btnSave: Button
    private lateinit var applicationName: String
    private val completeStructures = mutableListOf<ReportStructureWithAnswers>()

    private var qrCode: String = ""
    private var username: String = ""



    // Stocker les réponses: question_id -> answer_text
    private val answers = mutableMapOf<Int, String>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)
        val sharedPrefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val qrData = sharedPrefs.getString("qrData", null)
        val sharedLoginData = getSharedPreferences("LoginData", MODE_PRIVATE)
        qrCode = qrData.toString()
        username = sharedLoginData.getString("loggedUsername", null).toString()

        UIUtils.applySystemBarsInsets(findViewById(R.id.main))
        UIUtils.initToolbar(
            this,
            getString(R.string.report_title),
            actionIconRes = R.drawable.outline_collections_bookmark_24,
            onBackClick = { finish()
                            val prefs = getSharedPreferences("REPORT_PREFS", MODE_PRIVATE)
                            prefs.edit {
                                putString("reportPage", "reportPage")
                            }
                          },
            onActionClick = { recreate() }
        )

        recyclerView = findViewById(R.id.reportRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        btnHistory = findViewById(R.id.btnHistory)
        btnSave = findViewById(R.id.btnSave)

        btnHistory.setOnClickListener {
            openHistory()
        }

        btnSave.setOnClickListener {
            goToVerifyAnswers()
        }

        fetchFullReportStructure()

    }

    private fun fetchFullReportStructure() {
        val app = application as MyApp
        applicationName = app.application_name

        RetrofitClient.instance
            .getRepportTitles(applicationName)
            .enqueue(object : Callback<TitlesResponse> {
                override fun onResponse(
                    call: Call<TitlesResponse>,
                    response: Response<TitlesResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val titles = response.body()!!.data.titles
                        if (titles.isEmpty()) {
                            adapter = ReportAdapter(emptyList(), ::onAnswerChanged)
                            recyclerView.adapter = adapter
                        } else {
                            fetchSubtitlesAndQuestionsForTitles(titles)
                        }
                    } else {
                        Toast.makeText(
                            this@ReportActivity,
                            "Error retrieving titles",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<TitlesResponse>, t: Throwable) {
                    Log.e("ReportActivity", "getRepportTitles failed", t)
                    Toast.makeText(
                        this@ReportActivity,
                        "Network error: ${t.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun fetchSubtitlesAndQuestionsForTitles(titles: List<String>) {
        val reportTitles = mutableListOf<ReportTitle>()
        var pendingTitles = titles.size

        titles.forEach { title ->
            RetrofitClient.instance
                .getRepportSubtitles(applicationName, title)
                .enqueue(object : Callback<SubtitlesResponse> {
                    override fun onResponse(
                        call: Call<SubtitlesResponse>,
                        response: Response<SubtitlesResponse>
                    ) {
                        if (response.isSuccessful && response.body() != null) {
                            val subs = response.body()!!.data.subtitles
                            if (subs.isEmpty()) {
                                reportTitles.add(ReportTitle(title, emptyList()))
                                pendingTitles--
                                if (pendingTitles == 0) buildAndDisplayUi(reportTitles)
                            } else {
                                fetchQuestionsForSubtitles(title, subs) { subtitleList ->
                                    reportTitles.add(ReportTitle(title, subtitleList))
                                    pendingTitles--
                                    if (pendingTitles == 0) buildAndDisplayUi(reportTitles)
                                }
                            }
                        } else {
                            reportTitles.add(ReportTitle(title, emptyList()))
                            pendingTitles--
                            if (pendingTitles == 0) buildAndDisplayUi(reportTitles)
                        }
                    }

                    override fun onFailure(call: Call<SubtitlesResponse>, t: Throwable) {
                        Log.e("ReportActivity", "getRepportSubtitles failed", t)
                        Toast.makeText(
                            this@ReportActivity,
                            "Network error: ${t.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                })
        }
    }

    private fun fetchQuestionsForSubtitles(
        title: String,
        subtitles: List<String>,
        callback: (List<ReportSubtitle>) -> Unit
    ) {
        val result = mutableListOf<ReportSubtitle>()
        var pendingSubs = subtitles.size

        subtitles.forEach { subtitle ->
            RetrofitClient.instance
                .getRepportQuestions(applicationName, title, subtitle)
                .enqueue(object : Callback<QuestionsResponse> {
                    override fun onResponse(
                        call: Call<QuestionsResponse>,
                        response: Response<QuestionsResponse>
                    ) {
                        if (response.isSuccessful && response.body() != null &&
                            response.body()!!.status == "success"
                        ) {
                            val qdtoList = response.body()!!.data.questions
                            val questions = qdtoList.map { dto ->
                                ReportQuestion(
                                    id = dto.id,
                                    question_text = dto.question_text,
                                    question_type = dto.question_type,
                                    is_required = dto.is_required,
                                    options = dto.options
                                )
                            }
                            result.add(ReportSubtitle(subtitle, questions))
                        } else {
                            result.add(ReportSubtitle(subtitle, emptyList()))
                        }
                        pendingSubs--
                        if (pendingSubs == 0) callback(result)
                    }

                    override fun onFailure(call: Call<QuestionsResponse>, t: Throwable) {
                        Log.e("ReportActivity", "getRepportQuestions failed", t)
                        Toast.makeText(
                            this@ReportActivity,
                            "Network error: ${t.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                })
        }
    }

    private fun buildAndDisplayUi(reportTitles: List<ReportTitle>) {
        val sortedTitles = reportTitles.sortedBy { it.title.lowercase() }
        val uiItems = mutableListOf<ReportUiItem>()

        // ✅ Vider la liste précédente
        completeStructures.clear()

        sortedTitles.forEach { t ->
            uiItems.add(ReportUiItem.TitleItem(t.title))
            t.subtitles.sortedBy { it.subtitle.lowercase() }.forEach { sub ->
                uiItems.add(ReportUiItem.SubtitleItem(sub.subtitle))

                // Créer la structure pour cette section
                val questionsForThisSection = mutableListOf<QuestionWithAnswer>()

                sub.questions.forEach { q ->
                    uiItems.add(ReportUiItem.QuestionItem(q))
                    questionsForThisSection.add(
                        QuestionWithAnswer(
                            id = q.id,
                            question_text = q.question_text,
                            answer = "",  // Sera rempli par l'utilisateur
                            is_required = q.is_required,
                            question_type = q.question_type,
                            options = q.options
                        )
                    )
                }

                // Ajouter cette structure seulement s'il y a des questions
                if (questionsForThisSection.isNotEmpty()) {
                    completeStructures.add(
                        ReportStructureWithAnswers(
                            title = t.title,
                            subtitle = sub.subtitle,
                            questions = questionsForThisSection
                        )
                    )
                }
            }
        }

        adapter = ReportAdapter(uiItems, ::onAnswerChanged)
        recyclerView.adapter = adapter
    }



    // Callback appelé quand une réponse change
    private fun onAnswerChanged(questionId: Int, answer: String) {
        answers[questionId] = answer

        // ✅ Mettre à jour dans completeStructures
        completeStructures.forEach { structure ->
            structure.questions.forEachIndexed { index, q ->
                if (q.id == questionId) {
                    structure.questions[index] = q.copy(answer = answer)
                }
            }
        }

        Log.d("ReportActivity", "Answer updated: q$questionId = '$answer'")
    }


    private fun openHistory() {
        startActivity(Intent(this, ReportHistoryActivity::class.java).apply {
            putExtra("application", applicationName)
            putExtra("username", username)
            putExtra("qr_code", qrCode)
        })
    }

    private fun goToVerifyAnswers() {
        // ✅ Validation améliorée
        val allQuestions = completeStructures.flatMap { it.questions }
        val missingRequired = allQuestions.filter { q ->
            q.is_required && q.answer.trim().isEmpty()
        }

        Log.d("ReportActivity", "Total questions: ${allQuestions.size}")
        Log.d("ReportActivity", "Missing required: ${missingRequired.size}")
        Log.d("ReportActivity", "Missing questions: ${missingRequired.map { it.question_text }}")

        if (missingRequired.isNotEmpty()) {
            val missingText = missingRequired.take(3).joinToString("\n• ") { it.question_text }
            Toast.makeText(
                this,
                "⚠️ Missing mandatory questions:\n• $missingText${if (missingRequired.size > 3) "\n... and ${missingRequired.size - 3} others" else ""}",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (completeStructures.isEmpty()) {
            Toast.makeText(this, "No questions to check", Toast.LENGTH_SHORT).show()
            return
        }

        startActivity(Intent(this, VerifyAnswersActivity::class.java).apply {
            putExtra("application", applicationName)
            putExtra("username", username)
            putExtra("qr_code", qrCode)
            putExtra("structures", ArrayList(completeStructures))
        })
    }

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        val prefs = getSharedPreferences("REPORT_PREFS", MODE_PRIVATE)
        prefs.edit {
            putString("reportPage", "reportPage")
        }

        super.onBackPressed()
    }



}
