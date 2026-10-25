package dev.focuslock.blocker

import dev.focuslock.pin.Clock

class DecisionThrottle(private val clock: Clock, private val windowMs: Long = 1_000) {

    private var lastDecision: Decision? = null
    private var lastFiredMs: Long = 0L

    /** true = execute; false = same decision fired within windowMs, skip */
    fun shouldFire(decision: Decision): Boolean {
        if (decision == Decision.Allow) return false

        val now = clock.nowMs()
        if (decision == lastDecision && now - lastFiredMs < windowMs) return false

        lastDecision = decision
        lastFiredMs = now
        return true
    }
}
