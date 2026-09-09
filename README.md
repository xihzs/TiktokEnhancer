# Tiktok Enhancer

A clean, modern Vector framework module for TikTok Global, TikTok Asia, and Douyin.

Bypasses SIM card region restrictions, eliminates sponsored feed ads, unlocks unrestricted downloads, and downloads stories and videos without watermarks.

---

## Prerequisites

This module requires **[Vector](https://github.com/JingMatrix/Vector)** (the modern successor/rebrand of LSPosed) installed via Magisk, KernelSU, or APatch.

---

## Supported Applications

- **TikTok Global** (`com.zhiliaoapp.musically`)
- **TikTok Asia** (`com.ss.android.ugc.trill`)
- **Douyin / TikTok China** (`com.ss.android.ugc.aweme`)

The in-app profile menu automatically detects installed packages and only displays apps present on your device.

---

## Features

- **Region & Carrier Changer**: Freely switch feeds across 60+ countries worldwide or enter custom ISO / carrier codes.
- **Force Region Filtering**: Smart feed filtering to enforce content from your selected country.
- **Block Countries**: Filter out videos and creators from selected blacklisted countries.
- **Download 24h Stories**: Download story videos and photo mode posts directly from the native share sheet.
- **Watermark-Free Downloads**: Direct stream redirection and transcode bypass for clean, full-quality video saves without outro cards.
- **Ad & Commercial Blocker**: Eliminates commercial video ads, promoted posts, and marketing cards.
- **No-Nav UI**: Clean single-page interface with a hamburger profile switcher and zero navigation bloat.

---

## Installation & Setup

1. **Install Vector**:
   - Download and install the latest release of **[Vector](https://github.com/JingMatrix/Vector)**.
2. **Install Tiktok Enhancer**:
   - Download the latest APK from Releases (or build from source) and install it on your device:
     ```bash
     adb install -r app/build/outputs/apk/debug/app-debug.apk
     ```
3. **Enable in Vector Manager**:
   - Open **Vector Manager** → **Modules** → Enable **Tiktok Enhancer**.
   - Under Scope, check the box for your installed app (**TikTok**, **TikTok Asia**, and/or **Douyin**).
4. **Configure Settings**:
   - Open **Tiktok Enhancer** from your launcher or directly from Vector Manager.
   - Tap the hamburger menu in the top app bar to switch active profiles.
   - Choose your target region and enable your preferred features.
   - Tap **Restart TikTok** (or **Restart Douyin**) to apply changes immediately.

---

## Building from Source

Prerequisites: JDK 17+ and Android SDK.

```bash
git clone https://github.com/xihzs/TiktokEnhancer.git
cd TiktokEnhancer
./gradlew assembleDebug
```

Compiled APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```
