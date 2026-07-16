package com.convox.app.util

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object Presence {

    /**
     * While the given screen is visible, update my profile's last_seen
     * every 25 seconds. Another user is considered "online" if their
     * last_seen is fresher than 40 seconds.
     */
    fun startHeartbeat(owner: LifecycleOwner) {
        val myUid = Supa.currentUid ?: return
        owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    try {
                        Supa.client.postgrest["profiles"].update(
                            { set("last_seen", System.currentTimeMillis()) }
                        ) {
                            filter { eq("id", myUid) }
                        }
                    } catch (_: Exception) { }
                    delay(25_000)
                }
            }
        }
    }

    /** Human text for a last_seen timestamp. */
    fun statusText(lastSeen: Long): String {
        if (lastSeen <= 0) return ""
        val diff = System.currentTimeMillis() - lastSeen
        return when {
            diff < 40_000 -> "online"
            diff < 60_000 * 60 -> "last seen ${diff / 60_000} min ago"
            diff < 60_000 * 60 * 24 -> "last seen ${diff / (60_000 * 60)} h ago"
            else -> "last seen ${diff / (60_000 * 60 * 24)} d ago"
        }
    }
}
