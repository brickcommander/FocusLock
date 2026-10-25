package dev.focuslock.blocker

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import dev.focuslock.AppContainer
import dev.focuslock.BuildConfig
import dev.focuslock.FocusApp
import dev.focuslock.lock.BreakWindow
import dev.focuslock.pin.SystemClock
import android.os.SystemClock as AndroidClock

/**
 * Thin glue: AccessibilityEvent → ScreenEvent → BlockDecider → execute(Decision). No decisions here.
 *
 * Spike findings (emulator "FocusLock-Test", API 37, 2026-10-25):
 * 1. Events arrive only for packages in android:packageNames. Opening Settings, Phone and the
 *    launcher produced no events; the dummy target produced type 32 (window state) and 2048
 *    (content) events.
 * 2. startActivity from the service works while another app is in front: ActivityTaskManager logs
 *    "BAL_ALLOW_TOKEN", result 0 (bound accessibility service exemption). Also works ~25 s after boot.
 * 3. performGlobalAction(GLOBAL_ACTION_HOME) returns true, BUT it is asynchronous: the launcher
 *    start landed after BlockedActivity (2 s later right after boot) and covered it. So execute()
 *    launches Home + BlockedActivity as one ordered startActivities() call instead.
 * 4. After boot the service connects ~15–30 s after boot_completed. A blocked app already in front
 *    sends no new event, so onServiceConnected checks the active window once.
 * 5. Test tooling: while this service is enabled, `uiautomator dump` often fails with
 *    "null root node"; toggling the service off and on via `settings put` makes the next dump work.
 *    Each dump also re-binds this service (UiAutomation suppresses other services while connected).
 * 6. This service sees LESS than `uiautomator dump`: Settings hides sensitive controls (admin
 *    "Deactivate" buttons, "Clear storage", "Automatic date and time") from non-tool services.
 *    Record Settings texts from the debug log "settings texts=[...]", not from uiautomator.
 * 7. A Settings screen opened from the notification shade arrives while the shade still owns the
 *    active window, so rootInActiveWindow is the shade, and the screen under the shade is often
 *    missing from `windows` too. Static screens such as the device-admin page send no event when
 *    the shade closes. So texts are read from the event's own window, and while the guard is on,
 *    Settings is re-read every SETTINGS_WATCH_MS while it is visible, and for SETTINGS_HIDDEN_MS
 *    after it was last seen (covered by the shade).
 */
class FocusAccessibilityService : AccessibilityService() {

    private val decider = BlockDecider(SettingsGuard())
    private val throttle = DecisionThrottle(SystemClock)
    private val scanThrottle = ScanThrottle(SystemClock)
    private val handler = Handler(Looper.getMainLooper())

    @Volatile
    private var isLocked = false

    @Volatile
    private var rules = BlockRules.DEFAULT

    @Volatile
    private var breakWindow: BreakWindow? = null

    private val lockListener: (Boolean) -> Unit = { locked -> isLocked = locked }

    private val breakListener: (BreakWindow?) -> Unit = { window -> breakWindow = window }

    @Volatile
    private var limitedPackages: Set<String> = emptySet()

    /** Fast in-process filter: never rely solely on serviceInfo.packageNames (Samsung ignores it). */
    @Volatile
    private var watchedSet: Set<String> = emptySet()

    private val rulesListener: (BlockRules) -> Unit = { newRules ->
        rules = newRules
        updatePackageFilter()
    }

    private val limitsListener: () -> Unit = {
        limitedPackages = container.dailyLimits.limits.keys
        updatePackageFilter()
    }

    private val container: AppContainer
        get() = (application as FocusApp).container

    // ── Settings watch ──
    private var watchedSettingsPkg: String? = null
    private var settingsLastSeenMs = 0L
    private val settingsWatch = Runnable { onSettingsWatch() }

    // ── Daily-allowance timer ──
    private var timedPkg: String? = null
    private var lastTickElapsedMs = 0L
    private val usageTick = Runnable { onUsageTick() }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val store = container.lockStateStore
        isLocked = store.isLocked
        store.addListener(lockListener)
        breakWindow = store.breakWindow
        store.addBreakListener(breakListener)
        rules = container.blockRulesStore.rules
        container.blockRulesStore.addListener(rulesListener)
        limitedPackages = container.dailyLimits.limits.keys
        container.limitsStore.addListener(limitsListener)
        updatePackageFilter()
        debugLog { "connected, isLocked=$isLocked" }
        // After boot (or re-enable) a blocked app may already be in front and send no new event.
        safely { foregroundPackage()?.let { handle(it) } }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        if (pkg !in watchedSet) return // fast in-process filter (Samsung ignores setServiceInfo)
        val isWindowStateChange = event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        debugLog { "event pkg=$pkg type=${event.eventType} class=${event.className}" }
        safely { handle(pkg, isWindowStateChange, eventRoot = { eventWindowRoot(event, pkg) }) }
    }

    private fun handle(
        pkg: String,
        isWindowStateChange: Boolean = true,
        eventRoot: () -> AccessibilityNodeInfo? = { null },
    ) {
        val locked = isLocked
        val inFocus = container.lockManager.activeFocusSession() != null
        val onBreak = !inFocus && breakWindow?.isActive(SystemClock.nowMs()) == true
        val hasLimits = limitedPackages.isNotEmpty()
        val root by lazy { rootFor(pkg) ?: eventRoot() }
        val urlBarText =
            if (decider.needsUrlBar(pkg, locked, onBreak, inFocus)) BrowserUrlReader.read(root, pkg) else null
        val needsTexts = decider.needsScreenTexts(pkg, locked, inFocus, hasLimits)
        val screenTexts =
            if (needsTexts && scanThrottle.shouldScan(isWindowStateChange)) ScreenTextReader.read(root) else emptyList()
        if (screenTexts.isNotEmpty()) debugLog { "settings texts=$screenTexts" }

        val screenEvent = ScreenEvent(packageName = pkg, urlBarText = urlBarText, screenTexts = screenTexts)
        val decision =
            decider.decide(screenEvent, locked, rules, onBreak, inFocus, container.dailyLimits::remainingMs, hasLimits)

        if (decision is Decision.AllowTimed) track(decision.pkg, decision.remainingMs)
        else if (pkg == timedPkg) stopTracking()

        if (needsTexts && decision == Decision.Allow) watchSettings(pkg, seenNow = root != null)
        else if (pkg == watchedSettingsPkg) stopSettingsWatch()
        if (throttle.shouldFire(decision)) execute(decision)
    }

    /**
     * Home and the Blocked screen go out as one ordered launch, so Blocked always ends up on top.
     * performGlobalAction(HOME) is asynchronous (2 s after boot in the spike) and can land on top of it.
     */
    private fun execute(decision: Decision) {
        // After a site block is dismissed, give the user time to close/switch the tab.
        if (decision is Decision.BlockSite && System.currentTimeMillis() < BlockedActivity.gracePeriodUntilMs) return

        val reason = BlockedReason.describe(decision, container.installedApps::label) ?: return
        if (decision is Decision.BlockSite) {
            // Site block: just the Blocked screen (no Home), so pressing OK returns to the browser.
            startActivity(BlockedActivity.intent(this, reason, isSiteBlock = true))
        } else {
            startActivities(arrayOf(BlockedActivity.homeIntent(), BlockedActivity.intent(this, reason)))
        }
        debugLog { "blocked $decision" }
    }

    /** The root of the window showing [pkg]: the active window if it matches, else any app window. */
    private fun rootFor(pkg: String): AccessibilityNodeInfo? {
        rootInActiveWindow?.takeIf { it.packageName?.toString() == pkg }?.let { return it }
        return windows
            .asSequence()
            .filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
            .mapNotNull { it.root }
            .firstOrNull { it.packageName?.toString() == pkg }
    }

    /** The window the event came from; can be found even while the shade covers it. */
    private fun eventWindowRoot(event: AccessibilityEvent, pkg: String): AccessibilityNodeInfo? =
        event.source?.window?.root?.takeIf { it.packageName?.toString() == pkg }

    private fun foregroundPackage(): String? = rootInActiveWindow?.packageName?.toString()

    // ── Settings watch ──

    private fun watchSettings(pkg: String, seenNow: Boolean) {
        val now = AndroidClock.elapsedRealtime()
        if (watchedSettingsPkg != pkg) settingsLastSeenMs = now
        watchedSettingsPkg = pkg
        if (seenNow) settingsLastSeenMs = now
        if (now - settingsLastSeenMs > SETTINGS_HIDDEN_MS) {
            stopSettingsWatch()
            return
        }
        if (!handler.hasCallbacks(settingsWatch)) handler.postDelayed(settingsWatch, SETTINGS_WATCH_MS)
    }

    private fun onSettingsWatch() = safely {
        val pkg = watchedSettingsPkg ?: return@safely
        handle(pkg, isWindowStateChange = true)
    }

    private fun stopSettingsWatch() {
        watchedSettingsPkg = null
        handler.removeCallbacks(settingsWatch)
    }

    // ── Daily-allowance timer ──

    /** Counts foreground time of an app on its daily allowance; the tick re-decides, so it blocks on time. */
    private fun track(pkg: String, remainingMs: Long) {
        val now = AndroidClock.elapsedRealtime()
        if (timedPkg != pkg) {
            // Another timed app was in front until this event.
            timedPkg?.let { previous -> container.dailyLimits.addUsage(previous, countedSince(now)) }
            timedPkg = pkg
            lastTickElapsedMs = now
            handler.removeCallbacks(usageTick)
        }
        // Frequent content events must not keep pushing the tick back.
        if (!handler.hasCallbacks(usageTick)) {
            handler.postDelayed(usageTick, remainingMs.coerceIn(MIN_TICK_MS, TICK_MS))
        }
    }

    private fun onUsageTick() = safely {
        val pkg = timedPkg ?: return@safely
        val now = AndroidClock.elapsedRealtime()
        val counted = countedSince(now)
        lastTickElapsedMs = now
        if (!isInteractive() || foregroundPackage() != pkg) {
            stopTracking()
            return@safely
        }
        container.dailyLimits.addUsage(pkg, counted)
        handle(pkg, isWindowStateChange = false)
    }

    private fun countedSince(nowElapsedMs: Long): Long =
        (nowElapsedMs - lastTickElapsedMs).coerceIn(0L, MAX_COUNTED_GAP_MS)

    private fun stopTracking() {
        timedPkg = null
        handler.removeCallbacks(usageTick)
    }

    private fun isInteractive(): Boolean = getSystemService(PowerManager::class.java)?.isInteractive != false

    /**
     * Updates both the in-process [watchedSet] (authoritative) and
     * [serviceInfo].packagesNames (best-effort; Samsung One UI may ignore it).
     * */
    private fun updatePackageFilter() = safely {
        val pkgs = BlockList.watchedPackages(rules, limitedPackages)
        watchedSet = pkgs
        val info = serviceInfo ?: return@safely
        info.packageNames = pkgs.toTypedArray()
        serviceInfo = info
        debugLog { "watching ${pkgs.size} packages" }
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        cleanUp()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        cleanUp()
        super.onDestroy()
    }

    private fun cleanUp() {
        handler.removeCallbacksAndMessages(null)
        timedPkg = null
        container.lockStateStore.removeListener(lockListener)
        container.lockStateStore.removeBreakListener(breakListener)
        container.blockRulesStore.removeListener(rulesListener)
        container.limitsStore.removeListener(limitsListener)
    }

    /**
     * A crash here takes the whole process down, and Android may then stop the service, which
     * would silently switch off all blocking. Window and node calls can throw on some ROMs.
     */
    private inline fun safely(block: () -> Unit) {
        try {
            block()
        } catch (e: RuntimeException) {
            if (BuildConfig.DEBUG) Log.w(TAG, "ignored", e)
        }
    }

    private inline fun debugLog(message: () -> String) {
        if (BuildConfig.DEBUG) Log.d(TAG, message())
    }

    private companion object {
        const val TAG = "FocusLock"
        const val SETTINGS_WATCH_MS = 500L
        const val SETTINGS_HIDDEN_MS = 15_000L
        const val TICK_MS = 5_000L
        const val MIN_TICK_MS = 1_000L
        const val MAX_COUNTED_GAP_MS = 2 * TICK_MS
    }
}
