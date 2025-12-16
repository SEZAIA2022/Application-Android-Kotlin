package com.houssein.sezaia.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.houssein.sezaia.R
import com.houssein.sezaia.ui.screen.ReportQuestion
import com.houssein.sezaia.ui.screen.ReportUiItem

class ReportAdapter(
    private val items: List<ReportUiItem>,
    private val onAnswerChanged: (Int, String) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is ReportUiItem.TitleItem -> 0
        is ReportUiItem.SubtitleItem -> 1
        is ReportUiItem.QuestionItem -> 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            0 -> TitleViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_report_title, parent, false))
            1 -> SubtitleViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_report_subtitle, parent, false))
            else -> QuestionViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_report_question, parent, false), onAnswerChanged)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ReportUiItem.TitleItem -> (holder as TitleViewHolder).bind(item.title)
            is ReportUiItem.SubtitleItem -> (holder as SubtitleViewHolder).bind(item.subtitle)
            is ReportUiItem.QuestionItem -> (holder as QuestionViewHolder).bind(item.question)
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

    inner class QuestionViewHolder(itemView: View, val onAnswerChanged: (Int, String) -> Unit) : RecyclerView.ViewHolder(itemView) {
        fun bind(question: ReportQuestion) {
            itemView.findViewById<TextView>(R.id.tvQuestionText).text = question.question_text

            when (question.question_type) {
                "open" -> bindOpenQuestion(question)
                "qcm" -> bindQcmQuestion(question)
                "yes_no" -> bindYesNoQuestion(question)
            }
        }

        private fun bindOpenQuestion(question: ReportQuestion) {
            val etAnswer = itemView.findViewById<EditText>(R.id.etAnswer)
            itemView.findViewById<RadioGroup>(R.id.rgOptions).visibility = View.GONE
            itemView.findViewById<RadioGroup>(R.id.rgYesNo).visibility = View.GONE

            etAnswer.visibility = View.VISIBLE

            // Capturer la réponse au moment où l'utilisateur quitte le champ
            etAnswer.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val answer = etAnswer.text.toString().trim()
                    onAnswerChanged(question.id, answer)
                }
            }

            // Aussi capturer quand l'utilisateur change le texte (optionnel)
            etAnswer.setOnEditorActionListener { _, _, _ ->
                val answer = etAnswer.text.toString().trim()
                onAnswerChanged(question.id, answer)
                false
            }
        }

        private fun bindQcmQuestion(question: ReportQuestion) {
            val rgOptions = itemView.findViewById<RadioGroup>(R.id.rgOptions)
            itemView.findViewById<EditText>(R.id.etAnswer).visibility = View.GONE
            itemView.findViewById<RadioGroup>(R.id.rgYesNo).visibility = View.GONE

            rgOptions.visibility = View.VISIBLE
            rgOptions.removeAllViews()

            // ✅ IMPORTANT: On doit créer les RadioButtons avec des IDs uniques
            question.options?.forEachIndexed { index, option ->
                val rb = RadioButton(itemView.context)
                rb.id = View.generateViewId()  // Générer un ID unique
                rb.text = option

                rgOptions.addView(rb)
            }

            // ✅ Écouter le changement de RadioButton sélectionné
            rgOptions.setOnCheckedChangeListener { _, checkedId ->
                if (checkedId != -1) {  // -1 = aucun bouton sélectionné
                    val selectedRb = itemView.findViewById<RadioButton>(checkedId)
                    val answer = selectedRb.text.toString().trim()

                    // ✅ Envoyer la réponse sélectionnée
                    onAnswerChanged(question.id, answer)
                }
            }
        }

        private fun bindYesNoQuestion(question: ReportQuestion) {
            val rgYesNo = itemView.findViewById<RadioGroup>(R.id.rgYesNo)
            itemView.findViewById<EditText>(R.id.etAnswer).visibility = View.GONE
            itemView.findViewById<RadioGroup>(R.id.rgOptions).visibility = View.GONE

            rgYesNo.visibility = View.VISIBLE

            // ✅ Écouter le changement Oui/Non
            rgYesNo.setOnCheckedChangeListener { _, checkedId ->
                val answer = when (checkedId) {
                    itemView.findViewById<RadioButton>(R.id.rbYes).id -> "yes"
                    itemView.findViewById<RadioButton>(R.id.rbNo).id -> "no"
                    else -> ""
                }

                if (answer.isNotEmpty()) {
                    onAnswerChanged(question.id, answer)
                }
            }
        }
    }
}
