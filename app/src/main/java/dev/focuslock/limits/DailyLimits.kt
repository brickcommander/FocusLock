package dev.focuslock.limits

import dev.focuslock.pin.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Daily allowances for blocked apps. The day is the local calendar day, so every allowance
 * starts over at local midnight. Limits can only be changed while nothing is being blocked.
 */
class DailyLimits(
    private val limitsStore: LimitsStoreContract,
    private val usageStore: UsageStoreContract,
    private val clock: Clock,
    private val isBlocking: () -> Boolean,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
) {

    fun today(): LocalDate = Instant.ofEpochMilli(clock.nowMs()).atZone(zone()).toLocalDate()

    fun limitFor(pkg: String): DailyLimit = limitsStore.limits[pkg] ?: DailyLimit.NONE

    /** Minutes allowed today, 0 when the app has no allowance today. */
    fun allowedTodayMs(pkg: String): Long = limitFor(pkg).minutesOn(today().dayOfWeek) * MINUTE_MS

    fun usedTodayMs(pkg: String): Long = usageStore.usedMs(pkg, today())

    /** null: no allowance today (stays blocked). 0: used up. Otherwise the time left today. */
    fun remainingMs(pkg: String): Long? {
        val allowed = allowedTodayMs(pkg)
        if (allowed <= 0L) return null
        return (allowed - usedTodayMs(pkg)).coerceAtLeast(0L)
    }

    fun addUsage(pkg: String, ms: Long) = usageStore.add(pkg, today(), ms)

    /** @return false when refused because blocking is active (raising a limit would weaken it). */
    fun setLimit(pkg: String, limit: DailyLimit): Boolean {
        if (isBlocking()) return false
        limitsStore.put(pkg, limit)
        return true
    }

    companion object {
        const val MINUTE_MS = 60_000L
    }
}
