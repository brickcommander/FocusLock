package dev.focuslock.lock

data class LockReport(
    val applied: List<String>,
    val skippedNotInstalled: List<String>,
    val failed: List<String>,
)
