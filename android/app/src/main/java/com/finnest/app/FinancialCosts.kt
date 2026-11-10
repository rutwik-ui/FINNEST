package com.finnest.app

import java.math.BigDecimal

object FinancialCosts {
    val initial = listOf(
        "price" to "Property price", "tax" to "Applicable taxes / GST", "stamp" to "Stamp duty",
        "registration" to "Registration", "parking" to "Parking", "deposit" to "Maintenance deposit",
        "brokerage" to "Brokerage", "legal" to "Legal fees", "loan" to "Loan fees",
        "interiors" to "Interiors", "moving" to "Moving / contingency"
    )
    val recurring = listOf("emi" to "EMI estimate · monthly", "maintenance" to "Maintenance · monthly", "other" to "Other recurring costs · monthly")
    // Blank inputs are unknown. Invalid inputs must not be omitted from a displayed sum.
    sealed class Total {
        object Empty : Total()
        object Invalid : Total()
        data class Entered(val amount: BigDecimal, val count: Int) : Total()
    }
    fun total(values: List<String>): Total {
        var sum = BigDecimal.ZERO
        var count = 0
        for (value in values) {
            if (value.isBlank()) continue
            if (!value.matches(Regex("[0-9]{1,13}(\\.[0-9]{1,2})?"))) return Total.Invalid
            val amount = value.toBigDecimalOrNull() ?: return Total.Invalid
            if (amount > BigDecimal("1000000000000")) return Total.Invalid
            sum = sum.add(amount)
            count++
        }
        return if (count == 0) Total.Empty else Total.Entered(sum, count)
    }
}
