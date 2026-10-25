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

    // SharedPreferences only holds listeners weakly, so this field keeps it alive.
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == KEY_APPS || key == KEY_SITES) {
            val current = rules
            listeners.forEach { it(current) }
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
    }

    override var rules: BlockRules
        get() = BlockRules(
            apps = prefs.getStringSet(KEY_APPS, null)?.toSet() ?: BlockList.apps,
            sites = prefs.getStringSet(KEY_SITES, null)?.toSet() ?: BlockList.sites,
        )
        set(value) {
            prefs.edit {
                putStringSet(KEY_APPS, value.apps.toSet())
                putStringSet(KEY_SITES, value.sites.toSet())
            }
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
