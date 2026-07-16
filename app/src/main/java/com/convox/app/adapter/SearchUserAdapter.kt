package com.convox.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.convox.app.databinding.ItemSearchUserBinding
import com.convox.app.model.Profile

data class SearchResult(val user: Profile, var state: String) // state: "add", "pending", "friends"

class SearchUserAdapter(
    private val results: MutableList<SearchResult>,
    private val onActionClick: (SearchResult, Int) -> Unit
) : RecyclerView.Adapter<SearchUserAdapter.VH>() {

    inner class VH(val binding: ItemSearchUserBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemSearchUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = results[position]
        holder.binding.tvUsername.text = item.user.username
        holder.binding.tvAvatar.text = item.user.username.take(1).uppercase()

        when (item.state) {
            "pending" -> {
                holder.binding.btnAction.text = "Pending"
                holder.binding.btnAction.isEnabled = false
            }
            "friends" -> {
                holder.binding.btnAction.text = "Friends"
                holder.binding.btnAction.isEnabled = false
            }
            else -> {
                holder.binding.btnAction.text = "Add"
                holder.binding.btnAction.isEnabled = true
            }
        }

        holder.binding.btnAction.setOnClickListener { onActionClick(item, position) }
    }

    override fun getItemCount(): Int = results.size

    fun submitList(newList: List<SearchResult>) {
        results.clear()
        results.addAll(newList)
        notifyDataSetChanged()
    }

    fun updateState(position: Int, state: String) {
        if (position in results.indices) {
            results[position].state = state
            notifyItemChanged(position)
        }
    }
}
