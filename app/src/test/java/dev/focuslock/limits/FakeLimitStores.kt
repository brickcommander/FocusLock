package dev.focuslock.limits

import java.time.LocalDate

class FakeLimitsStore : LimitsStoreContract {
    private val map = mutableMapOf<String, DailyLimit>()
    private val listeners = mutableSetOf<() -> Unit>()

    override val limits: Map<String, DailyLimit> get() = map.toMap()

    override fun put(pkg: String, limit: DailyLimit) {
        if (limit.isEmpty) map.remove(pkg) else map[pkg] = limit
        listeners.toList().forEach { it() }
    }

    override fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    override fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }
}

/** Mirrors UsageStore: a single current day; touching another day starts from zero. */
class FakeUsageStore : UsageStoreContract {
    private var day: LocalDate? = null
    private val used = mutableMapOf<String, Long>()

    override fun usedMs(pkg: String, day: LocalDate): Long = if (day == this.day) used[pkg] ?: 0L else 0L

    override fun add(pkg: String, day: LocalDate, ms: Long) {
        if (ms <= 0L) return
        if (day != this.day) {
            used.clear()
            this.day = day
        }
        used[pkg] = (used[pkg] ?: 0L) + ms
    }
}
