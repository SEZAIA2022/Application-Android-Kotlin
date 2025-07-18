package com.houssein.sezaia.ui.screen

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.houssein.sezaia.R
import com.houssein.sezaia.model.data.MyApp
import com.houssein.sezaia.model.data.QuestionAnswer
import com.houssein.sezaia.model.response.Message
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.utils.UIUtils
import com.houssein.sezaia.ui.adapter.MessageAdapter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChatbotActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var buttonYes: Button
    private lateinit var buttonNo: Button
    private lateinit var adapter: MessageAdapter
    private lateinit var btnContinueToAppointment: Button
    private lateinit var commentLayout: LinearLayout
    private lateinit var applicationName: String
    private val questions = mutableListOf<Message>()
    private val messages = mutableListOf<Message>()
    private var currentQuestionIndex = 0
    private val questionResponseList = mutableListOf<QuestionAnswer>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chatbot)

        UIUtils.applySystemBarsInsets(findViewById(R.id.main))
        initializeViews()

        UIUtils.initToolbar(
            this, getString(R.string.chatbot),
            actionIconRes = R.drawable.baseline_density_medium_24, onBackClick = {finish()},
            onActionClick = { startActivity(Intent(this, SettingsActivity::class.java)) }
        )
    }

    override fun onResume() {
        super.onResume()
        resetChat()
        fetchQuestionsFromApi()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun resetChat() {
        messages.clear()
        questions.clear()
        questionResponseList.clear()
        currentQuestionIndex = 0
        adapter.notifyDataSetChanged()

        buttonYes.visibility = View.GONE
        buttonNo.visibility = View.GONE
        commentLayout.visibility = View.GONE
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.recyclerView)
        buttonYes = findViewById(R.id.buttonYes)
        buttonNo = findViewById(R.id.buttonNo)
        commentLayout = findViewById(R.id.commentLayout)
        btnContinueToAppointment = findViewById(R.id.buttonSend)

        adapter = MessageAdapter(messages)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        buttonYes.setOnClickListener {
            val answer = "Yes"
            val question = questions.getOrNull(currentQuestionIndex - 1)
            if (question != null) {
                questionResponseList.add(
                    QuestionAnswer(id = question.id, question = question.text, answer = answer)
                )
            }
            addMessage(answer, true, currentQuestionIndex)

            askNextQuestion()
        }

        buttonNo.setOnClickListener {
            val answer = "No"
            addMessage(answer, true, currentQuestionIndex)
            val question = questions.getOrNull(currentQuestionIndex - 1)
            if (question != null) {
                questionResponseList.add(
                    QuestionAnswer(id = question.id, question = question.text, answer = answer)
                )
            }
            askNextQuestion()
        }

        btnContinueToAppointment.setOnClickListener {
            val intent = Intent(this, AppointmentActivity::class.java)
            intent.putExtra("responses", ArrayList(questionResponseList))
            startActivity(intent)
        }
    }

    private fun askNextQuestion() {
        if (currentQuestionIndex < questions.size) {
            val question = questions[currentQuestionIndex]
            addMessage(question.text, false, question.id)
            currentQuestionIndex++

            buttonYes.visibility = View.VISIBLE
            buttonNo.visibility = View.VISIBLE
        } else {
            addMessage(getString(R.string.thank_you_message), false, currentQuestionIndex)
            buttonYes.visibility = View.GONE
            buttonNo.visibility = View.GONE
            commentLayout.visibility = View.VISIBLE
        }
    }

    private fun addMessage(text: String, isUser: Boolean, id: Int) {
        messages.add(Message(text = text, isUser = isUser, id = id))
        adapter.notifyItemInserted(messages.size - 1)
        recyclerView.scrollToPosition(messages.size - 1)
    }


    private fun fetchQuestionsFromApi() {
        val app = application as MyApp
        applicationName = app.application_name
        RetrofitClient.instance.getQuestions(applicationName).enqueue(object : Callback<List<Message>> {
            override fun onResponse(call: Call<List<Message>>, response: Response<List<Message>>) {
                if (response.isSuccessful) {
                    val fetchedQuestions = response.body()
                    if (fetchedQuestions != null) {
                        questions.clear()
                        questions.addAll(fetchedQuestions)
                        currentQuestionIndex = 0
                        askNextQuestion() // pose la première question automatiquement
                    }
                } else {
                    Toast.makeText(this@ChatbotActivity, getString(R.string.api_error), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Message>>, t: Throwable) {
                Toast.makeText(this@ChatbotActivity, getString(R.string.connection_error), Toast.LENGTH_SHORT).show()
            }
        })
    }
}
