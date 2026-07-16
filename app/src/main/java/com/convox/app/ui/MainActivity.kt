package com.convox.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.convox.app.R
import com.convox.app.databinding.ActivityMainBinding
import com.convox.app.util.Presence

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Presence.startHeartbeat(this)

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_chats -> show(ChatsFragment(), "Chats")
                R.id.nav_requests -> show(RequestsFragment(), "Requests")
                R.id.nav_profile -> show(ProfileFragment(), "Profile")
                R.id.nav_settings -> show(SettingsFragment(), "Settings")
            }
            true
        }

        if (savedInstanceState == null) show(ChatsFragment(), "Chats")
    }

    private fun show(fragment: Fragment, title: String) {
        binding.tvHeader.text = title
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
