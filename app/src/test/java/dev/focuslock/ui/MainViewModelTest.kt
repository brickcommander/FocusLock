package dev.focuslock.ui

import dev.focuslock.lock.FakeLockStateStore
import dev.focuslock.lock.LockManager
import dev.focuslock.pin.FakeClock
import dev.focuslock.pin.InMemoryPinStore
import dev.focuslock.pin.PinHasher
import dev.focuslock.pin.PinManager
import dev.focuslock.setup.FakeSetupChecker
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
    private lateinit var setup: FakeSetupChecker
    private lateinit var lockManager: LockManager

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        store = FakeLockStateStore()
        setup = FakeSetupChecker()
        pinManager = PinManager(InMemoryPinStore(), PinHasher(iterations = 1_000), FakeClock(), dispatcher)
        lockManager = LockManager(store, pinManager, setup, FakeClock())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = MainViewModel(lockManager, store, pinManager)

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
    fun `setup status is re-read on refresh`() {
        setup.serviceEnabled = false
        val vm = createViewModel()
        assertFalse(vm.uiState.value.setup.blockerServiceEnabled)

        setup.serviceEnabled = true
        vm.refresh()

        assertTrue(vm.uiState.value.setup.blockerServiceEnabled)
    }

    @Test
    fun `lock with incomplete setup stays unlocked and explains why`() {
        setup.adminActive = false
        val vm = createViewModelWithPin()

        vm.lock()

        assertFalse(vm.uiState.value.isLocked)
        assertEquals("Finish the setup checklist first", vm.uiState.value.infoMessage)
    }

    @Test
    fun `protection off banner shown when locked and service disabled`() {
        val vm = createViewModelWithPin()
        vm.lock()

        setup.serviceEnabled = false
        vm.refresh()

        assertTrue(vm.uiState.value.isProtectionOff)
    }

    @Test
    fun `protection off banner hidden when locked and setup complete`() {
        val vm = createViewModelWithPin()

        vm.lock()

        assertFalse(vm.uiState.value.isProtectionOff)
    }

    @Test
    fun `health banner hidden when unlocked even if service off`() {
        setup.serviceEnabled = false
        setup.adminActive = false

        val vm = createViewModelWithPin()

        assertFalse(vm.uiState.value.isLocked)
        assertFalse(vm.uiState.value.isProtectionOff)
    }

    @Test
    fun `remove completely with correct code unlocks and requests uninstall`() {
        val vm = createViewModelWithPin()
        vm.lock()

        vm.requestRemoveCompletely()
        vm.confirmRemoveCompletely()
        vm.submitRemoveCompletely(PIN)

        val state = vm.uiState.value
        assertFalse(state.isLocked)
        assertFalse(state.setup.deviceAdminActive)
        assertTrue(state.uninstallRequested)
        assertNull(state.pinDialog)
    }

    @Test
    fun `remove completely with wrong code keeps everything and does not request uninstall`() {
        val vm = createViewModelWithPin()
        vm.lock()

        vm.requestRemoveCompletely()
        vm.confirmRemoveCompletely()
        vm.submitRemoveCompletely("000000")

        val state = vm.uiState.value
        assertTrue(state.isLocked)
        assertEquals(0, setup.deactivateAdminCalls)
        assertFalse(state.uninstallRequested)
        assertEquals(PinMessage.Wrong(remaining = 4), state.pinMessage)
        assertEquals(PinDialogMode.REMOVE, state.pinDialog)
    }

    @Test
    fun `remove completely confirm dialog leads to the code dialog`() {
        val vm = createViewModelWithPin()

        vm.requestRemoveCompletely()
        assertTrue(vm.uiState.value.showRemoveConfirm)
        vm.confirmRemoveCompletely()

        assertFalse(vm.uiState.value.showRemoveConfirm)
        assertEquals(PinDialogMode.REMOVE, vm.uiState.value.pinDialog)
    }

    @Test
    fun `uninstall request is cleared once launched`() {
        val vm = createViewModelWithPin()
        vm.confirmRemoveCompletely()
        vm.submitRemoveCompletely(PIN)

        vm.onUninstallLaunched()

        assertFalse(vm.uiState.value.uninstallRequested)
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
