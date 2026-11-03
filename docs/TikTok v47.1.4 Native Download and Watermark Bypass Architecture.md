# TikTok v47.1.4 Native Download & Watermark Bypass Architecture

## Overview & Context
In TikTok v47.1.4 (`com.zhiliaoapp.musically`, version code `2024701040`), video, story, and photo mode downloads experienced two critical failures:
1. **Disabled / Hidden Download Option**: Native "Save video" and "Save photo" action tiles were missing, greyed out, or blocked with author restriction modals/toasts.
2. **Download Failure / Transcoding Hang**: When triggered (either via native sheet action or injected action button), downloads failed silently, threw error codes (e.g., `-114`), or hung indefinitely without saving the clean MP4 file to storage.

This document details the reverse engineering findings across Dalvik bytecode (`classes2.dex` through `classes42.dex`), root causes, and the architectural fix implemented in `WatermarkHook.java`.

---

## Related Knowledge Graph Links
- [[TikTok 47.1.4 Migration to Rust Native Core]]
- [[TikTok Enhancer - FYP Highest Resolution, Non-HD Filter, Battery and UI Overhaul]]
- [[TikTok Enhancer Rust Native Core]]
- [[TikTok Enhancer - Advanced Feature Roadmap and Hook Mechanics]]
- [[TikTok DM Anti-Delete Message Architecture]]

---

## 1. Deep Root Cause Analysis

### Failure Mode 1: WaterMarkListener Signature Mismatch (Fatal Transcode Crash)
When TikTok initiates a video download:
1. It downloads the video stream to an internal cache location (`inputPath`).
2. It passes a `WaterMarkBuilder` into `WaterMarkServiceImpl.waterMark(waterMarkBuilder)`.
3. The original module hook in `WatermarkHook.java` attempted to bypass native transcoding by copying `inputPath` directly to `outPath` and calling:
   ```java
   Method onSuccess = listener.getClass().getMethod("onSuccess");
   onSuccess.invoke(listener);
   param.setResult(null);
   ```
4. **Bytecode Inspection in `classes42.dex` (`com.ss.android.ugc.aweme.services.watermark.WaterMarkListener`)**:
   ```java
   public interface WaterMarkListener {
       void onCancel();
       void onError(int i);
       void onProgress(int i);
       void onStart();
       void onSuccess(String str); // Takes output file path argument!
   }
   ```
5. Because `getMethod("onSuccess")` (0 arguments) was called, Java threw `NoSuchMethodException`.
6. The catch block swallowed the exception, `param.setResult(null)` was never called, and TikTok proceeded into `WaterMarkServiceImpl.LIZ().waterMark(waterMarkBuilder)`.
7. `WaterMarkServiceImpl` passed the clean/non-watermarked stream into the native C++ engine (`libwatermark.so`), which failed with error `-114` or hung due to codec/stream mismatches, failing the download completely.

### Failure Mode 2: Missing TikTok v47.1.4 Permission & Control Models
Modern TikTok has deprecated legacy `AwemeControl.isCanDownload`:
- `AwemeControl` now only controls `canShare`, `canForward`, `canComment`, and `canShowComment`.
- Download permission gating has migrated into four separate models:
  1. `com.ss.android.ugc.aweme.feed.model.VideoControl` (`classes4.dex`):
     - `allowDownload` (`Boolean`): must be `Boolean.TRUE`.
     - `preventDownloadType` (`int`): must be `0`.
  2. `com.ss.android.ugc.aweme.feed.model.AwemeStatus` (`classes4.dex`):
     - `downloadStatus` (`int`): non-zero status blocks download. Must be `0`.
     - `isAllowShare` (`boolean`): must be `true`.
  3. `com.ss.android.ugc.aweme.profile.model.User` (`classes2.dex`):
     - `downloadSetting` (`int`): author permission gate. Must be `0`.
     - `preventDownload` (`boolean`): must be `false`.
  4. `com.ss.android.ugc.aweme.feed.model.Aweme` (`classes4.dex` / `classes5.dex`):
     - `preventDownload` / `isPreventDownload()` / `getPreventDownload$common_model_release()`: must return `false`.
     - `getIsCommentPostVideo()`: must return `false` (otherwise blocked on comment replies).

### Failure Mode 3: Share Sheet Action Assembly & Gatekeepers (`AbstractC31810IIx`, `0VYO`, `ShareExtServiceImpl`)
Tracing the share sheet click action in `X.0IIx` (`AbstractC31810IIx.java`):
```java
00kn.LJJIZ("download_method", str, sharePackage.extras);
if (!06Gp.LIZIZ.LJIJI(context, aweme, str)) {
    return;
}
if (!06Gp.LIZIZ.LJJZ(context, activity, r2, new 0Vzi(this))) {
    return;
}
LJIIJJI(activity, r2);
```
- `06Gp.LIZIZ` is `ShareExtServiceImpl`.
- `LJIJI(context, aweme, str)` delegates to `X.0VYO.LIZ(context, aweme, str)`, which checks age-gating (`09YH.LIZIZ()`), SD card capacity, and network constraints. If `LJIJI` returns `false`, execution terminates immediately.
- `LJJZ(context, activity, r15, r16)` checks `C391519ae.LIZ(aweme)`. If `popupMsg` or `toastMsg` are non-empty or if `code != 0`, it opens a modal or toast and aborts download.
- In `X.0IIy` (`C31820IIy.java`), `enable()` checks:
  ```java
  ACLCommonShare downloadGeneral;
  AwemeACLShare awemeACLShareInfo = this.LLJJIJI.getAwemeACLShareInfo();
  return (awemeACLShareInfo == null || (downloadGeneral = awemeACLShareInfo.getDownloadGeneral()) == null 
      || downloadGeneral.getShowType() != 2 
      || !AwemeExtKt.isSharedStoryVisible(this.LLJJIJI) 
      || this.LLJJIJI.getIsCommentPostVideo()) ? false : true;
  ```
  `downloadGeneral.getShowType()` must equal `2` (`SHOW_TYPE_NORMAL`), `isSharedStoryVisible` must be `true`, and `getIsCommentPostVideo` must be `false`.

### Failure Mode 4: Photo Mode URL Resolution
Modern TikTok photo stories use `PhotoModeImageInfo` (`classes6.dex`) containing `PhotoModeImageUrlModel` (`classes7.dex`).
The high-resolution clean image is stored in `displayImageNoWatermark` (annotated as `@SerializedName("display_image")`), with fallbacks in `targetMultiRateImageUrl` and `dynamicImage`.

---

## 2. Architectural Solution

### Layer A: Resilient WaterMarkListener Reflection & Bypass
In `WatermarkHook.hookWatermarkService`:
- Disable all watermark overlay flags on `WaterMarkBuilder`:
  `setAddEndMark(false)`, `setAddInterMark(false)`, `forceAIGCWatermark = false`, `isAddAiChatWatermark = false`, `isAddAiLiveWatermark = false`, `containC2PA = false`.
- Perform zero-copy transfer (`FileChannel.transferTo`) from `inputPath` to `outPath`.
- Dynamically detect and invoke `onSuccess(String)` with `outPath`, falling back to `onSuccess()` for backward compatibility:
  ```java
  boolean invoked = false;
  for (Method m : listener.getClass().getMethods()) {
      if ("onSuccess".equals(m.getName())) {
          Class<?>[] pTypes = m.getParameterTypes();
          if (pTypes.length == 1 && pTypes[0] == String.class) {
              m.invoke(listener, outPath);
              invoked = true;
              break;
          } else if (pTypes.length == 0) {
              m.invoke(listener);
              invoked = true;
              break;
          }
      }
  }
  if (invoked) {
      param.setResult(null);
      return;
  }
  ```
- Hook photo watermarking methods (`addAIPortraitWatermarkToImage`, `addAITextImageWaterMarkToImage`, `addAiChatWaterMarkToImage`, `readAIGCAddAIPortraitWatermarkForSingleImage`) to directly copy source image to destination.

### Layer B: Multi-Model Download Restriction Defeat
In `WatermarkHook.hookAwemeDownloadFlags` and `hookDownloadRestrictions`:
- **`Aweme`**:
  - `getDownloadWithoutWatermark()` -> `true`
  - `isPreventDownload()` -> `false`
  - `getPreventDownload()` -> `false`
  - `getPreventDownload$common_model_release()` -> `false`
  - `needTTSWatermarkWhenDownload()` -> `false`
  - `getIsCommentPostVideo()` -> `false`
- **`VideoControl`**:
  - Intercept `Aweme.getVideoControl()`: ensure non-null instance with `allowDownload = Boolean.TRUE`, `preventDownloadType = 0`, `isAllowDuet = true`, `isAllowDynamicWallpaper = true`, `isAllowReact = true`.
- **`AwemeStatus`**:
  - `getDownloadStatus()` -> `0`
  - `isAllowShare()` -> `true`
- **`User`**:
  - `getDownloadSetting()` -> `0`
  - `isPreventDownload()` -> `false`
  - `isPostDefaultDownloadSetting()` -> `true`
- **`AwemeControl`**:
  - `canShare()` -> `true`
  - `canForward()` -> `true`

### Layer C: Share Sheet Gatekeeper & Action Interception
In `WatermarkHook.hookACLShare`:
- **`ACLCommonShare`**:
  - `getCode()` -> `0`
  - `getShowType()` -> `2` (`SHOW_TYPE_NORMAL`)
  - `getTranscode()` -> `1` (`TRANSCODE_NO_WATERMARK`)
  - `getMute()` -> `false`
  - `getPopupMsg()` -> `""`
  - `getToastMsg()` -> `""`
- **`AwemeACLShare`**:
  - Guarantee non-null `downloadGeneral`, `downloadMaskPanel`, and `downloadSharePanel` with unlocked ACL attributes.
- **Pipeline Interceptors**:
  - `com.ss.android.ugc.aweme.share.ShareExtServiceImpl.LJIJI` -> `true`
  - `com.ss.android.ugc.aweme.share.ShareExtServiceImpl.LJJZ` -> `true`
  - `X.0VYO.LIZ` -> `true`
  - `X.19ae.LIZ` -> `0`
  - `X.02y7.LJIIIZ` -> `false`
  - `X.0IIy.enable` -> `true`
  - `X.0IIy.LJI` -> `false`
  - `X.0IJ3.enable` -> `true`
  - `X.0IJ3.LJIIL` -> `0`
  - `com.ss.android.ugc.aweme.feed.model.AwemeExtKt.isSharedStoryVisible` -> `true`
  - `com.ss.android.ugc.aweme.feed.model.AwemeExtKt.canShare` -> `true`

### Layer D: Video Model Stream Redirection & Address Resolution
In `WatermarkHook.hookVideoModel`:
- Redirect `getDownloadAddr`, `getNewDownloadAddr`, `getDownloadNoWatermarkAddr`, `getUIAlikeDownloadAddr`, and `getCaptionDownloadAddr` to the highest bitrate stream (`getHighestQualityPlayAddr` -> `getPlayAddr` -> `getPlayAddrH264` -> `getPlayAddrBytevc1` -> `sLastPlayedPlayAddr`).

### Layer E: Downloader Storage Resilience & save_path_security Interception
In `WatermarkHook.hookDownloaderStorage`:
- **`X.0Meh.LIZIZ(String)`**: Forces return `true`. Bypasses TikTok's internal `save_path_security` check so `socialbase.downloader` never rejects valid download or temporary target paths.
- **`X.0Meh.LIZJ(File)`**: Automatically creates directory hierarchy (`mkdirs()`) and validates directory existence before the downloader attempts to write chunks.
- **`X.0Meh.LIZ(File)`**: Guarantees fallback to `/storage/emulated/0/Download/TikTok` if the internal path calculation fails or returns empty.
- **`X.00kn.s2(Context, String)` & `X.00kn.r2(Context)`**:
  - Guards against `ContextImpl.getExternalFilesDirs` / `ContextImpl.getExternalCacheDirs` returning `null` due to Android 11+ `vold` preparation exceptions.
  - Automatically redirects to public download directories or app-internal files directory (`context.getFilesDir()` / `context.getCacheDir()`) if external paths fail.

---

## 3. Root Cause: "Download Stays at 0%" Analysis & Fix

### Trace Analysis
When initiating native download, the progress dialog remained frozen at `0%` before silently canceling. Logcat traces from `PID 425` and `PID 7602` revealed:
```text
W ContextImpl: Failed to ensure /storage/emulated/0/Android/data/com.zhiliaoapp.musically/cache
W ContextImpl: java.lang.IllegalStateException: Failed to prepare /storage/emulated/0/Android/data/com.zhiliaoapp.musically/cache
    at android.os.storage.IStorageManager$Stub$Proxy.mkdirs(IStorageManager.java:1305)
    at android.os.storage.StorageManager.mkdirs(StorageManager.java:1421)
    at android.app.ContextImpl.ensureExternalDirsExistOrFilter(ContextImpl.java:3931)
    at android.app.ContextImpl.getExternalCacheDirs(ContextImpl.java:999)
    at android.content.ContextWrapper.getExternalCacheDir(ContextWrapper.java:338)
    at X.00kn.r2(SourceFile:16777228)
    at X.0Meh.LIZIZ(SourceFile:16777262)
    at com.ss.android.socialbase.downloader.model.DownloadInfo.<init>(SourceFile:16777332)
```

### The Breakdown:
1. **Missing External Package Directory**:
   On devices running scoped storage (Android 11+ / HyperOS), `/data/media/0/Android/data/com.zhiliaoapp.musically` did not exist on fresh or modified environments.
2. **Framework Nulling**:
   When `ContextImpl.ensureExternalDirsExistOrFilter` encountered the `IllegalStateException` from `vold`/`StorageManagerService.mkdirs()`, it caught the exception and set the target directory to `null`.
3. **Downloader Rejection**:
   `C22110Meh.LIZIZ` checked `00kn.r2(appContext)` (external cache dir). Because it returned `null`, `LIZIZ` returned `false`. `DownloadInfo` could not initialize the temp/save paths, causing `DownloadTask.downloadImpl()` to abort, triggering `1NBa.onCanceled()` and leaving the UI stuck at `0%`.

### Complete Resolution:
1. Created `/data/media/0/Android/data/com.zhiliaoapp.musically/{files/Download,cache}` with ownership `media_rw:media_rw` (1023:1023) and permissions `777`.
2. Implemented `hookDownloaderStorage` in `WatermarkHook.java` to hook `0Meh.LIZIZ`, `0Meh.LIZJ`, `0Meh.LIZ`, `00kn.s2`, and `00kn.r2` to provide seamless, crash-free fallbacks for all current and future runs.

---

## 4. Verification & Build Proof
- **Build Toolchain**: JDK 17 (`/home/ash/.toolchain/jdk-17.0.20.1+1`).
- **Compilation**: `JAVA_HOME=/home/ash/.toolchain/jdk-17.0.20.1+1 ./gradlew assembleDebug` passed with exit code 0 (`BUILD SUCCESSFUL in 2s`).
- **Target APK Verified**: `com.zhiliaoapp.musically_47.1.4-2024701040_minAPI23(arm64-v8a,armeabi-v7a)(nodpi)_apkmirror.com.apk`.
- **Runtime Execution**: Tested live on connected device (`cdddbe0f`). Downloads proceed seamlessly past 0% to completion without watermarks.

