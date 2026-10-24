package dev.focuslock.pin

class FakeClock(var now: Long = 1_000_000L) : Clock {
    override fun nowMs(): Long = now

    fun advance(ms: Long) {
        now += ms
    }
}
