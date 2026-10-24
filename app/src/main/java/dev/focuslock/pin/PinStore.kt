package dev.focuslock.pin

import android.content.Context
import android.content.SharedPreferences

interface PinStoreContract {
    val saltB64: String?
    val hashB64: String?
    val iterations: Int
    var failedAttempts: Int
    var lockoutUntilEpochMs: Long

    fun saveCredential(saltB64: String, hashB64: String, iterations: Int)
}

/** Writes use commit(): a lost failed-attempt write would let a crash reset the lockout counter. */
class PinStore(context: Context) : PinStoreContract {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("focus_lock_pin", Context.MODE_PRIVATE)

    override val saltB64: String?
        get() = prefs.getString(KEY_SALT, null)

    override val hashB64: String?
        get() = prefs.getString(KEY_HASH, null)

    override val iterations: Int
        get() = prefs.getInt(KEY_ITERATIONS, 0)

    override var failedAttempts: Int
        get() = prefs.getInt(KEY_FAILED, 0)
        set(value) { prefs.edit().putInt(KEY_FAILED, value).commit() }

    override var lockoutUntilEpochMs: Long
        get() = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        set(value) { prefs.edit().putLong(KEY_LOCKOUT_UNTIL, value).commit() }

    override fun saveCredential(saltB64: String, hashB64: String, iterations: Int) {
        prefs.edit()
            .putString(KEY_SALT, saltB64)
            .putString(KEY_HASH, hashB64)
            .putInt(KEY_ITERATIONS, iterations)
            .putInt(KEY_FAILED, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)
            .commit()
    }

    companion object {
        private const val KEY_SALT = "salt"
        private const val KEY_HASH = "hash"
        private const val KEY_ITERATIONS = "iterations"
        private const val KEY_FAILED = "failed_attempts"
        private const val KEY_LOCKOUT_UNTIL = "lockout_until_epoch_ms"
    }
}
