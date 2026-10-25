package dev.focuslock.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.focuslock.apps.AppEntry
import dev.focuslock.apps.AppSource
import dev.focuslock.limits.DailyLimit
import dev.focuslock.limits.DailyLimits
import dev.focuslock.limits.LimitEditResult
import dev.focuslock.limits.LimitsEditor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek

data class LimitRow(
    val packageName: String,
    val label: String,
    val limit: DailyLimit,
    val allowedTodayMs: Long,
    val usedTodayMs: Long,
) {
    val leftTodayMs: Long get() = (allowedTodayMs - usedTodayMs).coerceAtLeast(0L)
}

data class LimitDraft(val packageName: String, val label: String, val limit: DailyLimit, val isNew: Boolean)

data class LimitsUiState(
    val rows: List<LimitRow> = emptyList(),
    val draft: LimitDraft? = null,
    val showPicker: Boolean = false,
    val pickerApps: List<AppEntry> = emptyList(),
    /** Changes need the code once one is set. */
    val needsCode: Boolean = false,
    val isInFocus: Boolean = false,
    val pinMessage: PinMessage? = null,
    val isBusy: Boolean = false,
    val message: String? = null,
)

class LimitsViewModel(
    private val dailyLimits: DailyLimits,
    private val editor: LimitsEditor,
    private val appSource: AppSource,
    private val isPinSet: () -> Boolean,
    private val isInFocus: () -> Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LimitsUiState())
    val uiState: StateFlow<LimitsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val rows = dailyLimits.limits
            .map { (pkg, limit) ->
                LimitRow(
                    packageName = pkg,
                    label = appSource.label(pkg) ?: pkg,
                    limit = limit,
                    allowedTodayMs = dailyLimits.allowedTodayMs(pkg),
                    usedTodayMs = dailyLimits.usedTodayMs(pkg),
                )
            }
            .sortedBy { it.label.lowercase() }
        _uiState.update { it.copy(rows = rows, needsCode = isPinSet(), isInFocus = isInFocus()) }
    }

    fun openPicker() {
        if (refuseInFocus()) return
        val hidden = dailyLimits.limits.keys + editor.unlimitable()
        val choices = appSource.launchableApps().filter { it.packageName !in hidden }
        _uiState.update { it.copy(showPicker = true, pickerApps = choices, message = null) }
    }

    fun closePicker() {
        _uiState.update { it.copy(showPicker = false) }
    }

    fun pickApp(packageName: String) {
        val label = appSource.label(packageName) ?: packageName
        _uiState.update {
            it.copy(
                showPicker = false,
                draft = LimitDraft(packageName, label, DailyLimit.everyDay(DEFAULT_MINUTES), isNew = true),
                pinMessage = null,
            )
        }
    }

    fun openEditor(packageName: String) {
        if (refuseInFocus()) return
        val row = _uiState.value.rows.firstOrNull { it.packageName == packageName } ?: return
        _uiState.update {
            it.copy(draft = LimitDraft(row.packageName, row.label, row.limit, isNew = false), pinMessage = null, message = null)
        }
    }

    fun setDay(day: DayOfWeek, minutes: Int) = editDraft { it.withDay(day, minutes) }

    fun setEveryDay(minutes: Int) = editDraft { DailyLimit.everyDay(minutes) }

    fun dismissEditor() {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(draft = null, pinMessage = null) }
    }

    fun save(pin: String) {
        val draft = _uiState.value.draft ?: return
        runBusy(successMessage = if (draft.isNew) "Limit added" else "Limit saved") {
            editor.save(pin, draft.packageName, draft.limit)
        }
    }

    fun remove(pin: String) {
        val draft = _uiState.value.draft ?: return
        if (draft.isNew) return dismissEditor()
        runBusy(successMessage = "Limit removed") { editor.remove(pin, draft.packageName) }
    }

    private fun runBusy(successMessage: String, block: suspend () -> LimitEditResult) {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true, pinMessage = null) }
        viewModelScope.launch {
            try {
                when (val result = block()) {
                    LimitEditResult.Saved -> closeEditor(successMessage)
                    is LimitEditResult.Refused -> _uiState.update { it.copy(pinMessage = result.reason.toPinMessage()) }
                    LimitEditResult.InFocusSession -> closeEditor(FOCUS_MESSAGE)
                    LimitEditResult.Protected -> closeEditor("This app can't be limited (it's needed to use the phone)")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(pinMessage = PinMessage.Failed) }
            } finally {
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }

    private fun closeEditor(message: String) {
        _uiState.update { it.copy(draft = null, pinMessage = null, message = message) }
        refresh()
    }

    private fun refuseInFocus(): Boolean {
        if (!isInFocus()) return false
        _uiState.update { it.copy(message = FOCUS_MESSAGE, isInFocus = true) }
        return true
    }

    private inline fun editDraft(change: (DailyLimit) -> DailyLimit) {
        _uiState.update { state ->
            val draft = state.draft ?: return@update state
            state.copy(draft = draft.copy(limit = change(draft.limit)))
        }
    }

    private companion object {
        const val DEFAULT_MINUTES = 60
        const val FOCUS_MESSAGE = "Limits can't be changed during a focus session"
    }
}
