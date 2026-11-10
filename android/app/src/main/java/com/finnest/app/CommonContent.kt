package com.finnest.app

data class ExpertRole(val title: String, val description: String)
data class RelatedCheck(val group: String, val title: String, val key: String, val destination: String)

object CommonContent {
    val professionalRoles = listOf(
        ExpertRole("Property lawyer", "Title, agreements, disclosures and local requirements."),
        ExpertRole("CA / tax advisor", "Transaction costs and personal financial questions."),
        ExpertRole("Home loan advisor", "Loan terms, eligibility, fees and repayment obligations."),
        ExpertRole("Property consultant", "Independent process or local market context."),
        ExpertRole("Architect / technical inspector", "Condition, plans, structure or visible concerns."),
        ExpertRole("Interior expert", "Layout, finishes and interior planning."),
        ExpertRole("Valuation expert", "Independent valuation within their professional scope.")
    )
    val optionalRoles = listOf(
        ExpertRole("Vastu expert · optional", "Personal preference guidance; not due diligence."),
        ExpertRole("Astrologer · optional", "Personal preference only; not due diligence.")
    )
    fun relatedChecks(scope: String): List<RelatedCheck> = when (scope) {
        "new_property" -> ReviewData.checks(emptyMap()).map { c ->
            val destination = when (c.group) {
                "Developer checks" -> "developer"; "Visit observations" -> "visit"; "Legal due diligence" -> "legal"
                "Cost prompts" -> "financial"; "Lender documents" -> "loan"; else -> error("Unknown checklist group")
            }
            RelatedCheck(c.group, c.title, "${c.key}_status", destination)
        }
        "resale_property" -> ResaleChecklists.sections.flatMap { s -> s.checks.map { (id, title) -> RelatedCheck(s.title, title, "${s.id}_${id}_status", s.id) } }
        else -> emptyList()
    }
}
