package com.houssein.sezaia.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.houssein.sezaia.R
import com.houssein.sezaia.model.response.ReportSubmission
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class HistoryAdapter(
    private val qrId: String,
    private val username: String,
    private val serialNumber: String,
    private val submissions: List<ReportSubmission>,
    private val onViewReport: (ReportSubmission) -> Unit

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
        @SuppressLint("SetTextI18n")
        fun bind(submission: ReportSubmission) {
            itemView.findViewById<TextView>(R.id.tvTitle).text = "QR ID: $qrId"
            itemView.findViewById<TextView>(R.id.tvSubtitle).text = "👤 Client: $username"
            itemView.findViewById<TextView>(R.id.tvUsername).text = "👷Technician: ${submission.tech_user ?: "Unknown"}"
            itemView.findViewById<TextView>(R.id.tvQRCode).text = "🔷Serial number: $serialNumber"

            // Formater la date
            try {
                val formatter = DateTimeFormatter.ISO_DATE_TIME
                val dateTime = LocalDateTime.parse(submission.submitted_at, formatter)
                val readableFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                itemView.findViewById<TextView>(R.id.tvSubmittedAt).text = "📅 ${dateTime.format(readableFormat)}"
            } catch (e: Exception) {
                itemView.findViewById<TextView>(R.id.tvSubmittedAt).text = "📅 ${submission.submitted_at}"
            }

            itemView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnViewReport)
                .setOnClickListener {
                    onViewReport(submission)
                }
        }
    }
}
