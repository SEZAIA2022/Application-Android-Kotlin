package com.houssein.sezaia.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.houssein.sezaia.R
import com.houssein.sezaia.model.response.ReportSubmission
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class HistoryAdapter(
    private val submissions: List<ReportSubmission>
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_report_history, parent, false)
        return HistoryViewHolder(view as ViewGroup)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(submissions[position])
    }

    override fun getItemCount() = submissions.size

    inner class HistoryViewHolder(itemView: ViewGroup) : RecyclerView.ViewHolder(itemView) {
        fun bind(submission: ReportSubmission) {
            itemView.findViewById<TextView>(R.id.tvTitle).text = submission.title
            itemView.findViewById<TextView>(R.id.tvSubtitle).text = submission.subtitle
            itemView.findViewById<TextView>(R.id.tvUsername).text = "👤 ${submission.username ?: "Anonymous"}"
            itemView.findViewById<TextView>(R.id.tvQRCode).text = "🔷 ${submission.qr_code ?: "N/A"}"
            itemView.findViewById<TextView>(R.id.tvStatus).text = submission.status.uppercase()

            // Formater la date
            try {
                val formatter = DateTimeFormatter.ISO_DATE_TIME
                val dateTime = LocalDateTime.parse(submission.submitted_at, formatter)
                val readableFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                itemView.findViewById<TextView>(R.id.tvSubmittedAt).text = "📅 ${dateTime.format(readableFormat)}"
            } catch (e: Exception) {
                itemView.findViewById<TextView>(R.id.tvSubmittedAt).text = "📅 ${submission.submitted_at}"
            }
        }
    }
}
