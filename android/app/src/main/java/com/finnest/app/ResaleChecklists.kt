package com.finnest.app

data class ResaleSection(val id: String, val title: String, val description: String, val defaultStatus: String, val freeTextStatus: Boolean, val hasAmount: Boolean, val checks: List<Pair<String, String>>)

object ResaleChecklists {
    val sections = listOf(
        ResaleSection("condition", "Condition inspection", "Record what you observe; arrange a qualified technical inspection for concerns. This checklist cannot assess structural safety.", "Not checked", false, false, ResaleCondition.checks),
        ResaleSection("seller", "Seller, ownership & title", "Keep seller statements separate from records reviewed. Ask a local property lawyer which records and authority checks apply.", "Not checked", false, false, listOf(
            "identity" to "Seller identity & authority", "ownership" to "Ownership records", "encumbrance" to "Loan / encumbrance status", "consent" to "Co-owner / consent questions", "identifiers" to "Property identifiers matching documents")),
        ResaleSection("society", "Society documents & dues", "Records, fees and processes vary by society and location. Ask for current statements and appropriate professional guidance.", "Unknown", true, true, listOf(
            "membership" to "Society / share certificate or applicable membership records", "maintenance" to "Maintenance dues", "tax" to "Property tax / utility dues", "transfer" to "Transfer requirements / fees", "parking" to "Parking records", "repairs" to "Major repairs / reserve fund questions", "disputes" to "Disclosed disputes / notices")),
        ResaleSection("legal", "Legal review", "Discussion prompts only. A qualified local lawyer should assess property-specific documents and their limitations.", "Not checked", false, false, listOf(
            "title_chain" to "Title chain", "encumbrance" to "Encumbrance / loan release", "approvals" to "Relevant approvals / plans", "tax" to "Property tax records", "agreement" to "Agreement terms", "authority" to "Seller authority", "society" to "Society records", "litigation" to "Litigation / dispute questions")),
        ResaleSection("handover", "Agreement & handover", "Coordinate legal, lender and local registration requirements with qualified professionals. FINNEST does not execute a purchase.", "Pending", true, false, listOf(
            "agreement" to "Lawyer-reviewed agreement", "promises" to "Written promises", "payment" to "Payment schedule", "possession" to "Possession date", "cancellation" to "Cancellation / refund terms", "charges" to "Additional charges", "registration" to "Registration documents / appointment", "loan_release" to "Loan / lien release, where applicable", "keys" to "Keys / access", "meters" to "Meter readings", "utilities" to "Utility transfers", "society" to "Society handover", "dues" to "Final dues / receipts"))
    )
    fun find(id: String) = sections.firstOrNull { it.id == id }
}
