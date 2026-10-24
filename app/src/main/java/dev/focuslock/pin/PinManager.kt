package dev.focuslock.pin

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.Base64

sealed class VerifyResult {
    data object Ok : VerifyResult()
    data class Wrong(val remaining: Int) : VerifyResult()
    data class LockedOut(val untilMs: Long) : VerifyResult()
    data object NotSet : VerifyResult()
}

class WrongPinException(val result: VerifyResult) : Exception("Code not accepted: $result")

class PinManager(
    private val store: PinStoreContract,
    private val hasher: PinHasher,
    private val clock: Clock,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {

    private val mutex = Mutex()

    fun isSet(): Boolean = store.hashB64 != null && store.saltB64 != null

    suspend fun setPin(pin: String): Result<Unit> = withContext(dispatcher) {
        mutex.withLock { setPinLocked(pin) }
    }

    suspend fun verify(pin: String): VerifyResult = withContext(dispatcher) {
        mutex.withLock { verifyLocked(pin) }
    }

    suspend fun changePin(old: String, new: String): Result<Unit> = withContext(dispatcher) {
        mutex.withLock { changePinLocked(old, new) }
    }

    private fun setPinLocked(pin: String): Result<Unit> {
        if (!isValidFormat(pin)) return Result.failure(IllegalArgumentException(INVALID_FORMAT))
        if (isSet()) return Result.failure(IllegalStateException("A code is already set"))
        saveNewPin(pin)
        return Result.success(Unit)
    }

    private fun changePinLocked(old: String, new: String): Result<Unit> {
        if (!isValidFormat(new)) return Result.failure(IllegalArgumentException(INVALID_FORMAT))
        val result = verifyLocked(old)
        if (result != VerifyResult.Ok) return Result.failure(WrongPinException(result))
        saveNewPin(new)
        return Result.success(Unit)
    }

    private fun verifyLocked(pin: String): VerifyResult {
        if (!isSet()) return VerifyResult.NotSet

        val now = clock.nowMs()
        val lockoutUntil = store.lockoutUntilEpochMs
        if (now < lockoutUntil) return VerifyResult.LockedOut(lockoutUntil)

        if (matchesStored(pin)) {
            store.failedAttempts = 0
            store.lockoutUntilEpochMs = 0L
            return VerifyResult.Ok
        }

        val failed = store.failedAttempts + 1
        if (failed >= MAX_ATTEMPTS) {
            val until = now + LOCKOUT_MS
            store.lockoutUntilEpochMs = until
            store.failedAttempts = 0
            return VerifyResult.LockedOut(until)
        }
        store.failedAttempts = failed
        return VerifyResult.Wrong(remaining = MAX_ATTEMPTS - failed)
    }

    private fun matchesStored(pin: String): Boolean {
        val salt = Base64.getDecoder().decode(store.saltB64)
        val expected = Base64.getDecoder().decode(store.hashB64)
        val actual = hashPin(pin, salt, store.iterations)
        return MessageDigest.isEqual(expected, actual)
    }

    private fun saveNewPin(pin: String) {
        val salt = hasher.newSalt()
        val hash = hashPin(pin, salt, hasher.iterations)
        store.saveCredential(
            saltB64 = Base64.getEncoder().encodeToString(salt),
            hashB64 = Base64.getEncoder().encodeToString(hash),
            iterations = hasher.iterations,
        )
    }

    private fun hashPin(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val chars = pin.toCharArray()
        try {
            return hasher.hash(chars, salt, iterations)
        } finally {
            chars.fill('\u0000')
        }
    }

    companion object {
        const val MIN_LENGTH = 6
        const val MAX_LENGTH = 12
        const val MAX_ATTEMPTS = 5
        const val LOCKOUT_MS = 60 * 60 * 1000L
        private const val INVALID_FORMAT = "Code must be 6–12 digits"
        private val PIN_FORMAT = Regex("^[0-9]{$MIN_LENGTH,$MAX_LENGTH}$")

        fun isValidFormat(pin: String): Boolean = PIN_FORMAT.matches(pin)
    }
}
