package com.houssein.sezaia.ui.screen

import android.annotation.SuppressLint
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.houssein.sezaia.R
import com.houssein.sezaia.model.response.QuestionMetaRow
import com.houssein.sezaia.model.response.QuestionsResponse
import com.houssein.sezaia.model.response.RepportMetaByIdsResponse
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.adapter.VerifyReportAdapter
import com.houssein.sezaia.ui.adapter.VerifyReportUiItem
import com.houssein.sezaia.ui.utils.UIUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ReportDetailsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnPrintPdf: MaterialButton
    private lateinit var btnFinish: MaterialButton

    private lateinit var tvSubmittedAtTop: TextView
    private lateinit var tvQrIdTop: TextView
    private lateinit var tvUsernameTop: TextView
    private lateinit var tvTechUser: TextView

    private lateinit var adapter: VerifyReportAdapter

    private var application: String = ""
    private var qrId: String = ""
    private var username: String = ""
    private var submittedAt: String = ""
    private var techUser: String = ""
    private var serialNumber: String = ""


    private var answersMap: Map<String, Any> = emptyMap()

    // pour PDF
    private var lastBuiltHtml: String = ""

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_details)

        UIUtils.applySystemBarsInsets(findViewById(R.id.main))
        UIUtils.initToolbar(this, "Report details", onBackClick = { finish() })

        recyclerView = findViewById(R.id.detailsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        btnPrintPdf = findViewById(R.id.btnPrintPdf)
        btnFinish = findViewById(R.id.btnFinish)

        tvSubmittedAtTop = findViewById(R.id.tvSubmittedAtTop)
        tvQrIdTop = findViewById(R.id.tvQrIdTop)
        tvUsernameTop = findViewById(R.id.tvUsernameTop)
        tvTechUser = findViewById(R.id.tvTechUser)

        // ===== extras (envoyés depuis HistoryAdapter) =====
        application = intent.getStringExtra("application") ?: ""
        qrId = intent.getStringExtra("qr_id") ?: ""
        serialNumber = intent.getStringExtra("serial_number")?: ""
        username = intent.getStringExtra("username") ?: ""
        submittedAt = intent.getStringExtra("submitted_at") ?: ""
        techUser = intent.getStringExtra("tech_user") ?: ""

        val answersJson = intent.getStringExtra("answers_json") ?: "{}"
        answersMap = Gson().fromJson(
            answersJson,
            object : TypeToken<Map<String, Any>>() {}.type
        ) ?: emptyMap()

        // Header
        tvSubmittedAtTop.text = "📅 Submitted: ${formatIso(submittedAt)}"
        tvQrIdTop.text =
            "🔷 QR ID: ${if (qrId.isBlank()) "N/A" else qrId}   |   🔢 Serial: ${if (serialNumber.isBlank()) "N/A" else serialNumber}"

        tvUsernameTop.text = "👤 User: ${if (username.isBlank()) "N/A" else username}"

        // Footer
        tvTechUser.text = "Technician: ${if (techUser.isBlank()) "N/A" else techUser}"

        btnFinish.setOnClickListener { finish() }

        btnPrintPdf.setOnClickListener {
            if (lastBuiltHtml.isBlank()) {
                Toast.makeText(this, "Nothing to print yet.", Toast.LENGTH_SHORT).show()
            } else {
                printHtmlToPdf(lastBuiltHtml)
            }
        }

        // ✅ charger TOUS les blocs title/subtitle + questions
        loadAllStructures()
    }

    /**
     * Charge toutes les questions + titres/sous-titres corrects
     * à partir des question_ids présents dans answersMap.
     */
    private fun loadAllStructures() {
        val questionIds = answersMap.keys.mapNotNull { it.toIntOrNull() }.distinct()

        if (questionIds.isEmpty()) {
            Toast.makeText(this, "No questions in this submission.", Toast.LENGTH_LONG).show()
            return
        }

        val csv = questionIds.joinToString(",")

        RetrofitClient.instance
            .getRepportMetaByQuestionIds(csv)
            .enqueue(object : Callback<RepportMetaByIdsResponse> {

                override fun onResponse(
                    call: Call<RepportMetaByIdsResponse>,
                    response: Response<RepportMetaByIdsResponse>
                ) {
                    if (!response.isSuccessful || response.body()?.status != "success") {
                        Toast.makeText(this@ReportDetailsActivity, "Failed to load report meta.", Toast.LENGTH_LONG).show()
                        return
                    }

                    val rows = response.body()!!.data

                    // Si application n’est pas fourni via intent, on peut le récupérer ici
                    if (application.isBlank() && rows.isNotEmpty()) {
                        application = rows.first().application
                    }

                    val groups = rows.groupBy { Triple(it.application, it.title, it.subtitle) }

                    if (groups.isEmpty()) {
                        Toast.makeText(this@ReportDetailsActivity, "No report groups found.", Toast.LENGTH_LONG).show()
                        return
                    }

                    fetchQuestionsForGroups(groups)
                }

                override fun onFailure(call: Call<RepportMetaByIdsResponse>, t: Throwable) {
                    Log.e("ReportDetails", "getRepportMetaByQuestionIds failed", t)
                    Toast.makeText(this@ReportDetailsActivity, "Network meta: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            })
    }

    /**
     * Pour chaque (application,title,subtitle), récupérer les questions,
     * injecter les réponses depuis answersMap, et construire la liste UI.
     */
    private fun fetchQuestionsForGroups(
        groups: Map<Triple<String, String, String>, List<QuestionMetaRow>>
    ) {
        val uiItems = mutableListOf<VerifyReportUiItem>()

        // Important: garder un ordre stable
        val sortedKeys = groups.keys.sortedWith(compareBy({ it.second.lowercase() }, { it.third.lowercase() }))

        var pending = sortedKeys.size
        if (pending == 0) {
            adapter = VerifyReportAdapter(uiItems)
            recyclerView.adapter = adapter
            return
        }

        // On accumule toutes les questions (pour HTML PDF)
        val allQuestionsForPdf = mutableListOf<Pair<String, Any>>() // pas obligatoire

        sortedKeys.forEach { key ->
            val (app, title, subtitle) = key

            RetrofitClient.instance
                .getRepportQuestions(app, title, subtitle)
                .enqueue(object : Callback<QuestionsResponse> {

                    override fun onResponse(call: Call<QuestionsResponse>, response: Response<QuestionsResponse>) {
                        if (response.isSuccessful && response.body()?.status == "success") {

                            val qdto = response.body()!!.data.questions

                            // ✅ bloc Title/SubTitle comme VerifyAnswersActivity
                            uiItems.add(VerifyReportUiItem.TitleItem(title))
                            uiItems.add(VerifyReportUiItem.SubtitleItem(subtitle))

                            qdto.forEach { dto ->
                                val ans = answersMap[dto.id.toString()]?.toString() ?: ""
                                uiItems.add(
                                    VerifyReportUiItem.QuestionItem(
                                        QuestionWithAnswer(
                                            id = dto.id,
                                            question_text = dto.question_text,
                                            question_type = dto.question_type,
                                            is_required = dto.is_required,
                                            options = dto.options,
                                            answer = ans
                                        )
                                    )
                                )
                            }
                        } else {
                            Log.e("ReportDetails", "getRepportQuestions failed for $title/$subtitle : ${response.code()}")
                        }

                        pending--
                        if (pending == 0) {
                            // Afficher
                            adapter = VerifyReportAdapter(uiItems)
                            recyclerView.adapter = adapter

                            // Construire HTML PDF multi-blocs
                            lastBuiltHtml = buildHtmlFromUiItems(
                                submittedAt = submittedAt,
                                qrId = qrId,
                                serialNumber = serialNumber,
                                username = username,
                                uiItems = uiItems,
                                techUser = techUser
                            )
                        }
                    }

                    override fun onFailure(call: Call<QuestionsResponse>, t: Throwable) {
                        Log.e("ReportDetails", "getRepportQuestions failed", t)

                        pending--
                        if (pending == 0) {
                            adapter = VerifyReportAdapter(uiItems)
                            recyclerView.adapter = adapter

                            lastBuiltHtml = buildHtmlFromUiItems(
                                submittedAt = submittedAt,
                                qrId = qrId,
                                serialNumber = serialNumber,
                                username = username,
                                uiItems = uiItems,
                                techUser = techUser
                            )
                        }
                    }
                })
        }
    }

    private fun formatIso(raw: String): String {
        if (raw.isBlank()) return "N/A"
        return try {
            val dt = LocalDateTime.parse(raw, DateTimeFormatter.ISO_DATE_TIME)
            dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        } catch (e: Exception) {
            raw
        }
    }

    /**
     * Impression PDF:
     * - WebView HTML
     * - PrintManager -> Save as PDF
     */
    private fun printHtmlToPdf(html: String) {
        val webView = WebView(this)
        webView.settings.javaScriptEnabled = false
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = getSystemService(PRINT_SERVICE) as PrintManager
                val safeQrId = if (qrId.isBlank()) "NA" else qrId
                val safeSerial = if (serialNumber.isBlank()) "NA" else serialNumber

                val jobName = "Report_${safeQrId}_${safeSerial}_${System.currentTimeMillis()}"

                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                printManager.print(
                    jobName,
                    printAdapter,
                    PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .build()
                )
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)
    }

    /**
     * HTML multi-blocs : Title/SubTitle + table question/réponse
     */
    private fun buildHtmlFromUiItems(
        submittedAt: String,
        qrId: String,
        serialNumber: String,
        username: String,
        uiItems: List<VerifyReportUiItem>,
        techUser: String
    ): String {

        val safeDate = formatIso(submittedAt)
        val safeTech = if (techUser.isBlank()) "N/A" else techUser

        val body = StringBuilder()
        var currentTitle: String? = null
        var currentSubtitle: String? = null

        fun openSectionIfNeeded() {
            if (currentTitle != null && currentSubtitle != null) {
                body.append("<h3 style='margin-top:18px;'>${escapeHtml(currentTitle!!)}</h3>")
                body.append("<div style='color:#555;margin-bottom:8px;'>${escapeHtml(currentSubtitle!!)}</div>")
                body.append("<table style='width:100%; border-collapse:collapse;'>")
                body.append("<thead><tr>")
                body.append("<th style='text-align:left;padding:8px;background:#f2f2f2;border:1px solid #ddd;width:60%;'>Question</th>")
                body.append("<th style='text-align:left;padding:8px;background:#f2f2f2;border:1px solid #ddd;width:40%;'>Answer</th>")
                body.append("</tr></thead><tbody>")
            }
        }

        fun closeSectionIfNeeded() {
            if (currentTitle != null && currentSubtitle != null) {
                body.append("</tbody></table>")
            }
        }

        var sectionOpen = false

        uiItems.forEach { item ->
            when (item) {
                is VerifyReportUiItem.TitleItem -> {
                    if (sectionOpen) {
                        closeSectionIfNeeded()
                        sectionOpen = false
                    }
                    currentTitle = item.title
                }
                is VerifyReportUiItem.SubtitleItem -> {
                    currentSubtitle = item.subtitle
                    openSectionIfNeeded()
                    sectionOpen = true
                }
                is VerifyReportUiItem.QuestionItem -> {
                    val q = item.question
                    val ans = (q.answer ?: "").ifBlank { "Unanswered" }
                    body.append(
                        """
                        <tr>
                          <td style="padding:8px;border:1px solid #ddd;">
                            <b>${escapeHtml(q.question_text)}</b><br/>
                            <span style="color:#666;">(${escapeHtml(q.question_type)})</span>
                          </td>
                          <td style="padding:8px;border:1px solid #ddd;">${escapeHtml(ans)}</td>
                        </tr>
                        """.trimIndent()
                    )
                }
            }
        }

        if (sectionOpen) {
            closeSectionIfNeeded()
        }

        return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8"/>
          <style>
            body { font-family: sans-serif; padding: 16px; }
            .meta { margin: 4px 0; color: #222; }
            .footer { margin-top: 24px; }
            .sigline { margin-top: 24px; border-top: 1px solid #000; width: 260px; }
          </style>
        </head>
        <body>
          <div class="meta"><b>Date:</b> $safeDate</div>
          <div class="meta"><b>QR ID:</b> ${escapeHtml(qrId)}</div>
          <div class="meta"><b>Serial Number:</b> ${escapeHtml(serialNumber)}</div>
          <div class="meta"><b>Username:</b> ${escapeHtml(username)}</div>

          ${body.toString()}

          <div class="footer">
            <div><b>Technician:</b> ${escapeHtml(safeTech)}</div>
            <div class="sigline"></div>
            <div style="color:#666; font-size:12px;">Signature</div>
          </div>
        </body>
        </html>
        """.trimIndent()
    }

    private fun escapeHtml(s: String): String {
        return s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}
