package com.finnest.app

import kotlin.math.pow

/** Illustrative fixed-rate monthly repayment, not a lender quote. */
object LoanEstimate {
    fun monthly(amount: String, annualRate: String, years: String): Double? {
        val principal = amount.toDoubleOrNull() ?: return null
        val rate = annualRate.toDoubleOrNull() ?: return null
        val tenure = years.toIntOrNull() ?: return null
        if (!principal.isFinite() || principal <= 0 || principal > 1_000_000_000_000.0 ||
            !rate.isFinite() || rate < 0 || rate > 100 || tenure !in 1..50) return null
        val months = tenure * 12
        val monthlyRate = rate / 1200.0
        if (monthlyRate == 0.0) return principal / months
        // This form avoids overflow from raising (1 + r) to a large positive power.
        return principal * monthlyRate / (1.0 - (1.0 + monthlyRate).pow(-months))
    }
}
