package com.finnest.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure contracts shared by the account and sync layers. No Android classes, so
 * these run on the JVM in CI without an emulator.
 */
class AccountContractTest {

    @Test
    fun `journey labels map to the database journey types`() {
        assertEquals("new_property", DraftSync.journeyType("new_property"))
        assertEquals("resale_property", DraftSync.journeyType("resale_property"))
        // The dashboard uses display labels in preferences.
        assertEquals("new_property", DraftSync.journeyType("New Property"))
        assertEquals("resale_property", DraftSync.journeyType("Resale Property"))
        // Anything unknown must never silently become resale.
        assertEquals("new_property", DraftSync.journeyType(""))
        assertEquals("new_property", DraftSync.journeyType("something else"))
    }

    @Test
    fun `sync status never claims cloud saving while signed out`() {
        val text = DraftSync.statusText(DraftSync.SyncState.SIGNED_OUT)
        assertTrue(text.contains("this device only", ignoreCase = true))
        assertFalse(text.contains("other devices", ignoreCase = true))
    }

    @Test
    fun `offline and error states admit the data is device only`() {
        listOf(DraftSync.SyncState.OFFLINE, DraftSync.SyncState.ERROR).forEach {
            assertTrue(DraftSync.statusText(it).contains("this device", ignoreCase = true))
        }
    }

    @Test
    fun `synced state names the other devices`() {
        assertTrue(DraftSync.statusText(DraftSync.SyncState.SYNCED).contains("other devices", ignoreCase = true))
    }

    @Test
    fun `session needing refresh is detected before the token expires`() {
        val now = 1_000_000_000_000L
        val fresh = AccountSession("u", "a@b.c", "access", "refresh", now + 10 * 60_000)
        val nearlyExpired = AccountSession("u", "a@b.c", "access", "refresh", now + 30_000)
        val alreadyExpired = AccountSession("u", "a@b.c", "access", "refresh", now - 1)
        assertFalse(AccountStore.needsRefresh(fresh, now))
        assertTrue(AccountStore.needsRefresh(nearlyExpired, now))
        assertTrue(AccountStore.needsRefresh(alreadyExpired, now))
    }
}
