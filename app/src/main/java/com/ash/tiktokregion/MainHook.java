package com.ash.tiktokregion;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import android.content.ContextWrapper;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "TikTokRegion";
    public static final String MODULE_PACKAGE = "com.ash.tiktokregion";

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
    private static volatile boolean sHideAds = true;
    private static volatile boolean sForceRegion = true;
    private static volatile boolean sStrictForceRegion = false;
    private static volatile boolean sDownloadStory = true;
    private static volatile boolean sBlockCountries = false;
    private static volatile Set<String> sBlockedCountries = java.util.Collections.emptySet();

    private static volatile Context sAppContext = null;
    private static volatile long sLastConfigFetchTime = 0;
    private static final long CONFIG_CACHE_MS = 5000;

    public static boolean isNoWatermarkEnabled() {
        return sNoWatermark;
    }

    public static boolean isBypassDownloadRestrictionEnabled() {
        return sBypassDownloadRestriction;
    }

    public static boolean isHideAdsEnabled() {
        return sHideAds;
    }

    public static boolean isForceRegionEnabled() {
        return sForceRegion;
    }

    public static boolean isStrictForceRegionEnabled() {
        return sStrictForceRegion;
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
            hookTelephonyManager(lpparam.classLoader);
            hookSubscriptionManager(lpparam.classLoader);
            hookSubscriptionInfo(lpparam.classLoader);
            hookSystemProperties(lpparam.classLoader);
            hookLocale(lpparam.classLoader);
        } else {
            XposedBridge.log(TAG + ": TikTok China (Douyin) detected - skipping telephony, locale, and network region spoofing");
        }

        WatermarkHook.hook(lpparam.classLoader);
        AdsHook.hook(lpparam.classLoader);

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
                Log.i(TAG, "Hooked isModuleActive() -> true");
            }
        } catch (Throwable t) {
            Log.e(TAG, "Failed hooking isModuleActive", t);
        }
    }

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
                if (ctx != null) {
                    installDeferredHooks(ctx.getClassLoader());
                    new Thread(() -> refreshConfig(ctx), "TikTokEnhancer-ConfigRefresh").start();
                }
            }
        };

        try {
            XposedHelpers.findAndHookMethod(ContextWrapper.class, "attachBaseContext", Context.class, appAttachHook);
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
                            if (ctx != null) {
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

    private void hookClassLoader(ClassLoader classLoader) {
        if (sClassLoaderHooked) return;
        sClassLoaderHooked = true;

        XC_MethodHook loadClassHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                Object result = param.getResult();
                if (result instanceof Class<?>) {
                    onClassLoaded((Class<?>) result);
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
        if ("com.bytedance.ttnet.TTNetInit".equals(name)) {
            hookTTNetInitClass(clazz);
        } else if ("com.ss.ugc.clientai.core.api.FeatureProducer".equals(name)) {
            hookFeatureProducerClass(clazz);
        } else if ("com.ss.android.ugc.aweme.setting.services.SettingServiceImpl".equals(name)) {
            hookSettingServiceImplClass(clazz);
        } else if ("X.03IJ".equals(name) || "LX.03IJ".equals(name)) {
            hookParamMapClass(clazz);
        } else if ("com.ss.android.ugc.aweme.watermark.WaterMarkServiceImpl".equals(name)
                || "com.ss.android.ugc.aweme.services.watermark.WaterMarkBuilder".equals(name)) {
            WatermarkHook.hookWatermarkServiceClass(clazz);
        } else if ("com.ss.android.ugc.aweme.base.model.UrlModel".equals(name)) {
            WatermarkHook.hookUrlModelClass(clazz);
        } else if ("com.ss.android.ugc.aweme.feed.model.FeedItemList".equals(name)) {
            AdsHook.hookFeedItemListClass(clazz);
        } else if ("com.ss.android.ugc.aweme.feed.panel.BaseListFragmentPanel".equals(name)
                || "com.ss.android.ugc.aweme.feed.panel.FullFeedFragmentPanel".equals(name)) {
            AdsHook.hookFeedPanel(clazz.getClassLoader());
        } else if ("com.ss.android.ugc.aweme.feed.model.Aweme".equals(name)) {
            WatermarkHook.hookAwemeClass(clazz);
        }
    }

    public static void installDeferredHooks(ClassLoader classLoader) {
        if (classLoader == null) return;
        boolean isChina = isChinaPackage();
        if (!isChina) {
            hookCronetNetworkStack(classLoader);
            hookClientAIFeatures(classLoader);
        }
        WatermarkHook.hook(classLoader);
        AdsHook.hook(classLoader);
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
                    sHideAds = bundle.getBoolean(ConfigProvider.KEY_HIDE_ADS, true);
                    sForceRegion = bundle.getBoolean(ConfigProvider.KEY_FORCE_REGION, true);
                    sStrictForceRegion = bundle.getBoolean(ConfigProvider.KEY_STRICT_FORCE_REGION, false);
                    sDownloadStory = bundle.getBoolean(ConfigProvider.KEY_DOWNLOAD_STORY, true);
                    sBlockCountries = bundle.getBoolean(ConfigProvider.KEY_BLOCK_COUNTRIES, false);
                    sBlockedCountries = parseIsoSet(bundle.getString(ConfigProvider.KEY_BLOCKED_COUNTRY_LIST, ""));

                    Log.i(TAG, "Config loaded via ContentProvider IPC: enabled=" + sEnabled
                            + ", country=" + sCountryIso + ", op=" + sOperatorName + " (" + sOperatorMccMnc + ")"
                            + ", forceRegion=" + sForceRegion + ", strictForceRegion=" + sStrictForceRegion
                            + ", blockCountries=" + sBlockCountries + " (" + sBlockedCountries.size() + ")"
                            + ", hideAds=" + sHideAds + ", downloadStory=" + sDownloadStory);
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
            sHideAds = prefs.getBoolean(MainActivity.KEY_HIDE_ADS, true);
            sForceRegion = prefs.getBoolean(MainActivity.KEY_FORCE_REGION, true);
            sStrictForceRegion = prefs.getBoolean(MainActivity.KEY_STRICT_FORCE_REGION, false);
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

            Log.i(TAG, "Config loaded via XSharedPreferences: enabled=" + sEnabled
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
                                    Log.i(TAG, "Wrapped TTNetInit.setCronetDepend in dynamic proxy");
                                }
                            }
                        }
                    });
                    sTTNetInitHooked = true;
                    Log.i(TAG, "Hooked TTNetInit.setCronetDepend successfully");
                }
            }
            checkAndProxyExistingCronetProvider(ttnetInitClass);
        } catch (Throwable t) {
            Log.d(TAG, "hookTTNetInitClass failed: " + t.getMessage());
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
                    Log.i(TAG, "Replaced existing TTNetInit.sCronetProvider with dynamic proxy");
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

        Class<?> paramMapClass = XposedHelpers.findClassIfExists("X.03IJ", classLoader);
        if (paramMapClass == null) {
            paramMapClass = XposedHelpers.findClassIfExists("LX.03IJ", classLoader);
        }
        if (paramMapClass != null) {
            hookParamMapClass(paramMapClass);
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
                        if ("f_global_carrier_region_v2".equals(featureKey)
                                || "f_global_sys_region".equals(featureKey)
                                || "f_global_account_region".equals(featureKey)
                                || "f_global_residence".equals(featureKey)) {
                            param.setResult(sCountryIso.toUpperCase(Locale.ROOT));
                        } else if ("f_global_mcc_mnc".equals(featureKey)) {
                            param.setResult(sOperatorMccMnc);
                        }
                    }
                }
            };

            for (Method m : fpClass.getDeclaredMethods()) {
                if (m.getName().startsWith("getStringFeature")) {
                    XposedBridge.hookMethod(m, featureHook);
                }
            }
            sFeatureProducerHooked = true;
            Log.i(TAG, "Hooked FeatureProducer.getStringFeature for ClientAI parameters");
        } catch (Throwable t) {
            Log.d(TAG, "hookFeatureProducerClass failed: " + t.getMessage());
        }
    }

    public static void hookSettingServiceImplClass(Class<?> settingServiceClass) {
        if (settingServiceClass == null) return;
        try {
            for (Method m : settingServiceClass.getDeclaredMethods()) {
                if ("installCommonParams".equals(m.getName()) && m.getParameterTypes().length == 0) {
                    XposedBridge.hookMethod(m, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            // Handled by LX.03IJ map hook
                        }
                    });
                }
            }
        } catch (Throwable ignored) {}
    }

    public static void hookParamMapClass(Class<?> paramMapClass) {
        if (paramMapClass == null || sParamMapHooked) return;
        try {
            for (Method m : paramMapClass.getDeclaredMethods()) {
                if ("LIZ".equals(m.getName()) && m.getParameterTypes().length == 2
                        && m.getParameterTypes()[0] == String.class && m.getParameterTypes()[1] == String.class) {
                    XposedBridge.hookMethod(m, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!sEnabled) return;
                            String key = (String) param.args[0];
                            if ("carrier_region".equals(key)
                                    || "carrier_region_v2".equals(key)
                                    || "sys_region".equals(key)
                                    || "account_region".equals(key)
                                    || "residence".equals(key)) {
                                param.args[1] = sCountryIso.toUpperCase(Locale.ROOT);
                            } else if ("mcc_mnc".equals(key)) {
                                param.args[1] = sOperatorMccMnc;
                            }
                        }
                    });
                    sParamMapHooked = true;
                    Log.i(TAG, "Hooked LX.03IJ.LIZ for network common parameter map");
                }
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

