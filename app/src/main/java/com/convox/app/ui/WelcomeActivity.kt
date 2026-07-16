package com.convox.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.convox.app.databinding.ActivityWelcomeBinding
import com.convox.app.util.Supa
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Skip straight to home if already logged in
        lifecycleScope.launch {
            try { Supa.client.auth.awaitInitialization() } catch (_: Exception) { }
            if (Supa.client.auth.currentUserOrNull() != null) {
                startActivity(Intent(this@WelcomeActivity, MainActivity::class.java))
                finish()
            }
        }

        binding.btnGetStarted.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
        binding.btnLoginNav.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}
