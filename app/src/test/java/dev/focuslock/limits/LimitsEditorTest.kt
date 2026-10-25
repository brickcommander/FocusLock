package dev.focuslock.limits

import dev.focuslock.pin.FakeClock
import dev.focuslock.pin.InMemoryPinStore
import dev.focuslock.pin.PinHasher
import dev.focuslock.pin.PinManager
import dev.focuslock.pin.VerifyResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LimitsEditorTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var store: FakeLimitsStore
    private lateinit var pinManager: PinManager
    private var inFocus = false
    private lateinit var editor: LimitsEditor

    @Before
    fun setUp() {
        store = FakeLimitsStore()
        pinManager = PinManager(InMemoryPinStore(), PinHasher(iterations = 1_000), FakeClock(), dispatcher)
        inFocus = false
        editor = LimitsEditor(store, pinManager, isInFocus = { inFocus }, protectedPackages = { setOf(LAUNCHER) })
    }

    private suspend fun withCode() = pinManager.setPin(PIN)

    @Test
    fun `before a code exists, changes need no code`() = runTest(dispatcher) {
        val result = editor.save("", APP, DailyLimit.everyDay(30))

        assertEquals(LimitEditResult.Saved, result)
        assertEquals(DailyLimit.everyDay(30), store.limits[APP])
    }

    @Test
    fun `saving with the right code stores the limit`() = runTest(dispatcher) {
        withCode()

        val result = editor.save(PIN, APP, DailyLimit.everyDay(30))

        assertEquals(LimitEditResult.Saved, result)
        assertEquals(DailyLimit.everyDay(30), store.limits[APP])
    }

    @Test
    fun `a wrong code changes nothing`() = runTest(dispatcher) {
        withCode()
        store.put(APP, DailyLimit.everyDay(30))

        val result = editor.save("wrong-code", APP, DailyLimit.everyDay(600))

        assertTrue(result is LimitEditResult.Refused && result.reason is VerifyResult.Wrong)
        assertEquals(DailyLimit.everyDay(30), store.limits[APP])
    }

    @Test
    fun `an empty code is refused without crashing`() = runTest(dispatcher) {
        withCode()

        val result = editor.save("", APP, DailyLimit.everyDay(30))

        assertEquals(LimitEditResult.Refused(VerifyResult.Empty), result)
        assertNull(store.limits[APP])
    }

    @Test
    fun `removing needs the code`() = runTest(dispatcher) {
        withCode()
        store.put(APP, DailyLimit.everyDay(30))

        val refused = editor.remove("wrong-code", APP)
        val removed = editor.remove(PIN, APP)

        assertTrue(refused is LimitEditResult.Refused)
        assertEquals(LimitEditResult.Saved, removed)
        assertNull(store.limits[APP])
    }

    @Test
    fun `no changes during a focus session, even with the code`() = runTest(dispatcher) {
        withCode()
        store.put(APP, DailyLimit.everyDay(30))
        inFocus = true

        assertEquals(LimitEditResult.InFocusSession, editor.save(PIN, APP, DailyLimit.everyDay(600)))
        assertEquals(LimitEditResult.InFocusSession, editor.remove(PIN, APP))
        assertEquals(DailyLimit.everyDay(30), store.limits[APP])
    }

    @Test
    fun `phone essentials can't be limited`() = runTest(dispatcher) {
        assertEquals(LimitEditResult.Protected, editor.save("", LAUNCHER, DailyLimit.NONE))
        assertEquals(LimitEditResult.Protected, editor.save("", "com.android.settings", DailyLimit.NONE))
        assertEquals(LimitEditResult.Protected, editor.save("", "dev.focuslock", DailyLimit.NONE))
        assertTrue(store.limits.isEmpty())
    }

    private companion object {
        const val PIN = "friend-code-42"
        const val APP = "com.instagram.android"
        const val LAUNCHER = "com.sec.android.app.launcher"
    }
}
