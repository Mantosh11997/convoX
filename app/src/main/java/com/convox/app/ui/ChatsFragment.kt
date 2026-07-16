package com.convox.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.convox.app.adapter.ChatsAdapter
import com.convox.app.databinding.FragmentChatsBinding
import com.convox.app.model.Chat
import com.convox.app.model.Message
import com.convox.app.util.Supa
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ChatsFragment : Fragment() {

    private var _binding: FragmentChatsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ChatsAdapter
    private var channel: RealtimeChannel? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val myUid = Supa.currentUid ?: return

        adapter = ChatsAdapter(myUid, mutableListOf()) { chat ->
            val intent = Intent(requireContext(), ChatActivity::class.java)
            intent.putExtra("chatId", chat.id)
            intent.putExtra("otherUid", chat.otherUid(myUid))
            intent.putExtra("otherUsername", chat.otherUsername(myUid))
            startActivity(intent)
        }
        binding.rvChats.layoutManager = LinearLayoutManager(requireContext())
        binding.rvChats.adapter = adapter

        binding.fabAddFriend.setOnClickListener {
            startActivity(Intent(requireContext(), AddFriendActivity::class.java))
        }

        subscribeRealtime(myUid)

        // Poll list + unread counts every 3 seconds while visible
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    loadChats(myUid)
                    delay(3000)
                }
            }
        }
    }

    private fun loadChats(myUid: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val chats = Supa.client.postgrest["chats"].select {
                    filter { or { eq("user_a", myUid); eq("user_b", myUid) } }
                    order("last_message_time", Order.DESCENDING)
                }.decodeList<Chat>()

                // Unseen messages sent to me (RLS already limits to my chats)
                val unseen = Supa.client.postgrest["messages"].select {
                    filter {
                        eq("seen", false)
                        neq("sender_id", myUid)
                    }
                }.decodeList<Message>()
                val counts = unseen.groupingBy { it.chatId }.eachCount()

                if (_binding == null) return@launch
                adapter.submitList(chats)
                adapter.setUnreadCounts(counts)
                binding.tvEmpty.visibility = if (chats.isEmpty()) View.VISIBLE else View.GONE
            } catch (_: Exception) { }
        }
    }

    private fun subscribeRealtime(myUid: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val ch = Supa.client.channel("chats-list")
                channel = ch
                ch.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "chats"
                }.onEach {
                    loadChats(myUid)
                }.launchIn(viewLifecycleOwner.lifecycleScope)
                ch.subscribe()
            } catch (_: Exception) { }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val ch = channel
        channel = null
        if (ch != null) {
            lifecycleScope.launch { try { ch.unsubscribe() } catch (_: Exception) {} }
        }
        _binding = null
    }
}
