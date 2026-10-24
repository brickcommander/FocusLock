package dev.focuslock.ui

import dev.focuslock.lock.FakeLockStateStore
import dev.focuslock.lock.LockManager
import dev.focuslock.policy.FakePolicyController
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MainViewModelTest {

    private lateinit var fake: FakePolicyController
    private lateinit var store: FakeLockStateStore
    private lateinit var lockManager: LockManager

    @Before
    fun setUp() {
        fake = FakePolicyController()
        store = FakeLockStateStore()
        lockManager = LockManager(fake, store)
    }

    private fun createViewModel() = MainViewModel(fake, lockManager, store)

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
        val vm = createViewModel()

        vm.lock()

        assertTrue(vm.uiState.value.isLocked)
    }

    @Test
    fun `state shows unlocked after unlock`() {
        fake.deviceOwner = true
        val vm = createViewModel()

        vm.lock()
        assertTrue(vm.uiState.value.isLocked)

        vm.unlock()
        assertFalse(vm.uiState.value.isLocked)
    }
}
