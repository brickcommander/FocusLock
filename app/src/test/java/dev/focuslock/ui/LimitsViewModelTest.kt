package dev.focuslock.ui

import dev.focuslock.apps.AppEntry
import dev.focuslock.apps.FakeAppSource
import dev.focuslock.limits.DailyLimit
import dev.focuslock.limits.DailyLimits
import dev.focuslock.limits.FakeLimitsStore
import dev.focuslock.limits.FakeUsageStore
import dev.focuslock.limits.LimitsEditor
import dev.focuslock.pin.FakeClock
import dev.focuslock.pin.InMemoryPinStore
import dev.focuslock.pin.PinHasher
import dev.focuslock.pin.PinManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class LimitsViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var store: FakeLimitsStore
    private lateinit var limits: DailyLimits
    private lateinit var pinManager: PinManager
    private var inFocus = false
    private lateinit var vm: LimitsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        store = FakeLimitsStore()
        limits = DailyLimits(store, FakeUsageStore(), FakeClock(), zone = { ZoneOffset.UTC })
        pinManager = PinManager(InMemoryPinStore(), PinHasher(iterations = 1_000), FakeClock(), dispatcher)
        runTest(dispatcher) { pinManager.setPin(PIN) }
        inFocus = false
        vm = createViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = LimitsViewModel(
        dailyLimits = limits,
        editor = LimitsEditor(store, pinManager, isInFocus = { inFocus }, protectedPackages = { emptySet() }),
        appSource = FakeAppSource(listOf(AppEntry(INSTA, "Instagram"), AppEntry(YT, "YouTube"), AppEntry(WA, "WhatsApp"))),
        isPinSet = pinManager::isSet,
        isInFocus = { inFocus },
    )

    @Test
    fun `starts empty and asks for the code`() {
        assertTrue(vm.uiState.value.rows.isEmpty())
        assertTrue(vm.uiState.value.needsCode)
    }

    @Test
    fun `picker offers apps that have no limit yet`() {
        store.put(INSTA, DailyLimit.everyDay(30))
        vm.refresh()

        vm.openPicker()

        assertEquals(listOf("WhatsApp", "YouTube"), vm.uiState.value.pickerApps.map { it.label }.sorted())
    }

    @Test
    fun `adding an app with the code stores its limit`() {
        vm.openPicker()
        vm.pickApp(WA)
        vm.setEveryDay(45)

        vm.save(PIN)

        assertNull(vm.uiState.value.draft)
        assertEquals(DailyLimit.everyDay(45), limits.limitFor(WA))
        assertEquals(listOf("WhatsApp"), vm.uiState.value.rows.map { it.label })
    }

    @Test
    fun `editing with the code saves per-day minutes`() {
        store.put(INSTA, DailyLimit.everyDay(30))
        vm.refresh()
        vm.openEditor(INSTA)
        vm.setEveryDay(60)
        vm.setDay(DayOfWeek.SUNDAY, 120)

        vm.save(PIN)

        assertEquals(DailyLimit.everyDay(60).withDay(DayOfWeek.SUNDAY, 120), limits.limitFor(INSTA))
    }

    @Test
    fun `a wrong code keeps the editor open and changes nothing`() {
        store.put(INSTA, DailyLimit.everyDay(30))
        vm.refresh()
        vm.openEditor(INSTA)
        vm.setEveryDay(600)

        vm.save("wrong-code")

        assertEquals(PinMessage.Wrong(4), vm.uiState.value.pinMessage)
        assertTrue(vm.uiState.value.draft != null)
        assertEquals(DailyLimit.everyDay(30), limits.limitFor(INSTA))
    }

    @Test
    fun `an empty code asks for the code`() {
        vm.openPicker()
        vm.pickApp(WA)

        vm.save("")

        assertEquals(PinMessage.Empty, vm.uiState.value.pinMessage)
        assertNull(limits.limitFor(WA))
    }

    @Test
    fun `removing with the code deletes the limit`() {
        store.put(INSTA, DailyLimit.everyDay(30))
        vm.refresh()
        vm.openEditor(INSTA)

        vm.remove(PIN)

        assertNull(limits.limitFor(INSTA))
        assertTrue(vm.uiState.value.rows.isEmpty())
    }

    @Test
    fun `cancel keeps the old limit`() {
        store.put(INSTA, DailyLimit.everyDay(30))
        vm.refresh()
        vm.openEditor(INSTA)
        vm.setEveryDay(60)

        vm.dismissEditor()

        assertEquals(DailyLimit.everyDay(30), limits.limitFor(INSTA))
    }

    @Test
    fun `nothing can be opened during a focus session`() {
        store.put(INSTA, DailyLimit.everyDay(30))
        inFocus = true
        vm.refresh()

        vm.openEditor(INSTA)
        vm.openPicker()

        assertTrue(vm.uiState.value.isInFocus)
        assertNull(vm.uiState.value.draft)
        assertFalse(vm.uiState.value.showPicker)
        assertEquals("Limits can't be changed during a focus session", vm.uiState.value.message)
    }

    private companion object {
        const val PIN = "friend-code-42"
        const val INSTA = "com.instagram.android"
        const val YT = "com.google.android.youtube"
        const val WA = "com.whatsapp"
    }
}
