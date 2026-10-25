package dev.focuslock.blocker

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.util.concurrent.CopyOnWriteArraySet

interface BlockRulesStoreContract {
    var rules: BlockRules

    fun addListener(listener: (BlockRules) -> Unit)
    fun removeListener(listener: (BlockRules) -> Unit)
}

/** Stores the block list on the device only. Until the user edits it, the defaults apply. */
class BlockRulesStore(context: Context) : BlockRulesStoreContract {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("focus_lock_rules", Context.MODE_PRIVATE)

    private val listeners = CopyOnWriteArraySet<(BlockRules) -> Unit>()

    // Notified from the setter: an OnSharedPreferenceChangeListener is only held weakly and its
    // keep-alive field is stripped by R8, so release builds stopped seeing edits (see LockStateStore).

    override var rules: BlockRules
        get() = BlockRules(
            apps = prefs.getStringSet(KEY_APPS, null)?.toSet() ?: BlockList.apps,
            sites = prefs.getStringSet(KEY_SITES, null)?.toSet() ?: BlockList.sites,
        )
        set(value) {
            val changed = value != rules
            prefs.edit {
                putStringSet(KEY_APPS, value.apps.toSet())
                putStringSet(KEY_SITES, value.sites.toSet())
            }
            if (changed) listeners.forEach { it(value) }
        }

    override fun addListener(listener: (BlockRules) -> Unit) {
        listeners.add(listener)
    }

    override fun removeListener(listener: (BlockRules) -> Unit) {
        listeners.remove(listener)
    }

    private companion object {
        const val KEY_APPS = "apps"
        const val KEY_SITES = "sites"
    }
}
