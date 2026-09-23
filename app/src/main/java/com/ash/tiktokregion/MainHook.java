package com.ash.tiktokregion;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "TikTokRegion";
    public static final String MODULE_PACKAGE = "com.ash.tiktokregion";

    public static void log(String msg) {
        Log.i(TAG, msg);
        try {
            XposedBridge.log(TAG + ": " + msg);
        } catch (Throwable ignored) {}
    }

    public static void logD(String msg) {
        Log.d(TAG, msg);
    }

    public static final String PACKAGE_GLOBAL = "com.zhiliaoapp.musically";
    public static final String PACKAGE_ASIA = "com.ss.android.ugc.trill";
    public static final String PACKAGE_CHINA = "com.ss.android.ugc.aweme";

    private static final Set<String> TARGET_PACKAGES = new HashSet<>(Arrays.asList(
            PACKAGE_GLOBAL,
            PACKAGE_ASIA,
            PACKAGE_CHINA,
            "com.zhiliaoapp.musically.go",
            "com.tiktok.business"
    ));

    private static volatile String sCurrentPackage = null;

    public static boolean isChinaPackage() {
        return PACKAGE_CHINA.equals(sCurrentPackage);
    }

    public static String getCurrentPackage() {
        return sCurrentPackage;
    }

    private static volatile boolean sEnabled = true;
    private static volatile String sCountryIso = "us";
    private static volatile String sOperatorMccMnc = "310260";
    private static volatile String sOperatorName = "T-Mobile";
    private static volatile String sMcc = "310";
    private static volatile String sMnc = "260";
    private static volatile boolean sSpoofLocale = false;
    private static volatile String sLocaleLang = "en";
    private static volatile String sLocaleCountry = "US";
    private static volatile Locale sCachedSpoofedLocale = new Locale("en", "US");
    private static volatile boolean sNoWatermark = true;
    private static volatile boolean sBypassDownloadRestriction = true;
    private static volatile boolean sHDUpload = true;
    private static volatile boolean sHideAds = true;
    private static volatile boolean sHidePymk = false;
    private static volatile boolean sForceRegion = true;
    private static volatile boolean sStrictForceRegion = false;
    private static volatile boolean sLockedRegionFilter = false;
    private static volatile boolean sLanguageFilter = false;
    private static volatile Set<String> sAllowedLanguages = java.util.Collections.emptySet();
    private static volatile String sAllowedLanguagesRaw = "";
    private static volatile boolean sDownloadStory = true;
    private static volatile boolean sBlockCountries = false;
    private static volatile Set<String> sBlockedCountries = java.util.Collections.emptySet();

    private static volatile Context sAppContext = null;
    private static volatile long sLastConfigFetchTime = 0;
    private static final long CONFIG_CACHE_MS = 5000;

    public static boolean isEnabled() {
        return sEnabled;
    }

    public static boolean isNoWatermarkEnabled() {
        return sNoWatermark;
    }

    public static boolean isBypassDownloadRestrictionEnabled() {
        return sBypassDownloadRestriction;
    }

    public static boolean isHDUploadEnabled() {
        return sHDUpload;
    }

    public static boolean isHideAdsEnabled() {
        return sHideAds;
    }

    public static boolean isHidePymkEnabled() {
        return sHidePymk;
    }

    public static boolean isForceRegionEnabled() {
        return sForceRegion;
    }

    public static boolean isStrictForceRegionEnabled() {
        return sStrictForceRegion;
    }

    public static boolean isLockedRegionFilterEnabled() {
        return sLockedRegionFilter;
    }

    public static boolean isLanguageFilterEnabled() {
        return sLanguageFilter;
    }

    public static Set<String> getAllowedLanguages() {
        return sAllowedLanguages;
    }

    public static String getAllowedLanguagesRaw() {
        return sAllowedLanguagesRaw;
    }

    public static boolean isDownloadStoryEnabled() {
        return sDownloadStory;
    }

    public static boolean isBlockCountriesEnabled() {
        return sBlockCountries;
    }

    public static Set<String> getBlockedCountries() {
        return sBlockedCountries;
    }

    public static String getTargetCountryIso() {
        return sCountryIso;
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        if (MODULE_PACKAGE.equals(lpparam.packageName)) {
            hookSelfStatus(lpparam.classLoader);
            return;
        }

        if (!TARGET_PACKAGES.contains(lpparam.packageName)) {
            return;
        }

        sCurrentPackage = lpparam.packageName;
        boolean isChina = isChinaPackage();

        XposedBridge.log(TAG + ": Initializing TikTok hooks for package: " + lpparam.packageName + " (isChina=" + isChina + ")");

        loadConfigFromPrefs();

        hookApplicationLifecycle(lpparam.classLoader);
        hookClassLoader(lpparam.classLoader);

        if (!isChina) {
            hookCronetNetworkStack(lpparam.classLoader);
            hookClientAIFeatures(lpparam.classLoader);
            hookNetworkCommonParams(lpparam.classLoader);
            hookTelephonyManager(lpparam.classLoader);
            hookSubscriptionManager(lpparam.classLoader);
            hookSubscriptionInfo(lpparam.classLoader);
            hookSystemProperties(lpparam.classLoader);
            hookLocale(lpparam.classLoader);
            hookUrlQueryParams(lpparam.classLoader);
        } else {
            XposedBridge.log(TAG + ": TikTok China (Douyin) detected - skipping telephony, locale, and network region spoofing");
        }

        if (isChina) {
            XposedBridge.log(TAG + ": TikTok China (Douyin) detected - hooks will be initialized deferred after SafeMode loader");
        } else {
            try {
                XposedBridge.log(TAG + ": STEP 1: Calling WatermarkHook.hook");
                WatermarkHook.hook(lpparam.classLoader);
                XposedBridge.log(TAG + ": STEP 1: WatermarkHook.hook finished");
            } catch (Throwable t) {
                XposedBridge.log(TAG + ": WatermarkHook.hook error: " + t.getMessage());
            }
        }

        try {
            XposedBridge.log(TAG + ": STEP 3: Calling AdsHook.hook");
            AdsHook.hook(lpparam.classLoader);
            XposedBridge.log(TAG + ": STEP 3: AdsHook.hook finished");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": AdsHook.hook error: " + t.getMessage());
        }

        if (!isChina) {
            try {
                HDUploadHook.hook(lpparam.classLoader);
            } catch (Throwable t) {
                XposedBridge.log(TAG + ": HDUploadHook.hook error: " + t.getMessage());
            }
            try {
                hookContentLanguageService(lpparam.classLoader);
            } catch (Throwable ignored) {}
        }

        XposedBridge.log(TAG + ": All initial hooks dispatched for " + lpparam.packageName);
    }

    private void hookSelfStatus(ClassLoader classLoader) {
        try {
            Class<?> mainActivityClass = XposedHelpers.findClassIfExists(
                    MODULE_PACKAGE + ".MainActivity", classLoader);
            if (mainActivityClass != null) {
                XposedHelpers.findAndHookMethod(mainActivityClass, "isModuleActive",
                        new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) {
                                param.setResult(true);
                            }
                        });
                log("Hooked isModuleActive() -> true");
            }
        } catch (Throwable t) {
            logD("Failed hooking isModuleActive: " + t.getMessage());
        }
    }

    private static final AtomicBoolean sLifecycleInitialized = new AtomicBoolean(false);
    private static volatile boolean sDeferredHooksInstalled = false;

    private void hookApplicationLifecycle(ClassLoader classLoader) {
        XC_MethodHook appAttachHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                Context context = null;
                if (param.args != null && param.args.length > 0 && param.args[0] instanceof Context) {
                    context = (Context) param.args[0];
                }
                if (param.thisObject instanceof Application) {
                    sAppContext = (Application) param.thisObject;
                }
                final Context ctx = (context != null) ? context : sAppContext;
                if (ctx != null && sLifecycleInitialized.compareAndSet(false, true)) {
                    installDeferredHooks(ctx.getClassLoader());
                    new Thread(() -> refreshConfig(ctx), "TikTokEnhancer-ConfigRefresh").start();
                }
            }
        };

        try {
            XposedHelpers.findAndHookMethod(Application.class, "attachBaseContext", Context.class, appAttachHook);
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, appAttachHook);
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(
                    Application.class,
                    "onCreate",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (param.thisObject instanceof Application) {
                                sAppContext = (Application) param.thisObject;
                            }
                            final Context ctx = sAppContext;
                            if (ctx != null && sLifecycleInitialized.compareAndSet(false, true)) {
                                installDeferredHooks(ctx.getClassLoader());
                                new Thread(() -> refreshConfig(ctx), "TikTokEnhancer-ConfigRefresh").start();
                            }
                        }
                    }
            );
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(
                    Activity.class,
                    "onResume",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (param.thisObject instanceof Activity) {
                                Activity act = (Activity) param.thisObject;
                                if (sAppContext == null) {
                                    sAppContext = act.getApplication();
                                }
                                checkConfigRefreshAsync(act);
                            }
                        }
                    }
            );
        } catch (Throwable ignored) {}
    }

    private static volatile boolean sClassLoaderHooked = false;
    private static final ThreadLocal<Boolean> sInLoadClass = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private void hookClassLoader(ClassLoader classLoader) {
        if (sClassLoaderHooked) return;
        sClassLoaderHooked = true;

        XC_MethodHook loadClassHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (sInLoadClass.get()) return;
                sInLoadClass.set(Boolean.TRUE);
                try {
                    Object result = param.getResult();
                    if (result instanceof Class<?>) {
                        onClassLoaded((Class<?>) result);
                    }
                } finally {
                    sInLoadClass.set(Boolean.FALSE);
                }
            }
        };

        try {
            XposedHelpers.findAndHookMethod(ClassLoader.class, "loadClass", String.class, boolean.class, loadClassHook);
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(ClassLoader.class, "loadClass", String.class, loadClassHook);
        } catch (Throwable ignored) {}
    }

    public static void onClassLoaded(Class<?> clazz) {
        if (clazz == null) return;
        String name = clazz.getName();

        if (name.startsWith("android.") || name.startsWith("java.") || name.startsWith("javax.")
                || name.startsWith("kotlin.") || name.startsWith("androidx.") || name.startsWith("com.google.")
                || name.startsWith("com.ash.tiktokregion.")) {
            return;
        }

        if ("com.bytedance.ttnet.TTNetInit".equals(name)) {
            hookTTNetInitClass(clazz);
        } else if ("com.ss.ugc.clientai.core.api.FeatureProducer".equals(name)) {
            hookFeatureProducerClass(clazz);
        } else if ("com.ss.android.ugc.aweme.setting.services.SettingServiceImpl".equals(name)) {
            hookSettingServiceImplClass(clazz);
        } else if ("X.03JI".equals(name) || "LX.03JI".equals(name)
                || "X.03IJ".equals(name) || "LX.03IJ".equals(name)
                || "X.04JI".equals(name) || "LX.04JI".equals(name)) {
            hookParamMapClass(clazz);
        } else if ("X.03jl".equals(name) || "LX.03jl".equals(name)
                || "X.03im".equals(name) || "LX.03im".equals(name)
                || "X.04jl".equals(name) || "LX.04jl".equals(name)) {
            hookNetworkCommonParamsClass(clazz);
        } else if ("com.ss.android.ugc.aweme.watermark.WaterMarkServiceImpl".equals(name)
                || "com.ss.android.ugc.aweme.services.watermark.WaterMarkBuilder".equals(name)) {
            if (isChinaPackage()) {
                DouyinWatermarkHook.hookWatermarkServiceClass(clazz);
            } else {
                WatermarkHook.hookWatermarkServiceClass(clazz);
            }
        } else if ("com.ss.android.ugc.aweme.base.model.UrlModel".equals(name)) {
            WatermarkHook.hookUrlModelClass(clazz);
        } else if ("com.ss.android.ugc.aweme.feed.model.FeedItemList".equals(name)) {
            AdsHook.hookFeedItemListClass(clazz);
        } else if ("com.ss.android.ugc.aweme.feed.panel.BaseListFragmentPanel".equals(name)
                || "com.ss.android.ugc.aweme.feed.panel.FullFeedFragmentPanel".equals(name)
                || "com.ss.android.ugc.aweme.feed.panel.RecommendFeedFragmentPanel".equals(name)
                || "com.ss.android.ugc.aweme.feed.panel.FollowFeedFragmentPanelMT".equals(name)
                || "com.ss.android.ugc.aweme.stemfeed.panel.StemFeedFragmentPanel".equals(name)
                || "com.ss.android.ugc.aweme.repostfeed.feed.RepostFeedPanel".equals(name)) {
            AdsHook.hookFeedPanel(clazz.getClassLoader());
        } else if ("com.ss.android.ugc.aweme.feed.model.Aweme".equals(name)) {
            if (isChinaPackage()) {
                DouyinWatermarkHook.hookAwemeClass(clazz);
            } else {
                WatermarkHook.hookAwemeClass(clazz);
            }
        } else if ("com.ss.android.ugc.aweme.feed.model.Video".equals(name)) {
            if (isChinaPackage()) {
                DouyinWatermarkHook.hookVideoClass(clazz);
            }
        } else if ("com.ss.android.ugc.aweme.follow.presenter.FollowFeedList".equals(name)) {
            AdsHook.hookFollowFeedListClass(clazz);
        } else if ("com.ss.android.ugc.aweme.friendstab.api.FriendsFeedResponse".equals(name)) {
            AdsHook.hookFriendsFeedResponseClass(clazz);
        } else if (isChinaPackage()) {
            DouyinWatermarkHook.onClassLoaded(clazz);
        } else {
            HDUploadHook.onClassLoaded(clazz);
        }
    }

    public static void installDeferredHooks(ClassLoader classLoader) {
        if (classLoader == null || sDeferredHooksInstalled) return;
        sDeferredHooksInstalled = true;
        boolean isChina = isChinaPackage();
        if (!isChina) {
            hookCronetNetworkStack(classLoader);
            hookClientAIFeatures(classLoader);
            hookNetworkCommonParams(classLoader);
            hookUrlQueryParams(classLoader);
        }
        if (isChina) {
            DouyinWatermarkHook.hook(classLoader);
        } else {
            WatermarkHook.hook(classLoader);
        }
        AdsHook.hook(classLoader);
        if (!isChina) {
            HDUploadHook.hook(classLoader);
        }
    }

    public static void checkConfigRefreshAsync(Context context) {
        long now = System.currentTimeMillis();
        if (now - sLastConfigFetchTime >= CONFIG_CACHE_MS) {
            final Context ctx = (context != null) ? context : sAppContext;
            if (ctx != null) {
                new Thread(() -> refreshConfig(ctx), "TikTokEnhancer-ConfigRefresh").start();
            }
        }
    }

    public static synchronized void refreshConfig(Context context) {
        long now = System.currentTimeMillis();
        if (now - sLastConfigFetchTime < CONFIG_CACHE_MS) {
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
                    sEnabled = bundle.getBoolean(ConfigProvider.KEY_ENABLED, true);
                    sCountryIso = bundle.getString(ConfigProvider.KEY_SIM_COUNTRY, "us");
                    sOperatorMccMnc = bundle.getString(ConfigProvider.KEY_SIM_OPERATOR, "310260");
                    sOperatorName = bundle.getString(ConfigProvider.KEY_OPERATOR_NAME, "T-Mobile");
                    sMcc = bundle.getString(ConfigProvider.KEY_MCC, "310");
                    sMnc = bundle.getString(ConfigProvider.KEY_MNC, "260");
                    sSpoofLocale = bundle.getBoolean(ConfigProvider.KEY_SPOOF_LOCALE, false);
                    sLocaleLang = bundle.getString(ConfigProvider.KEY_LOCALE_LANG, "en");
                    sLocaleCountry = bundle.getString(ConfigProvider.KEY_LOCALE_COUNTRY, "US");
                    sCachedSpoofedLocale = new Locale(sLocaleLang, sLocaleCountry);
                    sNoWatermark = bundle.getBoolean(ConfigProvider.KEY_NO_WATERMARK, true);
                    sBypassDownloadRestriction = bundle.getBoolean(ConfigProvider.KEY_BYPASS_DOWNLOAD_RESTRICTION, true);
                    sHDUpload = bundle.getBoolean(ConfigProvider.KEY_HD_UPLOAD, true);
                    sHideAds = bundle.getBoolean(ConfigProvider.KEY_HIDE_ADS, true);
                    sHidePymk = bundle.getBoolean(ConfigProvider.KEY_HIDE_PYMK, false);
                    sForceRegion = bundle.getBoolean(ConfigProvider.KEY_FORCE_REGION, true);
                    sStrictForceRegion = bundle.getBoolean(ConfigProvider.KEY_STRICT_FORCE_REGION, false);
                    sLockedRegionFilter = bundle.getBoolean(ConfigProvider.KEY_LOCKED_REGION_FILTER, false);
                    sLanguageFilter = bundle.getBoolean(ConfigProvider.KEY_LANGUAGE_FILTER, false);
                    sAllowedLanguagesRaw = bundle.getString(ConfigProvider.KEY_ALLOWED_LANGUAGES, "");
                    sAllowedLanguages = parseIsoSet(sAllowedLanguagesRaw);
                    sDownloadStory = bundle.getBoolean(ConfigProvider.KEY_DOWNLOAD_STORY, true);
                    sBlockCountries = bundle.getBoolean(ConfigProvider.KEY_BLOCK_COUNTRIES, false);
                    sBlockedCountries = parseIsoSet(bundle.getString(ConfigProvider.KEY_BLOCKED_COUNTRY_LIST, ""));

                    log("Config loaded via ContentProvider IPC: enabled=" + sEnabled
                            + ", country=" + sCountryIso + ", op=" + sOperatorName + " (" + sOperatorMccMnc + ")"
                            + ", forceRegion=" + sForceRegion + ", strictForceRegion=" + sStrictForceRegion
                            + ", lockedRegionFilter=" + sLockedRegionFilter + ", languageFilter=" + sLanguageFilter + " (" + sAllowedLanguages.size() + ")"
                            + ", blockCountries=" + sBlockCountries + " (" + sBlockedCountries.size() + ")"
                            + ", hideAds=" + sHideAds + ", hidePymk=" + sHidePymk + ", downloadStory=" + sDownloadStory);
                    return;
                }
            } catch (Throwable t) {
                Log.d(TAG, "ContentProvider IPC call failed, falling back to prefs: " + t.getMessage());
            }
        }

        loadConfigFromPrefs();
    }

    private static Set<String> parseIsoSet(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return java.util.Collections.emptySet();
        }
        String[] parts = raw.split("[,;]");
        Set<String> set = new HashSet<>();
        for (String p : parts) {
            String trimmed = p.trim().toLowerCase(Locale.ROOT);
            if (!trimmed.isEmpty()) {
                set.add(trimmed);
            }
        }
        return set;
    }

    private static void loadConfigFromPrefs() {
        try {
            XSharedPreferences prefs = new XSharedPreferences(MODULE_PACKAGE, MainActivity.PREF_NAME);
            prefs.reload();

            sEnabled = prefs.getBoolean(MainActivity.KEY_ENABLED, true);
            String presetId = prefs.getString(MainActivity.KEY_PRESET_ID, "US_TMOBILE");
            CountryPreset preset = CountryPreset.getById(presetId);

            boolean isCustom = prefs.getBoolean(MainActivity.KEY_IS_CUSTOM, false);
            sSpoofLocale = prefs.getBoolean(MainActivity.KEY_SPOOF_LOCALE, false);
            sNoWatermark = prefs.getBoolean(MainActivity.KEY_NO_WATERMARK, true);
            sBypassDownloadRestriction = prefs.getBoolean(MainActivity.KEY_BYPASS_DOWNLOAD_RESTRICTION, true);
            sHDUpload = prefs.getBoolean(MainActivity.KEY_HD_UPLOAD, true);
            sHideAds = prefs.getBoolean(MainActivity.KEY_HIDE_ADS, true);
            sHidePymk = prefs.getBoolean(MainActivity.KEY_HIDE_PYMK, false);
            sForceRegion = prefs.getBoolean(MainActivity.KEY_FORCE_REGION, true);
            sStrictForceRegion = prefs.getBoolean(MainActivity.KEY_STRICT_FORCE_REGION, false);
            sLockedRegionFilter = prefs.getBoolean(MainActivity.KEY_LOCKED_REGION_FILTER, false);
            sLanguageFilter = prefs.getBoolean(MainActivity.KEY_LANGUAGE_FILTER, false);
            sAllowedLanguagesRaw = prefs.getString(MainActivity.KEY_ALLOWED_LANGUAGES, "");
            sAllowedLanguages = parseIsoSet(sAllowedLanguagesRaw);
            sDownloadStory = prefs.getBoolean(MainActivity.KEY_DOWNLOAD_STORY, true);
            sBlockCountries = prefs.getBoolean(MainActivity.KEY_BLOCK_COUNTRIES, false);
            sBlockedCountries = parseIsoSet(prefs.getString(MainActivity.KEY_BLOCKED_COUNTRY_LIST, ""));

            if (isCustom) {
                sCountryIso = prefs.getString(MainActivity.KEY_CUSTOM_ISO, preset.getCountryIso()).toLowerCase();
                sOperatorMccMnc = prefs.getString(MainActivity.KEY_CUSTOM_OPERATOR, preset.getOperatorMccMnc());
                sOperatorName = prefs.getString(MainActivity.KEY_CUSTOM_NAME, preset.getOperatorName());
            } else {
                sCountryIso = preset.getCountryIso();
                sOperatorMccMnc = preset.getOperatorMccMnc();
                sOperatorName = preset.getOperatorName();
            }

            if (sOperatorMccMnc != null && sOperatorMccMnc.length() >= 5) {
                sMcc = sOperatorMccMnc.substring(0, 3);
                sMnc = sOperatorMccMnc.substring(3);
            } else {
                sMcc = preset.getMcc();
                sMnc = preset.getMnc();
            }

            sLocaleLang = preset.getLocaleLanguage();
            sLocaleCountry = preset.getLocaleCountry();
            sCachedSpoofedLocale = new Locale(sLocaleLang, sLocaleCountry);

            log("Config loaded via XSharedPreferences: enabled=" + sEnabled
                    + ", country=" + sCountryIso + ", forceRegion=" + sForceRegion
                    + ", blockCountries=" + sBlockCountries + " (" + sBlockedCountries.size() + ")");
        } catch (Throwable t) {
            Log.e(TAG, "Failed loading XSharedPreferences fallback", t);
        }
    }

    private static volatile boolean sTTNetInitHooked = false;

    private static void hookCronetNetworkStack(ClassLoader classLoader) {
        if (classLoader == null) return;

        Class<?> ttnetClass = XposedHelpers.findClassIfExists("com.bytedance.ttnet.TTNetInit", classLoader);
        if (ttnetClass != null) {
            hookTTNetInitClass(ttnetClass);
        }

        String[] cronetClasses = {
                "com.bytedance.frameworks.baselib.network.http.cronet.ICronetAppProvider",
                "com.bytedance.ttnet.cronet.AbsCronetDependAdapter"
        };

        for (String className : cronetClasses) {
            hookMethodReturn(classLoader, className, "getRegion", new MethodReturnValue() {
                @Override
                public Object getValue() {
                    return sEnabled ? sCountryIso.toUpperCase(Locale.ROOT) : null;
                }
            });

            hookMethodReturn(classLoader, className, "getCarrierRegion", new MethodReturnValue() {
                @Override
                public Object getValue() {
                    return sEnabled ? sCountryIso.toUpperCase(Locale.ROOT) : null;
                }
            });

            hookMethodReturn(classLoader, className, "getSysRegion", new MethodReturnValue() {
                @Override
                public Object getValue() {
                    return sEnabled ? sCountryIso.toUpperCase(Locale.ROOT) : null;
                }
            });

            hookMethodReturn(classLoader, className, "getSimOperator", new MethodReturnValue() {
                @Override
                public Object getValue() {
                    return sEnabled ? sOperatorMccMnc : null;
                }
            });

            hookMethodReturn(classLoader, className, "getNetworkOperator", new MethodReturnValue() {
                @Override
                public Object getValue() {
                    return sEnabled ? sOperatorMccMnc : null;
                }
            });

            hookMethodReturn(classLoader, className, "getAppInitialRegionInfo", new MethodReturnValue() {
                @Override
                public Object getValue() {
                    return sEnabled ? sCountryIso.toUpperCase(Locale.ROOT) : null;
                }
            });
        }
    }

    public static void hookTTNetInitClass(Class<?> ttnetInitClass) {
        if (ttnetInitClass == null || sTTNetInitHooked) return;
        try {
            for (Method method : ttnetInitClass.getDeclaredMethods()) {
                if ("setCronetDepend".equals(method.getName()) && method.getParameterTypes().length == 1) {
                    final Class<?> providerInterface = method.getParameterTypes()[0];
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (param.args != null && param.args.length > 0 && param.args[0] != null) {
                                Object original = param.args[0];
                                if (!Proxy.isProxyClass(original.getClass())) {
                                    param.args[0] = createCronetProxy(original, providerInterface);
                                    log("Wrapped TTNetInit.setCronetDepend in dynamic proxy");
                                }
                            }
                        }
                    });
                    sTTNetInitHooked = true;
                    log("Hooked TTNetInit.setCronetDepend successfully");
                }
            }
            checkAndProxyExistingCronetProvider(ttnetInitClass);
        } catch (Throwable t) {
            logD("hookTTNetInitClass failed: " + t.getMessage());
        }
    }

    private static void checkAndProxyExistingCronetProvider(Class<?> ttnetInitClass) {
        try {
            Field f = XposedHelpers.findFieldIfExists(ttnetInitClass, "sCronetProvider");
            if (f != null) {
                f.setAccessible(true);
                Object current = f.get(null);
                if (current != null && !Proxy.isProxyClass(current.getClass())) {
                    Class<?> providerInterface = f.getType();
                    f.set(null, createCronetProxy(current, providerInterface));
                    log("Replaced existing TTNetInit.sCronetProvider with dynamic proxy");
                }
            }
        } catch (Throwable ignored) {}
    }

    private static Object createCronetProxy(final Object originalProvider, final Class<?> providerInterface) {
        ClassLoader cl = providerInterface.getClassLoader();
        if (cl == null && originalProvider != null) {
            cl = originalProvider.getClass().getClassLoader();
        }
        if (cl == null) {
            cl = MainHook.class.getClassLoader();
        }
        return Proxy.newProxyInstance(
                cl,
                new Class<?>[]{providerInterface},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        String mName = method.getName();
                        if (sEnabled) {
                            if ("getRegion".equals(mName)
                                    || "getCarrierRegion".equals(mName)
                                    || "getSysRegion".equals(mName)
                                    || "getAppInitialRegionInfo".equals(mName)) {
                                return sCountryIso.toUpperCase(Locale.ROOT);
                            }
                            if ("getSimOperator".equals(mName) || "getNetworkOperator".equals(mName)) {
                                return sOperatorMccMnc;
                            }
                        }
                        return method.invoke(originalProvider, args);
                    }
                }
        );
    }

    private static volatile boolean sFeatureProducerHooked = false;
    private static volatile boolean sParamMapHooked = false;
    private static volatile boolean sNetworkParamsHooked = false;

    private static final String[] PARAM_MAP_CLASS_NAMES = {
            "X.03II", "LX.03II",
            "X.04II", "LX.04II",
            "X.03JI", "LX.03JI",
            "X.04JI", "LX.04JI",
            "X.03IJ", "LX.03IJ",
            "X.04IJ", "LX.04IJ",
            "X.03IK", "LX.03IK",
            "X.04IK", "LX.04IK",
            "X.03IL", "LX.03IL",
            "X.04IL", "LX.04IL",
    };

    private static final String[] NETWORK_COMMON_PARAMS_CLASSES = {
            "X.03iB", "LX.03iB",
            "X.04iB", "LX.04iB",
            "X.03jl", "LX.03jl",
            "X.04jl", "LX.04jl",
            "X.03im", "LX.03im",
            "X.04im", "LX.04im",
            "X.03in", "LX.03in",
            "X.04in", "LX.04in",
    };

    public static void hookNetworkCommonParams(ClassLoader classLoader) {
        if (classLoader == null || sNetworkParamsHooked) return;
        for (String className : NETWORK_COMMON_PARAMS_CLASSES) {
            Class<?> clazz = XposedHelpers.findClassIfExists(className, classLoader);
            if (clazz != null) {
                hookNetworkCommonParamsClass(clazz);
                if (sNetworkParamsHooked) break;
            }
        }
    }

    public static void hookClientAIFeatures(ClassLoader classLoader) {
        if (classLoader == null) return;

        Class<?> fpClass = XposedHelpers.findClassIfExists("com.ss.ugc.clientai.core.api.FeatureProducer", classLoader);
        if (fpClass != null) {
            hookFeatureProducerClass(fpClass);
        }

        Class<?> settingServiceClass = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.setting.services.SettingServiceImpl", classLoader);
        if (settingServiceClass != null) {
            hookSettingServiceImplClass(settingServiceClass);
        }

        if (!sParamMapHooked) {
            for (String className : PARAM_MAP_CLASS_NAMES) {
                Class<?> paramMapClass = XposedHelpers.findClassIfExists(className, classLoader);
                if (paramMapClass != null) {
                    hookParamMapClass(paramMapClass);
                    if (sParamMapHooked) break;
                }
            }
        }

        if (!sNetworkParamsHooked) {
            hookNetworkCommonParams(classLoader);
        }
    }

    public static void hookFeatureProducerClass(Class<?> fpClass) {
        if (fpClass == null || sFeatureProducerHooked) return;
        try {
            XC_MethodHook featureHook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!sEnabled || param.args == null || param.args.length == 0) return;
                    String featureKey = null;
                    for (Object arg : param.args) {
                        if (arg instanceof String && ((String) arg).startsWith("f_global_")) {
                            featureKey = (String) arg;
                            break;
                        }
                    }
                    if (featureKey != null) {
                        String upper = sCountryIso.toUpperCase(Locale.ROOT);
                        switch (featureKey) {

                            case "f_global_carrier_region":
                            case "f_global_carrier_region_v2":
                            case "f_global_sys_region":
                            case "f_global_account_region":
                            case "f_global_residence":
                            case "f_global_region":
                            case "f_global_op_region":
                            case "f_global_current_region":
                            case "f_global_store_region":
                                param.setResult(upper);
                                break;

                            case "f_global_mcc_mnc":
                                param.setResult(sOperatorMccMnc);
                                break;

                            case "f_global_fake_region":
                                param.setResult("");
                                break;

                            case "f_global_timezone":
                            case "f_global_timezone_name":
                            case "f_global_timezone_display":
                                String tz = getTimezoneForCountry(sCountryIso);
                                if (tz != null) param.setResult(tz);
                                break;
                            case "f_global_timezone_offset":
                                String tzOff = getTimezoneOffsetForCountry(sCountryIso);
                                if (tzOff != null) param.setResult(tzOff);
                                break;

                            case "f_global_language":
                            case "f_global_app_language":
                            case "f_global_keyboard_language":
                                if (sSpoofLocale) {
                                    param.setResult(sLocaleLang);
                                }
                                break;
                            case "f_global_content_language":
                                if (sLanguageFilter && !sAllowedLanguages.isEmpty()) {
                                    param.setResult(String.join(",", sAllowedLanguages));
                                } else if (sSpoofLocale) {
                                    param.setResult(sLocaleLang);
                                }
                                break;
                            case "f_global_locale":
                                if (sSpoofLocale) {
                                    param.setResult(sLocaleLang + "-" + sLocaleCountry);
                                }
                                break;
                        }
                    }
                }
            };

            for (Method m : fpClass.getDeclaredMethods()) {
                if (m.getName().startsWith("getStringFeature") || m.getName().startsWith("getFeature")) {
                    XposedBridge.hookMethod(m, featureHook);
                }
            }
            sFeatureProducerHooked = true;
            log("Hooked FeatureProducer for ClientAI parameters (region + timezone + language)");
        } catch (Throwable t) {
            logD("hookFeatureProducerClass failed: " + t.getMessage());
        }
    }

    private static volatile boolean sSettingServiceHooked = false;

    public static void hookSettingServiceImplClass(Class<?> settingServiceClass) {
        if (settingServiceClass == null || sSettingServiceHooked) return;
        sSettingServiceHooked = true;
        try {
            for (Method m : settingServiceClass.getDeclaredMethods()) {
                if ("installCommonParams".equals(m.getName()) && m.getParameterTypes().length == 0) {
                    XposedBridge.hookMethod(m, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!sParamMapHooked) {
                                try {
                                    discoverParamMapFromSettingService(param.thisObject);
                                } catch (Throwable t) {
                                    logD("Dynamic param map discovery failed: " + t.getMessage());
                                }
                            }
                            if (!sNetworkParamsHooked && param.thisObject != null) {
                                ClassLoader cl = param.thisObject.getClass().getClassLoader();
                                if (cl != null) {
                                    hookNetworkCommonParams(cl);
                                }
                            }
                        }
                    });
                    log("Hooked SettingServiceImpl.installCommonParams for dynamic param map discovery");
                }
            }
        } catch (Throwable ignored) {}
    }

    private static void discoverParamMapFromSettingService(Object settingService) {
        if (settingService == null || sParamMapHooked) return;
        try {

            Class<?> serviceClass = settingService.getClass();
            for (Field field : serviceClass.getDeclaredFields()) {
                field.setAccessible(true);
                Object fieldValue = field.get(settingService);
                if (fieldValue == null) continue;
                Class<?> fieldClass = fieldValue.getClass();

                for (Method m : fieldClass.getDeclaredMethods()) {
                    if (m.getParameterTypes().length == 2
                            && m.getParameterTypes()[0] == String.class
                            && m.getParameterTypes()[1] == String.class) {
                        hookParamMapMethod(fieldClass, m.getName());
                        if (sParamMapHooked) {
                            log("Dynamically discovered param map class: " + fieldClass.getName() + "." + m.getName());
                            return;
                        }
                    }
                }
            }

            for (Field field : serviceClass.getDeclaredFields()) {
                if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
                field.setAccessible(true);
                Object fieldValue = field.get(null);
                if (fieldValue == null) continue;
                Class<?> fieldClass = fieldValue.getClass();
                for (Method m : fieldClass.getDeclaredMethods()) {
                    if (m.getParameterTypes().length == 2
                            && m.getParameterTypes()[0] == String.class
                            && m.getParameterTypes()[1] == String.class) {
                        hookParamMapMethod(fieldClass, m.getName());
                        if (sParamMapHooked) {
                            log("Dynamically discovered param map class (static): " + fieldClass.getName() + "." + m.getName());
                            return;
                        }
                    }
                }
            }
        } catch (Throwable t) {
            logD("discoverParamMapFromSettingService error: " + t.getMessage());
        }
    }

    public static void injectSpoofedNetworkParams(Map<String, String> map) {
        if (map == null || !sEnabled) return;
        String upper = sCountryIso.toUpperCase(Locale.ROOT);
        map.put("carrier_region", upper);
        map.put("carrier_region1", upper);
        map.put("carrier_region_v2", upper);
        map.put("sys_region", upper);
        map.put("account_region", upper);
        map.put("residence", upper);
        map.put("region", upper);
        map.put("app_region", upper);
        map.put("device_region", upper);
        map.put("user_region", upper);
        map.put("current_region", upper);
        map.put("op_region", upper);
        map.put("store_region", upper);
        map.put("sim_region", upper);
        map.put("priority_region", upper);
        map.put("reg_store_region", upper);
        map.put("user_selected_region", upper);
        map.put("mcc_mnc", sOperatorMccMnc);

        String tz = getTimezoneForCountry(sCountryIso);
        if (tz != null) {
            map.put("timezone_name", tz);
            map.put("tz_name", tz);
        }
        String tzOff = getTimezoneOffsetForCountry(sCountryIso);
        if (tzOff != null) {
            map.put("timezone_offset", tzOff);
            map.put("tz_offset", tzOff);
        }

        if (sSpoofLocale) {
            map.put("language", sLocaleLang);
            map.put("app_language", sLocaleLang);
            map.put("content_language", sLocaleLang);
        }
    }

    public static void hookNetworkCommonParamsClass(Class<?> clazz) {
        if (clazz == null || sNetworkParamsHooked) return;
        try {
            for (Method m : clazz.getDeclaredMethods()) {
                Class<?>[] pTypes = m.getParameterTypes();

                if (pTypes.length >= 3
                        && Context.class.isAssignableFrom(pTypes[0])
                        && (pTypes[1] == boolean.class || pTypes[1] == Boolean.class)
                        && Map.class.isAssignableFrom(pTypes[2])) {
                    XposedBridge.hookMethod(m, new XC_MethodHook() {
                        @Override
                        @SuppressWarnings("unchecked")
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!sEnabled) return;
                            try {
                                Map<String, String> map = (Map<String, String>) param.args[2];
                                if (map != null) {
                                    injectSpoofedNetworkParams(map);
                                }
                            } catch (Throwable ignored) {}
                        }
                    });
                    sNetworkParamsHooked = true;
                    log("Hooked TTNet common params: " + clazz.getName() + "." + m.getName() + "(Context, boolean, Map, ...)");
                }

                if (pTypes.length >= 3
                        && Context.class.isAssignableFrom(pTypes[0])
                        && StringBuilder.class.isAssignableFrom(pTypes[1])
                        && (pTypes[2] == boolean.class || pTypes[2] == Boolean.class)
                        && m.getReturnType() == String.class) {
                    XposedBridge.hookMethod(m, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!sEnabled) return;
                            try {
                                String resultUrl = (String) param.getResult();
                                if (resultUrl != null && !resultUrl.isEmpty()) {
                                    String rewritten = rewriteQueryString(resultUrl);
                                    if (!resultUrl.equals(rewritten)) {
                                        param.setResult(rewritten);
                                        StringBuilder sb = (StringBuilder) param.args[1];
                                        if (sb != null) {
                                            sb.delete(0, sb.length());
                                            sb.append(rewritten);
                                        }
                                    }
                                }
                            } catch (Throwable ignored) {}
                        }
                    });
                    log("Hooked TTNet URL builder: " + clazz.getName() + "." + m.getName() + "(Context, StringBuilder, ...)");
                }
            }
        } catch (Throwable t) {
            logD("hookNetworkCommonParamsClass failed for " + clazz.getName() + ": " + t.getMessage());
        }
    }


    private static final XC_MethodHook PARAM_MAP_HOOK = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) {
            if (!sEnabled) return;
            String key = (String) param.args[0];
            if (key == null) return;
            switch (key) {

                case "carrier_region":
                case "carrier_region1":
                case "carrier_region_v2":
                case "sys_region":
                case "account_region":
                case "residence":
                case "region":
                case "app_region":
                case "device_region":
                case "user_region":
                case "current_region":
                case "op_region":
                case "store_region":
                case "sim_region":
                case "priority_region":
                case "reg_store_region":
                case "user_selected_region":
                    param.args[1] = sCountryIso.toUpperCase(Locale.ROOT);
                    break;

                case "mcc_mnc":
                    param.args[1] = sOperatorMccMnc;
                    break;

                case "timezone_name":
                case "tz_name": {
                    String tz = getTimezoneForCountry(sCountryIso);
                    if (tz != null) param.args[1] = tz;
                    break;
                }
                case "timezone_offset":
                case "tz_offset": {
                    String tzOff = getTimezoneOffsetForCountry(sCountryIso);
                    if (tzOff != null) param.args[1] = tzOff;
                    break;
                }

                case "language":
                case "content_language":
                    if (sSpoofLocale) {
                        param.args[1] = sLocaleLang;
                    }
                    break;
            }
        }
    };

    public static void hookParamMapClass(Class<?> paramMapClass) {
        if (paramMapClass == null || sParamMapHooked) return;
        hookParamMapMethod(paramMapClass, "LIZ");

        try {
            for (Method m : paramMapClass.getDeclaredMethods()) {
                Class<?>[] pTypes = m.getParameterTypes();
                if (pTypes.length >= 2 && Map.class.isAssignableFrom(pTypes[0])
                        && (pTypes[1] == boolean.class || pTypes[1] == Boolean.class)) {
                    XposedBridge.hookMethod(m, new XC_MethodHook() {
                        @Override
                        @SuppressWarnings("unchecked")
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!sEnabled) return;
                            try {
                                Map<String, String> map = (Map<String, String>) param.args[0];
                                if (map != null) {
                                    injectSpoofedNetworkParams(map);
                                }
                            } catch (Throwable ignored) {}
                        }
                    });
                    log("Hooked param map builder: " + paramMapClass.getName() + "." + m.getName() + "(Map, boolean, ...)");
                }
                if (pTypes.length >= 2 && StringBuilder.class.isAssignableFrom(pTypes[0])
                        && (pTypes[1] == boolean.class || pTypes[1] == Boolean.class)) {
                    XposedBridge.hookMethod(m, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!sEnabled) return;
                            try {
                                StringBuilder sb = (StringBuilder) param.args[0];
                                if (sb != null && sb.length() > 0) {
                                    String rewritten = rewriteQueryString(sb.toString());
                                    if (!sb.toString().equals(rewritten)) {
                                        sb.delete(0, sb.length());
                                        sb.append(rewritten);
                                    }
                                }
                            } catch (Throwable ignored) {}
                        }
                    });
                    log("Hooked param map URL builder: " + paramMapClass.getName() + "." + m.getName() + "(StringBuilder, boolean, ...)");
                }
            }
        } catch (Throwable ignored) {}
    }

    private static void hookParamMapMethod(Class<?> paramMapClass, String methodName) {
        if (paramMapClass == null || sParamMapHooked) return;
        try {
            for (Method m : paramMapClass.getDeclaredMethods()) {
                if (m.getName().equals(methodName) && m.getParameterTypes().length == 2
                        && m.getParameterTypes()[0] == String.class && m.getParameterTypes()[1] == String.class) {
                    XposedBridge.hookMethod(m, PARAM_MAP_HOOK);
                    sParamMapHooked = true;
                    log("Hooked param map: " + paramMapClass.getName() + "." + methodName + " for network common parameters");
                    return;
                }
            }
        } catch (Throwable t) {
            logD("hookParamMapMethod failed for " + paramMapClass.getName() + ": " + t.getMessage());
        }
    }

    static String getTimezoneForCountry(String countryIso) {
        if (countryIso == null) return null;
        switch (countryIso.toLowerCase(Locale.ROOT)) {
            case "us": return "America/New_York";
            case "gb": return "Europe/London";
            case "jp": return "Asia/Tokyo";
            case "kr": return "Asia/Seoul";
            case "de": return "Europe/Berlin";
            case "fr": return "Europe/Paris";
            case "br": return "America/Sao_Paulo";
            case "in": return "Asia/Kolkata";
            case "au": return "Australia/Sydney";
            case "ca": return "America/Toronto";
            case "mx": return "America/Mexico_City";
            case "id": return "Asia/Jakarta";
            case "ru": return "Europe/Moscow";
            case "tr": return "Europe/Istanbul";
            case "sa": return "Asia/Riyadh";
            case "ae": return "Asia/Dubai";
            case "eg": return "Africa/Cairo";
            case "th": return "Asia/Bangkok";
            case "vn": return "Asia/Ho_Chi_Minh";
            case "ph": return "Asia/Manila";
            case "sg": return "Asia/Singapore";
            case "my": return "Asia/Kuala_Lumpur";
            case "il": return "Asia/Jerusalem";
            case "pk": return "Asia/Karachi";
            case "bd": return "Asia/Dhaka";
            case "ng": return "Africa/Lagos";
            case "ar": return "America/Argentina/Buenos_Aires";
            case "co": return "America/Bogota";
            case "cl": return "America/Santiago";
            case "it": return "Europe/Rome";
            case "es": return "Europe/Madrid";
            case "nl": return "Europe/Amsterdam";
            case "pl": return "Europe/Warsaw";
            case "se": return "Europe/Stockholm";
            case "no": return "Europe/Oslo";
            case "fi": return "Europe/Helsinki";
            case "dk": return "Europe/Copenhagen";
            case "at": return "Europe/Vienna";
            case "ch": return "Europe/Zurich";
            case "pt": return "Europe/Lisbon";
            case "be": return "Europe/Brussels";
            case "ie": return "Europe/Dublin";
            case "nz": return "Pacific/Auckland";
            case "za": return "Africa/Johannesburg";
            case "tw": return "Asia/Taipei";
            case "hk": return "Asia/Hong_Kong";
            case "cn": return "Asia/Shanghai";
            default: return null;
        }
    }

    static String getTimezoneOffsetForCountry(String countryIso) {
        String tzId = getTimezoneForCountry(countryIso);
        if (tzId == null) return null;
        try {
            java.util.TimeZone tz = java.util.TimeZone.getTimeZone(tzId);
            int offsetMs = tz.getRawOffset();
            return String.valueOf(offsetMs / 1000);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static volatile boolean sUrlQueryParamsHooked = false;

    private static final Set<String> URL_REGION_PARAMS = new HashSet<>(Arrays.asList(
            "carrier_region", "carrier_region1", "carrier_region_v2",
            "sys_region", "account_region", "residence", "region",
            "app_region", "device_region", "user_region", "current_region",
            "op_region", "store_region", "sim_region", "priority_region",
            "reg_store_region", "user_selected_region", "store_country",
            "country_code", "country"
    ));

    private static void hookUrlQueryParams(ClassLoader classLoader) {
        if (sUrlQueryParamsHooked) return;
        sUrlQueryParamsHooked = true;

        try {
            XposedHelpers.findAndHookMethod(
                    Uri.Builder.class,
                    "appendQueryParameter",
                    String.class, String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!sEnabled) return;
                            String key = (String) param.args[0];
                            if (key == null) return;

                            if (URL_REGION_PARAMS.contains(key)) {
                                param.args[1] = sCountryIso.toUpperCase(Locale.ROOT);
                            } else if ("mcc_mnc".equals(key)) {
                                param.args[1] = sOperatorMccMnc;
                            } else if ("timezone_name".equals(key) || "tz_name".equals(key)) {
                                String tz = getTimezoneForCountry(sCountryIso);
                                if (tz != null) param.args[1] = tz;
                            } else if ("timezone_offset".equals(key) || "tz_offset".equals(key)) {
                                String tzOff = getTimezoneOffsetForCountry(sCountryIso);
                                if (tzOff != null) param.args[1] = tzOff;
                            } else if ("content_language".equals(key)) {
                                if (sLanguageFilter && !sAllowedLanguages.isEmpty()) {
                                    param.args[1] = String.join(",", sAllowedLanguages);
                                } else if (sSpoofLocale) {
                                    param.args[1] = sLocaleLang;
                                }
                            } else if (sSpoofLocale && "language".equals(key)) {
                                param.args[1] = sLocaleLang;
                            }
                        }
                    }
            );
            log("Hooked Uri.Builder.appendQueryParameter for URL-level region param interception");
        } catch (Throwable t) {
            logD("Uri.Builder.appendQueryParameter hook failed: " + t.getMessage());
        }

        try {
            XposedHelpers.findAndHookMethod(
                    "android.net.Uri$Builder", classLoader,
                    "encodedQuery",
                    String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!sEnabled || param.args == null || param.args[0] == null) return;
                            String query = (String) param.args[0];
                            param.args[0] = rewriteQueryString(query);
                        }
                    }
            );
        } catch (Throwable ignored) {}

        try {
            Class<?> httpUrlBuilder = XposedHelpers.findClassIfExists("okhttp3.HttpUrl$Builder", classLoader);
            if (httpUrlBuilder != null) {
                XposedHelpers.findAndHookMethod(httpUrlBuilder, "addQueryParameter",
                        String.class, String.class,
                        new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) {
                                if (!sEnabled) return;
                                String key = (String) param.args[0];
                                if (key == null) return;

                                if (URL_REGION_PARAMS.contains(key)) {
                                    param.args[1] = sCountryIso.toUpperCase(Locale.ROOT);
                                } else if ("mcc_mnc".equals(key)) {
                                    param.args[1] = sOperatorMccMnc;
                                }
                            }
                        }
                );
                log("Hooked okhttp3.HttpUrl.Builder.addQueryParameter for URL-level region param interception");
            }
        } catch (Throwable ignored) {}
    }

    public static String rewriteQueryString(String query) {
        if (query == null || query.isEmpty()) return query;
        String upper = sCountryIso.toUpperCase(Locale.ROOT);
        for (String param : URL_REGION_PARAMS) {

            query = query.replaceAll("(?<=[?&]|^)" + param + "=[^&]*", param + "=" + upper);
        }
        query = query.replaceAll("(?<=[?&]|^)mcc_mnc=[^&]*", "mcc_mnc=" + sOperatorMccMnc);
        String tz = getTimezoneForCountry(sCountryIso);
        if (tz != null) {
            query = query.replaceAll("(?<=[?&]|^)timezone_name=[^&]*", "timezone_name=" + tz);
            query = query.replaceAll("(?<=[?&]|^)tz_name=[^&]*", "tz_name=" + tz);
        }
        String tzOff = getTimezoneOffsetForCountry(sCountryIso);
        if (tzOff != null) {
            query = query.replaceAll("(?<=[?&]|^)timezone_offset=[^&]*", "timezone_offset=" + tzOff);
            query = query.replaceAll("(?<=[?&]|^)tz_offset=[^&]*", "tz_offset=" + tzOff);
        }
        if (sLanguageFilter && !sAllowedLanguages.isEmpty()) {
            query = query.replaceAll("(?<=[?&]|^)content_language=[^&]*", "content_language=" + String.join(",", sAllowedLanguages));
        } else if (sSpoofLocale) {
            query = query.replaceAll("(?<=[?&]|^)content_language=[^&]*", "content_language=" + sLocaleLang);
        }
        if (sSpoofLocale) {
            query = query.replaceAll("(?<=[?&]|^)language=[^&]*", "language=" + sLocaleLang);
            query = query.replaceAll("(?<=[?&]|^)app_language=[^&]*", "app_language=" + sLocaleLang);
        }
        return query;
    }

    private static void hookContentLanguageService(ClassLoader classLoader) {
        if (classLoader == null) return;
        try {
            Class<?> cls = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.contentlanguage.ContentLanguageServiceImpl", classLoader);
            if (cls != null) {
                XposedHelpers.findAndHookMethod(cls, "getLanguage", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (sLanguageFilter && !sAllowedLanguages.isEmpty()) {
                            param.setResult(new ArrayList<>(sAllowedLanguages));
                        }
                    }
                });
                log("Hooked ContentLanguageServiceImpl.getLanguage()");
            }
        } catch (Throwable ignored) {}
    }

    private void hookTelephonyManager(ClassLoader classLoader) {
        final String className = "android.telephony.TelephonyManager";

        hookMethodReturn(classLoader, className, "getSimCountryIso", new MethodReturnValue() {
            @Override
            public Object getValue() {
                return sEnabled ? sCountryIso.toLowerCase() : null;
            }
        });

        hookMethodReturn(classLoader, className, "getSimCountryIso", new Class<?>[]{int.class}, new MethodReturnValue() {
            @Override
            public Object getValue() {
                return sEnabled ? sCountryIso.toLowerCase() : null;
            }
        });

        hookMethodReturn(classLoader, className, "getNetworkCountryIso", new MethodReturnValue() {
            @Override
            public Object getValue() {
                return sEnabled ? sCountryIso.toLowerCase() : null;
            }
        });

        hookMethodReturn(classLoader, className, "getNetworkCountryIso", new Class<?>[]{int.class}, new MethodReturnValue() {
            @Override
            public Object getValue() {
                return sEnabled ? sCountryIso.toLowerCase() : null;
            }
        });

        hookMethodReturn(classLoader, className, "getSimOperator", new MethodReturnValue() {
            @Override
            public Object getValue() {
                return sEnabled ? sOperatorMccMnc : null;
            }
        });

        hookMethodReturn(classLoader, className, "getSimOperator", new Class<?>[]{int.class}, new MethodReturnValue() {
            @Override
            public Object getValue() {
                return sEnabled ? sOperatorMccMnc : null;
            }
        });

        hookMethodReturn(classLoader, className, "getNetworkOperator", new MethodReturnValue() {
            @Override
            public Object getValue() {
                return sEnabled ? sOperatorMccMnc : null;
            }
        });

        hookMethodReturn(classLoader, className, "getNetworkOperator", new Class<?>[]{int.class}, new MethodReturnValue() {
            @Override
            public Object getValue() {
                return sEnabled ? sOperatorMccMnc : null;
            }
        });

        hookMethodReturn(classLoader, className, "getSimOperatorName", new MethodReturnValue() {
            @Override
            public Object getValue() {
                return sEnabled ? sOperatorName : null;
            }
        });

        hookMethodReturn(classLoader, className, "getSimOperatorName", new Class<?>[]{int.class}, new MethodReturnValue() {
            @Override
            public Object getValue() {
                return sEnabled ? sOperatorName : null;
            }
        });

        hookMethodReturn(classLoader, className, "getNetworkOperatorName", new MethodReturnValue() {
            @Override
            public Object getValue() {
                return sEnabled ? sOperatorName : null;
            }
        });

        hookMethodReturn(classLoader, className, "getNetworkOperatorName", new Class<?>[]{int.class}, new MethodReturnValue() {
            @Override
            public Object getValue() {
                return sEnabled ? sOperatorName : null;
            }
        });

        hookMethodReturn(classLoader, className, "getSimState", new MethodReturnValue() {
            @Override
            public Object getValue() {
                return sEnabled ? 5 : null;
            }
        });

        hookMethodReturn(classLoader, className, "getSimState", new Class<?>[]{int.class}, new MethodReturnValue() {
            @Override
            public Object getValue() {
                return sEnabled ? 5 : null;
            }
        });

        hookMethodReturn(classLoader, className, "getPhoneType", new MethodReturnValue() {
            @Override
            public Object getValue() {
                return sEnabled ? 1 : null;
            }
        });

        hookMethodReturn(classLoader, className, "hasIccCard", new MethodReturnValue() {
            @Override
            public Object getValue() {
                return sEnabled ? true : null;
            }
        });

        hookMethodReturn(classLoader, className, "hasIccCard", new Class<?>[]{int.class}, new MethodReturnValue() {
            @Override
            public Object getValue() {
                return sEnabled ? true : null;
            }
        });

        hookMethodReturn(classLoader, className, "isNetworkRoaming", new MethodReturnValue() {
            @Override
            public Object getValue() {
                return sEnabled ? false : null;
            }
        });

        hookMethodReturn(classLoader, className, "isNetworkRoaming", new Class<?>[]{int.class}, new MethodReturnValue() {
            @Override
            public Object getValue() {
                return sEnabled ? false : null;
            }
        });

        hookMethodReturn(classLoader, className, "getLine1Number", new MethodReturnValue() {
            @Override
            public Object getValue() {
                return sEnabled ? "" : null;
            }
        });

        hookMethodReturn(classLoader, className, "getLine1Number", new Class<?>[]{int.class}, new MethodReturnValue() {
            @Override
            public Object getValue() {
                return sEnabled ? "" : null;
            }
        });

        hookMethodReturn(classLoader, className, "getSubscriberId", new MethodReturnValue() {
            @Override
            public Object getValue() {
                return sEnabled ? (sOperatorMccMnc + "123456789") : null;
            }
        });

        hookMethodReturn(classLoader, className, "getSubscriberId", new Class<?>[]{int.class}, new MethodReturnValue() {
            @Override
            public Object getValue() {
                return sEnabled ? (sOperatorMccMnc + "123456789") : null;
            }
        });

        hookMethodReturn(classLoader, className, "getSimSerialNumber", new MethodReturnValue() {
            @Override
            public Object getValue() {
                return sEnabled ? ("8901260" + sOperatorMccMnc + "12345") : null;
            }
        });

        hookMethodReturn(classLoader, className, "getSimSerialNumber", new Class<?>[]{int.class}, new MethodReturnValue() {
            @Override
            public Object getValue() {
                return sEnabled ? ("8901260" + sOperatorMccMnc + "12345") : null;
            }
        });
    }

    private void hookSubscriptionManager(ClassLoader classLoader) {
        final String className = "android.telephony.SubscriptionManager";

        hookMethodReturn(classLoader, className, "getActiveSubscriptionInfoCount", new MethodReturnValue() {
            @Override
            public Object getValue() {
                return sEnabled ? 1 : null;
            }
        });

        hookMethodReturn(classLoader, className, "getDefaultSubscriptionId", new MethodReturnValue() {
            @Override
            public Object getValue() {
                return sEnabled ? 1 : null;
            }
        });

        hookMethodReturn(classLoader, className, "getDefaultDataSubscriptionId", new MethodReturnValue() {
            @Override
            public Object getValue() {
                return sEnabled ? 1 : null;
            }
        });
    }

    private void hookSubscriptionInfo(ClassLoader classLoader) {
        final String className = "android.telephony.SubscriptionInfo";

        hookMethodReturn(classLoader, className, "getCountryIso", new MethodReturnValue() {
            @Override
            public Object getValue() {
                return sEnabled ? sCountryIso.toLowerCase() : null;
            }
        });

        hookMethodReturn(classLoader, className, "getMcc", new MethodReturnValue() {
            @Override
            public Object getValue() {
                if (!sEnabled) return null;
                try {
                    return Integer.parseInt(sMcc);
                } catch (Throwable ignored) {
                    return 310;
                }
            }
        });

        hookMethodReturn(classLoader, className, "getMnc", new MethodReturnValue() {
            @Override
            public Object getValue() {
                if (!sEnabled) return null;
                try {
                    return Integer.parseInt(sMnc);
                } catch (Throwable ignored) {
                    return 260;
                }
            }
        });

        hookMethodReturn(classLoader, className, "getMccString", new MethodReturnValue() {
            @Override
            public Object getValue() {
                return sEnabled ? sMcc : null;
            }
        });

        hookMethodReturn(classLoader, className, "getMncString", new MethodReturnValue() {
            @Override
            public Object getValue() {
                return sEnabled ? sMnc : null;
            }
        });

        hookMethodReturn(classLoader, className, "getCarrierName", new MethodReturnValue() {
            @Override
            public Object getValue() {
                return sEnabled ? sOperatorName : null;
            }
        });

        hookMethodReturn(classLoader, className, "getDisplayName", new MethodReturnValue() {
            @Override
            public Object getValue() {
                return sEnabled ? sOperatorName : null;
            }
        });
    }

    private void hookSystemProperties(ClassLoader classLoader) {
        try {
            Class<?> sysPropClass = XposedHelpers.findClassIfExists("android.os.SystemProperties", classLoader);
            if (sysPropClass == null) return;

            XC_MethodHook propHook = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!sEnabled || param.args == null || param.args.length == 0) return;
                    String key = (String) param.args[0];
                    if (key == null || !key.startsWith("gsm.")) return;

                    if (key.startsWith("gsm.sim.operator.iso-country") || key.startsWith("gsm.operator.iso-country")) {
                        param.setResult(sCountryIso.toLowerCase());
                    } else if (key.startsWith("gsm.sim.operator.numeric") || key.startsWith("gsm.operator.numeric")) {
                        param.setResult(sOperatorMccMnc);
                    } else if (key.startsWith("gsm.sim.operator.alpha") || key.startsWith("gsm.operator.alpha")) {
                        param.setResult(sOperatorName);
                    } else if (key.startsWith("gsm.sim.state")) {
                        param.setResult("LOADED");
                    }
                }
            };

            XposedHelpers.findAndHookMethod(sysPropClass, "get", String.class, propHook);
            XposedHelpers.findAndHookMethod(sysPropClass, "get", String.class, String.class, propHook);
        } catch (Throwable t) {
            Log.d(TAG, "SystemProperties hook failed: " + t.getMessage());
        }
    }

    private void hookLocale(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                    Locale.class,
                    "getDefault",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (sEnabled && sSpoofLocale) {
                                param.setResult(sCachedSpoofedLocale != null ? sCachedSpoofedLocale : new Locale(sLocaleLang, sLocaleCountry));
                            }
                        }
                    }
            );
        } catch (Throwable t) {
            Log.d(TAG, "Locale hook failed: " + t.getMessage());
        }
    }

    private interface MethodReturnValue {
        Object getValue();
    }

    private static void hookMethodReturn(ClassLoader classLoader, String className, String methodName, final MethodReturnValue callback) {
        hookMethodReturn(classLoader, className, methodName, new Class<?>[0], callback);
    }

    private static void hookMethodReturn(ClassLoader classLoader, String className, String methodName, Class<?>[] parameterTypes, final MethodReturnValue callback) {
        try {
            Class<?> clazz = XposedHelpers.findClassIfExists(className, classLoader);
            if (clazz == null) return;

            Object[] args = new Object[parameterTypes.length + 1];
            System.arraycopy(parameterTypes, 0, args, 0, parameterTypes.length);
            args[parameterTypes.length] = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (sEnabled) {
                        Object val = callback.getValue();
                        if (val != null) {
                            param.setResult(val);
                        }
                    }
                }
            };

            XposedHelpers.findAndHookMethod(clazz, methodName, args);
        } catch (Throwable ignored) {
        }
    }
}

