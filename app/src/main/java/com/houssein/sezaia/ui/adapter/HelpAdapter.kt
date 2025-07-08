package com.houssein.sezaia.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.houssein.sezaia.R
import com.houssein.sezaia.model.HelpItem

class HelpAdapter(private val items: List<HelpItem>) :
    RecyclerView.Adapter<HelpAdapter.HelpViewHolder>() {

    inner class HelpViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.helpTitle)
        val description: TextView = itemView.findViewById(R.id.helpDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HelpViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_help, parent, false)
        return HelpViewHolder(view)
    }

    override fun onBindViewHolder(holder: HelpViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.description.text = item.description

        holder.description.visibility = if (item.isExpanded) View.VISIBLE else View.GONE

        holder.title.setOnClickListener {
            item.isExpanded = !item.isExpanded
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = items.size
}
