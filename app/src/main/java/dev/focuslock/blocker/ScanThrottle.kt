package dev.focuslock.blocker

import dev.focuslock.pin.Clock

/** Limits Settings screen-text scans: every window change, but content changes (scrolling) at most every intervalMs. */
class ScanThrottle(private val clock: Clock, private val intervalMs: Long = 300) {

    private var lastScanMs: Long? = null

    fun shouldScan(isWindowStateChange: Boolean): Boolean {
        val now = clock.nowMs()
        val last = lastScanMs
        if (!isWindowStateChange && last != null && now - last < intervalMs) return false
        lastScanMs = now
        return true
    }
}
