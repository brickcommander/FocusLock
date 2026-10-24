package dev.focuslock.ui

import dev.focuslock.policy.FakePolicyController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainViewModelTest {

    @Test
    fun `state shows isDeviceOwner true when policy returns true`() {
        val fake = FakePolicyController().apply { deviceOwner = true }
        val vm = MainViewModel(fake)

        assertTrue(vm.uiState.value.isDeviceOwner)
    }

    @Test
    fun `state shows isDeviceOwner false when policy returns false`() {
        val fake = FakePolicyController().apply { deviceOwner = false }
        val vm = MainViewModel(fake)

        assertFalse(vm.uiState.value.isDeviceOwner)
    }

    @Test
    fun `clearDeviceOwner delegates to policy and updates state`() {
        val fake = FakePolicyController().apply { deviceOwner = true }
        val vm = MainViewModel(fake)

        assertTrue(vm.uiState.value.isDeviceOwner)

        vm.clearDeviceOwner()

        assertFalse(vm.uiState.value.isDeviceOwner)
        assertTrue(fake.calls.contains("clearDeviceOwner"))
    }

    @Test
    fun `refresh re-reads device owner status`() {
        val fake = FakePolicyController().apply { deviceOwner = false }
        val vm = MainViewModel(fake)

        assertFalse(vm.uiState.value.isDeviceOwner)

        fake.deviceOwner = true
        vm.refresh()

        assertTrue(vm.uiState.value.isDeviceOwner)
    }
}
