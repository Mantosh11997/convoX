package com.convox.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.convox.app.databinding.ActivityLoginBinding
import com.convox.app.util.Supa
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener { attemptLogin() }
        binding.tvGoToSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    private fun attemptLogin() {
        val username = binding.etUsername.text.toString().trim().lowercase()
        val password = binding.etPassword.text.toString()

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter username and password", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                // Resolve username -> login email via secure SQL function
                val email = Supa.client.postgrest.rpc(
                    "get_login_email",
                    buildJsonObject { put("uname", username) }
                ).decodeAsOrNull<String>()

                if (email.isNullOrEmpty()) {
                    setLoading(false)
                    Toast.makeText(this@LoginActivity, "No account with that username", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                Supa.client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                goToMain()
            } catch (e: Exception) {
                setLoading(false)
                Toast.makeText(this@LoginActivity, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
