# 🔒 FocusLock

**FocusLock** is a lightweight, privacy-focused Android app designed to help you regain focus and stop mindlessly scrolling through distracting apps and websites.

Unlike intrusive blockers that drain your battery with continuous background VPNs or require complex ADB commands, FocusLock uses Android's native Accessibility Service and Device Administrator APIs to enforce strict blocking with **zero battery drain** and **100% offline privacy**.

---

## 📸 Key Features

- **📱 App Blocking**: Block social media and distracting apps (e.g., Instagram, YouTube, Reddit, custom installed apps).
- **🌐 Website Blocking**: Block distracting websites (e.g., `youtube.com`, `reddit.com`, `instagram.com`) directly inside supported browsers (Google Chrome, Brave Browser) without needing a VPN.
- **⏱️ Daily Usage Limits**: Set per-app daily time allowances (e.g., max 30 mins/day on Instagram). Limits reset automatically every local calendar midnight and apply regardless of lock state.
- **🎯 Focus Sessions**: Lock yourself into a 1-to-6 hour strict focus session that **cannot be ended early** under any circumstances.
- **☕ Breaks**: Take timed, passcode-protected breaks when you genuinely need brief access.
- **🔑 Friend Passcode Protection**: Store your unlock PIN as an irreversible cryptographic hash. Hand your phone to a trusted friend or family member to set the code so you can't bypass your own rules in weak moments.
- **🛡️ Smart Self-Protection**: Prevents force-stopping, clearing data/cache, or disabling permissions in Android Settings while FocusLock is locked.
- **🔋 Zero Battery Drain & Offline Privacy**: 
  - Uses **0 background services**, **no WorkManager**, **no alarms**, and **no VPN**.
  - Requires **NO internet permission** (`android.permission.INTERNET` is omitted completely).
  - All decisions are computed locally in milliseconds.

---

## 📥 Fast Download & Installation

### Option 1: Direct APK Download (Easiest)
You can directly download the official release APK pre-built at the root of this repository:

👉 **[Download FocusLock.apk](FocusLock.apk)** (or download from the GitHub repository files above).

#### How to Install:
1. Download `FocusLock.apk` onto your Android phone.
2. Open your device file manager or browser downloads and tap `FocusLock.apk`.
3. If prompted, enable **"Allow from this source"** / **"Install unknown apps"** in your device settings.
4. Tap **Install** and open **FocusLock**.

---

## 🚀 Initial Setup Guide (1 Minute)

When you launch FocusLock for the first time, complete the 3 setup steps shown on the home checklist:

1. **Enable Accessibility Service**:
   - Tap **Enable** next to Accessibility Service.
   - Find **FocusLock** in your phone's Accessibility settings and toggle it **ON**.
2. **Activate Device Admin**:
   - Tap **Activate** next to Device Admin.
   - Tap **Activate this device admin app** on the Android system prompt. (This prevents FocusLock from being uninstalled or force-stopped while locked).
3. **Set Passcode**:
   - Hand your phone to a trusted friend or family member.
   - Have them enter a secret 4-to-12 digit code that only they know.

Once all three steps are completed, your setup checklist turns green and protection is fully active!

---

## 📖 How to Use FocusLock

### 1. Starting & Unlocking Protection
- **Lock**: Tap the big **Lock** button on the main screen to activate blocking immediately.
- **Unlock**: Tap **Unlock**. Your friend must enter the secret code to disarm protection.

### 2. Customizing the Block List
Tap **Block list** under the Manage section:
- **Apps**: View or search your installed apps and toggle which apps should be blocked when locked.
- **Websites**: Add host suffixes (e.g., `twitter.com`, `tiktok.com`, `facebook.com`) to block them inside browsers.

### 3. Setting Daily Time Limits
Tap **Daily limits** under Manage:
- Choose any app and set allowed minutes per day (or select specific days of the week).
- Once your time budget for the day is used up, FocusLock automatically blocks that app until midnight.

### 4. Focus Sessions
Tap **Focus session** under Sessions:
- Select a duration from **1 hour to 6 hours**.
- During an active Focus Session, **unlocking is disabled** and the session **cannot be ended early**, even with the PIN.

### 5. Taking a Break
- Tap **Take a break** while locked.
- Enter the friend PIN and select break duration (e.g. 15 mins). FocusLock will pause blocking and automatically resume when time expires.

### 6. Uninstalling FocusLock
To prevent unauthorized removal during weak moments, FocusLock blocks access to its own Android Settings & Device Admin uninstall screens while locked.
To completely remove FocusLock:
1. Open **FocusLock**.
2. Scroll to the bottom and tap **Remove FocusLock**.
3. Enter your friend's PIN code.
4. FocusLock will cleanly deactivate Device Admin, clear its data, and open the system uninstall prompt.

---

## 🛠️ Building from Source

If you prefer to compile FocusLock yourself using Gradle or Android Studio:

### Prerequisites
- Android Studio Ladybug / Android Studio 2024.2+ or JDK 11+
- Android SDK (compileSdk 37, minSdk 30)

### Commands
Clone the repository:
```bash
git clone https://github.com/brickcommander/FocusLock.git
cd FocusLock
```

Build the release APK:
```bash
./gradlew app:assembleRelease
```
The compiled APK will be at `app/build/outputs/apk/release/app-release.apk`.

Run unit tests:
```bash
./gradlew test
```

---

## 🔒 Security & Privacy Guarantees

- **No Network Requests**: The app manifest contains no `INTERNET` permission. FocusLock cannot send or receive data over the network.
- **Local Data Only**: Passcodes are hashed with SHA-256 and stored locally in app-private `SharedPreferences`.
- **Zero Background Tracking**: Screen content and URL bars are inspected purely in-memory by local matchers to identify blocked hosts, and are never logged or saved to disk.

---

## 📄 License

This project is provided open-source for personal use and productivity.
