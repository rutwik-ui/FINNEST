package com.finnest.app

/** Filters are deliberately separate from the provisional checklist status labels. */
object JourneySummary {
    val filters = listOf("All saved statuses", "Not checked / pending", "Checked by you", "Information provided", "Professional review", "Other saved statuses")
    private val known = setOf("Not checked", "Pending / unknown", "Checked by you", "Information provided", "Professional review")
    fun filter(checks: List<ReviewData.Check>, index: Int): List<ReviewData.Check> = when (index) {
        0 -> checks.filter { it.status != null }
        1 -> checks.filter(ReviewData::pending)
        2 -> checks.filter { it.status == "Checked by you" }
        3 -> checks.filter { it.status == "Information provided" }
        4 -> checks.filter { it.status == "Professional review" }
        5 -> checks.filter { it.status != null && it.status !in known }
        else -> emptyList()
    }
}
