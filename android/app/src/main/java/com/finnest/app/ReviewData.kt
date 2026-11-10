package com.finnest.app

/** Review only known checklist keys. Never infer verification or completion from missing data. */
object ReviewData {
    val developerChecks = listOf(
        "track_record" to "Track record / delivery", "rera" to "Project RERA registration / disclosures",
        "land_title" to "Land ownership / title", "approvals" to "Approvals / sanctioned plans",
        "legal_history" to "Credible legal history", "financial_stability" to "Financial stability where evidence exists"
    )
    val lenderChecks = listOf(
        "identity" to "Identity and address requirements", "income_docs" to "Income documentation requirements",
        "bank" to "Bank statement period requested", "property" to "Property documents required", "fees" to "Processing and legal fee schedule"
    )
    data class Check(val group: String, val title: String, val key: String, val status: String?, val note: String, val source: String, val date: String)
    fun checks(draft: Map<String, String>): List<Check> {
        val groups = listOf(
            Triple("Developer checks", "developer", developerChecks), Triple("Visit observations", "visit", VisitObservations.checks),
            Triple("Legal due diligence", "legal", LegalDueDiligence.checks),
            Triple("Cost prompts", "financial_prompt", FinancialCosts.initial.drop(1)), Triple("Lender documents", "loan", lenderChecks)
        )
        return groups.flatMap { (group, prefix, prompts) -> prompts.map { (id, title) ->
            val base = "${prefix}_$id"
            Check(group, title, base, draft["${base}_status"]?.takeIf { it.isNotBlank() }, draft["${base}_note"].orEmpty(), draft["${base}_source"].orEmpty(), draft["${base}_date"].orEmpty())
        } }
    }
    fun pending(c: Check): Boolean = c.status == null || c.status == "Not checked" || c.status == "Pending / unknown"
}
