package com.houssein.sezaia.ui.screen

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

    private val questions: MutableList<Message> = mutableListOf()
    private val messages = mutableListOf<Message>()
    private var currentQuestionIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chatbot)

        // Appliquer les insets des barres système
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))

        // Initialisation de la vue
        initializeViews()

        // Appel API pour récupérer les questions
        fetchQuestionsFromApi()

        // Configuration de la toolbar
        UIUtils.initToolbar(
            this,getString(R.string.chatbot), onBackClick = {finish()},
            onActionClick = { startActivity(Intent(this, SettingsActivity::class.java)) }
        )
    }

    // Méthode pour initialiser les vues
    private fun initializeViews() {
        recyclerView = findViewById(R.id.recyclerView)
        buttonYes = findViewById(R.id.buttonYes)
        buttonNo = findViewById(R.id.buttonNo)
        commentLayout = findViewById(R.id.commentLayout)
        btnContinueToAppointment = findViewById(R.id.buttonSend)
        adapter = MessageAdapter(messages)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Configuration des écouteurs de boutons
        buttonYes.setOnClickListener {
            addMessage("Yes", true)
            askNextQuestion()
        }
        buttonNo.setOnClickListener {
            addMessage("No", true)
            askNextQuestion()
        }
        btnContinueToAppointment.setOnClickListener {
            val intent = Intent(this, AppointmentActivity::class.java)
            startActivity(intent)
        }
    }

    // Méthode pour poser la question suivante
    private fun askNextQuestion() {
        if (currentQuestionIndex < questions.size) {
            val question = questions[currentQuestionIndex]
            addMessage(question.text, false)
            currentQuestionIndex++
        } else {
            addMessage(getString(R.string.thank_you_message), false)
            buttonYes.visibility = View.GONE
            buttonNo.visibility = View.GONE
            commentLayout.visibility = View.VISIBLE
        }
    }

    // Méthode pour ajouter un message à la liste
    private fun addMessage(text: String, isUser: Boolean) {
        messages.add(Message(text, isUser))
        adapter.notifyItemInserted(messages.size - 1)
        recyclerView.scrollToPosition(messages.size - 1)
    }

    // Méthode pour récupérer les questions depuis l'API
    private fun fetchQuestionsFromApi() {
        RetrofitClient.instance.getQuestions().enqueue(object : Callback<List<Message>> {
            override fun onResponse(call: Call<List<Message>>, response: Response<List<Message>>) {
                if (response.isSuccessful) {
                    val fetchedQuestions = response.body()
                    if (fetchedQuestions != null) {
                        questions.clear()
                        questions.addAll(fetchedQuestions)
                        askNextQuestion()
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
