package com.finnest.app

import java.util.GregorianCalendar
import java.util.Locale

object ReminderDate {
    fun valid(value: String): Boolean {
        if (!value.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}"))) return false
        val parts = value.split("-").map { it.toInt() }
        if (parts[0] !in 1900..2100) return false
        return try {
            GregorianCalendar().apply { isLenient = false; clear(); set(parts[0], parts[1] - 1, parts[2]); timeInMillis }.let { true }
        } catch (_: IllegalArgumentException) { false }
    }
    fun display(value: String): String = if (valid(value)) "${value.substring(8, 10)}/${value.substring(5, 7)}/${value.substring(0, 4)}" else value
    fun fromParts(year: Int, month: Int, day: Int): String = String.format(Locale.ROOT, "%04d-%02d-%02d", year, month, day)
}
