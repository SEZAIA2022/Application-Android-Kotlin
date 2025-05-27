package com.houssein.sezaia.ui.screen

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.button.MaterialButton
import androidx.recyclerview.widget.RecyclerView
import com.houssein.sezaia.R

class TimeSlotAdapter(private val timeSlots: List<String>) : RecyclerView.Adapter<TimeSlotAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timeButton: MaterialButton = view.findViewById(R.id.timeButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_time_slot, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val timeSlot = timeSlots[position]
        holder.timeButton.text = timeSlot
        holder.timeButton.setOnClickListener {
            // Gestion de la sélection
        }
    }

    override fun getItemCount(): Int = timeSlots.size
}
