package dev.focuslock.limits

import dev.focuslock.pin.FakeClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId

class DailyLimitsTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private lateinit var clock: FakeClock
    private lateinit var limitsStore: FakeLimitsStore
    private lateinit var limits: DailyLimits

    /** Monday 2026-10-26 at the given local time. */
    private fun monday(hour: Int, minute: Int = 0): Long =
        LocalDateTime.of(2026, 10, 26, hour, minute).atZone(zone).toInstant().toEpochMilli()

    @Before
    fun setUp() {
        clock = FakeClock(monday(10))
        limitsStore = FakeLimitsStore()
        limits = DailyLimits(limitsStore, FakeUsageStore(), clock, zone = { zone })
    }

    @Test
    fun `app without a limit has no remaining time value`() {
        assertNull(limits.remainingMs(APP))
        assertFalse(limits.hasLimit(APP))
    }

    @Test
    fun `allowance counts down with usage`() {
        limitsStore.put(APP, DailyLimit.everyDay(30))

        limits.addUsage(APP, 10 * MINUTE)

        assertEquals(20 * MINUTE, limits.remainingMs(APP))
    }

    @Test
    fun `used up allowance is zero, never negative`() {
        limitsStore.put(APP, DailyLimit.everyDay(30))

        limits.addUsage(APP, 45 * MINUTE)

        assertEquals(0L, limits.remainingMs(APP))
    }

    @Test
    fun `allowance follows the day of the week`() {
        limitsStore.put(APP, DailyLimit.NONE.withDay(DayOfWeek.MONDAY, 60).withDay(DayOfWeek.TUESDAY, 15))

        assertEquals(60 * MINUTE, limits.remainingMs(APP))
        clock.now = monday(10) + 24 * HOUR
        assertEquals(15 * MINUTE, limits.remainingMs(APP))
        clock.now = monday(10) + 2 * 24 * HOUR
        assertEquals(0L, limits.remainingMs(APP))
    }

    @Test
    fun `a limit of none every day blocks all day but is still a limit`() {
        limitsStore.put(APP, DailyLimit.NONE)

        assertTrue(limits.hasLimit(APP))
        assertEquals(0L, limits.remainingMs(APP))
    }

    @Test
    fun `usage resets at local midnight`() {
        limitsStore.put(APP, DailyLimit.everyDay(30))
        clock.now = monday(23, 50)
        limits.addUsage(APP, 30 * MINUTE)
        assertEquals(0L, limits.remainingMs(APP))

        clock.now = monday(23, 59) + MINUTE

        assertEquals(30 * MINUTE, limits.remainingMs(APP))
    }

    @Test
    fun `midnight is local, not UTC`() {
        limitsStore.put(APP, DailyLimit.everyDay(30))
        // 05:00 IST is still Sunday in UTC, 06:00 IST is Monday in UTC: same local day.
        clock.now = monday(5)
        limits.addUsage(APP, 30 * MINUTE)

        clock.now = monday(6)

        assertEquals(0L, limits.remainingMs(APP))
    }

    @Test
    fun `encode and decode round trip`() {
        val limit = DailyLimit(listOf(0, 15, 30, 60, 90, 720, 45))

        assertEquals(limit, DailyLimit.decode(limit.encode()))
    }

    @Test
    fun `corrupt stored limits decode to null`() {
        listOf(null, "", "1,2,3", "a,b,c,d,e,f,g", "0,0,0,0,0,0,9999", "-1,0,0,0,0,0,0").forEach {
            assertNull("expected null for '$it'", DailyLimit.decode(it))
        }
    }

    @Test
    fun `withDay clamps to the allowed range`() {
        val limit = DailyLimit.NONE.withDay(DayOfWeek.FRIDAY, 10_000).withDay(DayOfWeek.MONDAY, -5)

        assertEquals(DailyLimit.MAX_MINUTES, limit.minutesOn(DayOfWeek.FRIDAY))
        assertEquals(0, limit.minutesOn(DayOfWeek.MONDAY))
        assertTrue(DailyLimit.NONE.isEmpty)
    }

    private companion object {
        const val APP = "com.instagram.android"
        const val MINUTE = 60_000L
        const val HOUR = 60 * MINUTE
    }
}
