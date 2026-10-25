package dev.focuslock.lock

/**
 * A one-off break: blocking pauses from [startMs] until [untilMs]. It is checked against the clock
 * on every screen event, so it needs no alarm. Moving the clock back before [startMs] ends it.
 */
data class BreakWindow(val startMs: Long, val untilMs: Long) {
    fun isActive(nowMs: Long): Boolean = nowMs in startMs until untilMs
}
