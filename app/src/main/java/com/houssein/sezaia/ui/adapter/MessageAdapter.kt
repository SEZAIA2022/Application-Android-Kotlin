package com.houssein.sezaia.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.houssein.sezaia.R
import com.houssein.sezaia.model.response.Message

class MessageAdapter(private val messages: List<Message>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_BOT = 0
        const val TYPE_USER = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) TYPE_USER else TYPE_BOT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_USER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_user, parent, false)
            UserViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_bot, parent, false)
            BotViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        if (holder is UserViewHolder) {
            holder.textViewMessage.text = message.text
        } else if (holder is BotViewHolder) {
            holder.textViewMessage.text = message.text
        }
    }

    override fun getItemCount() = messages.size

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewMessage: TextView = itemView.findViewById(R.id.textViewMessage)
    }

    class BotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewMessage: TextView = itemView.findViewById(R.id.textViewMessage)
    }
}
