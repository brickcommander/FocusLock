package dev.focuslock.ui

import androidx.lifecycle.ViewModel
import dev.focuslock.apps.AppSource
import dev.focuslock.blocker.BlockRulesStoreContract
import dev.focuslock.limits.DailyLimit
import dev.focuslock.limits.DailyLimits
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.DayOfWeek

data class LimitRow(
    val packageName: String,
    val label: String,
    val limit: DailyLimit,
    val allowedTodayMs: Long,
    val usedTodayMs: Long,
)

data class LimitDraft(val packageName: String, val label: String, val limit: DailyLimit)

data class LimitsUiState(
    val canEdit: Boolean = true,
    val rows: List<LimitRow> = emptyList(),
    val draft: LimitDraft? = null,
    val message: String? = null,
)

class LimitsViewModel(
    private val dailyLimits: DailyLimits,
    private val rulesStore: BlockRulesStoreContract,
    private val appSource: AppSource,
    private val isBlocking: () -> Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LimitsUiState())
    val uiState: StateFlow<LimitsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    /** Only blocked apps can have an allowance: the limit is how long they're NOT blocked each day. */
    fun refresh() {
        val rows = rulesStore.rules.apps
            .map { pkg ->
                LimitRow(
                    packageName = pkg,
                    label = appSource.label(pkg) ?: pkg,
                    limit = dailyLimits.limitFor(pkg),
                    allowedTodayMs = dailyLimits.allowedTodayMs(pkg),
                    usedTodayMs = dailyLimits.usedTodayMs(pkg),
                )
            }
            .sortedBy { it.label.lowercase() }
        _uiState.update { it.copy(canEdit = !isBlocking(), rows = rows) }
    }

    fun openEditor(packageName: String) {
        if (isBlocking()) {
            _uiState.update { it.copy(message = LOCKED_MESSAGE) }
            return
        }
        val row = _uiState.value.rows.firstOrNull { it.packageName == packageName } ?: return
        _uiState.update { it.copy(draft = LimitDraft(row.packageName, row.label, row.limit), message = null) }
    }

    fun setDay(day: DayOfWeek, minutes: Int) = editDraft { it.withDay(day, minutes) }

    fun setEveryDay(minutes: Int) = editDraft { DailyLimit.everyDay(minutes) }

    fun dismissEditor() {
        _uiState.update { it.copy(draft = null) }
    }

    fun saveDraft() {
        val draft = _uiState.value.draft ?: return
        val saved = dailyLimits.setLimit(draft.packageName, draft.limit)
        _uiState.update { it.copy(draft = null, message = if (saved) null else LOCKED_MESSAGE) }
        refresh()
    }

    private inline fun editDraft(change: (DailyLimit) -> DailyLimit) {
        _uiState.update { state ->
            val draft = state.draft ?: return@update state
            state.copy(draft = draft.copy(limit = change(draft.limit)))
        }
    }

    private companion object {
        const val LOCKED_MESSAGE = "Unlock first to change limits (not possible during a focus session)"
    }
}
