package com.houssein.sezaia.ui.screen

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.button.MaterialButton
import androidx.recyclerview.widget.RecyclerView
import com.houssein.sezaia.R

class TimeSlotAdapter(
    private val dayLabel: String,
    private val timeSlots: List<String>,
    private val selectedTime: String?,
    private val onTimeSlotSelected: (String, String) -> Unit
) : RecyclerView.Adapter<TimeSlotAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timeButton: MaterialButton = view.findViewById(R.id.timeButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_time_slot, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val timeSlot = timeSlots[position]
        val isSelected = timeSlot == selectedTime

        holder.timeButton.text = timeSlot

        // Style visuel sélectionné ou non
        if (isSelected) {
            holder.timeButton.setBackgroundColor(holder.itemView.context.getColor(R.color.blue))
            holder.timeButton.setTextColor(holder.itemView.context.getColor(R.color.white))
        } else {
            holder.timeButton.setBackgroundColor(holder.itemView.context.getColor(android.R.color.transparent))
            holder.timeButton.setTextColor(holder.itemView.context.getColor(R.color.blue))
        }

        // Action de clic : notifier la sélection
        holder.timeButton.setOnClickListener {
            onTimeSlotSelected(dayLabel, timeSlot)
        }
    }

    override fun getItemCount(): Int = timeSlots.size
}
