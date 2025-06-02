package com.houssein.sezaia.ui.screen

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.houssein.sezaia.R
import com.houssein.sezaia.model.data.DayItem

class DaysAdapter(
    private val days: List<DayItem>,
    private val onTimeSlotSelected: (String, String) -> Unit
) : RecyclerView.Adapter<DaysAdapter.DayViewHolder>() {

    private var selectedDayPosition: Int = RecyclerView.NO_POSITION
    private var selectedTimeSlot: String? = null

    class DayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dayCard: CardView = view.findViewById(R.id.dayCard)
        val dayButton: TextView = view.findViewById(R.id.dayTextView)
        val timeSlotRecyclerView: RecyclerView = view.findViewById(R.id.timeSlotRecyclerView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_date_button, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val day = days[position]
        holder.dayButton.text = day.label

        holder.timeSlotRecyclerView.layoutManager = GridLayoutManager(holder.itemView.context, 3)

        // Afficher les créneaux seulement si ce jour est sélectionné
        if (position == selectedDayPosition) {
            holder.timeSlotRecyclerView.adapter = TimeSlotAdapter(
                day.label,
                day.timeSlots,
                selectedTimeSlot
            ) { selectedDay, selectedTime ->
                selectedTimeSlot = selectedTime
                onTimeSlotSelected(selectedDay, selectedTime)
                notifyDataSetChanged()
            }
            holder.timeSlotRecyclerView.visibility = View.VISIBLE
        } else {
            holder.timeSlotRecyclerView.adapter = null
            holder.timeSlotRecyclerView.visibility = View.GONE
        }

        holder.dayButton.setOnClickListener {
            if (selectedDayPosition != position) {
                selectedDayPosition = position
                selectedTimeSlot = null
                notifyDataSetChanged()
            }
        }
    }

    override fun getItemCount(): Int = days.size
}
