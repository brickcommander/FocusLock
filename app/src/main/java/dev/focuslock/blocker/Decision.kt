package dev.focuslock.blocker

sealed interface Decision {
    data object Allow : Decision
    data class BlockApp(val pkg: String) : Decision
    data class BlockSite(val host: String) : Decision
    data object BlockSettings : Decision
}
