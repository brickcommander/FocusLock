package dev.focuslock.blocker

class BlockDecider(private val settingsGuard: SettingsGuard) {

    /** Reading the URL bar costs IPC, so only do it when the answer can matter. */
    fun needsUrlBar(pkg: String, isLocked: Boolean, isOnBreak: Boolean = false, isInFocus: Boolean = false): Boolean =
        appsBlocked(isLocked, isOnBreak, isInFocus) && pkg in BlockList.browsers

    /** Collecting Settings texts walks the view tree, so only do it when the answer can matter. */
    fun needsScreenTexts(pkg: String, isLocked: Boolean, isInFocus: Boolean = false): Boolean =
        (isLocked || isInFocus) && pkg in BlockList.settingsPackages

    /**
     * A break unblocks apps and websites, but the Settings guard stays on: otherwise the break could
     * be used to switch off the service or the admin, and protection would not come back afterwards.
     * A focus session blocks even when unlocked, and ignores breaks and daily allowances.
     *
     * @param remainingMs daily allowance left for an app: null = none today, 0 = used up.
     */
    fun decide(
        event: ScreenEvent,
        isLocked: Boolean,
        rules: BlockRules = BlockRules.DEFAULT,
        isOnBreak: Boolean = false,
        isInFocus: Boolean = false,
        remainingMs: (String) -> Long? = { null },
    ): Decision {
        if (!isLocked && !isInFocus) return Decision.Allow

        val pkg = event.packageName
        if (pkg in BlockList.neverBlock) return Decision.Allow

        if (appsBlocked(isLocked, isOnBreak, isInFocus)) {
            if (pkg in rules.apps && pkg !in BlockList.settingsPackages) {
                if (isInFocus) return Decision.BlockApp(pkg)
                return when (val left = remainingMs(pkg)) {
                    null -> Decision.BlockApp(pkg)
                    0L -> Decision.LimitReached(pkg)
                    else -> Decision.AllowTimed(pkg, left)
                }
            }

            if (pkg in BlockList.browsers) {
                val host = event.urlBarText?.let(HostMatcher::extractHost)
                if (host != null && HostMatcher.isBlockedHost(host, rules.sites)) return Decision.BlockSite(host)
            }
        }

        if (pkg in BlockList.settingsPackages && settingsGuard.isDangerous(event.screenTexts)) {
            return Decision.BlockSettings
        }

        return Decision.Allow
    }

    private fun appsBlocked(isLocked: Boolean, isOnBreak: Boolean, isInFocus: Boolean): Boolean =
        isInFocus || (isLocked && !isOnBreak)
}
