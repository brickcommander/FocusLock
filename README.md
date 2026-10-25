# 🔒 FocusLock — Free Open-Source App & Website Blocker for Android

[![Android](https://img.shields.io/badge/Android-11%2B%20%28API%2030--37%20Supported%20--%20Android%2016 Ready%29-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/brickcommander/FocusLock)
[![Language](https://img.shields.io/badge/Language-Kotlin%20100%25-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://github.com/brickcommander/FocusLock)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20%26%20Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://github.com/brickcommander/FocusLock)
[![Battery](https://img.shields.io/badge/Battery-0%25%20Drain%20%28No%20VPN%2FServices%29-brightgreen?style=for-the-badge&logo=battery&logoColor=white)](https://github.com/brickcommander/FocusLock)
[![Privacy](https://img.shields.io/badge/Privacy-100%25%20Offline%20%28No%20Internet%20Permission%29-blue?style=for-the-badge&logo=shield&logoColor=white)](https://github.com/brickcommander/FocusLock)
[![APK Download](https://img.shields.io/badge/Download-Release%20APK%20%28Direct%29-FF6F00?style=for-the-badge&logo=android&logoColor=white)](FocusLock.apk)

**FocusLock** is a powerful, lightweight, and 100% free open-source **Android app blocker**, **website blocker**, and **digital detox productivity tool**. It helps you stop mindlessly scrolling social media, defeat phone addiction, set daily screen time limits, and build deep focus habits.

Unlike intrusive distraction blockers that drain your battery with background VPNs or require paid subscriptions, FocusLock leverages Android's native Accessibility Service and Device Administrator APIs to enforce strict blocking with **zero battery drain**, **no subscriptions**, and **100% offline privacy**.

---

## 👨‍💻 Built by Pawan — Senior Software & Backend Engineer

Hi there! 👋 I'm **Pawan**, a Senior Software & Backend Engineer passionate about crafting high-performance mobile systems, scalable backend architectures, clean software design, and privacy-first digital tools.

I designed and built **FocusLock** out of a personal need for a zero-battery-drain, tamper-proof tool to eliminate mindless scrolling and reclaim deep focus—without paywalls, ad tracking, or invasive VPNs.

### 🌐 Let's Connect & Collaborate
I am always enthusiastic about connecting with developers, tech leads, founders, and creators across all domains! Whether you want to discuss backend/mobile engineering, give feedback on FocusLock, or explore new opportunities, feel free to reach out:

- ✉️ **Email**: [pawanlive2000@gmail.com](mailto:pawanlive2000@gmail.com)
- 📞 **Phone / WhatsApp**: [+91 9617466846](tel:+919617466846)

### ☕ Support Open-Source Development
FocusLock is completely free, transparent, and privacy-respecting. If this app helped you save hours of screen time, boost your productivity, or build healthier phone habits, consider supporting its ongoing open-source development!

- 💳 **Buy Me a Coffee (UPI)**: `9617466846` / `+91-9617466846`

---

## ⚡ Quick Download

Download the official pre-compiled Android release package directly from this repository:

👉 **[📥 Download FocusLock.apk (Direct Release)](FocusLock.apk)**

---

## 🚀 Why Choose FocusLock? (Feature Comparison)

| Feature | FocusLock | Other Free App Blockers | Paid Blockers (Opal / Freedom / One Sec) |
| :--- | :---: | :---: | :---: |
| **100% Free & Open-Source** | ✅ Yes | ❌ Limited Free Tier | ❌ $5 - $15/month |
| **Zero Battery Drain (No VPN)** | ✅ Yes | ❌ High Battery Drain | ❌ Runs Background VPN |
| **Tamper-Proof Friend Passcode** | ✅ Yes | ❌ Easy to Bypass | ❌ Requires Subscription |
| **Website Blocker (Chrome & Brave)** | ✅ Yes | ❌ Premium Feature | ✅ Yes |
| **Daily App Usage Time Budgets** | ✅ Yes | ❌ Basic / Paid | ✅ Yes |
| **Un-cancelable Strict Focus Sessions** | ✅ Yes | ❌ Easy to Cancel | ✅ Yes |
| **100% Offline & Zero Data Tracking** | ✅ Yes (No Internet Perm) | ❌ Cloud / Analytics | ❌ Requires Account Creation |
| **Self-Protection (Anti-Uninstall)** | ✅ Yes (Device Admin Guard) | ❌ Easy to Uninstall | ❌ Partial |

---

## ✨ Key Features & Capabilities

### 📱 1. Social Media & App Blocker
Instantly block distracting apps like **Instagram, TikTok, YouTube, Reddit, X (Twitter), Facebook, Snapchat, and games**. Opening a blocked app immediately redirects you to a clean "🔒 Blocked by FocusLock" screen and sends you back home.

### 🌐 2. Website Blocker for Android Browsers (No VPN)
Block distracting websites (e.g., `youtube.com`, `reddit.com`, `instagram.com`, `twitter.com`, or any custom domain) directly inside **Google Chrome** and **Brave Browser**. Unlike other web blockers, FocusLock **does not run a VPN** or slow down your internet speed.

### ⏱️ 3. Daily App Usage Time Budgets
Set daily screen time limits per app (e.g., max 30 minutes on Instagram per day). Daily usage tracks automatically and resets every local calendar midnight. Once your daily limit is exceeded, the app is blocked until tomorrow!

### 🎯 4. Strict Un-cancelable Focus Sessions
Need hours of uninterrupted deep work or study time? Start a **1 to 6-hour strict Focus Session**. During an active session, **unlocking is disabled** and the session **cannot be cancelled early**, even with the friend passcode.

### 🔑 5. Tamper-Proof Friend Passcode Protection
Most app blockers fail because you can easily unlock them during moments of weak willpower. FocusLock solves this by requiring a **Friend Passcode**. Passcodes are hashed irreversibly using salted SHA-256. Hand your phone to a trusted friend or family member to set a secret code that only they know!

> 💡 **Solo Setup Hack (No Friend Needed)**:  
> Don't have a friend nearby? You can set up protection completely by yourself:
> 1. Generate a long, complex, random text string (e.g., `X9#kL2!mP8$vQ4`).
> 2. Paste it into FocusLock as your secret passcode.
> 3. Save the string in a hard-to-reach location (like a scheduled email to yourself, a locked physical drawer, or on paper in another room) without memorizing it.  
> Since you can't remember the complex string, you won't be able to bypass your own lock during moments of impulse!

### 🛡️ 6. Anti-Bypass & Self-Protection (Device Admin Guard)
FocusLock includes a smart **Settings Guard** powered by Android Device Administrator permissions. While locked, FocusLock automatically blocks access to Android Settings screens that could be used to Force Stop, Clear Data/Cache, Disable Accessibility, or Uninstall the app.

### 🔋 7. Zero Battery Drain & 100% Private Architecture
- **0% Battery Impact**: Uses zero background workers, zero alarms, and no polling loops. The event listener only runs when you interact with specified blocked apps.
- **100% Offline & Private**: FocusLock explicitly **omits the `INTERNET` permission** (`android.permission.INTERNET`) from its code. Your data never leaves your device.

---

## 📥 Installation & 1-Minute Setup Guide

### Step 1: Download & Install the APK
1. Download **[FocusLock.apk](FocusLock.apk)** directly onto your Android device.
2. Open your File Manager or Browser Downloads and tap `FocusLock.apk`.
3. If prompted by Android, toggle **"Allow installation from this source"** or **"Install unknown apps"**.
4. Tap **Install** and open **FocusLock**.

### Step 2: Complete the 1-Minute Setup Checklist
When launching FocusLock for the first time, complete the 3 steps on the setup screen:

1. **Enable Accessibility Service**: Tap **Enable** $\rightarrow$ Select **FocusLock** under Installed Apps / Downloaded Apps $\rightarrow$ Toggle **ON**.
2. **Activate Device Admin**: Tap **Activate** $\rightarrow$ Tap **Activate this device admin app** on the Android system screen. *(Prevents force-stopping or uninstalling while locked)*.
3. **Set Friend Passcode**: Tap **Set Code** $\rightarrow$ Hand your phone to a trusted friend or partner to set a secret 4-to-12 digit PIN code (or use the **Solo Setup Hack** above).

Once setup is complete, all status lights turn green!

---

## 📖 How to Use FocusLock

### 🔒 Locking & Unlocking
- **Lock**: Tap **Lock** on the home screen to activate blocking immediately.
- **Unlock**: Tap **Unlock** and enter your friend's secret PIN code (or retrieve your saved random string) to disable protection.

### 💡 Solo Setup Hack (Setting Up Passcode Yourself)
If you are using FocusLock completely on your own without a friend:
1. **Create a Complex Passcode**: Generate or type a long, complex, random text string that you won't be able to memorize.
2. **Set It as Your Code**: Enter this random string when setting your FocusLock code.
3. **Hide the Code**: Store the string somewhere inaccessible during focus hours (e.g., inside a physical safe, written on paper in another room, or scheduled in an email to arrive tomorrow).  
This creates the exact same psychological barrier as having a friend set the code!

### 📋 Customizing Your Block List
Tap **Block list** under Manage:
- **Apps Tab**: Search through all installed apps on your device and toggle blocking ON/OFF.
- **Websites Tab**: Add custom website domain suffixes (e.g., `facebook.com`, `twitch.tv`, `netflix.com`).

### ⏳ Setting Daily Time Limits
Tap **Daily limits** under Manage:
- Select any app and choose allowed minutes per day (or set custom rules per day of the week).
- When the timer runs out, FocusLock automatically blocks the app until local midnight.

### ☕ Taking a Timed Break
Need brief access to a blocked app during the day?
1. Tap **Take a break** on the main screen.
2. Enter your secret PIN (or retrieve your stored string).
3. Select break duration (e.g., 15 minutes). FocusLock pauses blocking and automatically re-arms when time expires.

### 🗑️ Complete Removal & Uninstallation
Because FocusLock prevents unauthorized uninstallation while locked, use the built-in uninstall flow when you want to remove the app:
1. Open **FocusLock**.
2. Scroll to the bottom and tap **Remove FocusLock** (highlighted in red).
3. Enter your secret PIN code.
4. FocusLock cleanly deactivates Device Admin, resets storage, and opens the system uninstall prompt.

---

## ❓ Frequently Asked Questions (FAQ)

### Q: How much battery does FocusLock consume? Is the battery usage minimal?
**A:** FocusLock's battery usage is **virtually zero (0% battery drain)**. Unlike traditional app blockers that run continuous background services, polling timers, or local VPNs that keep your phone's CPU awake, FocusLock uses an event-driven architecture powered by Android's native Accessibility API. It stays completely dormant and consumes **0 CPU cycles** until a watched app or browser window opens. All blocking decisions take less than 1 millisecond in memory, making battery consumption completely imperceptible on your device.

### Q: Is FocusLock completely free? Are there any hidden subscriptions?
**A:** Yes, FocusLock is 100% free and open-source. There are no paid tiers, ads, tracking, or monthly subscriptions.

### Q: How does website blocking work in Chrome without a VPN?
**A:** FocusLock reads the browser's URL address bar text via Android's native Accessibility API in real-time. When a blocked domain (like `youtube.com`) is detected, FocusLock immediately sends you back to your home screen. This requires no network redirection, no VPN, and zero battery overhead.

### Q: Does FocusLock track my browsing history or personal data?
**A:** **No, absolutely not.** FocusLock contains **no internet permission** (`android.permission.INTERNET`). It is physically impossible for the app to send data outside your phone. All URL checks and app checks happen in-memory locally on your device.

### Q: What prevents me from disabling Accessibility or uninstalling FocusLock?
**A:** When locked, FocusLock's **Settings Guard** inspects system screens in real time. If you open Android Settings to force-stop, clear cache, disable Accessibility, or deactivate Device Admin for FocusLock, the guard immediately closes Settings and sends you home. The only way to remove protection is by entering the friend passcode inside FocusLock.

### Q: What happens if I forget my friend passcode?
**A:** FocusLock passcodes are designed to enforce self-control. If you forget your code, your friend who created it (or your saved backup string) must be used. Debug builds include an optional dev bypass for developers testing on emulators.

---

## 🛠️ Developer Setup & Building from Source

FocusLock is built with modern Android engineering standards:
- **Language**: 100% Kotlin
- **UI Toolkit**: Jetpack Compose + Material 3 Theme
- **Architecture**: Single-activity, manual dependency injection (`AppContainer`), pure Kotlin decision engines (`BlockDecider`, `HostMatcher`, `SettingsGuard`).
- **Dependencies**: Minimal (Compose BOM, Activity Compose, Lifecycle ViewModel, Core KTX). No Room, no DataStore, no Hilt, no Koin, no third-party network libraries.

### Prerequisites
- Android Studio 2024.2+ (Ladybug or newer)
- JDK 11+
- Android SDK (compileSdk 37, minSdk 30)

### Clone & Build
```bash
# Clone the repository
git clone https://github.com/brickcommander/FocusLock.git
cd FocusLock

# Build release APK
./gradlew app:assembleRelease

# Run unit tests
./gradlew test
```
The generated APK file will be at `app/build/outputs/apk/release/app-release.apk`.

---

## 🏷️ Search Keywords & Topics
`android-app-blocker` `website-blocker` `digital-detox` `screen-time-limits` `focus-app` `productivity-tool` `open-source-android-app` `social-media-blocker` `chrome-site-blocker` `jetpack-compose` `android-accessibility-service` `no-vpn` `zero-battery-drain` `friend-passcode` `anti-uninstall`

---

## 📜 License

FocusLock is open-source software provided under the [MIT License](LICENSE) for personal productivity and self-improvement.
