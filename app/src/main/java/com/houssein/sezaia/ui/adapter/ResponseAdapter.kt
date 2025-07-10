package com.houssein.sezaia.ui.adapter

import ResponseItem
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.houssein.sezaia.R

class ResponseAdapter(private val responses: List<ResponseItem>) :
    RecyclerView.Adapter<ResponseAdapter.ResponseViewHolder>() {

    class ResponseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val questionText: TextView = view.findViewById(R.id.questionText)
        val responseText: TextView = view.findViewById(R.id.responseText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResponseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_response, parent, false)
        return ResponseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ResponseViewHolder, position: Int) {
        val item = responses[position]
        holder.questionText.text = "Q: ${item.question_text ?: "Question inconnue"}"
        holder.responseText.text = "R: ${item.response ?: "Pas de réponse"}"
    }

    override fun getItemCount(): Int = responses.size
}
