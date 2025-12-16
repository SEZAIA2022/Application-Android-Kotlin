package com.houssein.sezaia.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.houssein.sezaia.R
import com.houssein.sezaia.ui.screen.QuestionWithAnswer

sealed class VerifyReportUiItem {
    data class TitleItem(val title: String) : VerifyReportUiItem()
    data class SubtitleItem(val subtitle: String) : VerifyReportUiItem()
    data class QuestionItem(val question: QuestionWithAnswer) : VerifyReportUiItem()
}

class VerifyReportAdapter(
    private val items: List<VerifyReportUiItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is VerifyReportUiItem.TitleItem -> 0
        is VerifyReportUiItem.SubtitleItem -> 1
        is VerifyReportUiItem.QuestionItem -> 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            0 -> TitleViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_report_title, parent, false))
            1 -> SubtitleViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_report_subtitle, parent, false))
            else -> QuestionViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_verify_question, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is VerifyReportUiItem.TitleItem -> (holder as TitleViewHolder).bind(item.title)
            is VerifyReportUiItem.SubtitleItem -> (holder as SubtitleViewHolder).bind(item.subtitle)
            is VerifyReportUiItem.QuestionItem -> (holder as QuestionViewHolder).bind(item.question)
        }
    }

    override fun getItemCount() = items.size

    inner class TitleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(title: String) {
            itemView.findViewById<TextView>(R.id.tvTitle).text = title
        }
    }

    inner class SubtitleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(subtitle: String) {
            itemView.findViewById<TextView>(R.id.tvSubtitle).text = subtitle
        }
    }

    inner class QuestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(question: QuestionWithAnswer) {
            itemView.findViewById<TextView>(R.id.tvQuestionText).text = question.question_text

            when (question.question_type) {
                "open" -> displayOpenAnswer(question)
                "qcm" -> displayQcmAnswer(question)
                "yes_no" -> displayYesNoAnswer(question)
            }
        }

        private fun displayOpenAnswer(question: QuestionWithAnswer) {
            val answerContainer = itemView.findViewById<LinearLayout>(R.id.answerContainer)
            itemView.findViewById<RadioGroup>(R.id.rgOptions).visibility = View.GONE
            itemView.findViewById<RadioGroup>(R.id.rgYesNo).visibility = View.GONE

            answerContainer.visibility = View.VISIBLE
            answerContainer.removeAllViews()

            // Afficher la réponse en texte lisible
            val tvAnswer = TextView(itemView.context)
            tvAnswer.text = question.answer.ifEmpty { "Unanswered" }
            tvAnswer.setPadding(8, 8, 8, 8)
            tvAnswer.setBackgroundResource(R.drawable.answer_display_bg)
            tvAnswer.setTextColor(itemView.context.resources.getColor(android.R.color.holo_blue_dark))

            answerContainer.addView(tvAnswer)
        }

        private fun displayQcmAnswer(question: QuestionWithAnswer) {
            val rgOptions = itemView.findViewById<RadioGroup>(R.id.rgOptions)
            itemView.findViewById<LinearLayout>(R.id.answerContainer).visibility = View.GONE
            itemView.findViewById<RadioGroup>(R.id.rgYesNo).visibility = View.GONE

            rgOptions.visibility = View.VISIBLE
            rgOptions.removeAllViews()
            rgOptions.isEnabled = false  // Désactiver l'interaction

            // Créer les RadioButton avec celui sélectionné cochés
            question.options?.forEach { option ->
                val rb = RadioButton(itemView.context)
                rb.id = View.generateViewId()
                rb.text = option
                rb.isEnabled = false  // Désactiver l'interaction

                // Cocher si c'est la réponse
                if (option == question.answer) {
                    rb.isChecked = true
                }

                rgOptions.addView(rb)
            }
        }

        private fun displayYesNoAnswer(question: QuestionWithAnswer) {
            val rgYesNo = itemView.findViewById<RadioGroup>(R.id.rgYesNo)
            itemView.findViewById<LinearLayout>(R.id.answerContainer).visibility = View.GONE
            itemView.findViewById<RadioGroup>(R.id.rgOptions).visibility = View.GONE

            rgYesNo.visibility = View.VISIBLE
            rgYesNo.isEnabled = false  // Désactiver l'interaction

            val rbYes = itemView.findViewById<RadioButton>(R.id.rbYes)
            val rbNo = itemView.findViewById<RadioButton>(R.id.rbNo)

            rbYes.isEnabled = false
            rbNo.isEnabled = false

            // Cocher la bonne réponse
            when (question.answer) {
                "yes" -> rbYes.isChecked = true
                "no" -> rbNo.isChecked = true
            }
        }
    }
}
