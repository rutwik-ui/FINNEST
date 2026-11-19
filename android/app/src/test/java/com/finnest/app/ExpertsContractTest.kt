package com.finnest.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpertsContractTest {
    private val json = """[
      {"id":"a1","name":"Asha Rao","specialty":"Property lawyer","city":"Pune","phone":"+91 98200 00000","email":"asha@example.com","bio":"Title checks."},
      {"id":"b2","name":"  ","specialty":"CA / tax advisor","city":"Mumbai","phone":null,"email":null,"bio":null},
      {"id":"c3","name":"Vikram Shah","specialty":"Home loan advisor","city":null,"phone":null,"email":"v@example.com","bio":""}
    ]"""

    @Test fun parsesRowsSkipsBlankNamesAndNulls() {
        val experts = ExpertsContract.parse(json)
        assertEquals(listOf("a1", "c3"), experts.map { it.id })
        assertEquals("", experts[1].city)
        assertEquals("", experts[1].phone)
    }

    @Test fun filtersByRoleAndSearchLikeTheWebsite() {
        val experts = ExpertsContract.parse(json)
        assertEquals(listOf("a1"), experts.filter { ExpertsContract.matches(it, "Property lawyer", "") }.map { it.id })
        assertTrue(ExpertsContract.matches(experts[0], null, "pune"))
        assertTrue(ExpertsContract.matches(experts[1], null, "LOAN"))
        assertFalse(ExpertsContract.matches(experts[1], null, "lawyer"))
        assertFalse(ExpertsContract.matches(experts[0], "Home loan advisor", "asha"))
    }
}
