package dev.focuslock.lock

import android.content.Context
import android.content.SharedPreferences

interface LockStateStoreContract {
    var isLocked: Boolean
    var lastAppliedEpochMs: Long
}

class LockStateStore(context: Context) : LockStateStoreContract {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("focus_lock_state", Context.MODE_PRIVATE)

    override var isLocked: Boolean
        get() = prefs.getBoolean(KEY_LOCKED, false)
        set(value) { prefs.edit().putBoolean(KEY_LOCKED, value).apply() }

    override var lastAppliedEpochMs: Long
        get() = prefs.getLong(KEY_LAST_APPLIED, 0L)
        set(value) { prefs.edit().putLong(KEY_LAST_APPLIED, value).apply() }

    companion object {
        private const val KEY_LOCKED = "is_locked"
        private const val KEY_LAST_APPLIED = "last_applied_epoch_ms"
    }
}
