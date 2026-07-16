package com.convox.app.adapter

import android.graphics.Color
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.convox.app.databinding.ItemMessageReceivedBinding
import com.convox.app.databinding.ItemMessageSentBinding
import com.convox.app.model.Message

class MessagesAdapter(
    private val myUid: String,
    private val messages: MutableList<Message>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_SENT = 1
        const val TYPE_RECEIVED = 2
        private const val SEEN_BLUE = 0xFF34B7F1.toInt()   // WhatsApp blue-tick color
        private const val GRAY = 0xFF667781.toInt()
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == myUid) TYPE_SENT else TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_SENT) {
            SentVH(ItemMessageSentBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        } else {
            ReceivedVH(ItemMessageReceivedBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]
        val timeStr = if (msg.timestamp > 0)
            DateFormat.format("h:mm a", msg.timestamp).toString() else ""

        when (holder) {
            is SentVH -> {
                holder.binding.tvMessage.text = msg.text
                val ticks = if (msg.seen) " ✓✓" else " ✓"
                holder.binding.tvTime.text = timeStr + ticks
                holder.binding.tvTime.setTextColor(if (msg.seen) SEEN_BLUE else GRAY)
            }
            is ReceivedVH -> {
                holder.binding.tvMessage.text = msg.text
                holder.binding.tvTime.text = timeStr
            }
        }
    }

    override fun getItemCount(): Int = messages.size

    fun submitList(newList: List<Message>) {
        messages.clear()
        messages.addAll(newList)
        notifyDataSetChanged()
    }

    class SentVH(val binding: ItemMessageSentBinding) : RecyclerView.ViewHolder(binding.root)
    class ReceivedVH(val binding: ItemMessageReceivedBinding) : RecyclerView.ViewHolder(binding.root)
}
