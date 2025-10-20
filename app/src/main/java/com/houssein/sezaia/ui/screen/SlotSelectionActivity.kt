package com.houssein.sezaia.ui.screen

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.widget.NestedScrollView
import com.google.android.material.button.MaterialButton
import com.houssein.sezaia.R
import com.houssein.sezaia.model.data.DayItem
import com.houssein.sezaia.model.data.MyApp
import com.houssein.sezaia.model.data.QuestionAnswer
import com.houssein.sezaia.model.response.TakenSlotsResponse
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.utils.UIUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SlotSelectionActivity : BaseActivity() {

    private lateinit var scroll: NestedScrollView
    private lateinit var daysRecyclerView: RecyclerView
    private lateinit var nextButton: MaterialButton

    private lateinit var applicationName: String
    private lateinit var loggedUser: String
    private lateinit var loggedEmail: String
    private lateinit var qrData: String

    private var totalTechs = 0
    private var selectedDate: LocalDate? = null
    private var selectedTimeSlot: String? = null
    private var takenSlotsMap: Map<String, Map<String, Int>> = emptyMap()

    private lateinit var responseList: ArrayList<QuestionAnswer> // passé à l'écran suivant

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_slot_selection)
        UIUtils.applySystemBarsInsets(findViewById(R.id.rootSlot))
        UIUtils.initToolbar(
            this, getString(R.string.appointment), R.drawable.baseline_density_medium_24,
            onBackClick = { finish() },
            onActionClick = { startActivity(Intent(this, SettingsActivity::class.java)) }
        )

        // Views
        scroll = findViewById(R.id.scroll)
        daysRecyclerView = findViewById(R.id.daysRecyclerView)
        nextButton = findViewById(R.id.nextButton)
        nextButton.isEnabled = false
        nextButton.alpha = 0.5f

        // Prefs
        val spLogin = getSharedPreferences("LoginData", MODE_PRIVATE)
        loggedUser = spLogin.getString("loggedUsername", "") ?: ""
        loggedEmail = spLogin.getString("LoggedEmail", "") ?: ""
        qrData = getSharedPreferences("MyPrefs", MODE_PRIVATE).getString("qrData", "") ?: ""
        applicationName = (application as MyApp).application_name

        @Suppress("UNCHECKED_CAST")
        responseList = (intent.getSerializableExtra("responses") as? ArrayList<QuestionAnswer>) ?: arrayListOf()

        fetchTakenSlots(loggedUser, applicationName) {
            if (totalTechs <= 0) {
                Toast.makeText(this, R.string.no_tech_available, Toast.LENGTH_LONG).show()
            } else {
                setupDaysRecyclerView()
            }
        }

        nextButton.setOnClickListener {
            val date = selectedDate
            val slot = selectedTimeSlot
            if (date == null || slot.isNullOrBlank()) {
                Toast.makeText(this, R.string.fill_required_fields, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val outFmt = DateTimeFormatter.ofPattern("EEEE, dd MMMM", Locale.ENGLISH)
            val formattedDate = "${date.format(outFmt)} $slot"

            val i = Intent(this, ConfirmAppointmentActivity::class.java).apply {
                putExtra("loggedUser", loggedUser)
                putExtra("loggedEmail", loggedEmail)
                putExtra("qrData", qrData)
                putExtra("applicationName", applicationName)
                putExtra("selectedDate", date.toString())   // ISO
                putExtra("selectedSlot", slot)
                putExtra("formattedDate", formattedDate)
                putExtra("responses", responseList)
            }
            startActivity(i)
        }
    }

    private fun setupDaysRecyclerView() {
        val days = generateUpcomingDays(14)
        val adapter = DaysAdapter(days) { dayItem, timeSlot ->
            selectedDate = dayItem.localDate
            selectedTimeSlot = timeSlot
            nextButton.isEnabled = true
            nextButton.alpha = 1f
        }
        daysRecyclerView.layoutManager = LinearLayoutManager(this)
        daysRecyclerView.adapter = adapter
        daysRecyclerView.isNestedScrollingEnabled = false
    }

    // ==== Réseau & dispo créneaux (reprend ta logique) ====

    private fun fetchTakenSlots(user: String, application: String, onComplete: () -> Unit) {
        RetrofitClient.instance.getTakenSlots(user, application)
            .enqueue(object : Callback<TakenSlotsResponse> {
                override fun onResponse(call: Call<TakenSlotsResponse>, response: Response<TakenSlotsResponse>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null && body.status == "success") {
                            takenSlotsMap = body.taken_slots ?: emptyMap()
                            totalTechs = body.total_techs
                        } else {
                            Toast.makeText(this@SlotSelectionActivity, "Erreur serveur.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@SlotSelectionActivity, "Réponse inattendue: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                    onComplete()
                }
                override fun onFailure(call: Call<TakenSlotsResponse>, t: Throwable) {
                    Toast.makeText(this@SlotSelectionActivity, "Erreur de connexion: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                    onComplete()
                }
            })
    }

    private fun generateUpcomingDays(count: Int): List<DayItem> {
        val list = mutableListOf<DayItem>()
        val calendar = Calendar.getInstance()
        val formatter = java.text.SimpleDateFormat("EEEE dd MMMM", Locale.ENGLISH)
        if (totalTechs <= 0) return emptyList()
        val maxScanDays = 365
        var scanned = 0
        while (list.size < count && scanned < maxScanDays) {
            val date = calendar.time
            val localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            val isWeekend = localDate.dayOfWeek in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
            val isHoliday = isPublicHoliday(localDate)
            if (!isWeekend && !isHoliday) {
                val available = generateTimeSlots(date)
                if (available.isNotEmpty()) list.add(DayItem(formatter.format(date), available, localDate))
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            scanned++
        }
        return list
    }

    private fun generateTimeSlots(date: Date): List<String> {
        if (totalTechs <= 0) return emptyList()
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
            if (takenCount < totalTechs) slots.add(slot)
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
}
