package dev.focuslock.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.focuslock.lock.BreakResult
import dev.focuslock.lock.LockManager
import dev.focuslock.lock.LockRefusal
import dev.focuslock.lock.LockResult
import dev.focuslock.lock.LockStateStoreContract
import dev.focuslock.lock.UnlockResult
import dev.focuslock.pin.PinManager
import dev.focuslock.pin.VerifyResult
import dev.focuslock.pin.WrongPinException
import dev.focuslock.setup.SetupStatus
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
    val setup: SetupStatus = SetupStatus(blockerServiceEnabled = false, deviceAdminActive = false, pinSet = false),
    val isLocked: Boolean = false,
    val lastAppliedMs: Long = 0L,
    val pinDialog: PinDialogMode? = null,
    val pinMessage: PinMessage? = null,
    val isBusy: Boolean = false,
    val infoMessage: String? = null,
    val showRemoveConfirm: Boolean = false,
    val uninstallRequested: Boolean = false,
    val showBreakDialog: Boolean = false,
    /** End of the break that is active right now, else null. */
    val breakUntilMs: Long? = null,
) {
    val isPinSet: Boolean get() = setup.pinSet

    /** Locked, but the service or the admin was turned off, so nothing is actually blocked. */
    val isProtectionOff: Boolean get() = isLocked && !(setup.blockerServiceEnabled && setup.deviceAdminActive)
}

class MainViewModel(
    private val lockManager: LockManager,
    private val lockStateStore: LockStateStoreContract,
    private val pinManager: PinManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.update {
            it.copy(
                setup = lockManager.setupStatus(),
                isLocked = lockStateStore.isLocked,
                lastAppliedMs = lockStateStore.lastAppliedEpochMs,
                breakUntilMs = lockManager.activeBreak()?.untilMs,
            )
        }
    }

    fun lock() {
        val info = when (val result = lockManager.lock()) {
            LockResult.Locked -> null
            is LockResult.Refused -> when (result.reason) {
                LockRefusal.NO_PIN -> "Set a code first"
                LockRefusal.SETUP_INCOMPLETE -> "Finish the setup checklist first"
            }
        }
        _uiState.update { it.copy(infoMessage = info) }
        refresh()
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
                UnlockResult.Unlocked -> closeDialog(info = null)
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

    fun openBreakDialog() {
        _uiState.update { it.copy(showBreakDialog = true, pinMessage = null) }
    }

    fun dismissBreakDialog() {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(showBreakDialog = false, pinMessage = null) }
    }

    fun submitBreak(pin: String, hours: Int) {
        runBusy {
            when (val result = lockManager.startBreak(pin, hours)) {
                is BreakResult.Started -> {
                    _uiState.update { it.copy(showBreakDialog = false, pinMessage = null, infoMessage = null) }
                    refresh()
                }
                BreakResult.NotLocked -> {
                    _uiState.update { it.copy(showBreakDialog = false, infoMessage = "Nothing is blocked right now") }
                    refresh()
                }
                BreakResult.InvalidHours -> _uiState.update {
                    it.copy(infoMessage = "Choose ${LockManager.BREAK_HOURS.first}–${LockManager.BREAK_HOURS.last} hours")
                }
                is BreakResult.Refused -> showPinMessage(result.reason.toPinMessage())
            }
        }
    }

    fun endBreak() {
        lockManager.endBreak()
        refresh()
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
                UnlockResult.Unlocked -> {
                    _uiState.update { it.copy(uninstallRequested = true) }
                    closeDialog(info = "Protection removed. Confirm the uninstall.")
                }
                is UnlockResult.Refused -> showPinMessage(result.reason.toPinMessage())
            }
        }
    }

    fun onUninstallLaunched() {
        _uiState.update { it.copy(uninstallRequested = false) }
    }

    fun devForceUnlock() {
        lockManager.devForceUnlock()
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
