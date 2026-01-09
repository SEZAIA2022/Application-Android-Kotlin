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
import com.houssein.sezaia.ui.screen.SimpleQuestionWithAnswer

class SimpleQuestionsReportStyleAdapter(
    private val questions: List<SimpleQuestionWithAnswer>,
    private val onAnswerChanged: (Int, String) -> Unit
) : RecyclerView.Adapter<SimpleQuestionsReportStyleAdapter.QuestionVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionVH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_report_question, parent, false)
        return QuestionVH(v, onAnswerChanged)
    }

    override fun onBindViewHolder(holder: QuestionVH, position: Int) {
        holder.bind(questions[position])
    }

    override fun getItemCount(): Int = questions.size

    inner class QuestionVH(
        itemView: View,
        private val onAnswerChanged: (Int, String) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvQuestionText: TextView = itemView.findViewById(R.id.tvQuestionText)
        private val etAnswer: EditText = itemView.findViewById(R.id.etAnswer)
        private val rgOptions: RadioGroup = itemView.findViewById(R.id.rgOptions)
        private val rgYesNo: RadioGroup = itemView.findViewById(R.id.rgYesNo)
        private val rbYes: RadioButton = itemView.findViewById(R.id.rbYes)
        private val rbNo: RadioButton = itemView.findViewById(R.id.rbNo)

        fun bind(q: SimpleQuestionWithAnswer) {
            tvQuestionText.text = if (q.is_required) " ${q.question_text}" else q.question_text

            // reset listeners (important avec RecyclerView)
            etAnswer.setOnFocusChangeListener(null)
            etAnswer.setOnEditorActionListener(null)
            rgOptions.setOnCheckedChangeListener(null)
            rgYesNo.setOnCheckedChangeListener(null)

            // reset UI
            etAnswer.visibility = View.GONE
            rgOptions.visibility = View.GONE
            rgYesNo.visibility = View.GONE

            // reset selections
            rgOptions.removeAllViews()
            rgYesNo.clearCheck()

            when (q.question_type) {
                "open" -> bindOpen(q)
                "qcm" -> bindQcm(q)
                "yes_no" -> bindYesNo(q)
                else -> bindOpen(q)
            }
        }

        private fun bindOpen(q: SimpleQuestionWithAnswer) {
            etAnswer.visibility = View.VISIBLE
            etAnswer.setText(q.answer)

            // capture quand il quitte le champ
            etAnswer.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val ans = etAnswer.text.toString().trim()
                    if (q.answer != ans) {
                        q.answer = ans
                        onAnswerChanged(q.id, ans)
                    }
                }
            }

            // capture aussi quand il valide clavier (optionnel)
            etAnswer.setOnEditorActionListener { _, _, _ ->
                val ans = etAnswer.text.toString().trim()
                if (q.answer != ans) {
                    q.answer = ans
                    onAnswerChanged(q.id, ans)
                }
                false
            }
        }

        private fun bindQcm(q: SimpleQuestionWithAnswer) {
            rgOptions.visibility = View.VISIBLE

            // create radios
            q.options.forEach { opt ->
                val rb = RadioButton(itemView.context).apply {
                    id = View.generateViewId()
                    text = opt
                    isChecked = (opt == q.answer)
                }
                rgOptions.addView(rb)
            }

            rgOptions.setOnCheckedChangeListener { group, checkedId ->
                if (checkedId != -1) {
                    val selected = group.findViewById<RadioButton>(checkedId)
                    val ans = selected.text.toString().trim()
                    if (q.answer != ans) {
                        q.answer = ans
                        onAnswerChanged(q.id, ans)
                    }
                }
            }
        }

        private fun bindYesNo(q: SimpleQuestionWithAnswer) {
            rgYesNo.visibility = View.VISIBLE

            // restore answer
            when (q.answer) {
                "yes" -> rbYes.isChecked = true
                "no" -> rbNo.isChecked = true
                else -> rgYesNo.clearCheck()
            }

            rgYesNo.setOnCheckedChangeListener { _, checkedId ->
                val ans = when (checkedId) {
                    rbYes.id -> "yes"
                    rbNo.id -> "no"
                    else -> ""
                }

                if (ans.isNotEmpty() && q.answer != ans) {
                    q.answer = ans
                    onAnswerChanged(q.id, ans)
                }
            }
        }
    }
}
