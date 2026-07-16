package com.convox.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.convox.app.databinding.FragmentProfileBinding
import com.convox.app.model.Profile
import com.convox.app.util.Supa
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val myUid = Supa.currentUid ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val me = Supa.client.postgrest["profiles"].select {
                    filter { eq("id", myUid) }
                }.decodeSingle<Profile>()
                if (_binding == null) return@launch
                binding.tvUsername.text = me.username
                binding.tvAvatar.text = me.username.take(1).uppercase()
                binding.etAbout.setText(me.about)
            } catch (_: Exception) { }
        }

        binding.btnSaveAbout.setOnClickListener {
            val about = binding.etAbout.text.toString().trim()
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    Supa.client.postgrest["profiles"].update(
                        { set("about", about) }
                    ) {
                        filter { eq("id", myUid) }
                    }
                    Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnLogout.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                try { Supa.client.auth.signOut() } catch (_: Exception) { }
                startActivity(Intent(requireContext(), WelcomeActivity::class.java))
                requireActivity().finish()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
