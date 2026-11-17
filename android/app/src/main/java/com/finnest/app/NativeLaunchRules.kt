package com.finnest.app

enum class NativeLaunchPhase { ONBOARDING, ACCOUNT, READY }
object NativeLaunchRules {
    fun decide(signedIn: Boolean, completedVersion: Int, publishedVersion: Int, hadAccount: Boolean, guestSession: Boolean): NativeLaunchPhase = when {
        completedVersion > 0 && completedVersion < publishedVersion -> NativeLaunchPhase.ONBOARDING
        signedIn -> NativeLaunchPhase.READY
        completedVersion == 0 -> NativeLaunchPhase.ONBOARDING
        hadAccount && !guestSession -> NativeLaunchPhase.ACCOUNT
        else -> NativeLaunchPhase.READY
    }
}