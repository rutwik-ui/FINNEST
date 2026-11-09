package com.finnest.app

object DashboardSteps {
    data class Step(val id: String, val number: String, val title: String, val subtitle: String)
    val all = listOf(
        Step("requirements", "01", "Requirements & budget", "Plan what matters to you"),
        Step("developer", "02", "Developer checks", "Record questions and sources"),
        Step("projects", "03", "Shortlist projects", "Keep up to five project names"),
        Step("visit", "4A", "Visit observations", "Record what you notice"),
        Step("feedback", "4B", "Resident feedback", "Keep accounts and follow-ups"),
        Step("loan", "05", "Home loan guide", "Explore an illustrative EMI"),
        Step("legal", "06", "Legal due diligence", "Prepare questions for a lawyer"),
        Step("financial", "07", "Financial due diligence", "Organize costs and loan terms"),
        Step("review", "08", "Final review", "Review your notes and next actions")
    )
}
