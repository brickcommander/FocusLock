# FocusLock v2 — Coding & Testing Plan (No Device Owner)

> **Audience:** AI coding/testing agents.
> **Owner:** one developer, personal use, Samsung Galaxy S25 Ultra.
> **Scope:** coding + testing only. Production hardening comes later.
> **Replaces:** `FocusLock-Plan.md` (v1, Device Owner). v1 is kept for reference only — do not follow it.

---

## 0. Read this first (rules for every agent)

### Why v2 exists
- v1 needed **Device Owner** → setup required removing **every account** + `adb`. Too painful and risky.
- v2 uses **no Device Owner**. Setup = install + flip 2 toggles + friend sets the code. ~1 minute.
- Trade-off (accepted): blocking is **"hard enough to stop weak-moment me"**, not tamper-proof.

### What the app does
- **Lock** button →
  - **Apps:** opening Instagram, Instagram Lite, YouTube, Reddit (and non-Chrome browsers) → instantly sent Home + "Blocked" screen.
  - **Websites:** in **Chrome**, visiting youtube.com / reddit.com / instagram.com (any subdomain) → sent Home + "Blocked" screen.
  - **Self-protection:** FocusLock is a **Device Admin** (can't uninstall / force stop directly), and the Settings screens that could disable FocusLock are blocked while locked.
- **Unlock** requires a **code set by a friend** (stored only as a hash). Reused from v1.
- **How:** one `AccessibilityService` that only receives events from a short list of packages → ~0% battery.

### How it works (one picture)
```
User opens Instagram
   │
   ▼
Android → FocusAccessibilityService.onAccessibilityEvent(pkg = com.instagram.android)
   │
   ▼
BlockDecider (pure Kotlin)  ── isLocked? ── no ──► do nothing
   │ yes
   ▼
Decision.BlockApp
   │
   ▼
performGlobalAction(HOME) + start BlockedActivity  ("🔒 Blocked by FocusLock")
```

### 🚫 Hard rules (never break these)
1. **No Device Owner / Profile Owner code.** No `dpm set-device-owner`, no `setPackagesSuspended`, no `setApplicationRestrictions`, no user restrictions.
2. **Exactly one** `AccessibilityService` (`FocusAccessibilityService`). **No** `VpnService`, foreground services, `BOOT_COMPLETED` receivers, WorkManager, alarms, or other background work.
3. **Do NOT** add the `INTERNET` permission or any network library.
4. **Privacy:** the service may only read (a) the event's package name and (b) Chrome's URL-bar text and (c) Settings screen texts for the guard. **Never store or log** URLs/texts in release builds. Debug logs only behind `BuildConfig.DEBUG`.
5. **Never block:** FocusLock itself (`dev.focuslock`), any launcher, dialer, Play Store, or Settings screens other than the specific guarded ones (Step 6). **Unlock must always stay reachable.**
6. **Thin Android, fat Kotlin:** all decisions live in **pure Kotlin** (`BlockDecider`, `HostMatcher`, `SettingsGuard`). The service only converts `AccessibilityEvent` → plain data and executes a `Decision`. No decision logic inside the service.
7. **Event handler must be fast:** no disk I/O, no hashing, no allocation-heavy work per event. Lock state is cached in memory (SharedPreferences listener).
8. Keep `android:testOnly="true"` in the debug manifest (lets `adb shell dpm remove-active-admin` work as an escape hatch).
9. Keep the **debug-only "DEV: Force unlock"** button (no PIN) as a safety net.
10. Test on the **emulator first**. Real phone only in Step 8.
11. Finish a step's **acceptance criteria** before starting the next step. **One commit per step.**

### Definition of "build is green"
```bash
./gradlew assembleDebug testDebugUnitTest lintDebug
```
All three must pass at the end of every step.

---

## 1. Tech stack

| Item | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 (main screen) |
| Build | Gradle Kotlin DSL + version catalog (`libs.versions.toml`) — unchanged |
| `minSdk` / `targetSdk` / `compileSdk` | **30 / 37 / 37** — unchanged |
| DI | Manual (`AppContainer`). **No Hilt/Koin.** |
| Storage | `SharedPreferences` (app-private). No Room, no DataStore. |
| Async | Kotlin coroutines (`Dispatchers.Default` for hashing) |
| Unit tests | JUnit4 + `kotlinx-coroutines-test`. **No Robolectric.** |
| Signing | Default debug keystore |

**Application ID:** `dev.focuslock`
**Admin receiver:** `dev.focuslock.admin.FocusAdminReceiver` (reused)
**Accessibility service:** `dev.focuslock.blocker.FocusAccessibilityService` (new)

---

## 2. Final project structure

```
FocusLock/
├── app/src/
│   ├── main/
│   │   ├── AndroidManifest.xml
│   │   ├── res/xml/device_admin.xml                 # reused
│   │   ├── res/xml/accessibility_service.xml        # NEW
│   │   └── java/dev/focuslock/
│   │       ├── FocusApp.kt / AppContainer.kt / MainActivity.kt
│   │       ├── admin/FocusAdminReceiver.kt          # reused + onDisableRequested
│   │       ├── blocker/                             # NEW
│   │       │   ├── BlockList.kt                     # hardcoded apps, sites, guarded packages
│   │       │   ├── HostMatcher.kt                   # pure: URL-bar text → host → blocked?
│   │       │   ├── ScreenEvent.kt                   # pure data from an AccessibilityEvent
│   │       │   ├── Decision.kt                      # Allow | BlockApp | BlockSite | BlockSettings
│   │       │   ├── BlockDecider.kt                  # pure: (event, isLocked) → Decision
│   │       │   ├── SettingsGuard.kt                 # pure: Settings screen texts → dangerous?
│   │       │   ├── DecisionThrottle.kt              # pure: don't fire same block twice in 1 s
│   │       │   ├── FocusAccessibilityService.kt     # thin Android glue
│   │       │   └── BlockedActivity.kt               # "🔒 Blocked" screen
│   │       ├── setup/                               # NEW
│   │       │   ├── SetupChecker.kt                  # interface
│   │       │   ├── AndroidSetupChecker.kt           # real impl
│   │       │   └── SetupStatus.kt
│   │       ├── lock/   LockManager.kt, LockStateStore.kt, UnlockResult.kt   # simplified
│   │       ├── pin/    (unchanged from v1)
│   │       └── ui/     MainViewModel.kt, MainScreen.kt, PinDialog.kt, SetupChecklist.kt
│   ├── debug/
│   │   ├── AndroidManifest.xml                      # testOnly="true"
│   │   └── res/xml/accessibility_service.xml        # same + dummytarget package
│   └── test/java/dev/focuslock/                     # JVM unit tests + fakes
└── dummytarget/                                     # reused test app
```

**Deleted in v2:** `policy/` (all of it: `PolicyController`, `DpmPolicyController`, `BrowserPolicy`, `RestrictionBundleMapper`, `RestrictionSchemaReader`) and their tests.

---

## 3. Threat model (what v2 does and does NOT stop)

| Bypass attempt | v2 result |
|---|---|
| Open Instagram / Lite / YouTube / Reddit | ✅ Blocked |
| Reinstall any of them | ✅ Still blocked (blocking is by package name, not by install) |
| youtube.com / m.youtube.com / reddit.com / instagram.com in Chrome | ✅ Blocked |
| Same sites in Chrome Incognito / Custom Tabs (links from Gmail etc.) | ✅ Blocked (same Chrome package) — **verify in Step 4** |
| Samsung Internet / Firefox / Brave | ✅ Blocked as apps |
| Uninstall / force stop FocusLock from App info | ✅ Greyed out (Device Admin) + screen guarded |
| Deactivate Device Admin / turn off Accessibility in Settings | ✅ Screen guarded (best effort, One UI text-based) |
| **Safe Mode** | ❌ Not stopped — third-party Accessibility is off in Safe Mode. **Accepted.** |
| **Change date/time to skip PIN lockout** | ⚠️ Partially — Step 6 guards the Date & time screen. **Accepted.** |
| **Other browser not on the list** | ⚠️ Add to `BlockList.apps` when noticed. **Accepted.** |
| `adb` from a computer | ❌ Not stopped. **Accepted** (dev phase). |

---

## Step 0 — Retire v1 safely (human + agent, ~15 min)

### Tasks
1. **Commit or stash** the current uncommitted v1 changes, then tag v1:
   ```bash
   git status                       # review pending changes first
   git tag v1-device-owner
   git switch -c v2-accessibility
   ```
2. On **every device/emulator running v1**:
   - Open FocusLock → **Unlock** (or debug **"DEV: Remove Device Owner"**).
   - Verify: `adb shell dumpsys device_policy | grep -i "device owner"` → nothing.
   - Verify blocked apps open again (v1 suspensions survive DO removal — see v1 note 4.9).
3. Emulator: you may now **sign into Google** (accounts are fine in v2).

### Acceptance
- Tag `v1-device-owner` exists. Working branch `v2-accessibility`. No device has FocusLock as Device Owner.

---

## Step 1 — Remove Device Owner code (keep PIN + lock state)

### Goal
App builds and runs **without any Device Owner code**. Lock/Unlock just flip a stored flag, gated by the friend's PIN (from v1).

### Implementation
1. **Delete** `policy/` package and its tests (`BrowserPolicyTest`, `RestrictionBundleMapperTest`, `FakePolicyController`).
2. **Manifest:** remove the `<queries>` block. Keep `REQUEST_DELETE_PACKAGES` and the `FocusAdminReceiver`.
3. **`LockManager`** (simplified):
   ```kotlin
   class LockManager(
       private val store: LockStateStoreContract,
       private val pinManager: PinManager,
       private val clock: Clock,
   ) {
       fun lock(): LockResult                         // refuses if no PIN set
       suspend fun unlock(pin: String): UnlockResult  // verify PIN, then isLocked = false
       fun devForceUnlock()                           // debug only, no PIN
   }
   ```
   - `lock()` sets `store.isLocked = true`, `store.lastAppliedEpochMs = now`.
   - Remove `LockReport`, `reapplyIfLocked`, `removeCompletely` (comes back in Step 7), `devRemoveDeviceOwner`.
   - `UnlockResult.Unlocked` no longer carries a report.
4. **`LockStateStoreContract`** gains change notification (needed by the service in Step 3):
   ```kotlin
   fun addListener(listener: (Boolean) -> Unit)
   fun removeListener(listener: (Boolean) -> Unit)
   ```
   Real impl uses `SharedPreferences.OnSharedPreferenceChangeListener` (keep a strong reference to it).
5. **UI:** remove Device Owner status, DO setup hint, "DEV: Remove Device Owner", the `LockReport` display. Keep Locked/Unlocked status, Lock, Unlock (PIN), Change code, Set code. Add debug-only **"DEV: Force unlock"**.
6. **`AppContainer`:** drop `schemaReader`, `policyController`.

### Unit tests
- Update `LockManagerTest`:
  - `lock refuses when no pin set`
  - `lock sets isLocked and lastApplied`
  - `unlock with wrong pin keeps locked`
  - `unlock with correct pin clears isLocked`
  - `lock twice is idempotent`
  - `devForceUnlock clears isLocked without pin`
- `FakeLockStateStore`: `listener is notified on isLocked change`.
- `PinManagerTest`, `PinHasherTest`: unchanged, must still pass.
- `MainViewModelTest`: remove DO assertions; state reflects locked/unlocked + pin set.

### Manual tests (emulator)
| # | Action | Expected |
|---|---|---|
| 1.1 | `grep -rn "DevicePolicyManager\|setPackagesSuspended" app/src/main` | Only `FocusAdminReceiver` imports remain (no DO calls) |
| 1.2 | Fresh install, open app | Prompt to set code; Lock disabled |
| 1.3 | Set code, Lock | Shows "Locked" |
| 1.4 | Unlock with wrong code ×5 | Tries countdown → lockout (v1 behaviour) |
| 1.5 | DEV: Force unlock | Shows "Unlocked" |

### Acceptance
- Build green. No Device Owner code in `main`. 1.1–1.5 pass.

---

## Step 2 — Pure blocking rules (no Android yet)

### Goal
All "should this be blocked?" logic exists as **pure Kotlin** with thorough unit tests, before any Accessibility code.

### Part A — Block list
```kotlin
object BlockList {
    const val SELF = "dev.focuslock"
    const val CHROME = "com.android.chrome"

    val apps: Set<String> = buildSet {
        add("com.instagram.android")
        add("com.instagram.lite")
        add("com.google.android.youtube")
        add("com.reddit.frontpage")
        add("com.sec.android.app.sbrowser")   // Samsung Internet
        add("org.mozilla.firefox")
        add("com.brave.browser")
        if (BuildConfig.DEBUG) add("dev.focuslock.dummytarget")
    }

    /** Host suffixes. "youtube.com" also blocks "m.youtube.com", "www.youtube.com". */
    val sites: Set<String> = setOf(
        "youtube.com", "youtu.be",
        "reddit.com", "redd.it",
        "instagram.com", "instagr.am",
    )

    val neverBlock: Set<String> = setOf(SELF, "com.android.vending")
}
```
- Guard test: `apps` and `neverBlock` must not overlap.

### Part B — `HostMatcher`
```kotlin
object HostMatcher {
    /** "https://m.youtube.com/watch?v=1" → "m.youtube.com"; "best pizza" → null */
    fun extractHost(urlBarText: String): String?
    fun isBlockedHost(host: String, blocked: Set<String> = BlockList.sites): Boolean
}
```
Rules:
- Trim, lowercase. Strip scheme (`http://`, `https://`), path, query, fragment, port, trailing dot.
- **Contains whitespace → `null`** (it's a search query, not a URL).
- Must contain a dot, otherwise `null`.
- Match = `host == suffix || host.endsWith(".$suffix")`. So `notyoutube.com` is **not** blocked.

### Part C — `ScreenEvent`, `Decision`, `BlockDecider`
```kotlin
data class ScreenEvent(
    val packageName: String,
    val urlBarText: String?,          // only for Chrome, else null
    val screenTexts: List<String>,    // only for Settings packages (Step 6), else empty
)

sealed interface Decision {
    data object Allow : Decision
    data class BlockApp(val pkg: String) : Decision
    data class BlockSite(val host: String) : Decision
    data object BlockSettings : Decision
}

class BlockDecider(private val settingsGuard: SettingsGuard) {
    fun decide(event: ScreenEvent, isLocked: Boolean): Decision
}
```
Order inside `decide`:
1. `!isLocked` → `Allow`.
2. `packageName in neverBlock` → `Allow`.
3. `packageName in apps` → `BlockApp`.
4. `packageName == CHROME` and `urlBarText` → host blocked → `BlockSite`.
5. Settings package and `settingsGuard.isDangerous(screenTexts)` → `BlockSettings`. (Stub `SettingsGuard` returning `false` until Step 6.)
6. Otherwise `Allow`.

### Part D — `DecisionThrottle`
```kotlin
class DecisionThrottle(private val clock: Clock, private val windowMs: Long = 1_000) {
    /** true = execute; false = same decision fired within windowMs, skip */
    fun shouldFire(decision: Decision): Boolean
}
```
- `Allow` → always `false` (nothing to execute).

### Unit tests
- `HostMatcherTest`:
  - `extracts host from full url` (`https://m.youtube.com/watch?v=1` → `m.youtube.com`)
  - `extracts host from bare domain` (`youtube.com` → `youtube.com`)
  - `handles port, path, query, uppercase, trailing dot`
  - `search text with spaces returns null` (`reddit.com alternatives` → `null`)
  - `single word returns null` (`youtube` → `null`)
  - `subdomain is blocked` (`old.reddit.com`, `www.instagram.com`)
  - `lookalike is not blocked` (`notyoutube.com`, `youtube.com.evil.io` → only blocked if suffix matches; document result)
  - `unrelated host is not blocked` (`google.com`)
- `BlockDeciderTest`:
  - `unlocked always allows`
  - `blocked app is blocked when locked` (each of the 4 target apps)
  - `self and play store never blocked`
  - `chrome with blocked host is BlockSite`
  - `chrome with search text is allowed`
  - `chrome with null url bar is allowed`
  - `unknown package is allowed`
- `DecisionThrottleTest` (FakeClock):
  - `first block fires`, `same block within 1s skipped`, `same block after 1s fires`, `different block fires immediately`, `allow never fires`.
- `BlockListTest`: `apps and neverBlock do not overlap`, `instagram lite is in apps`.

### Manual tests
- None (pure code). Review the test list against the rules above.

### Acceptance
- Build green. All listed tests exist and pass.

---

## Step 3 — Accessibility Service: block apps

### Goal
When locked, opening a blocked app sends the user Home and shows the Blocked screen.

### Part A — Spike (do first, ~30 min)
Debug-only service that just logs `event.packageName`, `event.eventType`, `event.className`.
Confirm on emulator (API 37) and write findings as a comment at the top of `FocusAccessibilityService.kt`:
1. Events arrive **only** for packages listed in `android:packageNames`.
2. `startActivity(BlockedActivity)` from the service works while another app is in front (background-activity-launch exemption for bound accessibility services).
3. `performGlobalAction(GLOBAL_ACTION_HOME)` returns `true`.

### Part B — Service config `res/xml/accessibility_service.xml`
```xml
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeWindowStateChanged|typeWindowContentChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagReportViewIds"
    android:canRetrieveWindowContent="true"
    android:notificationTimeout="100"
    android:packageNames="com.instagram.android,com.instagram.lite,com.google.android.youtube,com.reddit.frontpage,com.sec.android.app.sbrowser,org.mozilla.firefox,com.brave.browser,com.android.chrome"
    android:description="@string/a11y_service_description" />
```
- `src/debug/res/xml/accessibility_service.xml`: same + `dev.focuslock.dummytarget`.
- Settings packages are added in Step 6 (not yet).
- `a11y_service_description`: plain-English explanation ("FocusLock uses this only to detect when a blocked app or website opens. Nothing is stored or sent anywhere.").

### Part C — Manifest
```xml
<service
    android:name=".blocker.FocusAccessibilityService"
    android:exported="false"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/accessibility_service" />
</service>

<activity
    android:name=".blocker.BlockedActivity"
    android:exported="false"
    android:excludeFromRecents="true"
    android:launchMode="singleTask"
    android:taskAffinity="dev.focuslock.blocked" />
```

### Part D — `FocusAccessibilityService` (thin)
- `onServiceConnected`: read `isLocked` once, register `LockStateStore` listener → cache in a `@Volatile var`.
- `onAccessibilityEvent(event)`:
  1. `pkg = event.packageName ?: return`.
  2. Build `ScreenEvent(pkg, urlBarText = null, screenTexts = emptyList())` (Chrome/Settings filled in later steps).
  3. `decision = decider.decide(screenEvent, isLocked)`.
  4. If `throttle.shouldFire(decision)` → `execute(decision)`.
- `execute(BlockApp | BlockSite | BlockSettings)`:
  `performGlobalAction(GLOBAL_ACTION_HOME)` then start `BlockedActivity` with `FLAG_ACTIVITY_NEW_TASK` + reason extra.
- `onDestroy` / `onUnbind`: remove listener.
- **No** decision logic here (Hard rule 6).

### Part E — `BlockedActivity`
- Simple Compose screen: "🔒 Blocked by FocusLock" + what was blocked ("Instagram", "youtube.com") + **OK** button.
- **OK** and **Back** → start the Home intent (`ACTION_MAIN` + `CATEGORY_HOME`), then `finish()`. Never return to the blocked app.

### Part F — Main screen
- New line: **"Blocker service: ON / OFF"** (read `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`, contains our component).
- Button **"Turn on blocker"** → `Settings.ACTION_ACCESSIBILITY_SETTINGS`. (Full checklist comes in Step 5.)

### Unit tests
- No new pure logic here; Step 2 tests cover decisions.
- Add `BlockedReasonTest` if you add a pure function that maps `Decision` → display text.

### Manual tests (emulator)
Enable the service fast via adb for testing:
```bash
adb shell settings put secure enabled_accessibility_services dev.focuslock/dev.focuslock.blocker.FocusAccessibilityService
adb shell settings put secure accessibility_enabled 1
```
| # | Action | Expected |
|---|---|---|
| 3.1 | Unlocked → open dummy target | Opens normally |
| 3.2 | Lock → open dummy target | Home + "Blocked" screen within ~0.5 s |
| 3.3 | On Blocked screen press Back | Home screen, not the dummy target |
| 3.4 | Open dummy target from Recents | Blocked again |
| 3.5 | Uninstall + reinstall dummy target while locked, open it | Blocked |
| 3.6 | Unlock → open dummy target | Opens normally |
| 3.7 | Reboot (locked) → open dummy target | Blocked (service auto-reconnects) |
| 3.8 | Open Settings, Phone, Camera, Play Store while locked | All work normally |
| 3.9 | Turn service off in Settings → main screen | Shows "Blocker service: OFF" |
| 3.10 | `adb shell dumpsys accessibility \| grep -i focuslock` | Service listed as bound |

### Acceptance
- Build green. Spike findings documented. 3.1–3.10 pass.

---

## Step 4 — Chrome website blocking

### Goal
When locked, opening a blocked site in Chrome (normal, Incognito, Custom Tab) sends the user Home and shows the Blocked screen.

### ⚠️ Known unknown: Chrome's URL-bar view ID
Chrome's view IDs are not a public API and can change between versions. **Do not guess. Check it.**

### Part A — Spike (do first, ~30 min)
1. Open Chrome on the emulator at `youtube.com`, then run:
   ```bash
   adb shell uiautomator dump /sdcard/ui.xml && adb pull /sdcard/ui.xml
   ```
2. Find the URL bar node: record its `resource-id` (expected `com.android.chrome:id/url_bar`) and `text`.
3. Repeat for: Incognito tab, a Custom Tab (open a link from any app using Custom Tabs), URL bar **focused while typing**.
4. **Write the observed IDs + example texts as a comment in `ChromeUrlReader.kt`.**

### Part B — `ChromeUrlReader` (thin Android)
```kotlin
object ChromeUrlReader {
    val URL_BAR_IDS = listOf(
        "com.android.chrome:id/url_bar",
        // add any extra IDs found in the spike (e.g. Custom Tab title/url views)
    )
    fun read(root: AccessibilityNodeInfo?): String?
}
```
- Uses `root.findAccessibilityNodeInfosByViewId(id)` for each ID, returns first non-blank text.
- Call it **only** when `event.packageName == CHROME`.
- Recycle nodes per platform guidance (no-op on newer APIs, harmless).

### Part C — Wire into the service
- For Chrome events: `urlBarText = ChromeUrlReader.read(rootInActiveWindow)`.
- Same `BlockDecider` → `BlockSite` → same `execute()` path as apps.
- Content-change events from Chrome are frequent: `DecisionThrottle` (Step 2) already prevents repeated block screens.

### Known gap (accepted, document in code comment)
- The blocked tab stays open in Chrome. Re-opening Chrome blocks again until the user closes that tab. That's fine.

### Unit tests
- Add any spike-observed text formats to `HostMatcherTest` (e.g. if Chrome shows `youtube.com/shorts/abc` without scheme).

### Manual tests (emulator, Chrome updated from Play Store)
| # | Action | Expected |
|---|---|---|
| 4.1 | Locked → Chrome → `youtube.com` | Home + Blocked ("youtube.com") |
| 4.2 | `m.youtube.com`, `old.reddit.com`, `www.instagram.com`, `youtu.be/xyz` | All blocked |
| 4.3 | Incognito tab → `reddit.com` | Blocked |
| 4.4 | Gmail/any app link to `youtube.com` opening in a Chrome Custom Tab | Blocked (or document if not) |
| 4.5 | Google search `reddit.com alternatives` (results page) | **Not** blocked |
| 4.6 | `google.com`, `wikipedia.org` | Load normally |
| 4.7 | Unlock → `youtube.com` | Loads |
| 4.8 | Add youtube.com to Home screen (Chrome "Install app"/shortcut), open it while locked | Blocked (or document) |

### Acceptance
- Build green. Spike documented. 4.1–4.7 pass. 4.4 and 4.8 results recorded.

---

## Step 5 — Setup checklist + Device Admin

### Goal
A first-run checklist makes setup obvious. **Lock** only works when everything is green.

### Part A — `SetupChecker`
```kotlin
interface SetupChecker {
    fun isBlockerServiceEnabled(): Boolean
    fun isDeviceAdminActive(): Boolean
}

data class SetupStatus(
    val blockerServiceEnabled: Boolean,
    val deviceAdminActive: Boolean,
    val pinSet: Boolean,
) {
    val isComplete get() = blockerServiceEnabled && deviceAdminActive && pinSet
}
```
- `AndroidSetupChecker`:
  - service: parse `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` (colon-separated) for our flattened `ComponentName`.
  - admin: `DevicePolicyManager.isAdminActive(admin)` (**the only DPM read allowed**).
- Put the colon-string parsing in a pure function and unit-test it.

### Part B — Device Admin activation
- Button **"Turn on uninstall protection"** →
  ```kotlin
  Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
      .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
      .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Stops FocusLock from being uninstalled while locked.")
  ```
- `device_admin.xml` keeps `<uses-policies />` empty (we need **no** policies, only the "active admin" status).
- `FocusAdminReceiver.onDisableRequested()` → return warning text: "Turning this off removes FocusLock's protection. Ask your friend for the code instead."

### Part C — Checklist UI (`SetupChecklist.kt`)
```
Setup
 ✅ / ❌  1. Turn on blocker          [Open Accessibility settings]
 ✅ / ❌  2. Turn on uninstall protection   [Activate]
 ✅ / ❌  3. Friend sets the code     [Hand phone to friend]
```
- Re-check status in `onResume` (user returns from Settings).
- Help text under item 1 for sideloaded APKs: "If the toggle is greyed out: App info → ⋮ → **Allow restricted settings**."
- **Lock** button disabled until `SetupStatus.isComplete`.
- While **locked** and setup becomes incomplete (e.g. service turned off) → big red banner **"Protection is OFF"** + fix button.

### Part D — LockManager
- `LockManager` takes a `SetupChecker`. `lock()` refuses unless `SetupStatus.isComplete`.

### Unit tests
- `SetupStatusTest`: `complete only when all three true` (all 8 combinations).
- `EnabledServicesParserTest`: empty string, null, single entry, multiple entries, short-form component (`dev.focuslock/.blocker.FocusAccessibilityService`), different package with same class name → false.
- `LockManagerTest`: `lock refuses when service disabled`, `lock refuses when admin inactive`, `lock succeeds when setup complete`.
- `MainViewModelTest`: `protection off banner shown when locked and service disabled`.
- `FakeSetupChecker` in `test/`.

### Manual tests (emulator)
| # | Action | Expected |
|---|---|---|
| 5.1 | Fresh install | Checklist all ❌, Lock disabled |
| 5.2 | Tap "Open Accessibility settings", enable, come back | Item 1 ✅ |
| 5.3 | Tap "Activate", accept system dialog | Item 2 ✅ |
| 5.4 | Set code | Item 3 ✅, Lock enabled |
| 5.5 | Settings → Apps → FocusLock | Uninstall + Force stop greyed out |
| 5.6 | `adb uninstall dev.focuslock` | Fails (`DELETE_FAILED_DEVICE_POLICY_MANAGER`) |
| 5.7 | Settings → Device admin apps → FocusLock → Deactivate | Shows our warning text |
| 5.8 | Lock, then turn off service via adb: `adb shell settings put secure enabled_accessibility_services ""` → open app | Red "Protection is OFF" banner |
| 5.9 | `adb shell dpm remove-active-admin dev.focuslock/.admin.FocusAdminReceiver` | Success (escape hatch, `testOnly`) |

### Acceptance
- Build green. 5.1–5.9 pass.

---

## Step 6 — Settings guard (self-protection while locked)

### Goal
While locked, the Settings screens that could disable or remove FocusLock are blocked. Everything else in Settings works.

### Screens to guard
| # | Screen | Why |
|---|---|---|
| G1 | App info → **FocusLock** | Uninstall / Force stop / Clear storage |
| G2 | Device admin apps → **FocusLock** detail | Deactivate admin |
| G3 | Accessibility → **FocusLock** detail (toggle) | Turn off blocker |
| G4 | Date & time | Skip PIN lockout by moving the clock |

**Not guarded (must keep working):** the Apps list, the Accessibility main list, the Device admin list, everything else.

### ⚠️ Known unknown: One UI screen texts + packages
One UI uses different packages and texts than AOSP (e.g. Accessibility may live in `com.samsung.accessibility`). **Do not guess. Dump the screens.**

### Part A — Spike (emulator AND S25, ~45 min)
For each screen G1–G4 plus the "not guarded" lists:
```bash
adb shell uiautomator dump /sdcard/ui.xml && adb pull /sdcard/ui.xml
```
Record in a table at the top of `SettingsGuard.kt`:
- Package name of the window.
- Distinctive texts (e.g. "FocusLock" + "Force stop"; "Deactivate this device admin app"; "Use FocusLock"; "Automatic date and time").

### Part B — `SettingsGuard` (pure)
```kotlin
class SettingsGuard(private val rules: List<GuardRule> = GuardRules.default) {
    fun isDangerous(screenTexts: List<String>): Boolean
}

/** Matches when the screen contains ALL of `allOf` and ANY of `anyOf` (case-insensitive). */
data class GuardRule(val name: String, val allOf: Set<String>, val anyOf: Set<String>)
```
- Example: `GuardRule("app-info", allOf = setOf("FocusLock"), anyOf = setOf("Force stop", "Uninstall", "Clear storage"))`.
- Rules come from the spike. Include both AOSP (emulator) and One UI (S25) wordings.
- Keep it English-only for now (phone language = English). Document this.

### Part C — Wire into the service
- Add Settings packages from the spike (`com.android.settings`, `com.samsung.accessibility`, …) to `android:packageNames` in both `accessibility_service.xml` files.
- For Settings-package events only: collect visible texts from `rootInActiveWindow` (depth-first, cap at ~200 nodes), pass as `screenTexts`.
- Only on `typeWindowStateChanged` + throttled `typeWindowContentChanged` (Settings pages scroll).
- `BlockDecider` already returns `BlockSettings` when `isDangerous` → same `execute()` path.

### Unit tests (`SettingsGuardTest`)
- One test per guard rule G1–G4 using **real texts recorded in the spike** (AOSP + One UI).
- `apps list containing FocusLock is not dangerous` (FocusLock name without "Force stop" etc.).
- `accessibility main list is not dangerous`.
- `device admin list is not dangerous`.
- `matching is case-insensitive`.
- `empty screen is not dangerous`.
- `BlockDeciderTest`: `settings guard only applies to settings packages`, `settings guard ignored when unlocked`.

### Manual tests (emulator)
| # | Action | Expected |
|---|---|---|
| 6.1 | Locked → Settings → Apps → FocusLock | Blocked |
| 6.2 | Locked → long-press FocusLock icon → App info | Blocked |
| 6.3 | Locked → Device admin apps → FocusLock | Blocked |
| 6.4 | Locked → Accessibility → FocusLock | Blocked |
| 6.5 | Locked → Date & time | Blocked |
| 6.6 | Locked → Settings → Apps list, Wi-Fi, Bluetooth, Display, Accessibility main list | All work |
| 6.7 | Locked → Settings search "FocusLock" → tap result | Blocked when the detail page opens |
| 6.8 | Unlocked → 6.1–6.5 | All allowed |
| 6.9 | Locked → open FocusLock → Unlock with code | Works (FocusLock never blocked) |

### Acceptance
- Build green. Spike table documented. 6.1–6.9 pass.

---

## Step 7 — Health check + "Remove FocusLock completely"

### Goal
- Main screen always shows whether protection is really working.
- A PIN-protected way to fully remove the app without `adb`.

### Part A — Health check (on app open + `onResume`)
- Locked **and** (service off **or** admin inactive) → red banner "Protection is OFF" (from Step 5) + "Last locked: HH:mm".
- No background work — this only runs when the app is opened (Hard rule 2).

### Part B — Remove completely
- Button **"Remove FocusLock completely"** → confirm dialog ("This removes all protection") → `PinDialog` → on `Ok`:
  1. `store.isLocked = false`.
  2. `dpm.removeActiveAdmin(admin)` (an admin may remove itself — no Device Owner needed).
  3. Launch `Intent(Intent.ACTION_DELETE, Uri.parse("package:dev.focuslock"))`.
- Add to `SetupChecker` (or a small `AdminController` interface): `fun deactivateAdmin()`.
- The user turns off the Accessibility service themselves (uninstall also removes it).

### Unit tests
- `removeCompletely with wrong pin does nothing` (no admin call, still locked).
- `removeCompletely unlocks then deactivates admin` (order verified on fake).
- `MainViewModelTest`: `health banner hidden when unlocked even if service off`.

### Manual tests (emulator)
| # | Action | Expected |
|---|---|---|
| 7.1 | Locked → Remove completely → wrong code | Nothing changes |
| 7.2 | Correct code | Unlocked, admin removed, system uninstall dialog appears |
| 7.3 | Confirm uninstall | App gone; dummy target opens; Chrome loads youtube.com |
| 7.4 | Reinstall → full setup again | Checklist works from scratch |

### Acceptance
- Build green. 7.1–7.4 pass.

---

## Step 8 — Real phone (S25 Ultra), still dev mode

### Pre-flight (human, ~5 min)
1. **No account removal needed.** ✅
2. Developer options → **USB debugging ON** (for install + escape hatch only).
3. Settings → Security and privacy → **Auto Blocker**: if it blocks the `adb` install, turn it off temporarily.
4. Back up `~/.android/debug.keystore`.

### Install + setup
```bash
./gradlew installDebug
```
Then on the phone: open FocusLock → complete the checklist → friend sets the code → **Lock**.

### Full regression on phone (real apps)
| # | Check | Expected |
|---|---|---|
| 8.1 | Instagram, Instagram Lite, YouTube, Reddit (whichever installed) | Blocked |
| 8.2 | Samsung Internet, Firefox, Brave (whichever installed) | Blocked |
| 8.3 | Chrome: youtube.com / reddit.com / instagram.com (normal + Incognito) | Blocked |
| 8.4 | YouTube link from WhatsApp / Gmail (Custom Tab or app) | Blocked |
| 8.5 | Settings guard G1–G4 on **One UI** | Blocked |
| 8.6 | Settings: Wi-Fi, Bluetooth, Apps list, Battery, Display | Work |
| 8.7 | Reboot | Still locked, blocking works after unlock screen |
| 8.8 | Play Store: update an unrelated app; update FocusLock via `installDebug` | Works; service still enabled after FocusLock update |
| 8.9 | Netflix, GPay/UPI, bank app, camera, calls, Maps | Work |
| 8.10 | Secure Folder / Dual Messenger copy of Instagram (if present) | Record result (may not be blocked) |
| 8.11 | Wrong code ×5 | Lockout |
| 8.12 | Correct code | Everything unblocked |
| 8.13 | Battery → app usage after 24 h | FocusLock ≈ 0% |
| 8.14 | Escape hatch: `adb shell settings put secure enabled_accessibility_services ""` + `adb shell dpm remove-active-admin dev.focuslock/.admin.FocusAdminReceiver` | Blocking stops, app uninstallable |

### Acceptance
- All 8.x pass (8.10 recorded). Results in `TEST_RESULTS.md` (date, device, One UI version, Chrome version, pass/fail per row).

---

## Appendix A — Verification cheat sheet

```bash
# Is the blocker service enabled?
adb shell settings get secure enabled_accessibility_services

# Enable it from adb (emulator testing)
adb shell settings put secure enabled_accessibility_services dev.focuslock/dev.focuslock.blocker.FocusAccessibilityService
adb shell settings put secure accessibility_enabled 1

# Is it actually bound/running?
adb shell dumpsys accessibility | grep -i -A3 focuslock

# Device Admin status
adb shell dumpsys device_policy | grep -i -A2 focuslock

# Activate / remove Device Admin from adb (remove works because testOnly=true)
adb shell dpm set-active-admin dev.focuslock/.admin.FocusAdminReceiver
adb shell dpm remove-active-admin dev.focuslock/.admin.FocusAdminReceiver

# Dump the current screen's view tree (Chrome / Settings spikes)
adb shell uiautomator dump /sdcard/ui.xml && adb pull /sdcard/ui.xml

# EMERGENCY: stop all blocking + allow uninstall
adb shell settings put secure enabled_accessibility_services ""
adb shell dpm remove-active-admin dev.focuslock/.admin.FocusAdminReceiver
adb uninstall dev.focuslock

# App logs (debug builds only)
adb logcat -s FocusLock
```

---

## Appendix B — Agent handoff prompt (copy per step)

```
You are implementing Step <N> of FocusLock-Plan-v2.md.

1. Read sections 0, 1, 2, 3 and Step <N> fully before writing code.
2. Follow the Hard Rules in section 0. If a rule blocks you, stop and report.
3. Only implement Step <N>. Do not start other steps.
4. All block decisions live in pure Kotlin (BlockDecider, HostMatcher, SettingsGuard).
   The AccessibilityService only converts events and executes Decisions.
5. If the step has a Spike, do it first and document the findings in code comments.
6. Write every unit test listed in Step <N> (Arrange-Act-Assert, descriptive names).
7. Run: ./gradlew assembleDebug testDebugUnitTest lintDebug  — all must pass.
8. Output:
   - Summary of files changed
   - Spike findings (if any)
   - Unit test results
   - The manual test table for Step <N> with a "How to verify" note per row
   - Any deviation from the plan and why
9. Commit with message: "v2 Step <N>: <short title>"
```

### Testing-agent prompt
```
You are the tester for Step <N> of FocusLock-Plan-v2.md.

1. Do NOT change production code. You may only add/modify tests.
2. Run ./gradlew testDebugUnitTest and report failures with stack traces.
3. Review unit tests against the list in Step <N>; add any missing ones.
4. For each manual test row, give the exact adb commands / taps and expected output
   so a human can execute it in under 2 minutes.
5. Flag any violation of the Hard Rules in section 0 as a BLOCKER.
   Pay special attention to rule 4 (privacy) and rule 5 (never block self/launcher/dialer).
```

---

## Appendix C — Out of scope (later)

- Blocking Safe Mode (impossible without Device Owner).
- Monotonic-clock PIN lockout (survives date changes even without the Date & time guard).
- Non-English phone languages for `SettingsGuard`.
- Scheduled / timed locks, auto re-lock after temporary unlock.
- Editable block lists in the UI.
- Blocking YouTube inside other apps' embedded WebViews.
- Release build + custom keystore, removing `testOnly`.
