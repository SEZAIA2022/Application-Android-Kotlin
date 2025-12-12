package com.houssein.sezaia.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.houssein.sezaia.R
import com.houssein.sezaia.ui.screen.ReportQuestion
import com.houssein.sezaia.ui.screen.ReportUiItem

class ReportAdapter(
    private val items: List<ReportUiItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_TITLE = 1
        private const val VIEW_TYPE_SUBTITLE = 2
        private const val VIEW_TYPE_QUESTION = 3
    }

    override fun getItemViewType(position: Int): Int =
        when (items[position]) {
            is ReportUiItem.TitleItem -> VIEW_TYPE_TITLE
            is ReportUiItem.SubtitleItem -> VIEW_TYPE_SUBTITLE
            is ReportUiItem.QuestionItem -> VIEW_TYPE_QUESTION
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_TITLE -> {
                val v = inf.inflate(R.layout.item_report_title, parent, false)
                TitleViewHolder(v)
            }
            VIEW_TYPE_SUBTITLE -> {
                val v = inf.inflate(R.layout.item_report_subtitle, parent, false)
                SubtitleViewHolder(v)
            }
            VIEW_TYPE_QUESTION -> {
                val v = inf.inflate(R.layout.item_report_question, parent, false)
                QuestionViewHolder(v)
            }
            else -> throw IllegalArgumentException("Unknown viewType=$viewType")
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val it = items[position]) {
            is ReportUiItem.TitleItem -> (holder as TitleViewHolder).bind(it)
            is ReportUiItem.SubtitleItem -> (holder as SubtitleViewHolder).bind(it)
            is ReportUiItem.QuestionItem -> (holder as QuestionViewHolder).bind(it.question)
        }
    }

    class TitleViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        private val txt: TextView = v.findViewById(R.id.txtReportTitle)
        fun bind(item: ReportUiItem.TitleItem) {
            txt.text = item.title
        }
    }

    class SubtitleViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        private val txt: TextView = v.findViewById(R.id.txtReportSubtitle)
        fun bind(item: ReportUiItem.SubtitleItem) {
            txt.text = item.subtitle
        }
    }

    class QuestionViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        private val card: MaterialCardView = v.findViewById(R.id.cardQuestion)
        private val txtQuestion: TextView = v.findViewById(R.id.txtQuestionLabel)
        private val editAnswer: EditText = v.findViewById(R.id.editAnswerOpen)

        private val layoutYesNo: LinearLayout = v.findViewById(R.id.layoutYesNo)
        private val cbYes: CheckBox = v.findViewById(R.id.cbYes)
        private val cbNo: CheckBox = v.findViewById(R.id.cbNo)

        private val layoutQcm: LinearLayout = v.findViewById(R.id.layoutQcmOptions)

        fun bind(q: ReportQuestion) {
            txtQuestion.text = q.question_text

            // Reset visibilités
            editAnswer.visibility = View.GONE
            layoutYesNo.visibility = View.GONE
            layoutQcm.visibility = View.GONE

            when (q.question_type) {
                "open" -> {
                    editAnswer.visibility = View.VISIBLE
                }
                "yes_no" -> {
                    layoutYesNo.visibility = View.VISIBLE

                    cbYes.setOnCheckedChangeListener(null)
                    cbNo.setOnCheckedChangeListener(null)
                    cbYes.isChecked = false
                    cbNo.isChecked = false

                    cbYes.setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) cbNo.isChecked = false
                    }
                    cbNo.setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) cbYes.isChecked = false
                    }
                }
                "qcm" -> {
                    layoutQcm.visibility = View.VISIBLE
                    layoutQcm.removeAllViews()

                    q.options?.forEach { optText ->
                        val cb = CheckBox(itemView.context).apply {
                            text = optText
                        }
                        layoutQcm.addView(cb)
                    }
                }
            }

            if (q.is_required) {
                card.strokeWidth = 3
                // ⚠️ assure-toi d'avoir une couleur red dans colors.xml
                card.strokeColor = itemView.context.getColor(R.color.red)
            } else {
                card.strokeWidth = 0
            }
        }
    }
}
