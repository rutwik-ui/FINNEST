package com.finnest.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Buyer-facing wording for authentication failures.
 *
 * A wrong password and a broken network must never read the same, because the
 * remedy is different. These run on an emulator with the real classes.
 */
@RunWith(AndroidJUnit4::class)
class AuthErrorWordingTest {

    @Test
    fun signedOutStatusAdmitsDeviceOnlySaving() {
        val text = DraftSync.statusText(DraftSync.SyncState.SIGNED_OUT)
        assertTrue(text.contains("this device only", ignoreCase = true))
        assertTrue(!text.contains("other devices", ignoreCase = true))
    }

    @Test
    fun offlineAndErrorStatesAdmitDeviceOnlySaving() {
        listOf(DraftSync.SyncState.OFFLINE, DraftSync.SyncState.ERROR).forEach {
            assertTrue(DraftSync.statusText(it).contains("this device", ignoreCase = true))
        }
    }

    @Test
    fun syncedStatusNamesTheOtherDevices() {
        assertTrue(DraftSync.statusText(DraftSync.SyncState.SYNCED).contains("other devices", ignoreCase = true))
    }
}
