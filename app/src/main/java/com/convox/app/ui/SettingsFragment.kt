package com.convox.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.convox.app.databinding.FragmentSettingsBinding
import com.convox.app.model.Profile
import com.convox.app.util.Supa
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rowAccount.setOnClickListener {
            val myUid = Supa.currentUid ?: return@setOnClickListener
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val me = Supa.client.postgrest["profiles"].select {
                        filter { eq("id", myUid) }
                    }.decodeSingle<Profile>()
                    Toast.makeText(requireContext(), "Logged in as @${me.username}", Toast.LENGTH_LONG).show()
                } catch (_: Exception) { }
            }
        }
        binding.rowNotifications.setOnClickListener {
            Toast.makeText(requireContext(), "Notifications coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
