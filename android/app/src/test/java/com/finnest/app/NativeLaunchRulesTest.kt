package com.finnest.app

import org.junit.Assert.assertEquals
import org.junit.Test

class NativeLaunchRulesTest {
    @Test fun firstVisitStartsIntroduction() { assertEquals(NativeLaunchPhase.ONBOARDING, NativeLaunchRules.decide(false, 0, 1, false, false)) }
    @Test fun returningGuestGoesToDashboard() { assertEquals(NativeLaunchPhase.READY, NativeLaunchRules.decide(false, 1, 1, false, true)) }
    @Test fun validAccountGoesToDashboard() { assertEquals(NativeLaunchPhase.READY, NativeLaunchRules.decide(true, 0, 1, true, false)) }
    @Test fun expiredAccountRequestsLogin() { assertEquals(NativeLaunchPhase.ACCOUNT, NativeLaunchRules.decide(false, 1, 1, true, false)) }
    @Test fun publishedVersionReplaysIntroduction() { assertEquals(NativeLaunchPhase.ONBOARDING, NativeLaunchRules.decide(true, 1, 2, true, false)) }
}