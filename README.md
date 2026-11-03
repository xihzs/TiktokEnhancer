# Tiktok Enhancer - Xposed / LSPosed Module

A clean, minimalist, and powerful LSPosed module designed to bypass TikTok's SIM card region detection, download stories and videos without watermarks, and eliminate sponsored feed ads. Freely change your TikTok content region across **60+ countries worldwide** (including the **United States**, **Malaysia**, **Japan**, **United Kingdom**, **Singapore**, and more), download **24-hour stories (videos and photo mode)** directly from the share sheet, save clean videos **without any watermark or outro**, and **force feeds to show only content from your target region**.

---

## Features & Capabilities

### 1. Download from 24h Stories (Videos & Photo Mode)
- **Native Share Sheet Integration**:
  - Automatically adds the standard **"Save video"** button directly to the bottom action row of the Share panel (alongside "Report"), styled identically to native TikTok actions.
  - Tapping **"Save video"** dismisses the sheet and immediately downloads the clean, unwatermarked story video or photos to `/Download/`.
- **Story Unlocking**:
  - Hooks `UserStory.getStories()` and `Aweme.getUserStory()` to unlock download permissions without disrupting the story playback lifecycle.
  - Automatically redirects story video streams to raw unwatermarked playback streams (`playAddr`) and bypasses client-side transcode.
  - Unlocks `ACLCommonShare` so native download options are also enabled across share sheets.

### 2. Lossless No-Watermark Downloads
- **Client-Side Transcoder Bypass**: Intercepts `WaterMarkServiceImpl.waterMark()` and `watermarkForTikTokNow()`. Copies clean downloaded video directly to destination, bypassing TikTok's local FFmpeg/VEWatermark re-encoding (no bouncing corner logo, no ending outro card).
- **Stream Redirection**: Directs `Video.getDownloadAddr()`, `Video.getNewDownloadAddr()`, and `Video.getDownloadNoWatermarkAddr()` to the clean playback stream (`getPlayAddr()`).
- **Stream URL Rewriting**: Rewrites watermarked `/playwm/` URLs to `/play/` in `UrlModel.getUrlList()`.
- **Download Restrictions**: Bypasses `Aweme.isPreventDownload()` and enables saving even when authors disable downloads.

### 3. Force Target Region Modes
- **Network-Level Injection**: Hooks TikTok's core TTNet / Cronet network stack (`ICronetAppProvider` and `AbsCronetDependAdapter` methods: `getRegion()`, `getCarrierRegion()`, `getSysRegion()`, `getSimOperator()`, `getAppInitialRegionInfo()`) so all API queries automatically carry the target region parameters.
- **Smart Force Region (New)**: Inspects feed items and filters out videos from outside your chosen country on the For You feed, with built-in starvation fallback so the feed never hangs with network errors.
- **Strict Force Region (Previous)**: Strictly discards all non-region videos across feeds without fallback (legacy behavior).

### 4. Block Content & Songs from Selected Countries
- **Multi-Country Blacklist**: Select one or more countries to filter out from your feeds.
- **Deep Multi-Vector Detection**: Inspects video region tags (`aweme.getRegion()`), author region tags (`author.getRegion()`), author ISO country codes (`author.getIsoCountryCode()`), account regions (`author.getAccountRegion()`, `author.getLemon8StoreRegion()`), full country names (with automatic mapping to 2-letter ISO), author bio location, geofencing regions, and POI address details (`poi.addressInfo.regionCode`, `cityCode`, `cityName`, `address`, and `locationDesc`).
- **Block Country Songs & Sounds**: When enabled, eliminates audio originating from blacklisted regions:
  - Original sounds created by creators from blocked countries.
  - Sounds titled with localized original sound phrases (e.g., "suara asli" for Indonesia, "bunyi asal" for Malaysia, "âm thanh gốc" for Vietnam, "เสียงต้นฉบับ" for Thailand, "оригинальный звук" for Russia, "オリジナル楽曲" for Japan, "오리지널 사운드" for Korea, "原声" for China/Taiwan, "صوت أصلي" for Arabic countries, "som original" for Brazil, etc.).
  - Songs and audio containing native script alphabets (Thai, Cyrillic, Japanese Kana, Korean Hangul, Arabic, Devanagari, Hebrew, Greek).
  - Region-specific demonyms, genres, and audio metadata in song titles, artist names, `matchedSongInfo`, `matchedPGCSoundInfo`, and music extra JSON.
- **Strict Starvation Protection**: Feed items and sounds from blacklisted countries are strictly barred from being restored during Smart Force Region buffer starvation fallback.
- **Early Deserialization Sanitization**: Hooks `FeedItemList.setItems(List)` to sanitize incoming feed lists immediately as network responses arrive.
- **Dynamic Country Search & Custom ISO**: Choose from 60+ countries worldwide with real-time search, or input custom 2-letter ISO codes (e.g., `cn`, `ru`, `ir`) with instant flag generation.
- **Batch Management**: One-tap "Clear All" and active country counters with immediate synchronization across IPC.

### 5. Comprehensive Ad & Commercial Blocker
- **Multi-Vector Ad Detection**:
  - `aweme.isAd() == true`
  - `aweme.getAwemeRawAd() != null`
  - `aweme.getAwemeType() == 2 || aweme.getAwemeType() == 101` (promotional cards & feed ads)
  - `aweme.withFakeUser() == true`
  - `aweme.isWithPromotionalMusic() == true`
- **Feed Sanitization**: Clears `feedItemList.hasAd`, sets `preloadAds` to `null`, and hooks both `getItems()` and `getAwemeList()` across feed services.

### 6. Global Carrier & SIM Spoofing (60+ Countries)
- Real-time searchable country & carrier picker.
- Hooks `TelephonyManager`:
  - `getSimCountryIso()`, `getNetworkCountryIso()`
  - `getSimOperator()`, `getNetworkOperator()`
  - `getSimOperatorName()`, `getNetworkOperatorName()`
  - `getSimState()` (forces `SIM_STATE_READY`)
  - `hasIccCard()` (forces `true`)
  - `getPhoneType()` (forces `PHONE_TYPE_GSM`)
- Hooks `SubscriptionManager` & `SubscriptionInfo`.
- Hooks `android.os.SystemProperties` (`gsm.sim.operator.iso-country`, etc.).
- Optional manual override for any custom country ISO and operator codes.

### 7. DexKit 2.3.0 Engine & Zero-JNI High-Performance Core
- **Native C++ DEX Parsing**:
  - Leverages DexKit 2.3.0 (`io.luckypray:dexkit`) to dynamically scan and resolve obfuscated classes and methods across TikTok updates.
  - No fragile hardcoded ProGuard class names; automatically matches methods by signatures, parameter types, and opcode patterns.
- **Zero-JNI Overhead & Buttery Smooth Scrolling**:
  - Eliminated JNI boundary transitions and GC allocations on feed scrolling. Feed evaluation, URL cleaning, and country presets run directly on ART at native speed.
  - No cargo-ndk, Rust toolchain, or native `.so` builds required. Builds in under 1 second.

### 8. Instant Auto-Restart on Setting Changes
- **Zero Manual Effort**:
  - Whenever any setting, switch, country preset, custom carrier code, or blacklist is updated, the module automatically forces TikTok to reload.
  - Debounced root restart silently stops TikTok (`am force-stop`) and relaunches it seamlessly with updated hooks.
  - Instant tactile feedback notification confirms settings applied without leaving the app.

### 9. Force Highest Resolution Playback & Downloads
- **Maximum Bitrate Streaming**:
  - Intercepts `Video.getBitRate()` and dynamically sorts bitrate profiles descending by bitrate.
  - Hooks `getPlayAddr()`, `getProperPlayAddr()`, `getPlayAddrH264()`, and `getPlayAddrBytevc1()` to extract and stream the absolute highest resolution stream (1080p / 720p 60fps).
  - Unwatermarked downloads automatically capture the highest quality stream available.

### 10. FYP Non-HD Video Filter & Zero-Jank Performance
- **Feed Quality Filtering**:
  - Inspects Aweme video dimensions and bitrates on the FYP (For You Page) feed.
  - Drops sub-720p low-resolution content (540p, 480p, 360p) with auto-starvation safety fallback.
- **Battery Drain & Micro-Stutter Elimination**:
  - Eliminated global `WeakHashMap` synchronized monitor contention across video feed loops.
  - Cached reflection field writes on `Video` models with single-pass flags.
  - Expanded ContentProvider IPC cache window to 30s to suppress background Binder transaction storms during fast scrolling.

---

## Project Structure

```
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml          # Package queries, module declaration & scope
│   │   ├── assets/
│   │   │   └── xposed_init              # Points to com.ash.tiktokregion.MainHook
│   │   ├── java/com/ash/tiktokregion/
│   │   │   ├── MainHook.java            # Clean coordinator & Xposed entry point
│   │   │   ├── MainActivity.java        # Clean dark iOS companion interface with auto-restart
│   │   │   ├── ConfigProvider.java      # ContentProvider IPC & heartbeat tracking
│   │   │   ├── CountryPreset.java       # 60+ country presets & in-memory search
│   │   │   ├── CountryGridAdapter.java  # Preset picker grid adapter
│   │   │   ├── BlockedCountryGridAdapter.java # Multi-select grid adapter for blacklisted countries
│   │   │   ├── core/
│   │   │   │   ├── DexKitManager.java   # DexKit 2.3.0 bridge & method finder cache
│   │   │   │   ├── ModuleConfig.java    # Thread-safe in-memory configuration store
│   │   │   │   └── BaseHook.java        # Standard hook contract interface
│   │   │   └── hooks/
│   │   │       ├── network/
│   │   │       │   ├── TTNetHook.java   # ICronetAppProvider & TTNet parameter injection
│   │   │       │   └── UrlSanitizer.java# Pure Java URL cleaner (/playwm/ -> /play/)
│   │   │       ├── telephony/
│   │   │       │   ├── TelephonyHook.java  # TelephonyManager & SubscriptionManager spoofing
│   │   │       │   └── SystemPropHook.java # SystemProperties GSM operator spoofing
│   │   │       ├── feed/
│   │   │       │   ├── FeedFilterHook.java # FeedItemList ad and region filtering
│   │   │       │   └── AwemeEvaluator.java # Zero-JNI feed item evaluation
│   │   │       ├── media/
│   │   │       │   ├── WatermarkBypassHook.java # Watermark transcode bypass & stream redirect
│   │   │       │   ├── StoryDownloadHook.java   # 24h story tracking & share sheet "Save Story"
│   │   │       │   ├── VideoQualityHook.java    # Dynamic 1080p bitrate sorting
│   │   │       │   └── MediaDownloader.java     # DownloadManager & storage helper
│   │   │       ├── social/
│   │   │       │   ├── GhostModeHook.java       # Anonymous story & profile viewing
│   │   │       │   ├── AntiDeleteHook.java      # DM recall interceptor & disk cache
│   │   │       │   └── ProfileBannerHook.java   # Profile background banner unlocker
│   │   │       └── ui/
│   │   │           ├── PopupHook.java           # In-app popup & update dialog suppressor
│   │   │           └── MasterUploadHook.java    # 4K master upload uncapper
│   │   └── res/                         # iOS-inspired dark grouped theme, layouts, icons
│   ├── build.gradle                     # App Gradle build configuration (with DexKit 2.3.0)
│   └── proguard-rules.pro               # ProGuard keep rules for Xposed entry points
├── build.gradle                         # Root Gradle config
├── settings.gradle                      # Project repositories
├── build.sh                             # One-command build & install script
├── gradle.properties
├── local.properties
└── README.md
```

---

## Building the Module

### One-Command Full Build (DexKit 2.3.0 APK)

```bash
# Builds the Debug APK
./build.sh

# Or build the Release APK
./build.sh --release

# Or build both Debug and Release APKs
./build.sh --all

# Or compile and install directly to a connected device via ADB
./build.sh --install
```

The compiled APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release-unsigned.apk
```

---

## Verified Compatibility

- **Target App**: TikTok (`com.zhiliaoapp.musically`)
- **Verified Versions**: **v47.6.3** (Build `2024706030`) & **v47.1.4** (Build `2024701040`, `arm64-v8a` & `armeabi-v7a`)
- **Hook Integrity**: All TTNet Cronet providers, feed item models, watermark services, direct message anti-delete, ghost mode, and story hooks verified directly against v47.6.3 and v47.1.4 DEX bytecode.

---

## Installation & Setup

1. **Install the APK on your Android device**:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

2. **Activate in LSPosed**:
   - Open **LSPosed Manager** -> **Modules** -> Enable **Tiktok Enhancer**.
   - Ensure **TikTok** (`com.zhiliaoapp.musically`) is checked in scope.

3. **Configure Your Settings**:
   - Open **Tiktok Enhancer**.
   - Choose your preferred target country preset or toggle options.
   - **TikTok automatically restarts on every change**: no manual restarts required!
