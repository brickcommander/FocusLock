package dev.focuslock.lock

import android.content.Context
import android.content.SharedPreferences
import java.util.concurrent.CopyOnWriteArraySet

interface LockStateStoreContract {
    var isLocked: Boolean
    var lastAppliedEpochMs: Long

    fun addListener(listener: (Boolean) -> Unit)
    fun removeListener(listener: (Boolean) -> Unit)
}

class LockStateStore(context: Context) : LockStateStoreContract {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("focus_lock_state", Context.MODE_PRIVATE)

    private val listeners = CopyOnWriteArraySet<(Boolean) -> Unit>()

    // SharedPreferences only holds listeners weakly, so this field keeps it alive.
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        if (key == KEY_LOCKED) {
            val locked = prefs.getBoolean(KEY_LOCKED, false)
            listeners.forEach { it(locked) }
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
    }

    override var isLocked: Boolean
        get() = prefs.getBoolean(KEY_LOCKED, false)
        set(value) { prefs.edit().putBoolean(KEY_LOCKED, value).apply() }

    override var lastAppliedEpochMs: Long
        get() = prefs.getLong(KEY_LAST_APPLIED, 0L)
        set(value) { prefs.edit().putLong(KEY_LAST_APPLIED, value).apply() }

    override fun addListener(listener: (Boolean) -> Unit) {
        listeners.add(listener)
    }

    override fun removeListener(listener: (Boolean) -> Unit) {
        listeners.remove(listener)
    }

    companion object {
        private const val KEY_LOCKED = "is_locked"
        private const val KEY_LAST_APPLIED = "last_applied_epoch_ms"
    }
}
