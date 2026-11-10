package com.finnest.app

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalDataTest {
    @Test fun resetScopesAreExplicit() {
        assertEquals(setOf("new_property", "resale_property", "local_reminders", "local_profile"), LocalData.resetScopes)
    }
}
