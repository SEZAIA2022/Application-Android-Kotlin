package com.houssein.sezaia.ui.screen

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.houssein.sezaia.R
import com.houssein.sezaia.model.data.*
import com.houssein.sezaia.model.request.*
import com.houssein.sezaia.model.response.*
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.utils.UIUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.coroutines.resume

class AppointmentActivity : BaseActivity() {

    private lateinit var daysRecyclerView: RecyclerView
    private lateinit var confirmButton: MaterialButton
    private lateinit var commentEditText: TextInputEditText
    private lateinit var applicationName: String
    private lateinit var loggedUser: String
    private lateinit var loggedEmail: String
    private lateinit var qrData: String

    private var totalTechs: Int = 0
    private var selectedDayLabel: String? = null
    private var selectedDate: LocalDate? = null
    private var selectedTimeSlot: String? = null
    private var commentText: String = ""

    private lateinit var responseList: List<QuestionAnswer>
    private var takenSlotsMap: Map<String, Map<String, Int>> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appointment)

        UIUtils.applySystemBarsInsets(findViewById(R.id.main))
        UIUtils.initToolbar(
            this, getString(R.string.appointment), R.drawable.baseline_density_medium_24,
            onBackClick = { finish() },
            onActionClick = { startActivity(Intent(this, SettingsActivity::class.java)) }
        )

        // Init views
        daysRecyclerView = findViewById(R.id.daysRecyclerView)
        confirmButton = findViewById(R.id.confirmButton)
        commentEditText = findViewById(R.id.comment)
        confirmButton.isEnabled = false

        // Load shared preferences
        val sharedPref = getSharedPreferences("LoginData", MODE_PRIVATE)
        loggedUser = sharedPref.getString("loggedUsername", "") ?: ""
        loggedEmail = sharedPref.getString("LoggedEmail", "") ?: ""
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        qrData = sharedPreferences.getString("qrData", "") ?: ""

        val app = application as MyApp
        applicationName = app.application_name

        // Load answers
        @Suppress("UNCHECKED_CAST")
        responseList = (intent.getSerializableExtra("responses") as? ArrayList<*>)?.filterIsInstance<QuestionAnswer>()
            ?: emptyList()

        commentEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                commentText = s.toString()
                updateConfirmButtonState()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        fetchTakenSlots(loggedUser, applicationName) {
            setupDaysRecyclerView()
        }

        confirmButton.setOnClickListener {
            if (selectedDate != null && selectedTimeSlot != null && commentText.isNotBlank()) {
                lifecycleScope.launch {
                    try {
                        val dateKey = selectedDate!!.format(DateTimeFormatter.ISO_DATE) // ex: "2025-07-27"
                        val hourSlot = selectedTimeSlot!! // ex: "08:00"

                        val technicianEmail = fetchNearestTechnicianEmailSuspend(
                            loggedEmail, applicationName, dateKey, hourSlot
                        )

                        if (technicianEmail == null) {
                            Toast.makeText(this@AppointmentActivity, "No available technicians at this slot", Toast.LENGTH_LONG).show()
                            return@launch
                        }

                        val formatterOutput = DateTimeFormatter.ofPattern("EEEE, dd MMMM", Locale.ENGLISH)
                        val formattedDate = "${selectedDate!!.format(formatterOutput)} $hourSlot"

                        val responsesPayload = responseList.map { ResponseItem(it.id, it.answer) }

                        val combinedRequest = AskRepairWithResponsesRequest(
                            loggedUser, formattedDate, commentText, qrData,
                            responsesPayload, applicationName, technicianEmail
                        )

                        RetrofitClient.instance.sendAskRepairWithResponses(combinedRequest)
                            .enqueue(object : Callback<BaseResponse> {
                                override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                                    if (response.isSuccessful && response.body()?.status == "success") {
                                        Toast.makeText(this@AppointmentActivity, response.body()?.message, Toast.LENGTH_SHORT).show()
                                        val prefs = getSharedPreferences("MySuccessPrefs", MODE_PRIVATE)
                                        prefs.edit().apply {
                                            putString("title", getString(R.string.request_sent))
                                            putString("content", getString(R.string.request_message_sent))
                                            putString("button", getString(R.string.show_history))
                                            apply()
                                        }

                                        startActivity(Intent(this@AppointmentActivity, SuccessActivity::class.java))
                                    } else {
                                        Toast.makeText(this@AppointmentActivity, "Erreur lors de la réservation", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                                    Toast.makeText(this@AppointmentActivity, "Erreur réseau : ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            })
                    } catch (e: Exception) {
                        Toast.makeText(this@AppointmentActivity, "Erreur lors du traitement de la date.", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(this, "Please select a date, a time slot and fill in the comments.", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun fetchTakenSlots(user: String, application: String, onComplete: () -> Unit) {
        if (user.isEmpty() || application.isEmpty()) {
            Toast.makeText(this, "User or application is missing.", Toast.LENGTH_SHORT).show()
            onComplete()
            return
        }

        RetrofitClient.instance.getTakenSlots(user, application).enqueue(object : Callback<TakenSlotsResponse> {
            override fun onResponse(call: Call<TakenSlotsResponse>, response: Response<TakenSlotsResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.status == "success") {
                        takenSlotsMap = body.taken_slots ?: emptyMap()
                        totalTechs = body.total_techs
                    } else {
                        Toast.makeText(this@AppointmentActivity, "Server returned an error.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@AppointmentActivity, "Unexpected response: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
                onComplete()
            }

            override fun onFailure(call: Call<TakenSlotsResponse>, t: Throwable) {
                Toast.makeText(this@AppointmentActivity, "Connection error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                onComplete()
            }
        })
    }

    private fun setupDaysRecyclerView() {
        val days = generateUpcomingDays(14)
        val adapter = DaysAdapter(days) { dayItem, timeSlot ->
            selectedDayLabel = dayItem.label
            selectedDate = dayItem.localDate
            selectedTimeSlot = timeSlot
            updateConfirmButtonState()
            commentEditText.requestFocus()
            commentEditText.post {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.showSoftInput(commentEditText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            }
        }

        daysRecyclerView.layoutManager = LinearLayoutManager(this)
        daysRecyclerView.adapter = adapter
    }

    private fun updateConfirmButtonState() {
        val enabled = selectedDate != null && !selectedTimeSlot.isNullOrEmpty() && commentText.isNotBlank()
        confirmButton.isEnabled = enabled
        confirmButton.alpha = if (enabled) 1.0f else 0.5f
    }

    private fun generateUpcomingDays(count: Int): List<DayItem> {
        val list = mutableListOf<DayItem>()
        val calendar = Calendar.getInstance()
        val formatter = java.text.SimpleDateFormat("EEEE dd MMMM", Locale.ENGLISH)


        while (list.size < count) {
            val date = calendar.time
            val localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            val isWeekend = localDate.dayOfWeek in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
            val isHoliday = isPublicHoliday(localDate)

            if (!isWeekend && !isHoliday) {
                val availableSlots = generateTimeSlots(date)

                if (availableSlots.isNotEmpty()) {
                    list.add(DayItem(formatter.format(date), availableSlots, localDate))
                }
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return list
    }

    private fun generateTimeSlots(date: Date): List<String> {
        val calendar = Calendar.getInstance().apply { time = date }
        val format = java.text.SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateKey = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
        val takenForDate = takenSlotsMap[dateKey] ?: emptyMap()
        val slots = mutableListOf<String>()

        calendar.set(Calendar.HOUR_OF_DAY, 8)
        calendar.set(Calendar.MINUTE, 0)

        while (calendar.get(Calendar.HOUR_OF_DAY) <= 18) {
            val slot = format.format(calendar.time)
            val takenCount = takenForDate[slot] ?: 0
            if (takenCount < totalTechs) {
                slots.add(slot)
            }
            calendar.add(Calendar.HOUR_OF_DAY, 2)
        }

        return slots
    }

    private fun isPublicHoliday(date: LocalDate): Boolean {
        val fixed = setOf(
            LocalDate.of(date.year, 1, 1), LocalDate.of(date.year, 5, 1),
            LocalDate.of(date.year, 5, 8), LocalDate.of(date.year, 7, 14),
            LocalDate.of(date.year, 8, 15), LocalDate.of(date.year, 11, 1),
            LocalDate.of(date.year, 11, 11), LocalDate.of(date.year, 12, 25)
        )
        val easter = calculateEaster(date.year)
        return date in fixed || date in listOf(easter.plusDays(1), easter.plusDays(39), easter.plusDays(50))
    }

    private fun calculateEaster(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = ((h + l - 7 * m + 114) % 31) + 1
        return LocalDate.of(year, month, day)
    }

    private fun sendNotificationToAdmin(email: String, appName: String) {
        RetrofitClient.instance.notifyAdmin(
            NotificationRequest("New request", "admin", email, appName)
        ).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {}
            override fun onFailure(call: Call<Void>, t: Throwable) {}
        })
    }

    private fun sendEmail(toEmail: String, message: String) {
        RetrofitClient.instance.sendEmail(SendEmailRequest(toEmail, message))
            .enqueue(object : Callback<SendEmailResponse> {
                override fun onResponse(call: Call<SendEmailResponse>, response: Response<SendEmailResponse>) {
                    if (!response.isSuccessful) {
                        Toast.makeText(this@AppointmentActivity, "Erreur lors de l'envoi de l'email.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<SendEmailResponse>, t: Throwable) {
                    Toast.makeText(this@AppointmentActivity, "Échec de connexion pour l'envoi d'email.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    suspend fun fetchNearestTechnicianEmailSuspend(
        email: String,
        application: String,
        date: String,
        hourSlot: String
    ): String? =
        suspendCancellableCoroutine { continuation ->
            RetrofitClient.instance.getNearestAdminEmail(TechnicianRequest(email, application, date, hourSlot))
                .enqueue(object : Callback<TechnicianResponse> {
                    override fun onResponse(call: Call<TechnicianResponse>, response: Response<TechnicianResponse>) {
                        if (response.isSuccessful) {
                            continuation.resume(response.body()?.email)
                        } else {
                            continuation.resume(null)
                        }
                    }

                    override fun onFailure(call: Call<TechnicianResponse>, t: Throwable) {
                        continuation.resume(null)
                    }
                })
        }
}
