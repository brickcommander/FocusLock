package dev.focuslock.limits

import java.time.DayOfWeek

/**
 * How long a blocked app may be used on each day of the week, in minutes (index 0 = Monday).
 * 0 means no allowance that day: the app stays blocked like any other blocked app.
 */
data class DailyLimit(val minutes: List<Int>) {

    init {
        require(minutes.size == DAYS) { "Need $DAYS days, got ${minutes.size}" }
        require(minutes.all { it in 0..MAX_MINUTES }) { "Minutes must be 0..$MAX_MINUTES" }
    }

    fun minutesOn(day: DayOfWeek): Int = minutes[day.value - 1]

    fun withDay(day: DayOfWeek, value: Int): DailyLimit =
        DailyLimit(minutes.toMutableList().also { it[day.value - 1] = value.coerceIn(0, MAX_MINUTES) })

    val isEmpty: Boolean get() = minutes.all { it == 0 }

    fun encode(): String = minutes.joinToString(",")

    companion object {
        const val DAYS = 7
        const val STEP_MINUTES = 15
        const val MAX_MINUTES = 12 * 60

        val NONE = DailyLimit(List(DAYS) { 0 })

        fun everyDay(value: Int): DailyLimit = DailyLimit(List(DAYS) { value.coerceIn(0, MAX_MINUTES) })

        /** Corrupt or foreign data becomes null, which keeps the app fully blocked. */
        fun decode(raw: String?): DailyLimit? {
            val parts = raw?.split(',') ?: return null
            if (parts.size != DAYS) return null
            val minutes = parts.map { it.trim().toIntOrNull() ?: return null }
            if (minutes.any { it !in 0..MAX_MINUTES }) return null
            return DailyLimit(minutes)
        }
    }
}
