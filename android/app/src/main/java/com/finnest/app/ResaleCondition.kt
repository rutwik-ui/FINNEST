package com.finnest.app

/** User-recorded observations only; not a structural or technical inspection. */
object ResaleCondition {
    val checks = listOf(
        "walls" to "Walls / ceilings / leaks", "plumbing" to "Plumbing / water pressure",
        "electricals" to "Electricals", "light" to "Light / ventilation", "windows" to "Windows / doors",
        "flooring" to "Flooring", "kitchen" to "Kitchen / bathroom", "damp" to "Damp / pests",
        "lifts" to "Lifts / common areas", "parking" to "Parking", "maintenance" to "Building maintenance",
        "repairs" to "Repairs"
    )
}
