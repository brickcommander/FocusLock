package dev.focuslock.lock

import android.content.Context
import android.content.SharedPreferences

interface LockStateStoreContract {
    var isLocked: Boolean
}

class LockStateStore(context: Context) : LockStateStoreContract {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("focus_lock_state", Context.MODE_PRIVATE)

    override var isLocked: Boolean
        get() = prefs.getBoolean(KEY_LOCKED, false)
        set(value) { prefs.edit().putBoolean(KEY_LOCKED, value).apply() }

    companion object {
        private const val KEY_LOCKED = "is_locked"
    }
}
