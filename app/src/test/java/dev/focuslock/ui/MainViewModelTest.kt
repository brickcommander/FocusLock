package dev.focuslock.ui

import dev.focuslock.lock.FakeLockStateStore
import dev.focuslock.lock.LockManager
import dev.focuslock.pin.FakeClock
import dev.focuslock.pin.InMemoryPinStore
import dev.focuslock.pin.PinHasher
import dev.focuslock.pin.PinManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var store: FakeLockStateStore
    private lateinit var pinManager: PinManager
    private lateinit var lockManager: LockManager

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        store = FakeLockStateStore()
        pinManager = PinManager(InMemoryPinStore(), PinHasher(iterations = 1_000), FakeClock(), dispatcher)
        lockManager = LockManager(store, pinManager, FakeClock())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private var serviceEnabled = false

    private fun createViewModel() = MainViewModel(lockManager, store, pinManager) { serviceEnabled }

    private fun createViewModelWithPin(): MainViewModel = createViewModel().apply {
        submitSetPin(PIN, PIN)
    }

    @Test
    fun `fresh state is unlocked without a code`() {
        val vm = createViewModel()

        assertFalse(vm.uiState.value.isLocked)
        assertFalse(vm.uiState.value.isPinSet)
    }

    @Test
    fun `lock without a code stays unlocked and explains why`() {
        val vm = createViewModel()

        vm.lock()

        assertFalse(vm.uiState.value.isLocked)
        assertEquals("Set a code first", vm.uiState.value.infoMessage)
    }

    @Test
    fun `state shows isLocked after lock`() {
        val vm = createViewModelWithPin()

        vm.lock()

        assertTrue(vm.uiState.value.isLocked)
    }

    @Test
    fun `refresh re-reads the stored lock state`() {
        val vm = createViewModelWithPin()

        store.isLocked = true
        vm.refresh()

        assertTrue(vm.uiState.value.isLocked)
    }

    @Test
    fun `state shows unlocked after unlock with correct code`() {
        val vm = createViewModelWithPin()
        vm.lock()

        vm.openPinDialog(PinDialogMode.UNLOCK)
        vm.submitUnlock(PIN)

        assertFalse(vm.uiState.value.isLocked)
        assertNull(vm.uiState.value.pinDialog)
    }

    @Test
    fun `wrong unlock code shows remaining tries and stays locked`() {
        val vm = createViewModelWithPin()
        vm.lock()

        vm.openPinDialog(PinDialogMode.UNLOCK)
        vm.submitUnlock("000000")

        assertTrue(vm.uiState.value.isLocked)
        assertEquals(PinMessage.Wrong(remaining = 4), vm.uiState.value.pinMessage)
        assertEquals(PinDialogMode.UNLOCK, vm.uiState.value.pinDialog)
    }

    @Test
    fun `set code with mismatched confirmation is rejected`() {
        val vm = createViewModel()

        vm.openPinDialog(PinDialogMode.SET)
        vm.submitSetPin(PIN, "999999")

        assertEquals(PinMessage.Mismatch, vm.uiState.value.pinMessage)
        assertFalse(vm.uiState.value.isPinSet)
    }

    @Test
    fun `setting a code marks pin as set`() {
        val vm = createViewModel()

        vm.openPinDialog(PinDialogMode.SET)
        vm.submitSetPin(PIN, PIN)

        assertTrue(vm.uiState.value.isPinSet)
        assertNull(vm.uiState.value.pinDialog)
    }

    @Test
    fun `blocker service status is re-read on refresh`() {
        val vm = createViewModel()
        assertFalse(vm.uiState.value.isBlockerServiceEnabled)

        serviceEnabled = true
        vm.refresh()

        assertTrue(vm.uiState.value.isBlockerServiceEnabled)
    }

    @Test
    fun `dev force unlock clears the locked state`() {
        val vm = createViewModelWithPin()
        vm.lock()

        vm.devForceUnlock()

        assertFalse(vm.uiState.value.isLocked)
    }

    private companion object {
        const val PIN = "482916"
    }
}
