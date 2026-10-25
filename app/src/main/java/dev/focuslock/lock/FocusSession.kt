package dev.focuslock.lock

/**
 * Blocks everything on the block list until [untilMs], with no way to end it early.
 *
 * Unlike a break, moving the clock back does not end it (that would weaken blocking); LockManager
 * re-anchors it instead, so it can't stretch beyond its original length either.
 */
data class FocusSession(val startMs: Long, val untilMs: Long) {
    val durationMs: Long get() = untilMs - startMs

    fun isActive(nowMs: Long): Boolean = nowMs < untilMs
}
