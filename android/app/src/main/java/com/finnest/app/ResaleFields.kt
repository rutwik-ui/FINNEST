package com.finnest.app

/** Stable local keys for the visible planning fields in the resale prototype. */
object ResaleFields {
    val budget = listOf(
        "budget" to "Budget", "emi" to "Comfortable monthly EMI", "stamp" to "Stamp duty & registration",
        "brokerage" to "Brokerage", "repairs" to "Repairs", "legal" to "Legal fees",
        "society_transfer" to "Society transfer charges, where applicable", "interiors" to "Interiors",
        "maintenance" to "Monthly maintenance", "contingency" to "Contingency"
    )
    val configuration = listOf(
        "1bhk" to "1 BHK", "2bhk" to "2 BHK", "3bhk" to "3 BHK", "villa" to "Villa / duplex",
        "parking" to "Parking", "lift" to "Lift", "accessibility" to "Accessibility", "family_needs" to "Family needs"
    )
    val location = listOf(
        "workplace" to "Workplace", "schools" to "Schools", "healthcare" to "Healthcare", "transport" to "Transport",
        "shopping" to "Shopping", "family" to "Family", "safety" to "Safety", "environment" to "Environment", "traffic" to "Traffic"
    )
}
