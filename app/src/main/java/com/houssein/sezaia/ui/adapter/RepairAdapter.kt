package com.houssein.sezaia.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.houssein.sezaia.R
import com.houssein.sezaia.model.response.Repair

class RepairAdapter(
    private val repairs: List<Repair>,
    private val onDetailClick: (Repair) -> Unit
) : RecyclerView.Adapter<RepairAdapter.RepairViewHolder>() {

    inner class RepairViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        val qrTextView: TextView = itemView.findViewById(R.id.qrTextView)
        val btnDetails: Button = itemView.findViewById(R.id.btnDetail)
        val statusTextView: TextView = itemView.findViewById(R.id.statusTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RepairViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_repair, parent, false)
        return RepairViewHolder(view)
    }

    override fun onBindViewHolder(holder: RepairViewHolder, position: Int) {
        val repair = repairs[position]

        holder.dateTextView.text = repair.date
        holder.qrTextView.text = repair.qr_code ?: "N/A"

        // Capitalisation propre
        val status = (repair.status ?: "processing")
        val formattedStatus = status.replaceFirstChar { it.uppercase() }
        holder.statusTextView.text = formattedStatus

        // Couleur du texte selon le status
        val context = holder.itemView.context
        val colorRes = when (status.lowercase()) {
            "processing" -> R.color.yellow
            "repaired" -> R.color.green
            else -> android.R.color.darker_gray
        }
        holder.statusTextView.setTextColor(ContextCompat.getColor(context, colorRes))

        // Clic bouton détail
        holder.btnDetails.setOnClickListener {
            onDetailClick(repair)
        }
    }

    override fun getItemCount(): Int = repairs.size
}
