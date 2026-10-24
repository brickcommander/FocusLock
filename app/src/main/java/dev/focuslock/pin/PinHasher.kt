package dev.focuslock.pin

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PinHasher(val iterations: Int = DEFAULT_ITERATIONS) {

    fun hash(pin: CharArray, salt: ByteArray, iterations: Int = this.iterations): ByteArray {
        val spec = PBEKeySpec(pin, salt, iterations, KEY_LENGTH_BITS)
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    fun newSalt(): ByteArray = ByteArray(SALT_LENGTH_BYTES).also { random.nextBytes(it) }

    companion object {
        const val DEFAULT_ITERATIONS = 120_000
        const val SALT_LENGTH_BYTES = 16
        private const val KEY_LENGTH_BITS = 256
        private const val ALGORITHM = "PBKDF2WithHmacSHA256"
        private val random = SecureRandom()
    }
}
