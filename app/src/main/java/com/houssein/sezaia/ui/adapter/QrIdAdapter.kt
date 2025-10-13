package com.houssein.sezaia.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.houssein.sezaia.R

class QrIdAdapter(
    private val items: List<String>,
    private val onRequestClick: (qrId: String) -> Unit
) : RecyclerView.Adapter<QrIdAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val qrText: TextView = view.findViewById(R.id.qrTextView)
        val btnRequest: Button = view.findViewById(R.id.btnIntervention)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_qr_id, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val id = items[pos]
        h.qrText.text = "$id"
        h.btnRequest.text = h.itemView.context.getString(R.string.request_intervention)
        h.btnRequest.setOnClickListener { onRequestClick(id) }
    }

    override fun getItemCount() = items.size
}
