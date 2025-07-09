package com.houssein.sezaia.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.houssein.sezaia.R

class QrCodeAdapter(
    private val qrCodes: List<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<QrCodeAdapter.QrCodeViewHolder>() {

    inner class QrCodeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val qrCodeText: TextView = view.findViewById(R.id.qrCodeText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QrCodeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_qrcode, parent, false)
        return QrCodeViewHolder(view)
    }

    override fun onBindViewHolder(holder: QrCodeViewHolder, position: Int) {
        val qr = qrCodes[position]
        holder.qrCodeText.text = qr
        holder.itemView.setOnClickListener {
            onItemClick(qr)
        }
    }

    override fun getItemCount(): Int = qrCodes.size
}

