package dev.focuslock.limits

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.time.LocalDate
import java.util.concurrent.CopyOnWriteArraySet

interface LimitsStoreContract {
    val limits: Map<String, DailyLimit>

    fun put(pkg: String, limit: DailyLimit)
    fun remove(pkg: String)
    fun clear()

    fun addListener(listener: () -> Unit)
    fun removeListener(listener: () -> Unit)
}

/** Time used today per app. Reading or writing a different day starts from zero. */
interface UsageStoreContract {
    fun usedMs(pkg: String, day: LocalDate): Long
    fun add(pkg: String, day: LocalDate, ms: Long)
}

class LimitsStore(context: Context) : LimitsStoreContract {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("focus_lock_limits", Context.MODE_PRIVATE)

    private val listeners = CopyOnWriteArraySet<() -> Unit>()

    // SharedPreferences only holds listeners weakly, so this field keeps it alive.
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        listeners.forEach { it() }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
    }

    /** A corrupt entry reads as "no time any day": dropping it would quietly lift the limit. */
    override val limits: Map<String, DailyLimit>
        get() = prefs.all.mapValues { (_, raw) -> DailyLimit.decode(raw as? String) ?: DailyLimit.NONE }

    override fun put(pkg: String, limit: DailyLimit) {
        prefs.edit { putString(pkg, limit.encode()) }
    }

    override fun remove(pkg: String) {
        prefs.edit { remove(pkg) }
    }

    override fun clear() {
        prefs.edit { clear() }
    }

    override fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    override fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }
}

class UsageStore(context: Context) : UsageStoreContract {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("focus_lock_usage", Context.MODE_PRIVATE)

    @Synchronized
    override fun usedMs(pkg: String, day: LocalDate): Long {
        if (prefs.getString(KEY_DAY, null) != day.toString()) return 0L
        return prefs.getLong(PREFIX + pkg, 0L)
    }

    @Synchronized
    override fun add(pkg: String, day: LocalDate, ms: Long) {
        if (ms <= 0L) return
        val sameDay = prefs.getString(KEY_DAY, null) == day.toString()
        val used = if (sameDay) prefs.getLong(PREFIX + pkg, 0L) else 0L
        prefs.edit {
            if (!sameDay) {
                clear()
                putString(KEY_DAY, day.toString())
            }
            putLong(PREFIX + pkg, used + ms)
        }
    }

    private companion object {
        const val KEY_DAY = "day"
        const val PREFIX = "used_"
    }
}
