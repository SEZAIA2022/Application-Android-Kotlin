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
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class RepairAdapterDaily(
    private val repairs: List<Repair>,
    private val onDetailClick: (Repair) -> Unit
) : RecyclerView.Adapter<RepairAdapterDaily.RepairViewHolder>() {

    private val localeFr = Locale.FRENCH
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm", localeFr)

    inner class RepairViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.dateTextView)      // haut → gros texte
        val subtitleTextView: TextView = itemView.findViewById(R.id.qrTextView)     // bas → petite heure
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

        // -------- Haut : qr_id + username --------
        val qrInfo = buildString {
            append(repair.qr_code?.toString() ?: repair.qr_code ?: "—")
            if (!repair.username.isNullOrBlank()) append("  •  ${repair.username}")
        }
        holder.titleTextView.text = qrInfo

        // -------- Bas : heure --------
        holder.subtitleTextView.text = formatHour(repair.hour_slot)

        // -------- Statut + couleurs --------
        val status = repair.status.orEmpty()
        holder.statusTextView.text = status.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(localeFr) else it.toString()
        }

        val context = holder.itemView.context
        val colorRes = when (status.lowercase()) {
            "processing" -> R.color.yellow
            "repaired" -> R.color.green
            else -> android.R.color.darker_gray
        }
        holder.statusTextView.setTextColor(ContextCompat.getColor(context, colorRes))

        // -------- Bouton "Détails" --------
        holder.btnDetails.setOnClickListener { onDetailClick(repair) }
    }

    override fun getItemCount(): Int = repairs.size

    private fun formatHour(hourStr: String?): String {
        if (hourStr.isNullOrBlank()) return ""
        return try {
            val t = LocalTime.parse(hourStr)
            "🕒 ${t.format(timeFmt)}"
        } catch (_: Exception) {
            hourStr
        }
    }
}
