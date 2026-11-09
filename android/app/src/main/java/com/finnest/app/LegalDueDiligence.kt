package com.finnest.app

/** Prompts from the New Property legal due diligence prototype; not legal findings. */
object LegalDueDiligence {
    val checks = listOf(
        "ownership" to "Ownership & title chain",
        "title_documents" to "Original sale & title documents",
        "encumbrance" to "Encumbrance search or certificate",
        "plans" to "Sanctioned plans",
        "rera" to "RERA disclosures",
        "approvals" to "Approvals",
        "agreement" to "Agreement",
        "payment_schedule" to "Payment schedule",
        "possession_terms" to "Possession, cancellation & refund clauses"
    )
}
