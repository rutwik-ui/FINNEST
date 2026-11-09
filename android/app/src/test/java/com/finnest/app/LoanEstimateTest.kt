package com.finnest.app

import org.junit.Assert.*
import org.junit.Test

class LoanEstimateTest {
    @Test fun fixedRateExample() { assertEquals(8997.26, LoanEstimate.monthly("1000000", "8", "20")!!, 0.01) }
    @Test fun zeroInterest() { assertEquals(1000.0, LoanEstimate.monthly("12000", "0", "1")!!, 0.0001) }
    @Test fun rejectsMissingInvalidAndOutOfBounds() {
        listOf(Triple("", "8", "20"), Triple("0", "8", "20"), Triple("NaN", "8", "20"),
            Triple("100", "-1", "2"), Triple("100", "101", "2"), Triple("100", "8", "0"),
            Triple("100", "8", "51"), Triple("100", "8", "1.5"), Triple("1000000000001", "8", "2"))
            .forEach { (a, r, y) -> assertNull(LoanEstimate.monthly(a, r, y)) }
    }
}
