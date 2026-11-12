package com.finnest.app

/** Progress is recorded input, not verification, submission or an approval to buy. */
object JourneyProgress {
    data class Count(val recorded: Int, val total: Int) {
        val percent: Int get() = if (total == 0) 0 else recorded * 100 / total
    }
    private val requirements = listOf("loan", "down_payment", "stamp_duty", "gst", "brokerage", "interiors", "contingency", "emi", "maintenance", "carpet_area", "accessibility_needs") +
        listOf("workplace", "schools", "healthcare", "transport", "shopping", "family", "safety", "environment", "traffic", "infrastructure", "development").map { "location_$it" } +
        listOf("1bhk", "2bhk", "3bhk", "villa", "parking", "amenities", "accessibility", "vastu").map { "config_$it" }
    private fun statuses(prefix: String, checks: List<Pair<String, String>>) = checks.map { "${prefix}_${it.first}_status" }
    fun keys(scope: String, id: String): List<String> = if (scope == "resale_property") {
        if (id == "requirements") ResaleFields.budget.map { "budget_${it.first}" } +
            ResaleFields.configuration.map { "config_${it.first}" } + ResaleFields.location.map { "location_${it.first}" } +
            listOf("carpet_area", "building_age", "timeline")
        else ResaleChecklists.find(id)?.let { statuses(id, it.checks) }.orEmpty()
    } else when (id) {
        "requirements" -> requirements
        "developer" -> statuses("developer", ReviewData.developerChecks)
        "projects" -> listOf("shortlist_projects_v1")
        "visit" -> statuses("visit", VisitObservations.checks)
        "loan" -> listOf("loan_amount", "loan_rate", "loan_years", "loan_lender", "loan_income", "loan_obligations") + statuses("loan", ReviewData.lenderChecks)
        "legal" -> statuses("legal", LegalDueDiligence.checks)
        "financial" -> FinancialCosts.initial.map { "financial_initial_${it.first}" } + FinancialCosts.recurring.map { "financial_recurring_${it.first}" } +
            statuses("financial_prompt", FinancialCosts.initial.drop(1)) + listOf("lender", "rate_type", "fees", "tenure", "emi", "interest", "prepayment").map { "financial_comparison_$it" }
        "review" -> listOf("review_next_actions")
        else -> emptyList()
    }
    fun count(scope: String, id: String, draft: Map<String, String>): Count {
        val keys = keys(scope, id)
        return Count(keys.count { key ->
            val value = draft[key]?.trim().orEmpty()
            when {
                key == "shortlist_projects_v1" -> value.startsWith("[") && Regex("\"name\"\\s*:\\s*\"[^\"]+\"").containsMatchIn(value)
                key.startsWith("config_") || key.startsWith("location_") -> value == "true"
                else -> value.isNotEmpty()
            }
        }, keys.size)
    }
    fun total(scope: String, draft: Map<String, String>): Count {
        val ids = if (scope == "resale_property") listOf("requirements") + ResaleChecklists.sections.map { it.id }
            else listOf("requirements", "developer", "projects", "visit", "loan", "legal", "financial", "review")
        val counts = ids.map { count(scope, it, draft) }
        return Count(counts.sumOf { it.recorded }, counts.sumOf { it.total })
    }
}