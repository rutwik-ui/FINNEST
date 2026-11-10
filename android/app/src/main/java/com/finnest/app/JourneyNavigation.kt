package com.finnest.app

import android.content.Context
import android.content.Intent

object JourneyNavigation {
    fun intent(context: Context, id: String): Intent? = when (id) {
        "requirements" -> Intent(context, RequirementsActivity::class.java)
        "developer" -> Intent(context, DeveloperChecksActivity::class.java)
        "projects" -> Intent(context, ProjectsActivity::class.java)
        "visit" -> Intent(context, DeveloperChecksActivity::class.java).putExtra("visit_observations", true)
        "feedback" -> Intent(context, DeveloperChecksActivity::class.java).putExtra("resident_feedback", true)
        "loan" -> Intent(context, LoanGuideActivity::class.java)
        "legal" -> Intent(context, DeveloperChecksActivity::class.java).putExtra("legal_due_diligence", true)
        "financial" -> Intent(context, FinancialDueDiligenceActivity::class.java)
        "review" -> Intent(context, FinalReviewActivity::class.java)
        else -> null
    }
}
