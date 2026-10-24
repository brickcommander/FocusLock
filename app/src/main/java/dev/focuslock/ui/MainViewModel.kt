package dev.focuslock.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.focuslock.lock.LockManager
import dev.focuslock.lock.LockReport
import dev.focuslock.lock.LockStateStoreContract
import dev.focuslock.lock.UnlockResult
import dev.focuslock.pin.PinManager
import dev.focuslock.pin.VerifyResult
import dev.focuslock.pin.WrongPinException
import dev.focuslock.policy.PolicyController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PinDialogMode { SET, UNLOCK, CHANGE, REMOVE }

sealed class PinMessage {
    data class Wrong(val remaining: Int) : PinMessage()
    data class LockedOut(val untilMs: Long) : PinMessage()
    data object InvalidFormat : PinMessage()
    data object Mismatch : PinMessage()
    data object NotSet : PinMessage()
    data object AlreadySet : PinMessage()
}

data class MainUiState(
    val isDeviceOwner: Boolean = false,
    val isLocked: Boolean = false,
    val isPinSet: Boolean = false,
    val lastAppliedMs: Long = 0L,
    val lastReport: LockReport? = null,
    val pinDialog: PinDialogMode? = null,
    val pinMessage: PinMessage? = null,
    val isBusy: Boolean = false,
    val infoMessage: String? = null,
    val showRemoveConfirm: Boolean = false,
    val uninstallRequested: Boolean = false,
)

class MainViewModel(
    private val policyController: PolicyController,
    private val lockManager: LockManager,
    private val lockStateStore: LockStateStoreContract,
    private val pinManager: PinManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        reconcile()
    }

    /** Re-applies the lock if something drifted (e.g. a blocked app was installed), then refreshes. */
    fun reconcile() {
        val report = lockManager.reapplyIfLocked()
        refresh()
        if (report != null) {
            _uiState.update { it.copy(lastReport = report) }
        }
    }

    fun refresh() {
        _uiState.update {
            it.copy(
                isDeviceOwner = policyController.isDeviceOwner(),
                isLocked = lockStateStore.isLocked,
                isPinSet = pinManager.isSet(),
                lastAppliedMs = lockStateStore.lastAppliedEpochMs,
            )
        }
    }

    fun lock() {
        val report = lockManager.lock()
        refresh()
        _uiState.update { it.copy(lastReport = report, infoMessage = null) }
    }

    fun openPinDialog(mode: PinDialogMode) {
        _uiState.update { it.copy(pinDialog = mode, pinMessage = null) }
    }

    fun dismissPinDialog() {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(pinDialog = null, pinMessage = null) }
    }

    fun submitSetPin(pin: String, confirm: String) {
        if (pin != confirm) return showPinMessage(PinMessage.Mismatch)
        if (!PinManager.isValidFormat(pin)) return showPinMessage(PinMessage.InvalidFormat)
        runBusy {
            pinManager.setPin(pin).fold(
                onSuccess = { closeDialog(info = "Code saved") },
                onFailure = { e ->
                    showPinMessage(if (e is IllegalStateException) PinMessage.AlreadySet else PinMessage.InvalidFormat)
                },
            )
        }
    }

    fun submitUnlock(pin: String) {
        runBusy {
            when (val result = lockManager.unlock(pin)) {
                is UnlockResult.Unlocked -> {
                    _uiState.update { it.copy(isLocked = lockStateStore.isLocked, lastReport = result.report) }
                    closeDialog(info = null)
                }
                is UnlockResult.Refused -> showPinMessage(result.reason.toPinMessage())
            }
        }
    }

    fun submitChangePin(old: String, new: String, confirm: String) {
        if (new != confirm) return showPinMessage(PinMessage.Mismatch)
        if (!PinManager.isValidFormat(new)) return showPinMessage(PinMessage.InvalidFormat)
        runBusy {
            pinManager.changePin(old, new).fold(
                onSuccess = { closeDialog(info = "Code changed") },
                onFailure = { e ->
                    showPinMessage(
                        if (e is WrongPinException) e.result.toPinMessage() else PinMessage.InvalidFormat
                    )
                },
            )
        }
    }

    fun requestRemoveCompletely() {
        _uiState.update { it.copy(showRemoveConfirm = true) }
    }

    fun cancelRemoveCompletely() {
        _uiState.update { it.copy(showRemoveConfirm = false) }
    }

    fun confirmRemoveCompletely() {
        _uiState.update { it.copy(showRemoveConfirm = false, pinDialog = PinDialogMode.REMOVE, pinMessage = null) }
    }

    fun submitRemoveCompletely(pin: String) {
        runBusy {
            when (val result = lockManager.removeCompletely(pin)) {
                is UnlockResult.Unlocked -> {
                    _uiState.update { it.copy(lastReport = result.report, uninstallRequested = true) }
                    closeDialog(info = "All restrictions removed. Confirm the uninstall.")
                }
                is UnlockResult.Refused -> showPinMessage(result.reason.toPinMessage())
            }
        }
    }

    fun onUninstallLaunched() {
        _uiState.update { it.copy(uninstallRequested = false) }
    }

    fun clearDeviceOwner() {
        lockManager.devRemoveDeviceOwner()
        refresh()
    }

    private fun runBusy(block: suspend () -> Unit) {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true, pinMessage = null) }
        viewModelScope.launch {
            try {
                block()
            } finally {
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }

    private fun showPinMessage(message: PinMessage) {
        _uiState.update { it.copy(pinMessage = message) }
    }

    private fun closeDialog(info: String?) {
        _uiState.update { it.copy(pinDialog = null, pinMessage = null, infoMessage = info) }
        refresh()
    }

    private fun VerifyResult.toPinMessage(): PinMessage = when (this) {
        is VerifyResult.Wrong -> PinMessage.Wrong(remaining)
        is VerifyResult.LockedOut -> PinMessage.LockedOut(untilMs)
        VerifyResult.NotSet -> PinMessage.NotSet
        VerifyResult.Ok -> error("Ok is not a refusal")
    }
}
