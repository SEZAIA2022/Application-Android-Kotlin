package com.houssein.sezaia.ui.screen

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.houssein.sezaia.R
import com.houssein.sezaia.model.DayItem

class DaysAdapter(private val days: List<DayItem>) : RecyclerView.Adapter<DaysAdapter.DayViewHolder>() {

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

        holder.dayCard.setCardBackgroundColor(
            holder.itemView.context.getColor(
                if (day.isSelected) R.color.white else R.color.white
            )
        )

        holder.dayButton.setOnClickListener {
            days.forEach { it.isSelected = false }
            day.isSelected = true
            notifyDataSetChanged()
        }

        holder.timeSlotRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context, RecyclerView.HORIZONTAL, false)
        holder.timeSlotRecyclerView.adapter = if (day.isSelected) TimeSlotAdapter(day.timeSlots) else null
    }

    override fun getItemCount(): Int = days.size
}
