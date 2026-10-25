package dev.focuslock.limits

import dev.focuslock.pin.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Daily time limits per app. They apply whether FocusLock is locked or not. The day is the local
 * calendar day, so every limit starts over at local midnight. Changes go through [LimitsEditor].
 */
class DailyLimits(
    private val limitsStore: LimitsStoreContract,
    private val usageStore: UsageStoreContract,
    private val clock: Clock,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
) {

    fun today(): LocalDate = Instant.ofEpochMilli(clock.nowMs()).atZone(zone()).toLocalDate()

    val limits: Map<String, DailyLimit> get() = limitsStore.limits

    fun hasLimit(pkg: String): Boolean = pkg in limitsStore.limits

    fun limitFor(pkg: String): DailyLimit? = limitsStore.limits[pkg]

    /** Time allowed today, 0 when the app has no limit or no time today. */
    fun allowedTodayMs(pkg: String): Long = (limitFor(pkg)?.minutesOn(today().dayOfWeek) ?: 0) * MINUTE_MS

    fun usedTodayMs(pkg: String): Long = usageStore.usedMs(pkg, today())

    /** null: the app has no limit. 0: nothing left today. Otherwise the time left today. */
    fun remainingMs(pkg: String): Long? {
        if (!hasLimit(pkg)) return null
        return (allowedTodayMs(pkg) - usedTodayMs(pkg)).coerceAtLeast(0L)
    }

    fun addUsage(pkg: String, ms: Long) = usageStore.add(pkg, today(), ms)

    companion object {
        const val MINUTE_MS = 60_000L
    }
}
