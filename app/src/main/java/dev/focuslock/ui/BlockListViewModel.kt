package dev.focuslock.ui

import androidx.lifecycle.ViewModel
import dev.focuslock.apps.AppEntry
import dev.focuslock.apps.AppSource
import dev.focuslock.blocker.BlockList
import dev.focuslock.blocker.BlockListEditor
import dev.focuslock.blocker.EditResult
import dev.focuslock.lock.LockStateStoreContract
import dev.focuslock.lock.isBlocking
import dev.focuslock.pin.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class BlockListUiState(
    val isLocked: Boolean = false,
    val apps: List<AppEntry> = emptyList(),
    val sites: List<String> = emptyList(),
    val showPicker: Boolean = false,
    val pickerApps: List<AppEntry> = emptyList(),
    val message: String? = null,
)

class BlockListViewModel(
    private val editor: BlockListEditor,
    private val lockStore: LockStateStoreContract,
    private val appSource: AppSource,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BlockListUiState())
    val uiState: StateFlow<BlockListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val rules = editor.rules
        _uiState.update {
            it.copy(
                isLocked = lockStore.isBlocking(SystemClock.nowMs()),
                apps = rules.apps
                    .map { pkg -> AppEntry(pkg, appSource.label(pkg) ?: pkg) }
                    .sortedBy { app -> app.label.lowercase() },
                sites = rules.sites.sorted(),
            )
        }
    }

    fun openPicker() {
        val blocked = editor.rules.apps
        val hidden = blocked + appSource.protectedPackages() + BlockList.neverBlock + BlockList.settingsPackages
        val choices = appSource.launchableApps().filter { it.packageName !in hidden }
        _uiState.update { it.copy(showPicker = true, pickerApps = choices, message = null) }
    }

    fun closePicker() {
        _uiState.update { it.copy(showPicker = false) }
    }

    fun addApp(packageName: String) {
        val result = editor.addApp(packageName)
        _uiState.update { it.copy(showPicker = false, message = result.message(Kind.APP)) }
        refresh()
    }

    fun removeApp(packageName: String) {
        _uiState.update { it.copy(message = editor.removeApp(packageName).message(Kind.APP)) }
        refresh()
    }

    fun addSite(input: String) {
        _uiState.update { it.copy(message = editor.addSite(input).message(Kind.SITE)) }
        refresh()
    }

    fun removeSite(site: String) {
        _uiState.update { it.copy(message = editor.removeSite(site).message(Kind.SITE)) }
        refresh()
    }

    private enum class Kind { APP, SITE }

    private fun EditResult.message(kind: Kind): String? = when (this) {
        EditResult.OK -> null
        EditResult.LOCKED -> "Unlock first to change the block list (not possible during a focus session)"
        EditResult.INVALID -> when (kind) {
            Kind.APP -> "That isn't a package name (example: com.zhiliaoapp.musically)"
            Kind.SITE -> "That isn't a website (example: tiktok.com)"
        }
        EditResult.PROTECTED -> "This app can't be blocked (it's needed to use the phone)"
        EditResult.ALREADY_THERE -> "Already on the list"
    }
}
