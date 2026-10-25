package dev.focuslock.blocker

sealed interface Decision {
    data object Allow : Decision

    /** A blocked app that still has daily allowance left: allowed, but its time is being counted. */
    data class AllowTimed(val pkg: String, val remainingMs: Long) : Decision

    data class BlockApp(val pkg: String) : Decision
    data class LimitReached(val pkg: String) : Decision
    data class BlockSite(val host: String) : Decision
    data object BlockSettings : Decision
}
