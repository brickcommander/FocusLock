package dev.focuslock.ui

import dev.focuslock.apps.AppEntry
import dev.focuslock.apps.FakeAppSource
import dev.focuslock.blocker.BlockRules
import dev.focuslock.blocker.FakeBlockRulesStore
import dev.focuslock.limits.DailyLimit
import dev.focuslock.limits.DailyLimits
import dev.focuslock.limits.FakeLimitsStore
import dev.focuslock.limits.FakeUsageStore
import dev.focuslock.pin.FakeClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.ZoneOffset

class LimitsViewModelTest {

    private var blocking = false
    private lateinit var limits: DailyLimits
    private lateinit var vm: LimitsViewModel

    @Before
    fun setUp() {
        blocking = false
        limits = DailyLimits(FakeLimitsStore(), FakeUsageStore(), FakeClock(), isBlocking = { blocking }, zone = { ZoneOffset.UTC })
        vm = LimitsViewModel(
            dailyLimits = limits,
            rulesStore = FakeBlockRulesStore(BlockRules(apps = setOf(INSTA, YT), sites = emptySet())),
            appSource = FakeAppSource(listOf(AppEntry(INSTA, "Instagram"), AppEntry(YT, "YouTube"))),
            isBlocking = { blocking },
        )
    }

    @Test
    fun `lists blocked apps by name with no allowance`() {
        val rows = vm.uiState.value.rows

        assertEquals(listOf("Instagram", "YouTube"), rows.map { it.label })
        assertEquals(DailyLimit.NONE, rows.first().limit)
    }

    @Test
    fun `editing and saving stores the limit`() {
        vm.openEditor(INSTA)
        vm.setEveryDay(60)
        vm.setDay(DayOfWeek.SUNDAY, 120)

        vm.saveDraft()

        assertNull(vm.uiState.value.draft)
        assertEquals(DailyLimit.everyDay(60).withDay(DayOfWeek.SUNDAY, 120), limits.limitFor(INSTA))
    }

    @Test
    fun `cancel keeps the old limit`() {
        vm.openEditor(INSTA)
        vm.setEveryDay(60)

        vm.dismissEditor()

        assertEquals(DailyLimit.NONE, limits.limitFor(INSTA))
    }

    @Test
    fun `no editing while blocking`() {
        blocking = true
        vm.refresh()

        vm.openEditor(INSTA)

        assertFalse(vm.uiState.value.canEdit)
        assertNull(vm.uiState.value.draft)
        assertEquals("Unlock first to change limits (not possible during a focus session)", vm.uiState.value.message)
    }

    @Test
    fun `save is refused if blocking started while the editor was open`() {
        vm.openEditor(INSTA)
        vm.setEveryDay(60)
        blocking = true

        vm.saveDraft()

        assertEquals(DailyLimit.NONE, limits.limitFor(INSTA))
    }

    private companion object {
        const val INSTA = "com.instagram.android"
        const val YT = "com.google.android.youtube"
    }
}
