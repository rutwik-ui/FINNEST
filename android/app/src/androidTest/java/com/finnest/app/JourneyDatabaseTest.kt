package com.finnest.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JourneyDatabaseTest {
    private lateinit var database: JourneyDatabase
    @Before fun setup() { database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), JourneyDatabase::class.java).allowMainThreadQueries().build() }
    @After fun cleanup() { database.close() }
    @Test fun draftsAreIsolatedAndUpdatesReplaceExistingValues() {
        val dao = database.drafts()
        dao.put(DraftField("new_property", "loan", "100"))
        dao.put(DraftField("resale_property", "loan", "200"))
        dao.put(DraftField("new_property", "loan", "150"))
        assertEquals("150", dao.fields("new_property").single().value)
        assertEquals("200", dao.fields("resale_property").single().value)
    }
    @Test fun developerCheckFieldsRoundTripWithoutOverwritingStepOne() {
        val dao = database.drafts()
        dao.put(DraftField("new_property", "loan", "100"))
        dao.put(DraftField("new_property", "developer_land_title_status", "Information provided"))
        dao.put(DraftField("new_property", "developer_land_title_note", "Ask a lawyer"))
        dao.put(DraftField("new_property", "developer_land_title_source", "Seller"))
        dao.put(DraftField("new_property", "developer_land_title_date", "05/12/2026"))
        val saved = dao.fields("new_property").associate { it.fieldKey to it.value }
        assertEquals("100", saved["loan"])
        assertEquals("Information provided", saved["developer_land_title_status"])
        assertEquals("Ask a lawyer", saved["developer_land_title_note"])
        assertEquals("Seller", saved["developer_land_title_source"])
        assertEquals("05/12/2026", saved["developer_land_title_date"])
    }
}
