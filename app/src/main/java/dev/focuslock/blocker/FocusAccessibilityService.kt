package dev.focuslock.blocker

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import dev.focuslock.BuildConfig
import dev.focuslock.FocusApp
import dev.focuslock.lock.LockStateStoreContract
import dev.focuslock.pin.SystemClock

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
 */
class FocusAccessibilityService : AccessibilityService() {

    private val decider = BlockDecider(SettingsGuard())
    private val throttle = DecisionThrottle(SystemClock)

    @Volatile
    private var isLocked = false

    private val lockListener: (Boolean) -> Unit = { locked -> isLocked = locked }

    private val store: LockStateStoreContract
        get() = (application as FocusApp).container.lockStateStore

    override fun onServiceConnected() {
        super.onServiceConnected()
        isLocked = store.isLocked
        store.addListener(lockListener)
        debugLog { "connected, isLocked=$isLocked" }
        // After boot (or re-enable) a blocked app may already be in front and send no new event.
        rootInActiveWindow?.packageName?.toString()?.let(::handle)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        debugLog { "event pkg=$pkg type=${event.eventType} class=${event.className}" }
        handle(pkg)
    }

    private fun handle(pkg: String) {
        val screenEvent = ScreenEvent(packageName = pkg, urlBarText = null, screenTexts = emptyList())
        val decision = decider.decide(screenEvent, isLocked)
        if (throttle.shouldFire(decision)) execute(decision)
    }

    /**
     * Home and the Blocked screen go out as one ordered launch, so Blocked always ends up on top.
     * performGlobalAction(HOME) is asynchronous (2 s after boot in the spike) and can land on top of it.
     */
    private fun execute(decision: Decision) {
        val reason = BlockedReason.describe(decision) ?: return
        startActivities(arrayOf(BlockedActivity.homeIntent(), BlockedActivity.intent(this, reason)))
        debugLog { "blocked $decision" }
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        store.removeListener(lockListener)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        store.removeListener(lockListener)
        super.onDestroy()
    }

    private inline fun debugLog(message: () -> String) {
        if (BuildConfig.DEBUG) Log.d(TAG, message())
    }

    private companion object {
        const val TAG = "FocusLock"
    }
}
