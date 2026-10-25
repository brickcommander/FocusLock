package dev.focuslock.lock

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.util.concurrent.CopyOnWriteArraySet

interface LockStateStoreContract {
    var isLocked: Boolean
    var lastAppliedEpochMs: Long
    var breakWindow: BreakWindow?
    var focusSession: FocusSession?

    fun addListener(listener: (Boolean) -> Unit)
    fun removeListener(listener: (Boolean) -> Unit)
    fun addBreakListener(listener: (BreakWindow?) -> Unit)
    fun removeBreakListener(listener: (BreakWindow?) -> Unit)
    fun addFocusListener(listener: (FocusSession?) -> Unit)
    fun removeFocusListener(listener: (FocusSession?) -> Unit)
}

/** Blocking applies while locked or during a focus session (which works even when unlocked). */
fun LockStateStoreContract.isBlocking(nowMs: Long): Boolean =
    isLocked || focusSession?.isActive(nowMs) == true

class LockStateStore(context: Context) : LockStateStoreContract {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("focus_lock_state", Context.MODE_PRIVATE)

    private val listeners = CopyOnWriteArraySet<(Boolean) -> Unit>()
    private val breakListeners = CopyOnWriteArraySet<(BreakWindow?) -> Unit>()
    private val focusListeners = CopyOnWriteArraySet<(FocusSession?) -> Unit>()

    // SharedPreferences only holds listeners weakly, so this field keeps it alive.
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        when (key) {
            KEY_LOCKED -> {
                val locked = prefs.getBoolean(KEY_LOCKED, false)
                listeners.forEach { it(locked) }
            }
            KEY_BREAK_START, KEY_BREAK_UNTIL -> {
                val window = breakWindow
                breakListeners.forEach { it(window) }
            }
            KEY_FOCUS_START, KEY_FOCUS_UNTIL -> {
                val session = focusSession
                focusListeners.forEach { it(session) }
            }
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
    }

    override var isLocked: Boolean
        get() = prefs.getBoolean(KEY_LOCKED, false)
        set(value) { prefs.edit { putBoolean(KEY_LOCKED, value) } }

    override var lastAppliedEpochMs: Long
        get() = prefs.getLong(KEY_LAST_APPLIED, 0L)
        set(value) { prefs.edit { putLong(KEY_LAST_APPLIED, value) } }

    override var breakWindow: BreakWindow?
        get() {
            val until = prefs.getLong(KEY_BREAK_UNTIL, 0L)
            return if (until > 0L) BreakWindow(prefs.getLong(KEY_BREAK_START, 0L), until) else null
        }
        set(value) {
            prefs.edit {
                if (value == null) {
                    remove(KEY_BREAK_START)
                    remove(KEY_BREAK_UNTIL)
                } else {
                    putLong(KEY_BREAK_START, value.startMs)
                    putLong(KEY_BREAK_UNTIL, value.untilMs)
                }
            }
        }

    override var focusSession: FocusSession?
        get() {
            val until = prefs.getLong(KEY_FOCUS_UNTIL, 0L)
            return if (until > 0L) FocusSession(prefs.getLong(KEY_FOCUS_START, 0L), until) else null
        }
        set(value) {
            prefs.edit {
                if (value == null) {
                    remove(KEY_FOCUS_START)
                    remove(KEY_FOCUS_UNTIL)
                } else {
                    putLong(KEY_FOCUS_START, value.startMs)
                    putLong(KEY_FOCUS_UNTIL, value.untilMs)
                }
            }
        }

    override fun addListener(listener: (Boolean) -> Unit) {
        listeners.add(listener)
    }

    override fun removeListener(listener: (Boolean) -> Unit) {
        listeners.remove(listener)
    }

    override fun addBreakListener(listener: (BreakWindow?) -> Unit) {
        breakListeners.add(listener)
    }

    override fun removeBreakListener(listener: (BreakWindow?) -> Unit) {
        breakListeners.remove(listener)
    }

    override fun addFocusListener(listener: (FocusSession?) -> Unit) {
        focusListeners.add(listener)
    }

    override fun removeFocusListener(listener: (FocusSession?) -> Unit) {
        focusListeners.remove(listener)
    }

    companion object {
        private const val KEY_LOCKED = "is_locked"
        private const val KEY_LAST_APPLIED = "last_applied_epoch_ms"
        private const val KEY_BREAK_START = "break_start_epoch_ms"
        private const val KEY_BREAK_UNTIL = "break_until_epoch_ms"
        private const val KEY_FOCUS_START = "focus_start_epoch_ms"
        private const val KEY_FOCUS_UNTIL = "focus_until_epoch_ms"
    }
}
