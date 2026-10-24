package dev.focuslock.ui

import androidx.lifecycle.ViewModel
import dev.focuslock.policy.PolicyController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MainUiState(
    val isDeviceOwner: Boolean = false,
)

class MainViewModel(private val policyController: PolicyController) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.value = MainUiState(
            isDeviceOwner = policyController.isDeviceOwner(),
        )
    }

    fun clearDeviceOwner() {
        policyController.clearDeviceOwner()
        refresh()
    }
}
