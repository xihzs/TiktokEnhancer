package com.ash.tiktokregion;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.ash.tiktokregion.core.DexKitManager;
import com.ash.tiktokregion.core.ModuleConfig;
import com.ash.tiktokregion.hooks.feed.FeedFilterHook;
import com.ash.tiktokregion.hooks.media.StoryDownloadHook;
import com.ash.tiktokregion.hooks.media.VideoQualityHook;
import com.ash.tiktokregion.hooks.media.WatermarkBypassHook;
import com.ash.tiktokregion.hooks.network.TTNetHook;
import com.ash.tiktokregion.hooks.social.AntiDeleteHook;
import com.ash.tiktokregion.hooks.social.GhostModeHook;
import com.ash.tiktokregion.hooks.telephony.SystemPropHook;
import com.ash.tiktokregion.hooks.telephony.TelephonyHook;
import com.ash.tiktokregion.hooks.ui.MasterUploadHook;
import com.ash.tiktokregion.hooks.ui.PopupHook;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Clean coordinator for TikTok Enhancer.
 * Dispatches to modular feature packages and coordinates DexKit 2.3.0 lifecycle.
 */
public class MainHook implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    private static final String TAG = "TikTokRegion";
    public static final String PACKAGE_NAME = "com.ash.tiktokregion";

    public static final List<String> TARGET_PACKAGES = Arrays.asList(
            "com.zhiliaoapp.musically",
            "com.ss.android.ugc.trill",
            "com.zhiliaoapp.musically.go",
            "com.tiktok.business"
    );

    private static volatile Context sAppContext = null;
    private static volatile long sLastConfigFetchTime = 0;
    private static final long CONFIG_CACHE_MS = 1_500L;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        XposedBridge.log(TAG + ": Initializing in Zygote (DexKit 2.3.0 Engine)");
        new TelephonyHook().hook(MainHook.class.getClassLoader());
        new SystemPropHook().hook(MainHook.class.getClassLoader());
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (PACKAGE_NAME.equals(lpparam.packageName)) {
            hookSelfStatus(lpparam.classLoader);
            return;
        }

        if ("android".equals(lpparam.packageName)) {
            new TelephonyHook().hook(lpparam.classLoader);
            new SystemPropHook().hook(lpparam.classLoader);
            return;
        }

        if (!TARGET_PACKAGES.contains(lpparam.packageName)) {
            return;
        }

        XposedBridge.log(TAG + ": Target TikTok detected -> " + lpparam.packageName + " (" + lpparam.processName + ")");

        // 1. Initialize DexKit 2.3.0 native C++ dex-parser
        if (lpparam.appInfo != null && lpparam.appInfo.sourceDir != null) {
            DexKitManager.init(lpparam.appInfo.sourceDir);
        }

        // 2. Application lifecycle hook for Context and IPC config loading (attachBaseContext + onCreate)
        hookApplicationLifecycle(lpparam);

        // 3. Telephony and System Properties hooks inside TikTok process
        new TelephonyHook().hook(lpparam.classLoader);
        new SystemPropHook().hook(lpparam.classLoader);

        // 4. Initialize Modular Feature Hooks
        new TTNetHook().hook(lpparam.classLoader);
        new FeedFilterHook().hook(lpparam.classLoader);
        new WatermarkBypassHook().hook(lpparam.classLoader);
        new VideoQualityHook().hook(lpparam.classLoader);
        new StoryDownloadHook().hook(lpparam.classLoader);
        new GhostModeHook().hook(lpparam.classLoader);
        new AntiDeleteHook().hook(lpparam.classLoader);
        new PopupHook().hook(lpparam.classLoader);
        new MasterUploadHook().hook(lpparam.classLoader);
    }

    private void hookSelfStatus(ClassLoader classLoader) {
        try {
            Class<?> mainActivityClass = XposedHelpers.findClassIfExists(
                    PACKAGE_NAME + ".MainActivity", classLoader);
            if (mainActivityClass != null) {
                XposedHelpers.findAndHookMethod(mainActivityClass, "isModuleActive",
                        new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) {
                                param.setResult(true);
                            }
                        });
                XposedBridge.log(TAG + ": Hooked MainActivity.isModuleActive() -> true");
            }
        } catch (Throwable t) {
            Log.d(TAG, "Failed hooking isModuleActive: " + t.getMessage());
        }
    }

    private void hookApplicationLifecycle(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                    Application.class,
                    "attachBaseContext",
                    Context.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Context ctx = (Context) param.args[0];
                            if (ctx != null) {
                                sAppContext = ctx;
                                refreshConfig(ctx, true);
                            }
                        }
                    }
            );
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(
                    Application.class,
                    "onCreate",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Application app = (Application) param.thisObject;
                            sAppContext = app;
                            refreshConfig(app, true);
                            setupLiveConfigListeners(app);
                        }
                    }
            );
        } catch (Throwable t) {
            Log.d(TAG, "Application.onCreate hook fallback: " + t.getMessage());
        }

        // Hook Activity.onResume for real-time config sync without restarting TikTok
        try {
            XposedHelpers.findAndHookMethod(
                    Activity.class,
                    "onResume",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Activity activity = (Activity) param.thisObject;
                            if (activity != null) {
                                if (sAppContext == null) {
                                    sAppContext = activity.getApplicationContext();
                                }
                                refreshConfig(activity, false);
                            }
                        }
                    }
            );
            XposedBridge.log(TAG + ": Hooked Activity.onResume for seamless live config sync");
        } catch (Throwable t) {
            Log.d(TAG, "Activity.onResume hook failed: " + t.getMessage());
        }
    }

    public static final String ACTION_SETTINGS_CHANGED = "com.ash.tiktokregion.ACTION_SETTINGS_CHANGED";
    public static final String ACTION_FORCE_STOP = "com.ash.tiktokregion.ACTION_FORCE_STOP";
    public static final String EXTRA_AUTO_KILL = "auto_kill";

    private void setupLiveConfigListeners(final Application app) {
        // 1. Dynamic BroadcastReceiver for immediate non-root process termination or live config refresh
        try {
            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent == null) return;
                    String action = intent.getAction();
                    boolean autoKill = intent.getBooleanExtra(EXTRA_AUTO_KILL, true);

                    XposedBridge.log(TAG + ": Broadcast received (" + action + ", autoKill=" + autoKill + ")");

                    if (ACTION_FORCE_STOP.equals(action) || (ACTION_SETTINGS_CHANGED.equals(action) && autoKill)) {
                        XposedBridge.log(TAG + ": Self-terminating TikTok process (PID " + android.os.Process.myPid() + ") without root");
                        android.os.Process.killProcess(android.os.Process.myPid());
                        System.exit(0);
                        return;
                    }

                    refreshConfig(context != null ? context : app, true);
                }
            };
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_SETTINGS_CHANGED);
            filter.addAction(ACTION_FORCE_STOP);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                app.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                app.registerReceiver(receiver, filter);
            }
            XposedBridge.log(TAG + ": Registered non-root self-kill & settings broadcast receiver in TikTok");
        } catch (Throwable t) {
            Log.d(TAG, "Failed registering broadcast receiver: " + t.getMessage());
        }

        // 2. ContentObserver for immediate notification when ConfigProvider URI changes -> self-kill TikTok cleanly
        try {
            ContentObserver observer = new ContentObserver(new Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    XposedBridge.log(TAG + ": ContentObserver detected ConfigProvider change -> self-terminating TikTok (PID " + android.os.Process.myPid() + ")");
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(0);
                }
            };
            app.getContentResolver().registerContentObserver(ConfigProvider.CONTENT_URI, true, observer);
            XposedBridge.log(TAG + ": Registered ConfigProvider ContentObserver in TikTok");
        } catch (Throwable t) {
            Log.d(TAG, "Failed registering ContentObserver: " + t.getMessage());
        }
    }

    public static synchronized void refreshConfig(Context context, boolean force) {
        long now = System.currentTimeMillis();
        if (!force && (now - sLastConfigFetchTime < CONFIG_CACHE_MS)) {
            return;
        }
        sLastConfigFetchTime = now;

        if (context == null) {
            context = sAppContext;
        }

        if (context != null) {
            try {
                Uri uri = Uri.parse(ConfigProvider.CONTENT_URI.toString());
                Bundle bundle = context.getContentResolver().call(uri, ConfigProvider.METHOD_GET_CONFIG, null, null);
                if (bundle != null) {
                    ModuleConfig.updateFromBundle(bundle);
                    String logMsg = "Config refreshed: country=" + ModuleConfig.countryIso
                            + ", op=" + ModuleConfig.operatorName + ", forceRegion=" + ModuleConfig.forceRegion
                            + ", noWatermark=" + ModuleConfig.noWatermark;
                    Log.i(TAG, logMsg);
                    XposedBridge.log(TAG + ": " + logMsg);
                }
            } catch (Throwable t) {
                Log.d(TAG, "ContentProvider IPC call failed: " + t.getMessage());
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Backward Compatibility Delegates
    // ──────────────────────────────────────────────────────────────────────────
    public static boolean isEnabled() { return ModuleConfig.enabled; }
    public static String getTargetCountryIso() { return ModuleConfig.countryIso; }
    public static String getSimOperator() { return ModuleConfig.operatorMccMnc; }
    public static String getSimOperatorName() { return ModuleConfig.operatorName; }
    public static boolean isNoWatermarkEnabled() { return ModuleConfig.noWatermark; }
    public static boolean isBypassDownloadRestrictionEnabled() { return ModuleConfig.bypassDownloadRestriction; }
    public static boolean isDownloadStoryEnabled() { return ModuleConfig.downloadStory; }
    public static boolean isHideAdsEnabled() { return ModuleConfig.hideAds; }
    public static boolean isForceRegionEnabled() { return ModuleConfig.forceRegion; }
    public static boolean isStrictForceRegionEnabled() { return ModuleConfig.strictForceRegion; }
    public static boolean isBlockCountriesEnabled() { return ModuleConfig.blockCountries; }
    public static Set<String> getBlockedCountries() { return ModuleConfig.blockedCountries; }
    public static boolean isFilterNonHdEnabled() { return ModuleConfig.filterNonHd; }
    public static boolean isForceHighestQualityEnabled() { return ModuleConfig.forceHighestQuality; }
}
