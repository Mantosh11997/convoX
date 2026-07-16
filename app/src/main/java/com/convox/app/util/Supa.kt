package com.convox.app.util

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

object Supa {

    // ==========================================================
    // PASTE YOUR SUPABASE PROJECT VALUES HERE (see README step 1)
    // Project URL + anon/public key are found in:
    // Supabase dashboard -> Project Settings -> API
    // The anon key is safe to ship in the app; Row Level Security
    // policies protect the data.
    // ==========================================================
    private const val SUPABASE_URL = "https://lmcrpuuzlpyzicwvwyop.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxtY3JwdXV6bHB5emljd3Z3eW9wIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODQxNzgzNDgsImV4cCI6MjA5OTc1NDM0OH0.hNyT4L2-omM75E3du-TcanxYiPPS2g9Mt_Gi2i7FUH4"

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
        }
    }

    val currentUid: String?
        get() = client.auth.currentUserOrNull()?.id

    /** Deterministic chat id so two users always share one chat row. */
    fun chatIdFor(uidA: String, uidB: String): String =
        if (uidA < uidB) "${uidA}_$uidB" else "${uidB}_$uidA"

    fun requestIdFor(fromUid: String, toUid: String): String = "${fromUid}_$toUid"
}
