# TikTok Enhancer

A Vector framework module for TikTok and Douyin that bypasses regional geofencing, strips watermarks from video and story downloads, blocks feed advertisements, uncaps playback bitrate, and prevents 4K upload downscaling.

---

## Compatibility

| Target Application | Package Identifier | Verified Version |
| :--- | :--- | :--- |
| **TikTok Global** | `com.zhiliaoapp.musically` | v46.9.3 – v47.1.3 |
| **TikTok Asia** | `com.ss.android.ugc.trill` | v46.9.3 – v47.1.3 |
| **Douyin** | `com.ss.android.ugc.aweme` | v40.6.0+ |

### Framework Prerequisites
- Android 8.0 – 15 (API 26 – 35)
- Root environment: KernelSU, Magisk (Zygisk enabled), or APatch
- Framework provider: **[Vector](https://github.com/JingMatrix/Vector)** (Zygisk)

---

## Technical Capabilities

### Geolocation and Carrier Spoofing
TikTok detects regional origin across several redundant client layers beyond simple IP lookup. This module overrides those telemetry vectors in-memory:
- **Telephony Hooks**: Spoofs `TelephonyManager` methods (`getSimCountryIso`, `getNetworkCountryIso`, `getSimOperator`, `getNetworkOperator`, `getSimOperatorName`). Replaces cellular cell tower IDs (`CellLocation`, `CellInfo`) and MCC/MNC identifiers with presets matching the selected region.
- **System Layer**: Intercepts `TimeZone.getDefault()` and network location providers to match the spoofed territory without system-wide modifications.
- **Network Parameter Map**: Hooks TTNet Cronet client parameter generators (`carrier_region`, `sys_region`, `account_region`, `store_region`, `sim_region`, `mcc_mnc`), injecting the designated ISO country code into outgoing API query strings and headers.

### Download Restriction and Watermark Removal
- **Direct Stream Redirection**: Intercepts `Aweme.getVideo()` and internal getter methods (`getDownloadAddr`, `getDownloadNoWatermarkAddr`, `getUIAlikeDownloadAddr`). Replaces downscaled or stamped endpoints with clean, source-resolution TopObjectStorage (TOS) `playAddr` streams.
- **Download Enforcement**: Overrides ACL share permissions (`awemeACLShareInfo.setTranscode(1)`, `awemeControl.setCanShare(true)`, `videoControl.allowDownload = true`), re-enabling save options on videos with creator-disabled downloads.
- **24-Hour Stories & Photo Slides**: Hooks `UserStory.getStories()` and `Aweme.getPhotoModeImageInfo()`, appending a native download option directly inside the system share sheet.
- **Bounded Identity Tracking**: Uses `BoundedIdentitySet` (ring-buffered `IdentityHashMap`) to track processed media objects strictly by pointer equality (`==`), avoiding recursive `equals()` execution on obfuscated models during continuous scrolling.

### Feed Sanitization and Nearby Tab Removal
- **Ad Suppression**: Filters incoming responses in `FeedItemList.getItems()`, `getAwemeList()`, and fragment panels (`RecommendFeedFragmentPanel`, `FollowFeedFragmentPanelMT`, `RepostFeedPanel`). Strips commercial promotion cards, sponsored video nodes, and anchor links before the list binds to the RecyclerView.
- **Country and Language Filtering**: Drops videos originating from user-blacklisted territories or languages based on metadata extracted from author profiles and audio tags. Profiles manually visited by the user bypass this filter.
- **Nearby Tab Elimination**: Drops the "Nearby" / "Places" tab using a four-layer hook strategy: removes the tab indicator from `TabLayout`, drops the tab from `TabListProvider`, cleans response nodes in payload schemas, and strips location parameters from feed requests.
- **For You Page Cold Start**: Hooks main activity lifecycle callbacks to land directly on the "For You" feed when the app opens, preventing default landing on auxiliary commercial tabs.

### Media Quality and Upload Uncapping
- **Playback Bitrate Uncapper**: Intercepts `SimVideoUrlModel.getBitRate()` and `VideoUrlModel` resolution tiers, bypassing default mobile bandwidth clamping to deliver maximum bitrate streams.
- **VESDK 4K Upload Preservation**: Overrides ByteBench hardware profiling and VESDK encode configurations (`VEVideoEncodeSettings`), preventing client-side downscaling of 3840x2160 source files during import and synthesis.
- **Creator Country Indicator**: Attaches a national flag indicator badge adjacent to author profile avatars in the feed interface.
- **Telemetry HUD**: Displays real-time ingestion metadata (resolution, frame rate, container bitrate, transcode codec, and origin CDN host).

---

## Installation

1. Install the APK on your device:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
2. Open **Vector Manager**, navigate to the **Modules** tab, and toggle **TikTok Enhancer**.
3. Under module scope, check the target packages (`com.zhiliaoapp.musically`, `com.ss.android.ugc.trill`, or `com.ss.android.ugc.aweme`).
4. Launch **TikTok Enhancer** from your application drawer.
5. Select your target country preset or input custom carrier values, enable desired filters, and apply changes.
6. Force-close and reopen TikTok to load the hooked environment.

---

## Build from Source

### Prerequisites
- JDK 17 or higher
- Android SDK Build-Tools 34.0.0
- Gradle 8.14+ (wrapper included)

### Compilation Steps
```bash
git clone https://github.com/xihzs/TiktokEnhancer.git
cd TiktokEnhancer
./gradlew assembleDebug
```

The output file will be generated at:
```text
app/build/outputs/apk/debug/app-debug.apk
```

---

## Architecture

The project is structured into functional hook controllers:

```text
app/src/main/java/com/ash/tiktokregion/
├── MainHook.java                  # Vector module entry point & TTNet Cronet hooks
├── TelephonyAndSystemHook.java    # SIM, TelephonyManager, CellLocation, and TimeZone spoofing
├── WatermarkHook.java             # TOS clean stream substitution & story download injection
├── DouyinWatermarkHook.java       # Douyin v40+ watermark removal & permission bypass
├── AdsHook.java                   # FeedItemList filter, commercial ad suppression, blacklists
├── NearbyTabHook.java             # Quad-layer Nearby tab elimination
├── FeedLandingHook.java           # Cold-start For You landing enforcer
├── QualityAndTelemetryHook.java   # VESDK playback quality uncapper & telemetry HUD
├── HDUploadHook.java              # 4K master upload ByteBench resolution bypass
├── MediaDownloader.java           # Background parallel download worker using DownloadManager
├── ConfigProvider.java            # Encrypted SharedPreferences IPC bridge via ContentProvider
└── MainActivity.java              # Material settings interface & profile management
```

---

## License

This project is distributed under the GNU General Public License v3.0. Refer to [LICENSE](LICENSE) for details.
