# FocusLock — Coding & Testing Plan (Development Phase)

> **Audience:** AI coding/testing agents.
> **Owner:** one developer, personal use, Samsung Galaxy S25 Ultra.
> **Scope:** coding + testing only. Production hardening comes later.

---

## 0. Read this first (rules for every agent)

### What the app does
- Runs as **Device Owner** (Android enterprise mode, set once via `adb`).
- **Lock** button →
  - Suspends distracting apps (they can't open).
  - Blocks uninstalling those apps (so a reinstall can't clear the suspension).
  - Pushes a URL blocklist to **Chrome** (the only allowed browser; other browsers are suspended).
  - Protects itself from uninstall / force stop / clear data.
- **Unlock** requires a **code set by a friend** (stored only as a hash).
- **No background services.** Android enforces everything → ~0% battery.

### 🚫 Hard rules (never break these)
1. **Do NOT** add `UserManager.DISALLOW_DEBUGGING_FEATURES`. Ever. (Production only, not this plan.)
2. **Do NOT** add `UserManager.DISALLOW_FACTORY_RESET`.
3. **Do NOT** add the `INTERNET` permission or any network library.
4. **Do NOT** add background services, foreground services, AccessibilityService, VpnService, or `BOOT_COMPLETED` receivers.
5. **Never** suspend or uninstall-block: this app's own package, `com.android.settings`, any launcher, dialer, or Play Store.
6. **All** `DevicePolicyManager` calls go through the `PolicyController` interface. No direct DPM calls anywhere else.
7. Keep `android:testOnly="true"` in the debug manifest (lets us remove Device Owner via `adb`).
8. Keep the **debug-only "DEV: Remove Device Owner"** button (safety net) — see Step 1.
9. Test on the **emulator first**. Real phone only in Step 8.
10. Finish a step's **acceptance criteria** before starting the next step. One commit per step.

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
| UI | Jetpack Compose, Material 3, single Activity |
| Build | Gradle Kotlin DSL + version catalog (`libs.versions.toml`) |
| Versions | Latest stable from Android Studio's "Empty Activity" template |
| `minSdk` | **30** (needed for `setUserControlDisabledPackages`) |
| `targetSdk` / `compileSdk` | **35** |
| DI | Manual (`AppContainer`). **No Hilt/Koin.** |
| Storage | `SharedPreferences` (app-private). No Room, no DataStore. |
| Async | Kotlin coroutines (`viewModelScope`, `Dispatchers.Default` for hashing) |
| Unit tests | JUnit4 + `kotlinx-coroutines-test`. **No Robolectric.** |
| Signing | Default Android Studio debug keystore. No custom signing. |

**Application ID:** `dev.focuslock`
**Admin receiver:** `dev.focuslock.admin.FocusAdminReceiver`

---

## 2. Final project structure

```
FocusLock/
├── app/
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── res/xml/device_admin.xml
│       │   └── java/dev/focuslock/
│       │       ├── FocusApp.kt              # Application, builds AppContainer
│       │       ├── AppContainer.kt          # manual DI
│       │       ├── MainActivity.kt
│       │       ├── admin/FocusAdminReceiver.kt
│       │       ├── policy/
│       │       │   ├── PolicyController.kt      # interface
│       │       │   ├── DpmPolicyController.kt   # real impl (DevicePolicyManager)
│       │       │   ├── BlockList.kt             # hardcoded lists
│       │       │   ├── BrowserPolicy.kt         # pure Kotlin data class
│       │       │   └── RestrictionBundleMapper.kt
│       │       ├── lock/
│       │       │   ├── LockManager.kt           # lock/unlock orchestration
│       │       │   ├── LockReport.kt
│       │       │   └── LockStateStore.kt
│       │       ├── pin/
│       │       │   ├── Clock.kt
│       │       │   ├── PinHasher.kt
│       │       │   ├── PinStore.kt
│       │       │   └── PinManager.kt
│       │       └── ui/
│       │           ├── MainViewModel.kt
│       │           ├── MainScreen.kt
│       │           └── PinDialog.kt
│       ├── debug/AndroidManifest.xml    # testOnly="true"
│       └── test/java/dev/focuslock/     # JVM unit tests + fakes
└── dummytarget/                         # tiny test app to block on emulator
```

---

## Step 0 — Environment setup (human does this, ~20 min)

### Tasks
1. Install latest stable **Android Studio**.
2. Device Manager → create **Pixel 8, API 35, "Google Play" system image** (includes Chrome).
3. Boot emulator. **Do NOT sign into a Google account yet** (Device Owner requires zero accounts).
4. Verify adb:
   ```bash
   adb devices
   adb shell pm list users      # expect only: UserInfo{0:...}
   ```

### Acceptance
- Emulator boots, `adb devices` lists it, only user 0 exists.

---

## Step 1 — Project skeleton + Device Owner status

### Goal
App installs, can be made Device Owner via `adb`, and shows **"Device Owner: YES/NO"**.

### Implementation
1. Create project `FocusLock` (Empty Activity, Compose), package `dev.focuslock`, `minSdk 30`, `targetSdk 35`.
2. **`res/xml/device_admin.xml`**
   ```xml
   <device-admin>
       <uses-policies />
   </device-admin>
   ```
3. **`FocusAdminReceiver`** extends `DeviceAdminReceiver`. Empty body is fine.
4. **Manifest** (main):
   ```xml
   <receiver
       android:name=".admin.FocusAdminReceiver"
       android:exported="true"
       android:permission="android.permission.BIND_DEVICE_ADMIN">
       <meta-data
           android:name="android.app.device_admin"
           android:resource="@xml/device_admin" />
       <intent-filter>
           <action android:name="android.app.action.DEVICE_ADMIN_ENABLED" />
       </intent-filter>
   </receiver>
   ```
   - **No** `INTERNET` permission.
5. **`src/debug/AndroidManifest.xml`**
   ```xml
   <manifest xmlns:android="http://schemas.android.com/apk/res/android"
       xmlns:tools="http://schemas.android.com/tools">
       <application
           android:testOnly="true"
           tools:replace="android:testOnly" />
   </manifest>
   ```
6. **`PolicyController` interface** (start small, grow per step):
   ```kotlin
   interface PolicyController {
       fun isDeviceOwner(): Boolean
       fun clearDeviceOwner()
   }
   ```
7. **`DpmPolicyController`** implements it using `DevicePolicyManager` + `ComponentName(context, FocusAdminReceiver::class.java)`.
   - `clearDeviceOwner()` → `dpm.clearDeviceOwnerApp(packageName)` (deprecated but works; suppress warning).
8. **`AppContainer`** holds singletons; created in `FocusApp.onCreate()`.
9. **`MainScreen`** shows:
   - "Device Owner: YES / NO"
   - Setup hint text when NO (the `adb` command below).
   - **Debug-only** red button **"DEV: Remove Device Owner"** (visible only if `BuildConfig.DEBUG`). Calls `clearDeviceOwner()`.
   - Enable `buildFeatures { buildConfig = true }`.

### Unit tests
- `FakePolicyController` in `test/` (records calls, configurable `isDeviceOwner`).
- `MainViewModelTest`: state shows `isDeviceOwner = true/false` from fake.

### Manual tests (emulator)
```bash
./gradlew installDebug
adb shell dpm set-device-owner dev.focuslock/.admin.FocusAdminReceiver
# expect: "Success: Device owner set to package dev.focuslock"
```
| # | Action | Expected |
|---|---|---|
| 1.1 | Open app before `dpm` command | "Device Owner: NO" |
| 1.2 | Run `dpm` command, reopen app | "Device Owner: YES" |
| 1.3 | `adb shell dpm remove-active-admin dev.focuslock/.admin.FocusAdminReceiver` | "Success", app shows NO |
| 1.4 | Set DO again, tap "DEV: Remove Device Owner" | App shows NO |
| 1.5 | `adb shell dumpsys device_policy \| grep -i "device owner"` | Shows `dev.focuslock` when set |

### Acceptance
- Build green. All manual tests pass. Device Owner can be set and removed **both** ways (adb + button).

---

## Step 2 — Block apps (suspend + uninstall-block), no PIN yet

### Goal
**Lock** button suspends blocked apps and blocks their uninstall. **Unlock** button reverses it. (No PIN yet — added in Step 5.)

### Part A — Dummy target app (for deterministic testing)
- New module `dummytarget`, application ID `dev.focuslock.dummytarget`.
- One Activity with text "Dummy target opened". No other code.
- Install it on emulator: `./gradlew :dummytarget:installDebug`

### Part B — Block list
```kotlin
object BlockList {
    val apps: List<String> = buildList {
        add("com.google.android.youtube")
        add("com.reddit.frontpage")
        add("com.instagram.android")
        add("com.sec.android.app.sbrowser")   // Samsung Internet
        add("org.mozilla.firefox")
        add("com.brave.browser")
        if (BuildConfig.DEBUG) add("dev.focuslock.dummytarget")
    }

    val neverTouch: Set<String> = setOf(
        "dev.focuslock",
        "com.android.settings",
        "com.android.vending",
    )
}
```
- Add a **guard**: `LockManager` filters out anything in `neverTouch` before calling the controller. Unit-test this.

### Part C — PolicyController additions
```kotlin
fun isInstalled(pkg: String): Boolean
fun setAppsSuspended(pkgs: List<String>, suspended: Boolean): List<String>  // returns failed pkgs
fun setUninstallBlocked(pkg: String, blocked: Boolean)
```
- `isInstalled`: `packageManager.getPackageInfo(pkg, 0)` in try/catch `NameNotFoundException`.
  - Add `<queries>` in manifest listing every package in `BlockList.apps` + Chrome (Android 11+ package visibility).
- `setAppsSuspended` → `dpm.setPackagesSuspended(admin, pkgs.toTypedArray(), suspended).toList()`.
- `setUninstallBlocked` → `dpm.setUninstallBlocked(admin, pkg, blocked)`.

### Part D — LockManager
```kotlin
class LockManager(private val policy: PolicyController, private val store: LockStateStore) {
    fun lock(): LockReport
    fun unlock(): LockReport
}

data class LockReport(
    val applied: List<String>,
    val skippedNotInstalled: List<String>,
    val failed: List<String>,
)
```
- `lock()`:
  1. Require `isDeviceOwner()` else return error/throw `IllegalStateException`.
  2. Split `BlockList.apps` (minus `neverTouch`) into installed / not installed.
  3. Suspend installed ones; collect failed.
  4. `setUninstallBlocked(pkg, true)` for each installed one.
  5. `store.isLocked = true`.
- `unlock()`: reverse order, all with `false`, then `store.isLocked = false`.
- **Both must be idempotent** (safe to call twice).
- `LockStateStore`: `SharedPreferences` wrapper with `isLocked: Boolean`.

### Part E — UI
- Shows: Device Owner status, **Locked/Unlocked** status, buttons **Lock** / **Unlock**.
- After an action, show the `LockReport` (applied / skipped / failed lists).
- Lock button disabled if not Device Owner.

### Unit tests (`LockManagerTest`)
- `lock suspends only installed apps`
- `lock never touches packages in neverTouch`
- `lock blocks uninstall for each suspended app`
- `lock reports not-installed packages as skipped`
- `lock reports packages DPM failed to suspend`
- `unlock unsuspends and unblocks uninstall`
- `lock twice is idempotent`
- `lock throws when not device owner`

### Manual tests (emulator)
| # | Action | Expected |
|---|---|---|
| 2.1 | Tap **Lock** | Report lists `dev.focuslock.dummytarget` as applied, others skipped |
| 2.2 | Tap dummy target icon | System dialog: app blocked/paused by admin. Icon greyed. |
| 2.3 | `adb shell dumpsys package dev.focuslock.dummytarget \| grep -i suspended` | `suspended=true` |
| 2.4 | `adb uninstall dev.focuslock.dummytarget` | `DELETE_FAILED_OWNER_BLOCKED` |
| 2.5 | Settings → Apps → dummy target → Uninstall | Button disabled/blocked |
| 2.6 | Tap **Unlock** | Dummy target opens normally |
| 2.7 | `adb uninstall dev.focuslock.dummytarget` after unlock | `Success` (reinstall it after) |
| 2.8 | Lock → reboot emulator → check 2.2 | Still blocked |

### Acceptance
- Build green, unit tests pass, manual 2.1–2.8 pass.

---

## Step 3 — Block websites in Chrome

### Goal
When locked, Chrome blocks `youtube.com`, `youtu.be`, `reddit.com`, `instagram.com` and disables incognito.

### ⚠️ Known unknown: restriction value types
Chrome's managed-config schema decides whether `URLBlocklist` is a **string array** or a **JSON string**. **Do not guess. Read the schema at runtime.**

### Part A — Spike (do first, ~30 min)
- Add a debug-only button **"DEV: Log browser schema"**:
  ```kotlin
  val rm = context.getSystemService(RestrictionsManager::class.java)
  rm.getManifestRestrictions("com.android.chrome").forEach {
      Log.d("FocusLock", "${it.key} type=${it.type}")
  }
  ```
- Run it, filter logcat for `URLBlocklist` and `IncognitoModeAvailability`.
- **Write the observed types into a comment in `RestrictionBundleMapper.kt`.**

### Part B — Pure Kotlin model
```kotlin
data class BrowserPolicy(
    val urlBlocklist: List<String>,
    val incognitoModeAvailability: Int,   // 1 = disabled
)
```
- In `BlockList`:
  ```kotlin
  val sites = listOf("youtube.com", "youtu.be", "reddit.com", "redd.it", "instagram.com")
  const val CHROME = "com.android.chrome"
  ```

### Part C — Mapper + controller
- `RestrictionBundleMapper.toBundle(policy, schema: List<RestrictionEntry>)`:
  - If `URLBlocklist` type is `TYPE_MULTI_SELECT` or array-like → `putStringArray`.
  - If `TYPE_STRING` → `putString(JSON array string)`.
  - `IncognitoModeAvailability` → `putInt` if `TYPE_INTEGER`, `putString` if `TYPE_STRING`/`TYPE_CHOICE`.
- `PolicyController` additions:
  ```kotlin
  fun setBrowserPolicy(pkg: String, policy: BrowserPolicy?)   // null = clear (empty Bundle)
  ```
  → `dpm.setApplicationRestrictions(admin, pkg, bundle)`.
- `LockManager.lock()`: apply policy to Chrome **if installed** (skip + report if not). `unlock()`: pass `null`.

### Unit tests
- `BrowserPolicyTest`: blocklist contains all sites; incognito = 1.
- `LockManagerTest`: lock applies browser policy to Chrome when installed; skips it when not installed; unlock clears with `null`.
- Mapper logic: put the type-decision logic in a pure function `decideValueKind(type: Int): ValueKind` and unit test that (avoid `Bundle` in JVM tests).

### Manual tests (emulator)
- After DO is set, **now** sign into Google on the emulator, update Chrome from Play Store.

| # | Action | Expected |
|---|---|---|
| 3.1 | Lock, open Chrome → `chrome://policy` → "Reload policies" | `URLBlocklist` + `IncognitoModeAvailability` listed, status OK |
| 3.2 | Chrome → `youtube.com` | `ERR_BLOCKED_BY_ADMINISTRATOR` page |
| 3.3 | Chrome → `m.youtube.com`, `old.reddit.com`, `www.instagram.com` | Blocked |
| 3.4 | Chrome menu → New Incognito tab | Option missing/disabled |
| 3.6 | Unlock, reload policies, open `youtube.com` | Loads |
| 3.7 | Chrome → `google.com` while locked | Loads (no over-blocking) |

### Acceptance
- Build green. Spike result documented. 3.1–3.4, 3.6, 3.7 pass.

---

## Step 4 — Self-protection (dev-safe subset)

### Goal
While locked, the user can't trivially remove or neuter FocusLock.

### Policies to apply on **lock** (and reverse on **unlock**)
| Call | Effect |
|---|---|
| `setUninstallBlocked(admin, "dev.focuslock", true)` | Can't uninstall FocusLock |
| `setUserControlDisabledPackages(admin, listOf("dev.focuslock"))` | Can't force stop / clear data |
| `addUserRestriction(DISALLOW_SAFE_BOOT)` | No Safe Mode |
| `addUserRestriction(DISALLOW_ADD_USER)` | No new users/guest |
| `addUserRestriction(DISALLOW_CONFIG_DATE_TIME)` | Can't change clock (protects PIN lockout) |

> ❌ **Not** in this list: `DISALLOW_DEBUGGING_FEATURES`, `DISALLOW_FACTORY_RESET`. See hard rules.

### Implementation
- `PolicyController` additions:
  ```kotlin
  fun setUserRestriction(key: String, enabled: Boolean)
  fun setSelfControlDisabled(disabled: Boolean)   // empty list when false
  ```
- `BlockList.userRestrictions = listOf(DISALLOW_SAFE_BOOT, DISALLOW_ADD_USER, DISALLOW_CONFIG_DATE_TIME)`.
- Self uninstall-block uses the existing `setUninstallBlocked` but **bypasses** the `neverTouch` guard via a dedicated method `protectSelf(enabled: Boolean)` in `LockManager`.
- **"DEV: Remove Device Owner"** button must first call `unlock()` (no PIN in debug), then `clearDeviceOwner()`.

### Unit tests
- `lock adds every user restriction in BlockList`
- `lock protects self (uninstall block + control disabled)`
- `unlock removes every restriction and self protection`
- `debug removal unlocks before clearing device owner` (verify call order on fake)

### Manual tests (emulator)
| # | Action | Expected |
|---|---|---|
| 4.1 | Lock → Settings → Apps → FocusLock | Uninstall, Force stop, Clear storage disabled |
| 4.2 | `adb uninstall dev.focuslock` | `DELETE_FAILED_OWNER_BLOCKED` |
| 4.3 | `adb shell dumpsys user \| grep -i -A3 restrictions` | Shows `no_safe_boot`, `no_add_user`, `no_config_date_time` |
| 4.4 | Settings → Users / Multiple users | Can't add user |
| 4.5 | Settings → Date & time | Locked |
| 4.6 | Hold power → long-press Power off (Safe Mode prompt) | Safe Mode not offered / blocked |
| 4.7 | `./gradlew installDebug` while locked (app update) | Installs fine (update ≠ uninstall) |
| 4.8 | Unlock → 4.1 & 4.2 again | All allowed |
| 4.9 | Lock → `adb shell dpm remove-active-admin ...` | Success. Then verify FocusLock uninstallable & restrictions gone. **Document result.** |
| 4.10 | Lock → tap "DEV: Remove Device Owner" | Unlocks + removes DO, everything restored |

> 4.9 is our **emergency escape**. If it does NOT fully restore the phone, stop and report — do not continue to Step 5.

> **4.9 result (API 37 emulator, 2026-10-25):** `remove-active-admin` clears DO, user restrictions, self-protection and the Chrome policy, but **suspended apps stay suspended** (`suspendingPackage=android`). Accepted escape path: `adb shell dpm set-device-owner dev.focuslock/.admin.FocusAdminReceiver` → open FocusLock → **Unlock** (or DEV button). ⚠️ `set-device-owner` requires zero accounts, so on the real phone remove accounts first.
> **4.2 note:** while FocusLock is Device Owner, `adb uninstall dev.focuslock` returns `DELETE_FAILED_DEVICE_POLICY_MANAGER` (DO check runs before the uninstall-block check), locked or not.

### Acceptance
- Build green. 4.1–4.10 pass. Escape hatch (4.9, 4.10) confirmed working.

---

## Step 5 — Friend's code (PIN) gates Unlock

### Goal
- Lock requires a code to be set first.
- **Unlock requires the code.**
- Brute force is slowed by a lockout.

### Rules
- Code: **6–12 digits**.
- Stored as **PBKDF2WithHmacSHA256**, 16-byte random salt, **120,000 iterations**, 256-bit key. Store salt + hash (Base64) + iterations in `SharedPreferences`.
- **Never** store or log the plain code.
- Compare hashes with `MessageDigest.isEqual` (constant time).
- Hash on `Dispatchers.Default` (it takes ~0.5s).
- **Lockout:** 5 consecutive wrong codes → locked out for **1 hour**. Success resets the counter.
- Store `failedAttempts` and `lockoutUntilEpochMs`. Clock changes are blocked by `DISALLOW_CONFIG_DATE_TIME` (Step 4).
- **Change code** requires the current code.
- **Set code** is only allowed when no code exists.

### Implementation
```kotlin
interface Clock { fun nowMs(): Long }          // SystemClock impl + FakeClock in tests

class PinHasher(iterations: Int = 120_000) {
    fun hash(pin: CharArray, salt: ByteArray): ByteArray
    fun newSalt(): ByteArray
}

class PinManager(store: PinStore, hasher: PinHasher, clock: Clock) {
    fun isSet(): Boolean
    suspend fun setPin(pin: String): Result<Unit>          // fails if already set or invalid
    suspend fun verify(pin: String): VerifyResult           // Ok | Wrong(remaining) | LockedOut(untilMs)
    suspend fun changePin(old: String, new: String): Result<Unit>
}
```
- `LockManager.unlock(pin: String)` → calls `PinManager.verify` first; only unlocks on `Ok`.
- `LockManager.lock()` → refuses if `!pinManager.isSet()`.
- **DEV button** still unlocks **without** PIN (debug only).

### UI
- First run (DO = yes, no PIN): **"Hand the phone to your friend"** → `PinDialog` (enter + confirm, masked input, numeric keyboard).
- Unlock → `PinDialog` → shows "Wrong code, X tries left" or "Locked out until HH:mm".
- "Change code" button (needs old code).

### Unit tests
- `PinHasherTest`: same pin+salt → same hash; different salt → different hash.
- `PinManagerTest` (FakeClock, in-memory PinStore):
  - `setPin rejects < 6 or > 12 digits or non-digits`
  - `setPin fails when already set`
  - `verify correct pin returns Ok and resets counter`
  - `verify wrong pin decrements remaining`
  - `5 wrong pins triggers 1h lockout`
  - `verify during lockout returns LockedOut even with correct pin`
  - `lockout expires after 1h`
  - `plain pin never stored` (inspect fake store contents)
  - `changePin requires correct old pin`
- `LockManagerTest`:
  - `lock refuses when no pin set`
  - `unlock with wrong pin keeps everything locked` (no controller calls)
  - `unlock with correct pin reverses all policies`

### Manual tests (emulator)
| # | Action | Expected |
|---|---|---|
| 5.1 | Fresh install + DO, open app | Prompt to set code; Lock disabled |
| 5.2 | Set code `123456`, Lock | Locked |
| 5.3 | Unlock with `000000` ×5 | Countdown of tries, then "Locked out until …" |
| 5.4 | Correct code during lockout | Still refused |
| 5.5 | `adb shell run-as dev.focuslock cat shared_prefs/*.xml` | No plain `123456` anywhere |
| 5.6 | Wait out lockout (temporarily set lockout to 1 min in a **local** build only, never commit) → correct code | Unlocks |
| 5.7 | Change code with wrong old code | Refused |

### Acceptance
- Build green, all unit tests pass, 5.1–5.7 pass.

---

## Step 6 — Re-apply on app open (state reconciliation)

### Goal
If locked and something drifted (e.g. Instagram newly installed from Play Store), opening FocusLock re-applies the lock. **No background work.**

### Implementation
- `LockManager.reapplyIfLocked()`: if `store.isLocked` → run the same steps as `lock()` (idempotent).
- Call it from `MainViewModel.init`.
- UI shows a small "Last applied: HH:mm" line.

### Unit tests
- `reapply does nothing when unlocked`
- `reapply suspends a newly installed blocked app`

### Manual tests
| # | Action | Expected |
|---|---|---|
| 6.1 | Unlock, uninstall dummy target, Lock (dummy skipped), reinstall dummy target | Dummy opens (expected gap) |
| 6.2 | Open FocusLock | Dummy now suspended |

### Acceptance
- Build green, 6.1–6.2 pass.

---

## Step 7 — "Remove FocusLock completely" (PIN-gated)

### Goal
A safe, PIN-protected way to fully remove the app without adb or factory reset.

### Implementation
- Button **"Remove FocusLock completely"** → `PinDialog` → on `Ok`:
  1. `unlock()` internals (reverse all policies, including self protection).
  2. `clearDeviceOwner()`.
  3. Launch system uninstall: `Intent(Intent.ACTION_DELETE, Uri.parse("package:dev.focuslock"))`.
- Confirmation dialog before step 1 ("This removes all restrictions").

### Unit tests
- `removeCompletely with wrong pin does nothing`
- `removeCompletely unlocks then clears device owner` (order verified)

### Manual tests
| # | Action | Expected |
|---|---|---|
| 7.1 | Locked → Remove completely → wrong code | Nothing changes |
| 7.2 | Correct code | System uninstall dialog appears; after uninstall, dummy target opens, Chrome loads youtube.com, `dumpsys user` restrictions gone |

### Acceptance
- Build green, 7.1–7.2 pass.

---

## Step 8 — Real phone (S25 Ultra), still dev mode

### Pre-flight (human)
1. **Full phone backup** (Samsung Smart Switch or Samsung Cloud).
2. Remove **work profile**, **Secure Folder**, **Dual Messenger** clones, any guest/other users.
3. Settings → Security and privacy → **Auto Blocker → OFF**.
4. Developer options → **USB debugging ON**.
5. Remove **all accounts** (Google, Samsung, others).
6. Verify:
   ```bash
   adb shell pm list users        # only UserInfo{0:...}
   adb shell dumpsys account | grep -i "Accounts:"   # Accounts: 0
   ```
7. Back up `~/.android/debug.keystore` (copy to cloud/USB).

### Install
```bash
./gradlew installDebug
adb shell dpm set-device-owner dev.focuslock/.admin.FocusAdminReceiver
```
Then re-add accounts. Friend sets the code.

### Full regression on phone (use real apps instead of dummy)
| # | Check | Expected |
|---|---|---|
| 8.1 | YouTube, Instagram, Reddit, Samsung Internet, Firefox, Brave (whichever installed) | Won't open |
| 8.2 | Uninstall any of them | Blocked |
| 8.3 | Chrome: youtube.com / reddit.com / instagram.com | Blocked |
| 8.4 | Chrome incognito | Disabled |
| 8.5 | FocusLock uninstall / force stop / clear data | Blocked |
| 8.6 | Safe Mode, add user, change date/time | Blocked |
| 8.7 | Reboot | Everything still locked |
| 8.8 | Netflix, GPay/UPI app, bank app, camera, calls | Work (bank apps may complain about Developer options — expected in dev) |
| 8.9 | Wrong code ×5 | Lockout |
| 8.10 | Correct code | Everything restored |
| 8.11 | `adb shell dpm remove-active-admin dev.focuslock/.admin.FocusAdminReceiver` | Escape hatch works |
| 8.12 | Battery: Settings → Battery → app usage after 24h | FocusLock ≈ 0% |

### Acceptance
- All 8.x pass. Results recorded in `TEST_RESULTS.md` (date, device, One UI version, pass/fail per row).

---

## Appendix A — Verification cheat sheet

```bash
# Device owner?
adb shell dumpsys device_policy | grep -i -A2 "Device Owner"

# Is a package suspended?
adb shell dumpsys package <pkg> | grep -i suspended

# Uninstall blocked? (expect DELETE_FAILED_OWNER_BLOCKED)
adb uninstall <pkg>

# Active user restrictions
adb shell dumpsys user | grep -i -A5 restrictions

# Users on device
adb shell pm list users

# Emergency: remove device owner (works because testOnly=true)
adb shell dpm remove-active-admin dev.focuslock/.admin.FocusAdminReceiver
# ⚠️ Leaves blocked apps suspended. To lift: set DO again, open FocusLock, Unlock.
adb shell dpm set-device-owner dev.focuslock/.admin.FocusAdminReceiver

# App logs
adb logcat -s FocusLock
```

Browser check: open `chrome://policy` → **Reload policies**.

---

## Appendix B — Agent handoff prompt (copy per step)

```
You are implementing Step <N> of FocusLock-Plan.md.

1. Read sections 0, 1, 2 and Step <N> fully before writing code.
2. Follow the Hard Rules in section 0. If a rule blocks you, stop and report.
3. Only implement Step <N>. Do not start other steps.
4. All DevicePolicyManager calls go through PolicyController.
5. Write every unit test listed in Step <N> (Arrange-Act-Assert, descriptive names).
6. Run: ./gradlew assembleDebug testDebugUnitTest lintDebug  — all must pass.
7. Output:
   - Summary of files changed
   - Unit test results
   - The manual test table for Step <N> with a "How to verify" note per row
   - Any deviation from the plan and why
8. Commit with message: "Step <N>: <short title>"
```

### Testing-agent prompt
```
You are the tester for Step <N> of FocusLock-Plan.md.

1. Do NOT change production code. You may only add/modify tests.
2. Run ./gradlew testDebugUnitTest and report failures with stack traces.
3. Review unit tests against the list in Step <N>; add any missing ones.
4. For each manual test row, give the exact adb commands / taps and expected output
   so a human can execute it in under 2 minutes.
5. Flag any violation of the Hard Rules in section 0 as a BLOCKER.
```

---

## Appendix C — Out of scope (production phase, later)

- `DISALLOW_DEBUGGING_FEATURES`
- Release build + custom keystore
- Removing `testOnly`
- TOTP / authenticator-based unlock
- Emergency time-delayed unlock
- Auto re-lock timer after temporary unlock
- Editable block lists in UI
