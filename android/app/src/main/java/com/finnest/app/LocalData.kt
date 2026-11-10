package com.finnest.app

/** Explicitly enumerated; other scopes must never be silently erased. */
object LocalData {
    val resetScopes = setOf("new_property", "resale_property", "local_reminders", "local_profile")
}
