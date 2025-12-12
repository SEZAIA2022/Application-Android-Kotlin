package com.houssein.sezaia.ui.screen

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

// ===============================
// Modèles pour la structure UI
// ===============================

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
    val question_type: String,     // "open", "qcm", "yes_no"
    val is_required: Boolean,
    val options: List<String>?     // pour QCM
)

// Items à plat pour le RecyclerView : Title / Subtitle / Question
sealed class ReportUiItem {
    data class TitleItem(val title: String) : ReportUiItem()
    data class SubtitleItem(val subtitle: String) : ReportUiItem()
    data class QuestionItem(val question: ReportQuestion) : ReportUiItem()
}

// ===============================
// ReportActivity
// ===============================

class ReportActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ReportAdapter
    private lateinit var applicationName: String

    private var qrCode: String = ""
    private var username: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        // Récupérer ce qui vient de CameraActivity
        qrCode = intent.getStringExtra("qr_code") ?: ""
        username = intent.getStringExtra("username") ?: ""

        UIUtils.applySystemBarsInsets(findViewById(R.id.main))
        UIUtils.initToolbar(
            this,
            getString(R.string.report_title),
            actionIconRes = R.drawable.outline_collections_bookmark_24,
            onBackClick = { finish() },
            onActionClick = { recreate() }
        )

        recyclerView = findViewById(R.id.reportRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

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
                            adapter = ReportAdapter(emptyList())
                            recyclerView.adapter = adapter
                        } else {
                            fetchSubtitlesAndQuestionsForTitles(titles)
                        }
                    } else {
                        Toast.makeText(
                            this@ReportActivity,
                            "Erreur lors de la récupération des titres",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<TitlesResponse>, t: Throwable) {
                    Log.e("ReportActivity", "getRepportTitles failed", t)
                    Log.d("ReportActivity", "URL appelée: ${call.request().url}")

                    Toast.makeText(
                        this@ReportActivity,
                        "Erreur réseau (titles): ${t.localizedMessage ?: "Erreur inconnue"}",
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
                            Log.e("ReportActivity", "getRepportSubtitles error for title=$title")
                            reportTitles.add(ReportTitle(title, emptyList()))
                            pendingTitles--
                            if (pendingTitles == 0) buildAndDisplayUi(reportTitles)
                        }
                    }

                    override fun onFailure(call: Call<SubtitlesResponse>, t: Throwable) {
                        Log.e("ReportActivity", "getRepportSubtitles failed", t)

                        Toast.makeText(
                            this@ReportActivity,
                            "Erreur réseau (subtitles): ${t.localizedMessage ?: "Erreur inconnue"}",
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
                            Log.e("ReportActivity",
                                "getRepportQuestions error for $title / $subtitle")
                            result.add(ReportSubtitle(subtitle, emptyList()))
                        }
                        pendingSubs--
                        if (pendingSubs == 0) callback(result)
                    }

                    override fun onFailure(call: Call<QuestionsResponse>, t: Throwable) {
                        Log.e("ReportActivity", "getRepportQuestions failed", t)

                        Toast.makeText(
                            this@ReportActivity,
                            "Erreur réseau (questions): ${t.localizedMessage ?: "Erreur inconnue"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                })
        }
    }

    private fun buildAndDisplayUi(reportTitles: List<ReportTitle>) {
        val sortedTitles = reportTitles.sortedBy { it.title.lowercase() }
        val uiItems = mutableListOf<ReportUiItem>()

        sortedTitles.forEach { t ->
            uiItems.add(ReportUiItem.TitleItem(t.title))
            t.subtitles.sortedBy { it.subtitle.lowercase() }.forEach { sub ->
                uiItems.add(ReportUiItem.SubtitleItem(sub.subtitle))
                sub.questions.forEach { q ->
                    uiItems.add(ReportUiItem.QuestionItem(q))
                }
            }
        }

        adapter = ReportAdapter(uiItems)
        recyclerView.adapter = adapter
    }
}
