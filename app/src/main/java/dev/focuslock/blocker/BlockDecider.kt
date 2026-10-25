package dev.focuslock.blocker

class BlockDecider(private val settingsGuard: SettingsGuard) {

    /** Reading the URL bar costs IPC, so only do it when the answer can matter. */
    fun needsUrlBar(pkg: String, isLocked: Boolean, isOnBreak: Boolean = false, isInFocus: Boolean = false): Boolean =
        appsBlocked(isLocked, isOnBreak, isInFocus) && pkg in BlockList.browsers

    /** Collecting Settings texts walks the view tree, so only do it when the answer can matter. */
    fun needsScreenTexts(pkg: String, isLocked: Boolean, isInFocus: Boolean = false, hasLimits: Boolean = false): Boolean =
        guardOn(isLocked, isInFocus, hasLimits) && pkg in BlockList.settingsPackages

    /**
     * - Block list: enforced while locked, paused by a break.
     * - Daily limits: enforced locked or unlocked, paused by a break.
     * - Focus session: blocks block-list apps, limited apps and sites, ignoring breaks and time left.
     * - Settings guard: on while locked, in focus, or while any limit exists (a break keeps it on),
     *   otherwise the service or the admin could be switched off and protection would not come back.
     *
     * @param remainingMs daily time left for an app: null = no limit, 0 = nothing left today.
     */
    fun decide(
        event: ScreenEvent,
        isLocked: Boolean,
        rules: BlockRules = BlockRules.DEFAULT,
        isOnBreak: Boolean = false,
        isInFocus: Boolean = false,
        remainingMs: (String) -> Long? = { null },
        hasLimits: Boolean = false,
    ): Decision {
        val pkg = event.packageName
        if (pkg in BlockList.neverBlock) return Decision.Allow

        val isSettings = pkg in BlockList.settingsPackages
        val appsBlocked = appsBlocked(isLocked, isOnBreak, isInFocus)
        val limitsOn = isInFocus || !(isLocked && isOnBreak)
        val left = if (isSettings) null else remainingMs(pkg)

        if (!isSettings) {
            if (isInFocus && (left != null || pkg in rules.apps)) return Decision.BlockApp(pkg)
            if (limitsOn && left == 0L) return Decision.LimitReached(pkg)
            if (appsBlocked && left == null && pkg in rules.apps) return Decision.BlockApp(pkg)
        }

        if (appsBlocked && pkg in BlockList.browsers) {
            val host = event.urlBarText?.let(HostMatcher::extractHost)
            if (host != null && HostMatcher.isBlockedHost(host, rules.sites)) return Decision.BlockSite(host)
        }

        if (isSettings && guardOn(isLocked, isInFocus, hasLimits) && settingsGuard.isDangerous(event.screenTexts)) {
            return Decision.BlockSettings
        }

        if (limitsOn && left != null && left > 0L) return Decision.AllowTimed(pkg, left)
        return Decision.Allow
    }

    private fun appsBlocked(isLocked: Boolean, isOnBreak: Boolean, isInFocus: Boolean): Boolean =
        isInFocus || (isLocked && !isOnBreak)

    private fun guardOn(isLocked: Boolean, isInFocus: Boolean, hasLimits: Boolean): Boolean =
        isLocked || isInFocus || hasLimits
}
