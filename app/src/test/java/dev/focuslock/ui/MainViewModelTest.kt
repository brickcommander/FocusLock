package dev.focuslock.ui

import dev.focuslock.lock.FakeLockStateStore
import dev.focuslock.lock.LockManager
import dev.focuslock.pin.FakeClock
import dev.focuslock.pin.InMemoryPinStore
import dev.focuslock.pin.PinHasher
import dev.focuslock.pin.PinManager
import dev.focuslock.policy.FakePolicyController
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
    private lateinit var fake: FakePolicyController
    private lateinit var store: FakeLockStateStore
    private lateinit var pinManager: PinManager
    private lateinit var lockManager: LockManager

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        fake = FakePolicyController()
        store = FakeLockStateStore()
        pinManager = PinManager(InMemoryPinStore(), PinHasher(iterations = 1_000), FakeClock(), dispatcher)
        lockManager = LockManager(fake, store, pinManager, FakeClock())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = MainViewModel(fake, lockManager, store, pinManager)

    private fun createViewModelWithPin(): MainViewModel = createViewModel().apply {
        submitSetPin(PIN, PIN)
    }

    @Test
    fun `state shows isDeviceOwner true when policy returns true`() {
        fake.deviceOwner = true
        val vm = createViewModel()

        assertTrue(vm.uiState.value.isDeviceOwner)
    }

    @Test
    fun `state shows isDeviceOwner false when policy returns false`() {
        fake.deviceOwner = false
        val vm = createViewModel()

        assertFalse(vm.uiState.value.isDeviceOwner)
    }

    @Test
    fun `clearDeviceOwner delegates to policy and updates state`() {
        fake.deviceOwner = true
        val vm = createViewModel()

        assertTrue(vm.uiState.value.isDeviceOwner)

        vm.clearDeviceOwner()

        assertFalse(vm.uiState.value.isDeviceOwner)
        assertTrue(fake.calls.contains("clearDeviceOwner"))
    }

    @Test
    fun `refresh re-reads device owner status`() {
        fake.deviceOwner = false
        val vm = createViewModel()

        assertFalse(vm.uiState.value.isDeviceOwner)

        fake.deviceOwner = true
        vm.refresh()

        assertTrue(vm.uiState.value.isDeviceOwner)
    }

    @Test
    fun `state shows isLocked after lock`() {
        fake.deviceOwner = true
        val vm = createViewModelWithPin()

        vm.lock()

        assertTrue(vm.uiState.value.isLocked)
    }

    @Test
    fun `state shows unlocked after unlock with correct code`() {
        fake.deviceOwner = true
        val vm = createViewModelWithPin()
        vm.lock()

        vm.openPinDialog(PinDialogMode.UNLOCK)
        vm.submitUnlock(PIN)

        assertFalse(vm.uiState.value.isLocked)
        assertNull(vm.uiState.value.pinDialog)
    }

    @Test
    fun `wrong unlock code shows remaining tries and stays locked`() {
        fake.deviceOwner = true
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
        fake.deviceOwner = true
        val vm = createViewModel()

        vm.openPinDialog(PinDialogMode.SET)
        vm.submitSetPin(PIN, "999999")

        assertEquals(PinMessage.Mismatch, vm.uiState.value.pinMessage)
        assertFalse(vm.uiState.value.isPinSet)
    }

    @Test
    fun `setting a code marks pin as set`() {
        fake.deviceOwner = true
        val vm = createViewModel()

        vm.openPinDialog(PinDialogMode.SET)
        vm.submitSetPin(PIN, PIN)

        assertTrue(vm.uiState.value.isPinSet)
        assertNull(vm.uiState.value.pinDialog)
    }

    @Test
    fun `init reapplies lock when stored state is locked`() {
        fake.deviceOwner = true
        createViewModelWithPin().lock()
        fake.installedPackages.add("com.reddit.frontpage")

        val vm = createViewModel()

        assertTrue(fake.suspendedPackages.contains("com.reddit.frontpage"))
        assertTrue(vm.uiState.value.lastReport!!.applied.contains("com.reddit.frontpage"))
    }

    @Test
    fun `remove completely with correct code requests uninstall`() {
        fake.deviceOwner = true
        val vm = createViewModelWithPin()
        vm.lock()

        vm.requestRemoveCompletely()
        vm.confirmRemoveCompletely()
        vm.submitRemoveCompletely(PIN)

        assertTrue(vm.uiState.value.uninstallRequested)
        assertFalse(vm.uiState.value.isDeviceOwner)
        assertFalse(vm.uiState.value.isLocked)
    }

    @Test
    fun `remove completely with wrong code does not request uninstall`() {
        fake.deviceOwner = true
        val vm = createViewModelWithPin()
        vm.lock()

        vm.requestRemoveCompletely()
        vm.confirmRemoveCompletely()
        vm.submitRemoveCompletely("000000")

        assertFalse(vm.uiState.value.uninstallRequested)
        assertTrue(vm.uiState.value.isDeviceOwner)
        assertEquals(PinMessage.Wrong(remaining = 4), vm.uiState.value.pinMessage)
    }

    private companion object {
        const val PIN = "482916"
    }
}
