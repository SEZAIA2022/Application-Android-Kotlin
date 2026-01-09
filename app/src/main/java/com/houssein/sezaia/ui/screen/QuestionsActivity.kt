package com.houssein.sezaia.ui.screen

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.houssein.sezaia.R
import com.houssein.sezaia.model.data.MyApp
import com.houssein.sezaia.model.response.MachineTypeDto
import com.houssein.sezaia.model.response.MachineTypesResponse
import com.houssein.sezaia.model.response.SimpleQuestionDto
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.adapter.SimpleQuestionsReportStyleAdapter
import com.houssein.sezaia.ui.utils.UIUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.Serializable

data class SimpleQuestionWithAnswer(
    val id: Int,
    val question_text: String,
    val question_type: String,
    val is_required: Boolean,
    val options: List<String>,
    var answer: String = ""
)


data class QuestionAnswerPayload(
    val question_id: Int,
    val answer: String
) : Serializable

data class QuestionsSubmission(
    val application: String,
    val machine_type_id: Int,
    val machine_type_label: String,
    val answers: ArrayList<QuestionAnswerPayload>
) : Serializable

class QuestionsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnSave: MaterialButton
    private lateinit var adapter: SimpleQuestionsReportStyleAdapter

    private lateinit var applicationName: String

    // Dropdown (machine types)
    private lateinit var typeLayout: TextInputLayout
    private lateinit var typeDropdown: MaterialAutoCompleteTextView
    private lateinit var loading: CircularProgressIndicator

    private var machineTypes: List<MachineTypeDto> = emptyList()
    private var selectedMachineTypeId: Int? = null

    // answers: question_id -> answer
    private val answers = mutableMapOf<Int, String>()
    private val questionsWithAnswers = mutableListOf<SimpleQuestionWithAnswer>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_questions)

        UIUtils.applySystemBarsInsets(findViewById(R.id.main))

        UIUtils.initToolbar(
            this,
            "Questions",
            actionIconRes = R.drawable.baseline_density_medium_24,
            onBackClick = { finish() },
            onActionClick = { startActivity(Intent(this, SettingsActivity::class.java)) }
        )

        recyclerView = findViewById(R.id.questionsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        btnSave = findViewById(R.id.btnSaveQuestions)
        loading = findViewById(R.id.loadingQuestions)

        typeLayout = findViewById(R.id.machineTypeLayout)
        typeDropdown = findViewById(R.id.machineTypeDropdown)

        btnSave.setOnClickListener { validateAndContinue() }

        // application name
        val app = application as MyApp
        applicationName = app.application_name.lowercase()

        // 1) charger types
        fetchMachineTypes()
    }

    // -------------------------
    // 1) GET machine types
    // -------------------------
    private fun fetchMachineTypes() {
        setLoading(true)

        RetrofitClient.instance
            .getMachineTypes(applicationName)
            .enqueue(object : Callback<MachineTypesResponse> {
                override fun onResponse(
                    call: Call<MachineTypesResponse>,
                    response: Response<MachineTypesResponse>
                ) {
                    if (!response.isSuccessful) {
                        setLoading(false)
                        Toast.makeText(
                            this@QuestionsActivity,
                            "HTTP Error (types): ${response.code()}",
                            Toast.LENGTH_LONG
                        ).show()
                        return
                    }

                    val list = response.body()?.machine_types ?: emptyList()
                    machineTypes = list

                    if (list.isEmpty()) {
                        setLoading(false)
                        Toast.makeText(
                            this@QuestionsActivity,
                            "No machine types",
                            Toast.LENGTH_LONG
                        ).show()
                        // on vide aussi les questions
                        showQuestions(emptyList())
                        return
                    }

                    setupMachineTypeDropdown(list)

                    // ✅ auto-select le 1er type
                    val first = list.first()
                    selectedMachineTypeId = first.id
                    typeDropdown.setText(first.type, false)

                    // 2) charger questions du 1er type
                    fetchQuestionsByType(first.id)
                }

                override fun onFailure(call: Call<MachineTypesResponse>, t: Throwable) {
                    setLoading(false)
                    Log.e("QuestionsActivity", "getMachineTypes failed", t)
                    Toast.makeText(
                        this@QuestionsActivity,
                        "Network error (types): ${t.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun setupMachineTypeDropdown(list: List<MachineTypeDto>) {
        val labels = list.map { it.type }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            labels
        )
        typeDropdown.setAdapter(adapter)

        typeDropdown.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                val chosen = list[position]
                selectedMachineTypeId = chosen.id
                fetchQuestionsByType(chosen.id)
            }
    }

    // -------------------------
    // 2) GET questions by type
    // -------------------------
    private fun fetchQuestionsByType(machineTypeId: Int) {
        setLoading(true)

        RetrofitClient.instance
            .getQuestionsByMachineType(applicationName, machineTypeId)
            .enqueue(object : Callback<List<SimpleQuestionDto>> {
                override fun onResponse(
                    call: Call<List<SimpleQuestionDto>>,
                    response: Response<List<SimpleQuestionDto>>
                ) {
                    setLoading(false)

                    if (!response.isSuccessful) {
                        Toast.makeText(
                            this@QuestionsActivity,
                            "HTTP Error (questions): ${response.code()}",
                            Toast.LENGTH_LONG
                        ).show()
                        showQuestions(emptyList())
                        return
                    }

                    val list = response.body() ?: emptyList()
                    showQuestions(list)
                }

                override fun onFailure(call: Call<List<SimpleQuestionDto>>, t: Throwable) {
                    setLoading(false)
                    Log.e("QuestionsActivity", "getQuestionsByMachineType failed", t)
                    Toast.makeText(
                        this@QuestionsActivity,
                        "Network error (questions): ${t.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                    showQuestions(emptyList())
                }
            })
    }

    private fun showQuestions(list: List<SimpleQuestionDto>) {
        answers.clear()
        questionsWithAnswers.clear()

        if (list.isEmpty()) {
            adapter = SimpleQuestionsReportStyleAdapter(questionsWithAnswers, ::onAnswerChanged)

            recyclerView.adapter = adapter
            Toast.makeText(this, "No questions for this type", Toast.LENGTH_SHORT).show()
            return
        }

        questionsWithAnswers.addAll(
            list.map { dto ->
                SimpleQuestionWithAnswer(
                    id = dto.id,
                    question_text = dto.text,
                    question_type = dto.question_type,
                    is_required = dto.isRequiredBoolean(),
                    options = dto.options ?: emptyList(),
                    answer = ""
                )
            }
        )

        adapter = SimpleQuestionsReportStyleAdapter(questionsWithAnswers, ::onAnswerChanged)

        recyclerView.adapter = adapter
    }

    private fun onAnswerChanged(questionId: Int, answer: String) {
        answers[questionId] = answer

        val idx = questionsWithAnswers.indexOfFirst { it.id == questionId }
        if (idx != -1) questionsWithAnswers[idx].answer = answer
    }

    private fun validateAndContinue() {
        val missingRequired = questionsWithAnswers.filter { it.is_required && it.answer.trim().isEmpty() }

        if (missingRequired.isNotEmpty()) {
            val preview = missingRequired.take(3).joinToString("\n• ") { it.question_text }
            Toast.makeText(
                this,
                "Missing mandatory questions:\n• $preview" +
                        (if (missingRequired.size > 3) "\n... and ${missingRequired.size - 3} others" else ""),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val typeId = selectedMachineTypeId
        if (typeId == null) {
            Toast.makeText(this, "Please select a machine type", Toast.LENGTH_SHORT).show()
            return
        }

        val typeLabel = machineTypes.firstOrNull { it.id == typeId }?.type ?: ""

        val payloadAnswers = ArrayList<QuestionAnswerPayload>()
        questionsWithAnswers.forEach { q ->
            val ans = q.answer.trim()
            // optionnel: ignorer questions vides non obligatoires
            if (ans.isNotEmpty()) {
                payloadAnswers.add(QuestionAnswerPayload(q.id, ans))
            } else {
                // si tu veux envoyer aussi les vides, décommente:
                // payloadAnswers.add(QuestionAnswerPayload(q.id, ""))
            }
        }

        val submission = QuestionsSubmission(
            application = applicationName,
            machine_type_id = typeId,
            machine_type_label = typeLabel,
            answers = payloadAnswers
        )
        val submissionJson = Gson().toJson(submission)

        // ✅ Envoi vers ConfirmAppointmentActivity (sans affichage)
        startActivity(Intent(this, SlotSelectionActivity::class.java).apply {
            putExtra("questions_submission_json", submissionJson)
            // si tu as besoin aussi de qrCode/username/email/etc, passe-les ici:
            // putExtra("qr_id", qrId)
            // putExtra("technician_email", techEmail)
            // putExtra("comment", commentText)
        })
    }


    private fun setLoading(isLoading: Boolean) {
        loading.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnSave.isEnabled = !isLoading
        typeLayout.isEnabled = !isLoading
    }
}
