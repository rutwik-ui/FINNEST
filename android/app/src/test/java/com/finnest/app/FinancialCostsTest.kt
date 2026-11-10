package com.finnest.app

import java.math.BigDecimal
import org.junit.Assert.*
import org.junit.Test

class FinancialCostsTest {
    @Test fun emptyIsUnknownNotZero() { assertEquals(FinancialCosts.Total.Empty, FinancialCosts.total(listOf("", ""))) }
    @Test fun explicitZeroCountsAndPartialSumIsExact() {
        assertEquals(FinancialCosts.Total.Entered(BigDecimal("100.30"), 3), FinancialCosts.total(listOf("100.10", "", "0", "0.20")))
    }
    @Test fun invalidValuesNeverSilentlyDisappearFromSum() {
        listOf(".", "1.", "-1", "1.999", "NaN", "1000000000001").forEach {
            assertEquals(FinancialCosts.Total.Invalid, FinancialCosts.total(listOf("100", it)))
        }
    }
    @Test fun promptsHaveUniqueStableKeys() {
        assertEquals(11, FinancialCosts.initial.size)
        assertEquals(FinancialCosts.initial.size, FinancialCosts.initial.map { it.first }.toSet().size)
        assertEquals(3, FinancialCosts.recurring.size)
    }
}
