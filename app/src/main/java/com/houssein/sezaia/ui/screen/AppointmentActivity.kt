package com.houssein.sezaia.ui.screen

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.houssein.sezaia.R
import com.houssein.sezaia.model.data.DayItem
import com.houssein.sezaia.ui.utils.UIUtils
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

class AppointmentActivity : AppCompatActivity() {

    private lateinit var daysRecyclerView: RecyclerView
    private lateinit var confirmButton: MaterialButton

    private var selectedDay: String? = null
    private var selectedTimeSlot: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appointment)

        UIUtils.applySystemBarsInsets(findViewById(R.id.main))
        UIUtils.initToolbar(
            this, getString(R.string.appointment),
            actionIconRes = R.drawable.baseline_density_medium_24,
            onBackClick = { finish() },
            onActionClick = { /* SettingsActivity ici si besoin */ }
        )

        daysRecyclerView = findViewById(R.id.daysRecyclerView)
        confirmButton = findViewById(R.id.confirmButton)

        setupDaysRecyclerView()

        confirmButton.setOnClickListener {
            if (selectedDay != null && selectedTimeSlot != null) {
                Toast.makeText(
                    this,
                    "Rendez-vous confirmé pour le $selectedDay à $selectedTimeSlot",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "Veuillez sélectionner un jour et un créneau horaire.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupDaysRecyclerView() {
        val days = generateUpcomingDays(14)
        val adapter = DaysAdapter(days) { dayLabel, timeSlot ->
            selectedDay = dayLabel
            selectedTimeSlot = timeSlot
        }
        daysRecyclerView.layoutManager = LinearLayoutManager(this)
        daysRecyclerView.adapter = adapter
    }

    private fun generateUpcomingDays(count: Int): List<DayItem> {
        val list = mutableListOf<DayItem>()
        val calendar = Calendar.getInstance()
        val formatter = SimpleDateFormat("EEEE, dd MMMM", Locale.FRENCH)

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
                val label = formatter.format(date)
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
}
