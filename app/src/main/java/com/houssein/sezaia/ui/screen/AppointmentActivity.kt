package com.houssein.sezaia.ui.screen

import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract.CommonDataKinds.Email
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.houssein.sezaia.R
import com.houssein.sezaia.model.data.DayItem
import com.houssein.sezaia.model.data.QuestionAnswer
import com.houssein.sezaia.model.request.AskRepairRequest
import com.houssein.sezaia.model.request.SaveResponseRequest
import com.houssein.sezaia.model.request.SendEmailRequest
import com.houssein.sezaia.model.response.AskRepairResponse
import com.houssein.sezaia.model.response.SaveResponseResponse
import com.houssein.sezaia.model.response.SendEmailResponse
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.utils.UIUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

class AppointmentActivity : AppCompatActivity() {

    private lateinit var daysRecyclerView: RecyclerView
    private lateinit var confirmButton: MaterialButton
    private lateinit var commentEditText: TextInputEditText

    private var selectedDayLabel: String? = null
    private var selectedTimeSlot: String? = null
    private var commentText: String = ""

    private lateinit var responseList: List<QuestionAnswer>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appointment)

        UIUtils.applySystemBarsInsets(findViewById(R.id.main))
        UIUtils.initToolbar(
            this,
            getString(R.string.appointment),
            actionIconRes = R.drawable.baseline_density_medium_24,
            onBackClick = { finish() },
            onActionClick = { startActivity(Intent(this, SettingsActivity::class.java)) }
        )

        daysRecyclerView = findViewById(R.id.daysRecyclerView)
        confirmButton = findViewById(R.id.confirmButton)
        commentEditText = findViewById(R.id.comment)

        confirmButton.isEnabled = false

        @Suppress("UNCHECKED_CAST")
        responseList = intent.getSerializableExtra("responses") as? ArrayList<QuestionAnswer> ?: emptyList()

        commentEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                commentText = s.toString()
                updateConfirmButtonState()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        setupDaysRecyclerView()

        val sharedPref = getSharedPreferences("LoginData", MODE_PRIVATE)
        val loggedUser = sharedPref.getString("loggedUsername", null)
        val loggedEmail = sharedPref.getString("LoggedEmail", null)
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val qrData = sharedPreferences.getString("qrData", null)

        confirmButton.setOnClickListener {
            if (selectedDayLabel != null && selectedTimeSlot != null && commentText.isNotBlank()) {
                val username = loggedUser ?: return@setOnClickListener
                val qrCode = qrData ?: return@setOnClickListener


                // 👉 Date sans doublon
                val inputFormat = SimpleDateFormat("dd MMMM", Locale.ENGLISH)
                val parsedDate = inputFormat.parse(selectedDayLabel!!)
                val dayNameFormat = SimpleDateFormat("EEEE, dd MMMM", Locale.ENGLISH)
                val dayWithName = dayNameFormat.format(parsedDate)
                val formattedDate = "$dayWithName $selectedTimeSlot"  // ex: "Tuesday, 03 June 16:00"
                val toEmail =  loggedEmail
                println(toEmail.toString())
                val message = "Rendez vous confirme pour le $formattedDate\nCommentaire: $commentText"


                responseList.forEach { qa ->
                    sendResponseWithRetrofit(
                        qa.id.toString(),
                        qa.answer,
                        username,
                        qrCode
                    )
                }

                sendAskRepair(
                    username,
                    formattedDate,
                    commentText,
                    qrCode
                )

                sendEmail(toEmail.toString(), message)


            } else {
                Toast.makeText(this, "Veuillez sélectionner une date, un créneau et remplir le commentaire.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupDaysRecyclerView() {
        val days = generateUpcomingDays(14)
        val adapter = DaysAdapter(days) { dayLabel, timeSlot ->
            selectedDayLabel = dayLabel  // 👈 Sans heure
            selectedTimeSlot = timeSlot
            updateConfirmButtonState()
        }
        daysRecyclerView.layoutManager = LinearLayoutManager(this)
        daysRecyclerView.adapter = adapter
    }

    private fun updateConfirmButtonState() {
        val enabled = !selectedDayLabel.isNullOrEmpty() &&
                !selectedTimeSlot.isNullOrEmpty() &&
                commentText.isNotBlank()

        confirmButton.isEnabled = enabled
        confirmButton.alpha = if (enabled) 1.0f else 0.5f
    }

    private fun generateUpcomingDays(count: Int): List<DayItem> {
        val list = mutableListOf<DayItem>()
        val calendar = Calendar.getInstance()
        val formatter = SimpleDateFormat("dd MMMM", Locale.ENGLISH)

        val holidays = setOf(
            LocalDate.of(2025, 1, 1), LocalDate.of(2025, 4, 21), LocalDate.of(2025, 5, 1),
            LocalDate.of(2025, 5, 8), LocalDate.of(2025, 5, 29), LocalDate.of(2025, 6, 9),
            LocalDate.of(2025, 7, 14), LocalDate.of(2025, 8, 15), LocalDate.of(2025, 11, 1),
            LocalDate.of(2025, 11, 11), LocalDate.of(2025, 12, 25)
        )

        while (list.size < count) {
            val date = calendar.time
            val localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

            if (dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY && !holidays.contains(localDate)) {
                val label = formatter.format(date) // 👈 Ex: "03 June"
                list.add(DayItem(label, generateTimeSlots(date)))
            }

            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return list
    }

    private fun generateTimeSlots(date: Date): List<String> {
        val calendar = Calendar.getInstance().apply { time = date }
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val slots = mutableListOf<String>()
        calendar.set(Calendar.HOUR_OF_DAY, 8)
        calendar.set(Calendar.MINUTE, 0)
        while (calendar.get(Calendar.HOUR_OF_DAY) <= 18) {
            slots.add(timeFormat.format(calendar.time))
            calendar.add(Calendar.HOUR_OF_DAY, 2)
        }
        return slots
    }

    private fun sendResponseWithRetrofit(
        questionId: String,
        responseText: String,
        username: String,
        qrCode: String
    ) {
        val request = SaveResponseRequest(
            question_id = questionId,
            response = responseText,
            username = username,
            qr_code = qrCode
        )

        RetrofitClient.instance.saveResponse(request).enqueue(object : Callback<SaveResponseResponse> {
            override fun onResponse(call: Call<SaveResponseResponse>, response: Response<SaveResponseResponse>) {
                if (response.isSuccessful) {
                    val serverResponse = response.body()
                    Toast.makeText(this@AppointmentActivity, "✅ ${serverResponse?.message}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@AppointmentActivity, "❌ Erreur : ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<SaveResponseResponse>, t: Throwable) {
                Toast.makeText(this@AppointmentActivity, "❌ Échec : ${t.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun sendAskRepair(
        username: String,
        date: String,
        comment: String,
        qrCode: String
    ) {
        val request = AskRepairRequest(username, date, comment, qrCode)
        RetrofitClient.instance.sendAsk(request).enqueue(object : Callback<AskRepairResponse> {
            override fun onResponse(call: Call<AskRepairResponse>, response: Response<AskRepairResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@AppointmentActivity, "✅ ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@AppointmentActivity, "❌ Erreur : ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<AskRepairResponse>, t: Throwable) {
                Toast.makeText(this@AppointmentActivity, "❌ Échec réseau : ${t.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        })
    }

    fun sendEmail(toEmail: String, message: String) {
        val request = SendEmailRequest(to_email = toEmail, message = message)

        RetrofitClient.instance.sendEmail(request).enqueue(object : retrofit2.Callback<SendEmailResponse> {
            override fun onResponse(
                call: retrofit2.Call<SendEmailResponse>,
                response: retrofit2.Response<SendEmailResponse>
            ) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.message != null) {
                        // Succès
                        println("Email envoyé : ${body.message}")
                    } else if (body?.error != null) {
                        // Erreur renvoyée par le serveur
                        println("Erreur serveur : ${body.error}")
                    } else {
                        println("Réponse inattendue")
                    }
                } else {
                    println("Erreur HTTP : ${response.code()}")
                }
            }

            override fun onFailure(call: retrofit2.Call<SendEmailResponse>, t: Throwable) {
                println("Échec réseau : ${t.message}")
            }
        })
    }



}
