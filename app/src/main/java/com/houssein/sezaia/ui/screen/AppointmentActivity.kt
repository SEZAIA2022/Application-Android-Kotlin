package com.houssein.sezaia.ui.screen

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.inputmethod.InputMethodManager
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

    private val TAG = "AppointmentActivity"

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
        Log.d(TAG, "onCreate()")
        setContentView(R.layout.activity_appointment)

        UIUtils.applySystemBarsInsets(findViewById(R.id.main))
        UIUtils.initToolbar(
            this, getString(R.string.appointment), R.drawable.baseline_density_medium_24,
            onBackClick = {
                Log.d(TAG, "Toolbar back clicked")
                finish()
            },
            onActionClick = {
                Log.d(TAG, "Toolbar action clicked -> SettingsActivity")
                startActivity(Intent(this, SettingsActivity::class.java))
            }
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

        Log.d(TAG, "Prefs -> loggedUser=$loggedUser, loggedEmail=$loggedEmail, applicationName=$applicationName")
        Log.d(TAG, "qrData length=${qrData.length}")

        // Load answers
        @Suppress("UNCHECKED_CAST")
        responseList = (intent.getSerializableExtra("responses") as? ArrayList<*>)?.filterIsInstance<QuestionAnswer>()
            ?: emptyList()
        Log.d(TAG, "responseList size=${responseList.size}")

        commentEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                commentText = s.toString()
                updateConfirmButtonState()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        Log.d(TAG, "Calling fetchTakenSlots() …")
        fetchTakenSlots(loggedUser, applicationName) {
            Log.d(TAG, "fetchTakenSlots() completed: totalTechs=$totalTechs, takenSlotsMapDays=${takenSlotsMap.size}")
            if (totalTechs <= 0) {
                Log.w(TAG, "totalTechs <= 0 -> pas de techniciens. On n'initialise pas la liste de jours.")
                Toast.makeText(this, "Aucun technicien disponible pour le moment.", Toast.LENGTH_LONG).show()
                confirmButton.isEnabled = false
            } else {
                setupDaysRecyclerView()
            }
        }

        confirmButton.setOnClickListener {
            Log.d(TAG, "confirmButton clicked: selectedDate=$selectedDate, selectedTimeSlot=$selectedTimeSlot, comment='$commentText'")
            if (selectedDate != null && selectedTimeSlot != null && commentText.isNotBlank()) {
                lifecycleScope.launch {
                    try {
                        val dateKey = selectedDate!!.format(DateTimeFormatter.ISO_DATE) // ex: "2025-07-27"
                        val hourSlot = selectedTimeSlot!! // ex: "08:00"
                        Log.d(TAG, "Booking attempt for $dateKey $hourSlot")

                        val technicianEmail = fetchNearestTechnicianEmailSuspend(
                            loggedEmail, applicationName, dateKey, hourSlot
                        )
                        Log.d(TAG, "Nearest technician email = $technicianEmail")

                        if (technicianEmail == null) {
                            Toast.makeText(this@AppointmentActivity, "Aucun technicien disponible à ce créneau.", Toast.LENGTH_LONG).show()
                            return@launch
                        }

                        val formatterOutput = DateTimeFormatter.ofPattern("EEEE, dd MMMM", Locale.ENGLISH)
                        val formattedDate = "${selectedDate!!.format(formatterOutput)} $hourSlot"

                        val responsesPayload = responseList.map { ResponseItem(it.id, it.answer) }

                        val combinedRequest = AskRepairWithResponsesRequest(
                            loggedUser, formattedDate, commentText, qrData,
                            responsesPayload, applicationName, technicianEmail
                        )
                        Log.d(TAG, "Sending AskRepairWithResponsesRequest -> user=$loggedUser, date='$formattedDate', responses=${responsesPayload.size}, app=$applicationName, tech=$technicianEmail")

                        RetrofitClient.instance.sendAskRepairWithResponses(combinedRequest)
                            .enqueue(object : Callback<BaseResponse> {
                                override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                                    Log.d(TAG, "sendAskRepairWithResponses onResponse code=${response.code()} body=${response.body()}")
                                    if (response.isSuccessful && response.body()?.status == "success") {
                                        Toast.makeText(this@AppointmentActivity, response.body()?.message, Toast.LENGTH_SHORT).show()
                                        sendNotificationToAdmin(technicianEmail, applicationName)
                                        sendNotificationToUser(loggedEmail, applicationName)
                                        val prefs = getSharedPreferences("MySuccessPrefs", MODE_PRIVATE)
                                        prefs.edit().apply {
                                            putString("title", getString(R.string.request_sent))
                                            putString("content", getString(R.string.request_message_sent))
                                            putString("button", getString(R.string.show_history))
                                            apply()
                                        }
                                        startActivity(Intent(this@AppointmentActivity, SuccessActivity::class.java))
                                    } else {
                                        Log.w(TAG, "sendAskRepairWithResponses failed: status=${response.body()?.status}")
                                        Toast.makeText(this@AppointmentActivity, "Erreur lors de la réservation", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                                    Log.e(TAG, "sendAskRepairWithResponses onFailure", t)
                                    Toast.makeText(this@AppointmentActivity, "Erreur réseau : ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            })
                    } catch (e: Exception) {
                        Log.e(TAG, "Erreur lors du traitement de la date/booking", e)
                        Toast.makeText(this@AppointmentActivity, "Erreur lors du traitement de la date.", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(this, "Sélectionnez une date, un créneau et remplissez le commentaire.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchTakenSlots(user: String, application: String, onComplete: () -> Unit) {
        Log.d(TAG, "fetchTakenSlots(user='$user', application='$application')")
        if (user.isEmpty() || application.isEmpty()) {
            Log.w(TAG, "User or application is empty.")
            Toast.makeText(this, "Utilisateur ou application manquant.", Toast.LENGTH_SHORT).show()
            onComplete()
            return
        }

        RetrofitClient.instance.getTakenSlots(user, application).enqueue(object : Callback<TakenSlotsResponse> {
            override fun onResponse(call: Call<TakenSlotsResponse>, response: Response<TakenSlotsResponse>) {
                Log.d(TAG, "getTakenSlots onResponse code=${response.code()} body=${response.body()}")
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.status == "success") {
                        takenSlotsMap = body.taken_slots ?: emptyMap()
                        totalTechs = body.total_techs
                        Log.d(TAG, "Taken slots loaded: days=${takenSlotsMap.size}, totalTechs=$totalTechs")
                    } else {
                        Log.w(TAG, "Server returned error status or null body: body=$body")
                        Toast.makeText(this@AppointmentActivity, "Erreur serveur.", Toast.LENGTH_SHORT).show()
                        // on laisse totalTechs à 0 -> pas d’init de la liste (évite boucle)
                    }
                } else {
                    Log.w(TAG, "Unexpected response: code=${response.code()} msg=${response.message()}")
                    Toast.makeText(this@AppointmentActivity, "Réponse inattendue: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
                onComplete()
            }

            override fun onFailure(call: Call<TakenSlotsResponse>, t: Throwable) {
                Log.e(TAG, "getTakenSlots onFailure", t)
                Toast.makeText(this@AppointmentActivity, "Erreur de connexion: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                onComplete()
            }
        })
    }

    private fun setupDaysRecyclerView() {
        Log.d(TAG, "setupDaysRecyclerView() start")
        val days = generateUpcomingDays(14)
        Log.d(TAG, "generateUpcomingDays returned days.size=${days.size}")
        if (days.isEmpty()) {
            Log.w(TAG, "Aucune disponibilité trouvée (days empty).")
            Toast.makeText(this, "Aucune disponibilité trouvée.", Toast.LENGTH_SHORT).show()
            confirmButton.isEnabled = false
            return
        }

        val adapter = DaysAdapter(days) { dayItem, timeSlot ->
            Log.d(TAG, "Day selected: label='${dayItem.label}', date=${dayItem.localDate}, slot=$timeSlot")
            selectedDayLabel = dayItem.label
            selectedDate = dayItem.localDate
            selectedTimeSlot = timeSlot
            updateConfirmButtonState()

            commentEditText.requestFocus()
            commentEditText.post {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(commentEditText, InputMethodManager.SHOW_IMPLICIT)
            }
        }

        daysRecyclerView.layoutManager = LinearLayoutManager(this)
        daysRecyclerView.adapter = adapter
        Log.d(TAG, "setupDaysRecyclerView() finished")
    }

    private fun updateConfirmButtonState() {
        val enabled = selectedDate != null && !selectedTimeSlot.isNullOrEmpty() && commentText.isNotBlank()
        confirmButton.isEnabled = enabled
        confirmButton.alpha = if (enabled) 1.0f else 0.5f
        Log.d(TAG, "updateConfirmButtonState: enabled=$enabled (date=$selectedDate, slot=$selectedTimeSlot, commentLength=${commentText.length})")
    }

    private fun generateUpcomingDays(count: Int): List<DayItem> {
        val list = mutableListOf<DayItem>()
        val calendar = Calendar.getInstance()
        val formatter = java.text.SimpleDateFormat("EEEE dd MMMM", Locale.ENGLISH)

        // 🔒 garde-fou : si pas de techniciens, on ne calcule pas
        if (totalTechs <= 0) {
            Log.w(TAG, "generateUpcomingDays: totalTechs <= 0, retour liste vide.")
            return emptyList()
        }

        val maxScanDays = 365 // évite les boucles infinies si aucun slot n'est possible
        var scanned = 0

        while (list.size < count && scanned < maxScanDays) {
            val date = calendar.time
            val localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            val isWeekend = localDate.dayOfWeek in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
            val isHoliday = isPublicHoliday(localDate)

            if (!isWeekend && !isHoliday) {
                val availableSlots = generateTimeSlots(date)
                Log.v(TAG, "Day ${localDate}: slots=${availableSlots.size}")
                if (availableSlots.isNotEmpty()) {
                    list.add(DayItem(formatter.format(date), availableSlots, localDate))
                }
            } else {
                Log.v(TAG, "Day ${localDate}: skipped (weekend=$isWeekend, holiday=$isHoliday)")
            }

            calendar.add(Calendar.DAY_OF_YEAR, 1)
            scanned++
        }

        if (list.size < count) {
            Log.w(TAG, "generateUpcomingDays: only ${list.size}/$count jours trouvés après scan=$scanned (max=$maxScanDays)")
        }
        return list
    }

    private fun generateTimeSlots(date: Date): List<String> {
        if (totalTechs <= 0) {
            Log.w(TAG, "generateTimeSlots: totalTechs <= 0 -> []")
            return emptyList()
        }

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
            val available = takenCount < totalTechs
            Log.v(TAG, "  $dateKey $slot -> taken=$takenCount / totalTechs=$totalTechs -> available=$available")
            if (available) slots.add(slot)
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
        val isHoliday = date in fixed || date in listOf(easter.plusDays(1), easter.plusDays(39), easter.plusDays(50))
        if (isHoliday) Log.v(TAG, "isPublicHoliday($date) = true")
        return isHoliday
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
        Log.d(TAG, "sendNotificationToAdmin(email=$email, app=$appName)")
        RetrofitClient.instance.notifyAdmin(
            NotificationRequest("New request received", "admin", email, appName)
        ).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                Log.d(TAG, "notifyAdmin (admin) onResponse code=${response.code()}")
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e(TAG, "notifyAdmin (admin) onFailure", t)
            }
        })
    }

    private fun sendNotificationToUser(email: String, appName: String) {
        Log.d(TAG, "sendNotificationToUser(email=$email, app=$appName)")
        RetrofitClient.instance.notifyAdmin(
            NotificationRequest("New request sent successfully", "user", email, appName)
        ).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                Log.d(TAG, "notifyAdmin (user) onResponse code=${response.code()}")
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e(TAG, "notifyAdmin (user) onFailure", t)
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
            Log.d(TAG, "getNearestAdminEmail(email=$email, app=$application, date=$date, slot=$hourSlot)")
            RetrofitClient.instance.getNearestAdminEmail(TechnicianRequest(email, application, date, hourSlot))
                .enqueue(object : Callback<TechnicianResponse> {
                    override fun onResponse(call: Call<TechnicianResponse>, response: Response<TechnicianResponse>) {
                        Log.d(TAG, "getNearestAdminEmail onResponse code=${response.code()} body=${response.body()}")
                        if (response.isSuccessful) {
                            continuation.resume(response.body()?.email)
                        } else {
                            continuation.resume(null)
                        }
                    }
                    override fun onFailure(call: Call<TechnicianResponse>, t: Throwable) {
                        Log.e(TAG, "getNearestAdminEmail onFailure", t)
                        continuation.resume(null)
                    }
                })
        }
}
