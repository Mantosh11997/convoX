package com.convox.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.convox.app.databinding.ActivitySignupBinding
import com.convox.app.util.Supa
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSignup.setOnClickListener { attemptSignup() }
        binding.tvGoToLogin.setOnClickListener { finish() }
    }

    private fun attemptSignup() {
        val usernameRaw = binding.etUsername.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        if (usernameRaw.length < 3 || !usernameRaw.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            showError("Username must be 3+ characters: letters, numbers, underscore only")
            return
        }
        if (email.isEmpty() || password.length < 6) {
            showError("Enter a valid email and a password with 6+ characters")
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                // 1. Create the auth account (email confirmation should be
                //    disabled in Supabase settings for instant signup)
                Supa.client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }

                // 2. Claim the unique username via SQL function.
                //    The function inserts the profile row; the UNIQUE
                //    constraint on username_lower makes races impossible.
                Supa.client.postgrest.rpc(
                    "claim_username",
                    buildJsonObject { put("uname", usernameRaw) }
                )

                startActivity(Intent(this@SignupActivity, MainActivity::class.java))
                finish()
            } catch (e: Exception) {
                setLoading(false)
                val msg = e.message ?: "Signup failed"
                showError(
                    if (msg.contains("duplicate", true) || msg.contains("unique", true))
                        "Username \"$usernameRaw\" is already taken"
                    else msg
                )
            }
        }
    }

    private fun showError(msg: String) {
        binding.tvError.text = msg
        binding.tvError.visibility = View.VISIBLE
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSignup.isEnabled = !loading
        if (loading) binding.tvError.visibility = View.GONE
    }
}
