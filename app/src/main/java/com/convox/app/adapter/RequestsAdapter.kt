package com.convox.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.convox.app.databinding.ItemFriendRequestBinding
import com.convox.app.model.FriendRequest

class RequestsAdapter(
    private val requests: MutableList<FriendRequest>,
    private val onAccept: (FriendRequest) -> Unit,
    private val onReject: (FriendRequest) -> Unit
) : RecyclerView.Adapter<RequestsAdapter.VH>() {

    var sentMode = false

    inner class VH(val binding: ItemFriendRequestBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemFriendRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val req = requests[position]
        val name = if (sentMode) req.toUsername else req.fromUsername
        holder.binding.tvUsername.text = name
        holder.binding.tvAvatar.text = name.take(1).uppercase()

        if (sentMode) {
            holder.binding.btnAccept.visibility = View.GONE
            holder.binding.btnReject.visibility = View.GONE
        } else {
            holder.binding.btnAccept.visibility = View.VISIBLE
            holder.binding.btnReject.visibility = View.VISIBLE
            holder.binding.btnAccept.setOnClickListener { onAccept(req) }
            holder.binding.btnReject.setOnClickListener { onReject(req) }
        }
    }

    override fun getItemCount(): Int = requests.size

    fun submitList(newList: List<FriendRequest>) {
        requests.clear()
        requests.addAll(newList)
        notifyDataSetChanged()
    }
}
