package dev.focuslock.blocker

import dev.focuslock.lock.LockStateStoreContract
import dev.focuslock.lock.isBlocking
import dev.focuslock.pin.Clock
import dev.focuslock.pin.SystemClock

enum class EditResult { OK, LOCKED, INVALID, PROTECTED, ALREADY_THERE }

/**
 * The only way to change the block list. Every edit is refused while blocking (locked or in a
 * focus session), so the list can only be changed after the friend's code was entered.
 *
 * @param protectedPackages packages that must never be blocked (launcher, dialer, ...), read at
 *   edit time because the default launcher/dialer can change.
 */
class BlockListEditor(
    private val rulesStore: BlockRulesStoreContract,
    private val lockStore: LockStateStoreContract,
    private val protectedPackages: () -> Set<String>,
    private val clock: Clock = SystemClock,
) {

    val rules: BlockRules get() = rulesStore.rules

    fun addApp(input: String): EditResult {
        if (lockStore.isBlocking(clock.nowMs())) return EditResult.LOCKED
        val pkg = input.trim()
        if (!PACKAGE_NAME.matches(pkg)) return EditResult.INVALID
        if (pkg in alwaysAllowed()) return EditResult.PROTECTED
        val current = rulesStore.rules
        if (pkg in current.apps) return EditResult.ALREADY_THERE
        rulesStore.rules = current.copy(apps = current.apps + pkg)
        return EditResult.OK
    }

    fun removeApp(pkg: String): EditResult {
        if (lockStore.isBlocking(clock.nowMs())) return EditResult.LOCKED
        val current = rulesStore.rules
        rulesStore.rules = current.copy(apps = current.apps - pkg)
        return EditResult.OK
    }

    fun addSite(input: String): EditResult {
        if (lockStore.isBlocking(clock.nowMs())) return EditResult.LOCKED
        val site = normalizeSite(input) ?: return EditResult.INVALID
        val current = rulesStore.rules
        if (site in current.sites) return EditResult.ALREADY_THERE
        rulesStore.rules = current.copy(sites = current.sites + site)
        return EditResult.OK
    }

    fun removeSite(site: String): EditResult {
        if (lockStore.isBlocking(clock.nowMs())) return EditResult.LOCKED
        val current = rulesStore.rules
        rulesStore.rules = current.copy(sites = current.sites - site)
        return EditResult.OK
    }

    private fun alwaysAllowed(): Set<String> =
        BlockList.neverBlock + BlockList.settingsPackages + protectedPackages()

    companion object {
        private val PACKAGE_NAME = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$")

        /** "https://www.TikTok.com/foo" → "tiktok.com". The leading "www." is dropped so the
         *  suffix match also covers "m." and other subdomains. */
        fun normalizeSite(input: String): String? =
            HostMatcher.extractHost(input)?.removePrefix("www.")?.takeIf { it.contains('.') }
    }
}
