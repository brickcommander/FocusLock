package dev.focuslock.pin

class InMemoryPinStore : PinStoreContract {
    override var saltB64: String? = null
        private set
    override var hashB64: String? = null
        private set
    override var iterations: Int = 0
        private set
    override var failedAttempts: Int = 0
    override var lockoutUntilEpochMs: Long = 0L

    override fun saveCredential(saltB64: String, hashB64: String, iterations: Int) {
        this.saltB64 = saltB64
        this.hashB64 = hashB64
        this.iterations = iterations
        failedAttempts = 0
        lockoutUntilEpochMs = 0L
    }

    /** Every persisted value as text, for asserting the plain code never lands in storage. */
    fun allStoredValues(): List<String> = listOf(
        saltB64.orEmpty(),
        hashB64.orEmpty(),
        iterations.toString(),
        failedAttempts.toString(),
        lockoutUntilEpochMs.toString(),
    )
}
