package com.houssein.sezaia.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.houssein.sezaia.R
import com.houssein.sezaia.model.response.HelpItem

class HelpAdapter(private val helpList: List<HelpItem>) : RecyclerView.Adapter<HelpAdapter.HelpViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HelpViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_help, parent, false)
        return HelpViewHolder(view)
    }

    override fun onBindViewHolder(holder: HelpViewHolder, position: Int) {
        val helpItem = helpList[position]
        holder.bind(helpItem)

        holder.titleHelpTextView.setOnClickListener {
            // Inverse l'état d'expansion
            helpItem.isExpanded = !helpItem.isExpanded
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = helpList.size

    class HelpViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleHelpTextView: TextView = itemView.findViewById(R.id.helpTitle)
        val helpTextView: TextView = itemView.findViewById(R.id.helpDescription)

        fun bind(helpItem: HelpItem) {
            titleHelpTextView.text = helpItem.title_help
            helpTextView.text = helpItem.help
            helpTextView.visibility = if (helpItem.isExpanded) View.VISIBLE else View.GONE
        }
    }
}
