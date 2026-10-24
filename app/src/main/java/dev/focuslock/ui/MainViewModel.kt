package dev.focuslock.ui

import androidx.lifecycle.ViewModel
import dev.focuslock.lock.LockManager
import dev.focuslock.lock.LockReport
import dev.focuslock.lock.LockStateStoreContract
import dev.focuslock.policy.PolicyController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MainUiState(
    val isDeviceOwner: Boolean = false,
    val isLocked: Boolean = false,
    val lastReport: LockReport? = null,
)

class MainViewModel(
    private val policyController: PolicyController,
    private val lockManager: LockManager,
    private val lockStateStore: LockStateStoreContract,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(
            isDeviceOwner = policyController.isDeviceOwner(),
            isLocked = lockStateStore.isLocked,
        )
    }

    fun lock() {
        val report = lockManager.lock()
        _uiState.value = _uiState.value.copy(
            isLocked = lockStateStore.isLocked,
            lastReport = report,
        )
    }

    fun unlock() {
        val report = lockManager.unlock()
        _uiState.value = _uiState.value.copy(
            isLocked = lockStateStore.isLocked,
            lastReport = report,
        )
    }

    fun clearDeviceOwner() {
        policyController.clearDeviceOwner()
        refresh()
    }
}
