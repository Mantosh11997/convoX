package com.convox.app.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.convox.app.adapter.MessagesAdapter
import com.convox.app.databinding.ActivityChatBinding
import com.convox.app.model.Message
import com.convox.app.model.Profile
import com.convox.app.util.Presence
import com.convox.app.util.Supa
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TypingRow(
    @SerialName("chat_id") val chatId: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("updated_at") val updatedAt: Long = 0L
)

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var adapter: MessagesAdapter
    private var channel: RealtimeChannel? = null
    private lateinit var chatId: String
    private lateinit var otherUid: String
    private var lastSignature = ""
    private var lastTypingSent = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        chatId = intent.getStringExtra("chatId") ?: return
        otherUid = intent.getStringExtra("otherUid") ?: ""
        val otherUsername = intent.getStringExtra("otherUsername") ?: "Chat"

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = otherUsername
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val myUid = Supa.currentUid ?: return
        adapter = MessagesAdapter(myUid, mutableListOf())
        binding.rvMessages.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.rvMessages.adapter = adapter

        binding.btnSend.setOnClickListener { sendMessage(myUid) }

        binding.etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty()) return
                val now = System.currentTimeMillis()
                if (now - lastTypingSent > 1500) {
                    lastTypingSent = now
                    reportTyping(myUid, now)
                }
            }
        })

        Presence.startHeartbeat(this)
        loadMessages(myUid)
        subscribeRealtime(myUid)
        startPollingFallback(myUid)
    }

    /** Poll messages, typing state, and online status every 2 seconds. */
    private fun startPollingFallback(myUid: String) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    delay(2000)
                    loadMessages(myUid)
                    updateSubtitle()
                }
            }
        }
    }

    private fun reportTyping(myUid: String, now: Long) {
        lifecycleScope.launch {
            try {
                Supa.client.postgrest["typing"].upsert(
                    TypingRow(chatId = chatId, userId = myUid, updatedAt = now)
                )
            } catch (_: Exception) { }
        }
    }

    /** Toolbar subtitle: typing… beats online beats last seen. */
    private fun updateSubtitle() {
        if (otherUid.isEmpty()) return
        lifecycleScope.launch {
            try {
                val typingRows = Supa.client.postgrest["typing"].select {
                    filter {
                        eq("chat_id", chatId)
                        eq("user_id", otherUid)
                    }
                }.decodeList<TypingRow>()
                val isTyping = typingRows.firstOrNull()
                    ?.let { System.currentTimeMillis() - it.updatedAt < 4000 } == true

                if (isTyping) {
                    supportActionBar?.subtitle = "typing…"
                    return@launch
                }

                val profile = Supa.client.postgrest["profiles"].select {
                    filter { eq("id", otherUid) }
                }.decodeList<Profile>().firstOrNull()

                supportActionBar?.subtitle =
                    profile?.let { Presence.statusText(it.lastSeen).ifEmpty { null } }
            } catch (_: Exception) { }
        }
    }

    private fun loadMessages(myUid: String) {
        lifecycleScope.launch {
            try {
                val messages = Supa.client.postgrest["messages"].select {
                    filter { eq("chat_id", chatId) }
                    order("timestamp", Order.ASCENDING)
                }.decodeList<Message>()

                // Redraw only when content changed (count or seen states)
                val signature = "${messages.size}-${messages.count { it.seen }}"
                if (signature != lastSignature) {
                    lastSignature = signature
                    adapter.submitList(messages)
                    if (messages.isNotEmpty()) binding.rvMessages.scrollToPosition(messages.size - 1)
                }

                // Mark the other person's unseen messages as seen (I'm reading them now)
                if (messages.any { it.senderId != myUid && !it.seen }) {
                    Supa.client.postgrest["messages"].update(
                        { set("seen", true) }
                    ) {
                        filter {
                            eq("chat_id", chatId)
                            neq("sender_id", myUid)
                            eq("seen", false)
                        }
                    }
                }
            } catch (_: Exception) { }
        }
    }

    private fun subscribeRealtime(myUid: String) {
        lifecycleScope.launch {
            try {
                val ch = Supa.client.channel("chat-$chatId")
                channel = ch
                ch.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                    table = "messages"
                    filter("chat_id", FilterOperator.EQ, chatId)
                }.onEach {
                    lastSignature = ""
                    loadMessages(myUid)
                }.launchIn(lifecycleScope)
                ch.subscribe()
            } catch (_: Exception) { }
        }
    }

    private fun sendMessage(myUid: String) {
        val text = binding.etMessage.text.toString().trim()
        if (text.isEmpty()) return
        binding.etMessage.text?.clear()

        lifecycleScope.launch {
            try {
                val now = System.currentTimeMillis()
                Supa.client.postgrest["messages"].insert(
                    mapOf(
                        "chat_id" to chatId,
                        "sender_id" to myUid,
                        "text" to text,
                        "timestamp" to now.toString()
                    )
                )
                Supa.client.postgrest["chats"].update(
                    {
                        set("last_message", text)
                        set("last_message_time", now)
                    }
                ) {
                    filter { eq("id", chatId) }
                }
                Supa.client.postgrest["typing"].upsert(
                    TypingRow(chatId = chatId, userId = myUid, updatedAt = 0L)
                )
                lastSignature = ""
                loadMessages(myUid)
            } catch (_: Exception) { }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val ch = channel
        channel = null
        if (ch != null) {
            lifecycleScope.launch { try { ch.unsubscribe() } catch (_: Exception) {} }
        }
    }
}
