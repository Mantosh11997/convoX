package com.convox.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.convox.app.R
import com.convox.app.adapter.RequestsAdapter
import com.convox.app.databinding.FragmentRequestsBinding
import com.convox.app.model.FriendRequest
import com.convox.app.util.Supa
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class RequestsFragment : Fragment() {

    private var _binding: FragmentRequestsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: RequestsAdapter
    private var showingSent = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRequestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val myUid = Supa.currentUid ?: return

        adapter = RequestsAdapter(
            mutableListOf(),
            onAccept = { acceptRequest(it) },
            onReject = { rejectRequest(it) }
        )
        binding.rvRequests.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRequests.adapter = adapter

        binding.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            showingSent = checkedId == R.id.btnSent
            adapter.sentMode = showingSent
            loadRequests(myUid)
        }

        // Poll every 3 seconds while visible
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    loadRequests(myUid)
                    delay(3000)
                }
            }
        }
    }

    private fun loadRequests(myUid: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val requests = Supa.client.postgrest["friend_requests"].select {
                    filter {
                        if (showingSent) eq("from_uid", myUid) else eq("to_uid", myUid)
                        eq("status", "pending")
                    }
                }.decodeList<FriendRequest>()
                if (_binding == null) return@launch
                adapter.submitList(requests)
                binding.tvEmpty.text =
                    if (showingSent) "No pending sent requests" else "No pending friend requests"
                binding.tvEmpty.visibility = if (requests.isEmpty()) View.VISIBLE else View.GONE
            } catch (_: Exception) { }
        }
    }

    private fun acceptRequest(req: FriendRequest) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Supa.client.postgrest.rpc(
                    "accept_friend_request",
                    buildJsonObject { put("req_id", req.id) }
                )
                Toast.makeText(requireContext(), "You are now friends with ${req.fromUsername}", Toast.LENGTH_SHORT).show()
                Supa.currentUid?.let { loadRequests(it) }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun rejectRequest(req: FriendRequest) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Supa.client.postgrest["friend_requests"].update(
                    { set("status", "rejected") }
                ) {
                    filter { eq("id", req.id) }
                }
                Supa.currentUid?.let { loadRequests(it) }
            } catch (_: Exception) { }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
