package com.convox.app.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.convox.app.adapter.SearchResult
import com.convox.app.adapter.SearchUserAdapter
import com.convox.app.databinding.ActivityAddFriendBinding
import com.convox.app.model.FriendRequest
import com.convox.app.model.Profile
import com.convox.app.util.Supa
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AddFriendActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddFriendBinding
    private lateinit var adapter: SearchUserAdapter
    private var myUsername: String = ""
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddFriendBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = SearchUserAdapter(mutableListOf()) { item, position ->
            sendRequest(item, position)
        }
        binding.rvResults.layoutManager = LinearLayoutManager(this)
        binding.rvResults.adapter = adapter

        val myUid = Supa.currentUid ?: return
        lifecycleScope.launch {
            try {
                val me = Supa.client.postgrest["profiles"].select {
                    filter { eq("id", myUid) }
                }.decodeSingle<Profile>()
                myUsername = me.username
            } catch (_: Exception) { }
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val q = s.toString().trim().lowercase()
                searchJob?.cancel()
                if (q.length < 2) { adapter.submitList(emptyList()); return }
                searchJob = lifecycleScope.launch {
                    delay(300)  // debounce typing
                    search(q, myUid)
                }
            }
        })
    }

    private suspend fun search(query: String, myUid: String) {
        try {
            val users = Supa.client.postgrest["profiles"].select {
                filter {
                    like("username_lower", "$query%")
                    neq("id", myUid)
                }
                limit(20)
            }.decodeList<Profile>()

            val results = users.map { SearchResult(it, "loading") }.toMutableList()
            adapter.submitList(results)

            users.forEachIndexed { index, user ->
                val state = resolveState(myUid, user.id)
                adapter.updateState(index, state)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Search error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun resolveState(myUid: String, otherUid: String): String {
        return try {
            val chatId = Supa.chatIdFor(myUid, otherUid)
            val chat = Supa.client.postgrest["chats"].select {
                filter { eq("id", chatId) }
            }.decodeList<com.convox.app.model.Chat>()
            if (chat.isNotEmpty()) return "friends"

            val pending = Supa.client.postgrest["friend_requests"].select {
                filter {
                    or {
                        eq("id", Supa.requestIdFor(myUid, otherUid))
                        eq("id", Supa.requestIdFor(otherUid, myUid))
                    }
                    eq("status", "pending")
                }
            }.decodeList<FriendRequest>()
            if (pending.isNotEmpty()) "pending" else "add"
        } catch (_: Exception) { "add" }
    }

    private fun sendRequest(item: SearchResult, position: Int) {
        val myUid = Supa.currentUid ?: return
        if (myUsername.isEmpty()) {
            Toast.makeText(this, "Loading your profile, try again", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            try {
                val request = FriendRequest(
                    id = Supa.requestIdFor(myUid, item.user.id),
                    fromUid = myUid,
                    fromUsername = myUsername,
                    toUid = item.user.id,
                    toUsername = item.user.username,
                    status = "pending"
                )
                Supa.client.postgrest["friend_requests"].insert(request)
                adapter.updateState(position, "pending")
                Toast.makeText(this@AddFriendActivity, "Request sent to ${item.user.username}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@AddFriendActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
