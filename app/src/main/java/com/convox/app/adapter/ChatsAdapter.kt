package com.convox.app.adapter

import android.graphics.Typeface
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.convox.app.databinding.ItemChatBinding
import com.convox.app.model.Chat

class ChatsAdapter(
    private val myUid: String,
    private val chats: MutableList<Chat>,
    private val onClick: (Chat) -> Unit
) : RecyclerView.Adapter<ChatsAdapter.VH>() {

    private var unreadCounts: Map<String, Int> = emptyMap()

    inner class VH(val binding: ItemChatBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemChatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val chat = chats[position]
        val otherName = chat.otherUsername(myUid)
        val unread = unreadCounts[chat.id] ?: 0

        holder.binding.tvUsername.text = otherName
        holder.binding.tvAvatar.text = otherName.take(1).uppercase()
        holder.binding.tvLastMessage.text = chat.lastMessage.ifEmpty { "Say hi 👋" }
        holder.binding.tvTime.text = if (chat.lastMessageTime > 0)
            DateUtils.getRelativeTimeSpanString(chat.lastMessageTime) else ""

        if (unread > 0) {
            holder.binding.tvUnread.visibility = View.VISIBLE
            holder.binding.tvUnread.text = if (unread > 99) "99+" else unread.toString()
            holder.binding.tvLastMessage.setTypeface(null, Typeface.BOLD)
        } else {
            holder.binding.tvUnread.visibility = View.GONE
            holder.binding.tvLastMessage.setTypeface(null, Typeface.NORMAL)
        }

        holder.binding.root.setOnClickListener { onClick(chat) }
    }

    override fun getItemCount(): Int = chats.size

    fun submitList(newChats: List<Chat>) {
        chats.clear()
        chats.addAll(newChats)
        notifyDataSetChanged()
    }

    fun setUnreadCounts(counts: Map<String, Int>) {
        unreadCounts = counts
        notifyDataSetChanged()
    }
}
