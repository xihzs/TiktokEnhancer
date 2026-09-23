package com.ash.tiktokregion;

import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Dedicated hook module for Douyin (TikTok China - com.ss.android.ugc.aweme)
 * to bypass video watermark synthesis, outro cards, and download restrictions
 * on modern versions (v40.6.0+).
 */
public final class DouyinWatermarkHook {

    private static final String TAG = "TikTokEnhancer-DouyinWM";

    private static final Set<Object> sCleanedVideos = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private static volatile boolean sWatermarkServiceHooked = false;
    private static volatile boolean sMultiStateDownloadHooked = false;
    private static volatile boolean sSocialVMHooked = false;
    private static volatile boolean sAwemeHooked = false;
    private static volatile boolean sVideoHooked = false;
    private static volatile boolean sDownloadHelperHooked = false;
    private static volatile boolean sDownloadCallbackHooked = false;
    private static volatile boolean sComposerHooked = false;
    private static volatile boolean sPermResultHooked = false;
    private static volatile boolean sConsumerPermHooked = false;
    private static volatile boolean sPrivacyPermHooked = false;

    private DouyinWatermarkHook() {}

    /**
     * Entry point called from MainHook to hook Douyin components.
     */
    public static void hook(ClassLoader classLoader) {
        if (classLoader == null) return;
        try {
            hookAweme(classLoader);
            hookVideo(classLoader);
            hookWatermarkService(classLoader);
            hookDouyinDownload(classLoader);
            hookDownloadPipeline(classLoader);
            XposedBridge.log(TAG + ": Douyin hooks dispatched for classLoader");
        } catch (Throwable t) {
            Log.e(TAG, "DouyinWatermarkHook.hook failed", t);
        }
    }

    /**
     * Callback when any class is dynamically loaded in Douyin.
     */
    public static void onClassLoaded(Class<?> clazz) {
        if (clazz == null) return;
        String name = clazz.getName();
        if (name.startsWith("android.") || name.startsWith("java.") || name.startsWith("javax.")
                || name.startsWith("kotlin.") || name.startsWith("androidx.") || name.startsWith("com.google.")
                || name.startsWith("com.ash.tiktokregion.")) {
            return;
        }

        try {
            if ("com.ss.android.ugc.aweme.feed.model.Aweme".equals(name)) {
                hookAwemeClass(clazz);
            } else if ("com.ss.android.ugc.aweme.feed.model.Video".equals(name)) {
                hookVideoClass(clazz);
            } else if ("com.ss.android.ugc.aweme.watermark.WaterMarkServiceImpl".equals(name)
                    || "com.ss.android.ugc.aweme.services.watermark.WaterMarkBuilder".equals(name)) {
                hookWatermarkServiceClass(clazz);
            } else if ("com.ss.android.ugc.aweme.share.socialpanel.viewholder.MultiStateDownloadViewHolder".equals(name)
                    || name.endsWith(".MultiStateDownloadViewHolder")) {
                hookMultiStateDownloadClass(clazz);
            } else if ("com.ss.android.ugc.aweme.share.socialpanel.viewmodel.SocialActionsPanelVM".equals(name)) {
                hookSocialActionsPanelVMClass(clazz);
            } else if ("X.0xMa".equals(name) || "LX.0xMa".equals(name)
                    || "X.0xEd".equals(name) || "LX.0xEd".equals(name)
                    || "X.1Ny3".equals(name) || "LX.1Ny3".equals(name)
                    || "X.0muS".equals(name) || "LX.0muS".equals(name)) {
                hookActionClass(clazz);
            } else if ("X.1W4H".equals(name) || "LX.1W4H".equals(name)
                    || "X.1Nym".equals(name) || "LX.1Nym".equals(name)) {
                hookDownloadHelperClass(clazz);
            } else if ("X.1W4I".equals(name) || "LX.1W4I".equals(name)
                    || "X.1Nyo".equals(name) || "LX.1Nyo".equals(name)) {
                hookDownloadCallbackClass(clazz);
            } else if ("X.1W4J".equals(name) || "LX.1W4J".equals(name)) {
                hookWaterMarkComposerClass(clazz);
            } else if ("X.0xFq".equals(name) || "LX.0xFq".equals(name)) {
                hookPermissionResultClass(clazz);
            } else if ("com.ss.android.ugc.aweme.privacy.service.ConsumerPermissionService".equals(name)
                    || "com.ss.android.ugc.aweme.spi.ConsumerPermissionServiceImp".equals(name)
                    || "com.ss.android.ugc.aweme.privacy.service.IConsumerPermissionService".equals(name)) {
                hookConsumerPermissionClass(clazz);
            } else if ("com.ss.android.ugc.aweme.spi.PrivacyPermissionServiceImpl".equals(name)
                    || "com.ss.android.ugc.aweme.privacy.service.PrivacyPermissionService".equals(name)
                    || "com.ss.android.ugc.aweme.privacy.service.IPrivacyPermissionService".equals(name)) {
                hookPrivacyPermissionClass(clazz);
            }
        } catch (Throwable t) {
            Log.d(TAG, "onClassLoaded error for " + name + ": " + t.getMessage());
        }
    }

    /**
     * Hook Aweme download flags for Douyin to enable downloads.
     */
    private static void hookAweme(ClassLoader classLoader) {
        if (sAwemeHooked || classLoader == null) return;
        try {
            Class<?> clazz = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.feed.model.Aweme", classLoader);
            if (clazz != null) {
                sAwemeHooked = true;
                hookAwemeClass(clazz);
            }
        } catch (Throwable ignored) {}
    }

    public static void hookAwemeClass(Class<?> awemeClass) {
        if (awemeClass == null) return;

        try {
            XposedHelpers.findAndHookMethod(awemeClass, "getDownloadWithoutWatermark", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (MainHook.isNoWatermarkEnabled()) {
                        param.setResult(true);
                    }
                }
            });
            XposedBridge.log(TAG + ": Hooked Aweme.getDownloadWithoutWatermark() -> true");
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(awemeClass, "isPreventDownload", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (MainHook.isBypassDownloadRestrictionEnabled() || MainHook.isNoWatermarkEnabled()) {
                        param.setResult(false);
                    }
                }
            });
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(awemeClass, "getDownloadStatus", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (MainHook.isBypassDownloadRestrictionEnabled() || MainHook.isNoWatermarkEnabled()) {
                        param.setResult(0); // 0 = download allowed
                    }
                }
            });
            XposedBridge.log(TAG + ": Hooked Aweme.getDownloadStatus() -> 0");
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(awemeClass, "needTTSWatermarkWhenDownload", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (MainHook.isNoWatermarkEnabled()) {
                        param.setResult(false);
                    }
                }
            });
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(awemeClass, "getVideoControl", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Object vc = param.getResult();
                    if (vc != null && (MainHook.isBypassDownloadRestrictionEnabled() || MainHook.isNoWatermarkEnabled())) {
                        try { XposedHelpers.setObjectField(vc, "allowDownload", Boolean.TRUE); } catch (Throwable ignored) {}
                        try { XposedHelpers.setIntField(vc, "preventDownloadType", 0); } catch (Throwable ignored) {}
                        try { XposedHelpers.setObjectField(vc, "shareGrayed", Boolean.FALSE); } catch (Throwable ignored) {}
                        try { XposedHelpers.setObjectField(vc, "downloadIgnoreVisibility", Boolean.FALSE); } catch (Throwable ignored) {}
                        try { XposedHelpers.setObjectField(vc, "shareIgnoreVisibility", Boolean.FALSE); } catch (Throwable ignored) {}
                        try { XposedHelpers.setIntField(vc, "downloadSetting", 0); } catch (Throwable ignored) {}
                        try { XposedHelpers.setObjectField(vc, "allowShare", Boolean.TRUE); } catch (Throwable ignored) {}
                        try { XposedHelpers.setBooleanField(vc, "showWatermark", false); } catch (Throwable ignored) {}
                    }
                }
            });
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(awemeClass, "getAwemeControl", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Object control = param.getResult();
                    if (control != null && (MainHook.isBypassDownloadRestrictionEnabled() || MainHook.isNoWatermarkEnabled())) {
                        try { XposedHelpers.setBooleanField(control, "b", true); } catch (Throwable ignored) {}
                        try { XposedHelpers.setBooleanField(control, "d", true); } catch (Throwable ignored) {}
                        try { XposedHelpers.setBooleanField(control, "a", true); } catch (Throwable ignored) {}
                        try { XposedHelpers.setBooleanField(control, "e", true); } catch (Throwable ignored) {}
                        try { XposedHelpers.setBooleanField(control, "canShare", true); } catch (Throwable ignored) {}
                    }
                }
            });
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(awemeClass, "getVideo", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (param.thisObject != null) {
                        WatermarkHook.sCurrentAweme = param.thisObject;
                    }
                    Object video = param.getResult();
                    if (video != null) {
                        cleanDouyinVideo(video);
                    }
                }
            });
        } catch (Throwable ignored) {}
    }

    /**
     * Hook Video model to redirect downloadAddr to clean playAddr and clean internal fields.
     */
    private static void hookVideo(ClassLoader classLoader) {
        if (sVideoHooked || classLoader == null) return;
        try {
            Class<?> clazz = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.feed.model.Video", classLoader);
            if (clazz != null) {
                sVideoHooked = true;
                hookVideoClass(clazz);
            }
        } catch (Throwable ignored) {}
    }

    public static void hookVideoClass(Class<?> videoClass) {
        if (videoClass == null) return;

        XC_MethodHook redirectHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (!MainHook.isNoWatermarkEnabled()) return;
                Object video = param.thisObject;
                if (video == null) return;

                cleanDouyinVideo(video);
                Object playAddr = WatermarkHook.getCleanPlayAddr(video);
                if (playAddr != null) {
                    WatermarkHook.cleanUrlModel(playAddr);
                    param.setResult(playAddr);
                }
            }
        };

        String[] methods = {"getDownloadAddr", "getNewDownloadAddr", "getDownloadNoWatermarkAddr", "getUIAlikeDownloadAddr"};
        for (String method : methods) {
            try {
                XposedBridge.hookAllMethods(videoClass, method, redirectHook);
                XposedBridge.log(TAG + ": Hooked Douyin Video." + method + "() -> redirect to playAddr");
            } catch (Throwable ignored) {}
        }
    }

    /**
     * Clean Video instance fields directly (field v is downloadAddr, A is suffix download in Douyin 40.x).
     */
    public static void cleanDouyinVideo(Object video) {
        if (video == null || !MainHook.isNoWatermarkEnabled()) return;
        if (sCleanedVideos.contains(video)) return;
        sCleanedVideos.add(video);

        try {
            Object playAddr = WatermarkHook.getCleanPlayAddr(video);
            if (playAddr != null) {
                WatermarkHook.cleanUrlModel(playAddr);
                // In Douyin 40.6.0: v is downloadAddr, a is playAddr, LJIILJJIL() is getPlayAddr()
                try { XposedHelpers.setObjectField(video, "v", playAddr); } catch (Throwable ignored) {}
                try { XposedHelpers.setObjectField(video, "A", playAddr); } catch (Throwable ignored) {}
                try { XposedHelpers.setObjectField(video, "downloadAddr", playAddr); } catch (Throwable ignored) {}
                try { XposedHelpers.setObjectField(video, "newDownloadAddr", playAddr); } catch (Throwable ignored) {}
                try { XposedHelpers.setObjectField(video, "downloadNoWatermarkAddr", playAddr); } catch (Throwable ignored) {}
                try { XposedHelpers.setObjectField(video, "ui_alike_download_addr", playAddr); } catch (Throwable ignored) {}
                try { XposedHelpers.setObjectField(video, "suffixLogoAddr", null); } catch (Throwable ignored) {}
                try { XposedHelpers.setObjectField(video, "downloadSuffixLogoAddr", null); } catch (Throwable ignored) {}
                try { XposedHelpers.setObjectField(video, "caption_download_addr", null); } catch (Throwable ignored) {}
                try { XposedHelpers.setBooleanField(video, "w", false); } catch (Throwable ignored) {}
                try { XposedHelpers.setBooleanField(video, "C", false); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }

    public static void cleanDouyinAweme(Object aweme) {
        if (aweme == null || !MainHook.isNoWatermarkEnabled()) return;
        try {
            Object video = null;
            try { video = XposedHelpers.getObjectField(aweme, "video"); } catch (Throwable ignored) {}
            if (video == null) {
                try { video = XposedHelpers.callMethod(aweme, "getVideo"); } catch (Throwable ignored) {}
            }
            if (video != null) {
                cleanDouyinVideo(video);
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Hook Douyin download pipeline controller classes:
     * - 1W4H (formerly 1Nym): GalleryShareHelper / download controller
     * - 1W4I (formerly 1Nyo): download callback & URL setup
     * - 1W4J: watermark composer / gallery save router
     * - 0xFq: permission evaluation result
     * - ConsumerPermissionService: download permission service
     * - PrivacyPermissionServiceImpl: privacy check service
     */
    private static void hookDownloadPipeline(ClassLoader classLoader) {
        if (classLoader == null) return;

        // 1. Hook 1W4H (download controller)
        if (!sDownloadHelperHooked) {
            try {
                Class<?> helperClass = XposedHelpers.findClassIfExists("X.1W4H", classLoader);
                if (helperClass == null) helperClass = XposedHelpers.findClassIfExists("LX.1W4H", classLoader);
                if (helperClass == null) helperClass = XposedHelpers.findClassIfExists("X.1Nym", classLoader);
                if (helperClass == null) helperClass = XposedHelpers.findClassIfExists("LX.1Nym", classLoader);
                if (helperClass != null) {
                    sDownloadHelperHooked = true;
                    hookDownloadHelperClass(helperClass);
                }
            } catch (Throwable ignored) {}
        }

        // 2. Hook 1W4I (download callback setup)
        if (!sDownloadCallbackHooked) {
            try {
                Class<?> cbClass = XposedHelpers.findClassIfExists("X.1W4I", classLoader);
                if (cbClass == null) cbClass = XposedHelpers.findClassIfExists("LX.1W4I", classLoader);
                if (cbClass == null) cbClass = XposedHelpers.findClassIfExists("X.1Nyo", classLoader);
                if (cbClass == null) cbClass = XposedHelpers.findClassIfExists("LX.1Nyo", classLoader);
                if (cbClass != null) {
                    sDownloadCallbackHooked = true;
                    hookDownloadCallbackClass(cbClass);
                }
            } catch (Throwable ignored) {}
        }

        // 3. Hook 1W4J (watermark composer)
        if (!sComposerHooked) {
            try {
                Class<?> composerClass = XposedHelpers.findClassIfExists("X.1W4J", classLoader);
                if (composerClass == null) composerClass = XposedHelpers.findClassIfExists("LX.1W4J", classLoader);
                if (composerClass != null) {
                    sComposerHooked = true;
                    hookWaterMarkComposerClass(composerClass);
                }
            } catch (Throwable ignored) {}
        }

        // 4. Hook 0xFq (permission evaluation result)
        if (!sPermResultHooked) {
            try {
                Class<?> fqClass = XposedHelpers.findClassIfExists("X.0xFq", classLoader);
                if (fqClass == null) fqClass = XposedHelpers.findClassIfExists("LX.0xFq", classLoader);
                if (fqClass != null) {
                    sPermResultHooked = true;
                    hookPermissionResultClass(fqClass);
                }
            } catch (Throwable ignored) {}
        }

        // 5. Hook ConsumerPermissionService
        if (!sConsumerPermHooked) {
            try {
                Class<?> cpClass = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.privacy.service.ConsumerPermissionService", classLoader);
                if (cpClass != null) {
                    sConsumerPermHooked = true;
                    hookConsumerPermissionClass(cpClass);
                }
                Class<?> cpiClass = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.spi.ConsumerPermissionServiceImp", classLoader);
                if (cpiClass != null) {
                    hookConsumerPermissionClass(cpiClass);
                }
            } catch (Throwable ignored) {}
        }

        // 6. Hook PrivacyPermissionServiceImpl
        if (!sPrivacyPermHooked) {
            try {
                Class<?> ppiClass = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.spi.PrivacyPermissionServiceImpl", classLoader);
                if (ppiClass != null) {
                    sPrivacyPermHooked = true;
                    hookPrivacyPermissionClass(ppiClass);
                }
            } catch (Throwable ignored) {}
        }
    }

    public static void hookDownloadHelperClass(Class<?> helperClass) {
        if (helperClass == null) return;
        try {
            XposedBridge.hookAllConstructors(helperClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    clearWatermarkFlags(param.thisObject);
                }
            });

            String[] triggerMethods = {"LJIJ", "LJIILL", "LJIJI"};
            for (String mName : triggerMethods) {
                XposedBridge.hookAllMethods(helperClass, mName, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        clearWatermarkFlags(param.thisObject);
                        if (param.args != null && param.args.length > 0 && param.args[0] != null) {
                            cleanDouyinAweme(param.args[0]);
                            redirectHelperPlayAddr(param.thisObject, param.args[0]);
                        }
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        clearWatermarkFlags(param.thisObject);
                        if (param.args != null && param.args.length > 0 && param.args[0] != null) {
                            redirectHelperPlayAddr(param.thisObject, param.args[0]);
                        }
                    }
                });
            }
            XposedBridge.log(TAG + ": Hooked download helper class " + helperClass.getName());
        } catch (Throwable t) {
            Log.d(TAG, "hookDownloadHelperClass error: " + t.getMessage());
        }
    }

    public static void hookDownloadCallbackClass(Class<?> callbackClass) {
        if (callbackClass == null) return;
        try {
            XposedBridge.hookAllConstructors(callbackClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!MainHook.isNoWatermarkEnabled() || param.args == null) return;
                    try {
                        if (param.args.length > 0 && param.args[0] != null) {
                            clearWatermarkFlags(param.args[0]);
                        }
                        if (param.args.length > 1 && param.args[1] != null) {
                            cleanDouyinAweme(param.args[1]);
                            if (param.args.length > 0 && param.args[0] != null) {
                                redirectHelperPlayAddr(param.args[0], param.args[1]);
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            });

            XposedBridge.hookAllMethods(callbackClass, "LIZ", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!MainHook.isNoWatermarkEnabled()) return;
                    Object obj = param.thisObject;
                    if (obj == null) return;
                    try {
                        Object aweme = XposedHelpers.getObjectField(obj, "LIZ");
                        Object helper = XposedHelpers.getObjectField(obj, "LIZIZ");
                        cleanDouyinAweme(aweme);
                        clearWatermarkFlags(helper);
                        redirectHelperPlayAddr(helper, aweme);
                    } catch (Throwable ignored) {}
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!MainHook.isNoWatermarkEnabled()) return;
                    Object obj = param.thisObject;
                    if (obj == null) return;
                    try {
                        Object aweme = XposedHelpers.getObjectField(obj, "LIZ");
                        Object helper = XposedHelpers.getObjectField(obj, "LIZIZ");
                        clearWatermarkFlags(helper);
                        redirectHelperPlayAddr(helper, aweme);
                    } catch (Throwable ignored) {}
                }
            });
            XposedBridge.log(TAG + ": Hooked download callback class " + callbackClass.getName() + ".LIZ");
        } catch (Throwable t) {
            Log.d(TAG, "hookDownloadCallbackClass error: " + t.getMessage());
        }
    }

    public static void hookWaterMarkComposerClass(Class<?> composerClass) {
        if (composerClass == null) return;
        try {
            XposedBridge.hookAllConstructors(composerClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (param.args != null && param.args.length > 0 && param.args[0] != null) {
                        clearWatermarkFlags(param.args[0]);
                    }
                }
            });

            XposedBridge.hookAllMethods(composerClass, "LIZ", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!MainHook.isNoWatermarkEnabled()) return;
                    try {
                        Object helper = XposedHelpers.getObjectField(param.thisObject, "LIZIZ");
                        clearWatermarkFlags(helper);
                    } catch (Throwable ignored) {}
                }
            });
            XposedBridge.log(TAG + ": Hooked WaterMarkComposer class " + composerClass.getName());
        } catch (Throwable t) {
            Log.d(TAG, "hookWaterMarkComposerClass error: " + t.getMessage());
        }
    }

    private static void clearWatermarkFlags(Object helper) {
        if (helper == null || !MainHook.isNoWatermarkEnabled()) return;
        try { XposedHelpers.setBooleanField(helper, "LJJJJLL", false); } catch (Throwable ignored) {}
        try { XposedHelpers.setBooleanField(helper, "LJJJJZ", false); } catch (Throwable ignored) {}
        try { XposedHelpers.setBooleanField(helper, "LJJJJZI", false); } catch (Throwable ignored) {}
    }

    private static void redirectHelperPlayAddr(Object helper, Object aweme) {
        if (helper == null || aweme == null || !MainHook.isNoWatermarkEnabled()) return;
        try {
            Object cleanPlay = WatermarkHook.getCleanPlayAddrFromAweme(aweme);
            if (cleanPlay != null) {
                WatermarkHook.cleanUrlModel(cleanPlay);
                try { XposedHelpers.setObjectField(helper, "LJJIL", cleanPlay); } catch (Throwable ignored) {}
                try {
                    List<?> urls = (List<?>) XposedHelpers.callMethod(cleanPlay, "getUrlList");
                    if (urls != null && !urls.isEmpty() && urls.get(0) instanceof String) {
                        XposedHelpers.setObjectField(helper, "LJJIZ", urls.get(0));
                    }
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }

    public static void hookPermissionResultClass(Class<?> permResultClass) {
        if (permResultClass == null) return;
        try {
            ClassLoader cl = permResultClass.getClassLoader();
            Class<?> gfEnumClass = XposedHelpers.findClassIfExists("X.0xGF", cl);
            if (gfEnumClass == null) gfEnumClass = XposedHelpers.findClassIfExists("LX.0xGF", cl);
            final Object normalEnum;
            if (gfEnumClass != null && gfEnumClass.isEnum()) {
                Object found = null;
                try {
                    @SuppressWarnings("unchecked")
                    Class<Enum> rawEnumClass = (Class<Enum>) gfEnumClass;
                    found = Enum.valueOf(rawEnumClass, "NORMAL");
                } catch (Throwable t) {
                    Object[] constants = gfEnumClass.getEnumConstants();
                    if (constants != null) {
                        for (Object c : constants) {
                            if ("NORMAL".equalsIgnoreCase(String.valueOf(c))) {
                                found = c;
                                break;
                            }
                        }
                        if (found == null && constants.length > 0) {
                            found = constants[0];
                        }
                    }
                }
                normalEnum = found;
            } else {
                normalEnum = null;
            }

            XposedBridge.hookAllConstructors(permResultClass, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (normalEnum != null && (MainHook.isBypassDownloadRestrictionEnabled() || MainHook.isNoWatermarkEnabled())) {
                        if (param.args != null && param.args.length > 0) {
                            param.args[0] = normalEnum;
                        }
                    }
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (normalEnum != null && (MainHook.isBypassDownloadRestrictionEnabled() || MainHook.isNoWatermarkEnabled())) {
                        try { XposedHelpers.setObjectField(param.thisObject, "LIZ", normalEnum); } catch (Throwable ignored) {}
                    }
                }
            });

            // LIZ() returns true if NOT HIDDEN (normal)
            XposedBridge.hookAllMethods(permResultClass, "LIZ", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (MainHook.isBypassDownloadRestrictionEnabled() || MainHook.isNoWatermarkEnabled()) {
                        param.setResult(true);
                    }
                }
            });

            // LIZIZ() returns true if NORMAL
            XposedBridge.hookAllMethods(permResultClass, "LIZIZ", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (MainHook.isBypassDownloadRestrictionEnabled() || MainHook.isNoWatermarkEnabled()) {
                        param.setResult(true);
                    }
                }
            });

            XposedBridge.log(TAG + ": Hooked 0xFq permission result class");
        } catch (Throwable t) {
            Log.d(TAG, "hookPermissionResultClass error: " + t.getMessage());
        }
    }

    public static void hookConsumerPermissionClass(Class<?> serviceClass) {
        if (serviceClass == null) return;
        try {
            XC_MethodHook normalPermHook = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!MainHook.isBypassDownloadRestrictionEnabled() && !MainHook.isNoWatermarkEnabled()) return;
                    Object res = param.getResult();
                    if (res != null) {
                        try {
                            Class<?> gfClass = XposedHelpers.findClassIfExists("X.0xGF", res.getClass().getClassLoader());
                            if (gfClass == null) gfClass = XposedHelpers.findClassIfExists("LX.0xGF", res.getClass().getClassLoader());
                            if (gfClass != null && gfClass.isEnum()) {
                                Object normal = null;
                                try {
                                    @SuppressWarnings("unchecked")
                                    Class<Enum> rawEnumClass = (Class<Enum>) gfClass;
                                    normal = Enum.valueOf(rawEnumClass, "NORMAL");
                                } catch (Throwable t) {
                                    Object[] constants = gfClass.getEnumConstants();
                                    if (constants != null && constants.length > 0) normal = constants[0];
                                }
                                if (normal != null) {
                                    XposedHelpers.setObjectField(res, "LIZ", normal);
                                }
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            };

            XposedBridge.hookAllMethods(serviceClass, "LIZLLL", normalPermHook);
            XposedBridge.hookAllMethods(serviceClass, "LIZJ", normalPermHook);
            XposedBridge.log(TAG + ": Hooked ConsumerPermission methods on " + serviceClass.getName());
        } catch (Throwable t) {
            Log.d(TAG, "hookConsumerPermissionClass error: " + t.getMessage());
        }
    }

    public static void hookPrivacyPermissionClass(Class<?> serviceClass) {
        if (serviceClass == null) return;
        try {
            XposedBridge.hookAllMethods(serviceClass, "LJJJJJ", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (MainHook.isBypassDownloadRestrictionEnabled() || MainHook.isNoWatermarkEnabled()) {
                        param.setResult(false);
                    }
                }
            });
            XposedBridge.log(TAG + ": Hooked PrivacyPermission.LJJJJJ() -> false");
        } catch (Throwable t) {
            Log.d(TAG, "hookPrivacyPermissionClass error: " + t.getMessage());
        }
    }

    /**
     * Hook WaterMarkServiceImpl dynamically using targeted method names.
     */
    public static void hookWatermarkServiceClass(Class<?> serviceClazz) {
        if (serviceClazz == null) return;

        XC_MethodHook douyinSingleArgWatermarkHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!MainHook.isNoWatermarkEnabled()) return;

                Object builder = (param.args != null && param.args.length > 0) ? param.args[0] : null;
                if (builder == null) return;

                String inPath = null;
                String outPath = null;
                Object listener = null;

                // 1. Direct field lookups (standard obfuscated names in Douyin 40.x / 39.x)
                try { inPath = (String) XposedHelpers.getObjectField(builder, "LIZ"); } catch (Throwable ignored) {}
                try { outPath = (String) XposedHelpers.getObjectField(builder, "LIZIZ"); } catch (Throwable ignored) {}
                try { listener = XposedHelpers.getObjectField(builder, "LJI"); } catch (Throwable ignored) {}

                // 2. Fallback reflection across declared fields
                if (inPath == null || outPath == null) {
                    for (Field f : builder.getClass().getDeclaredFields()) {
                        if (f.getType() == String.class) {
                            try {
                                f.setAccessible(true);
                                String val = (String) f.get(builder);
                                if (val != null && val.length() > 0) {
                                    if (new File(val).exists()) {
                                        inPath = val;
                                    } else if (outPath == null) {
                                        outPath = val;
                                    }
                                }
                            } catch (Throwable ignored) {}
                        }
                    }
                }

                if (listener == null) {
                    for (Field f : builder.getClass().getDeclaredFields()) {
                        try {
                            f.setAccessible(true);
                            Object obj = f.get(builder);
                            if (obj != null) {
                                for (Method m : obj.getClass().getDeclaredMethods()) {
                                    if ("onSuccess".equals(m.getName()) || "LIZ".equals(m.getName())) {
                                        listener = obj;
                                        break;
                                    }
                                }
                            }
                        } catch (Throwable ignored) {}
                        if (listener != null) break;
                    }
                }

                if (inPath != null && outPath != null) {
                    File src = new File(inPath);
                    if (src.exists() && src.length() > 0) {
                        File dst = new File(outPath);
                        File parent = dst.getParentFile();
                        if (parent != null && !parent.exists()) {
                            parent.mkdirs();
                        }
                        if (dst.exists()) {
                            dst.delete();
                        }

                        boolean copied = copyFile(src, dst);
                        if (copied) {
                            XposedBridge.log(TAG + ": Douyin watermark bypassed via clean copy! ("
                                     + param.method.getName() + ") -> " + outPath);

                            if (listener != null) {
                                try {
                                    Method onProg = listener.getClass().getMethod("onProgress", int.class);
                                    onProg.invoke(listener, 100);
                                } catch (Throwable ignored) {}

                                boolean notified = false;
                                try {
                                    Method onSucc = listener.getClass().getMethod("onSuccess", String.class);
                                    onSucc.invoke(listener, outPath);
                                    notified = true;
                                } catch (Throwable ignored) {}

                                if (!notified) {
                                    try {
                                        Method onSucc = listener.getClass().getMethod("onSuccess");
                                        onSucc.invoke(listener);
                                        notified = true;
                                    } catch (Throwable ignored) {}
                                }

                                if (!notified) {
                                    try {
                                        Method liz = listener.getClass().getMethod("LIZ", int.class);
                                        liz.invoke(listener, 0);
                                    } catch (Throwable ignored) {}
                                }
                            }
                            // Suppress actual watermark synthesis entirely
                            param.setResult(null);
                        }
                    }
                }
            }
        };

        XC_MethodHook douyin5ArgHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!MainHook.isNoWatermarkEnabled() || param.args == null || param.args.length < 5) return;
                String inPath = null;
                String outPath = null;
                Object callback = param.args[4];

                if (param.args[1] instanceof String && param.args[2] instanceof String) {
                    inPath = (String) param.args[1];
                    outPath = (String) param.args[2];
                } else if (param.args[0] instanceof String && param.args[1] instanceof String) {
                    inPath = (String) param.args[0];
                    outPath = (String) param.args[1];
                }

                if (inPath != null && outPath != null) {
                    File src = new File(inPath);
                    if (src.exists() && src.length() > 0) {
                        File dst = new File(outPath);
                        File parent = dst.getParentFile();
                        if (parent != null && !parent.exists()) parent.mkdirs();
                        if (dst.exists()) dst.delete();

                        boolean copied = copyFile(src, dst);
                        if (copied) {
                            XposedBridge.log(TAG + ": Douyin 5-arg watermark bypassed! ("
                                    + param.method.getName() + ") -> " + outPath);
                            notify5ArgCallback(callback);
                            param.setResult(null);
                        }
                    }
                }
            }
        };

        try {
            // Hook O0, i5 (single-arg synthesis)
            XposedBridge.hookAllMethods(serviceClazz, "O0", douyinSingleArgWatermarkHook);
            XposedBridge.hookAllMethods(serviceClazz, "i5", douyinSingleArgWatermarkHook);

            // Hook q6, s2, LIZIZ (5-arg synthesis)
            XposedBridge.hookAllMethods(serviceClazz, "q6", douyin5ArgHook);
            XposedBridge.hookAllMethods(serviceClazz, "s2", douyin5ArgHook);
            XposedBridge.hookAllMethods(serviceClazz, "LIZIZ", douyin5ArgHook);

            XposedBridge.log(TAG + ": Hooked WaterMarkServiceImpl methods (O0, q6, LIZIZ, i5, s2)");
        } catch (Throwable t) {
            Log.d(TAG, "hookWatermarkServiceClass failed: " + t.getMessage());
        }
    }

    private static void notify5ArgCallback(Object callback) {
        if (callback == null) return;
        try {
            Method m = callback.getClass().getMethod("LIZ", int.class);
            m.invoke(callback, 0);
        } catch (Throwable t) {
            for (Method m : callback.getClass().getDeclaredMethods()) {
                if (m.getParameterTypes().length == 1 &&
                        (m.getParameterTypes()[0] == int.class || m.getParameterTypes()[0] == Integer.class)) {
                    try {
                        m.setAccessible(true);
                        m.invoke(callback, 0);
                        return;
                    } catch (Throwable ignored) {}
                }
            }
        }
    }

    private static void hookWatermarkService(ClassLoader classLoader) {
        if (sWatermarkServiceHooked || classLoader == null) return;
        try {
            Class<?> serviceClazz = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.watermark.WaterMarkServiceImpl", classLoader);
            if (serviceClazz != null) {
                sWatermarkServiceHooked = true;
                hookWatermarkServiceClass(serviceClazz);
            }
            Class<?> builderClazz = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.services.watermark.WaterMarkBuilder", classLoader);
            if (builderClazz != null) {
                hookWatermarkServiceClass(builderClazz);
            }
        } catch (Throwable t) {
            Log.d(TAG, "hookWatermarkService failed: " + t.getMessage());
        }
    }

    /**
     * Hook Douyin social panel download button and restriction bypass.
     */
    public static void hookDouyinDownload(ClassLoader classLoader) {
        if (classLoader == null) return;

        try {
            hookMultiStateDownload(classLoader);
            hookSocialActionsPanelVM(classLoader);
        } catch (Throwable t) {
            Log.d(TAG, "hookDouyinDownload sub-hooks failed: " + t.getMessage());
        }

        // 1. Hook GcZ / 0GcZ
        try {
            Class<?> gczClass = XposedHelpers.findClassIfExists("X.0GcZ", classLoader);
            if (gczClass == null) gczClass = XposedHelpers.findClassIfExists("LX.0GcZ", classLoader);
            if (gczClass != null) {
                XposedBridge.hookAllMethods(gczClass, "LIZ", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (MainHook.isNoWatermarkEnabled()) {
                            param.setResult(null);
                        }
                    }
                });
            }
        } catch (Throwable ignored) {}

        // 2. Hook Douyin Action classes dynamically
        String[] actionClasses = {"X.0xMa", "LX.0xMa", "X.0xEd", "LX.0xEd", "X.1Ny3", "LX.1Ny3", "X.0muS", "LX.0muS"};
        for (String acName : actionClasses) {
            try {
                Class<?> acClass = XposedHelpers.findClassIfExists(acName, classLoader);
                if (acClass != null) {
                    hookActionClass(acClass);
                }
            } catch (Throwable ignored) {}
        }

        // 3. Hook SheetAction$DefaultImpls
        try {
            Class<?> sheetActionDefault = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.sharer.ui.SheetAction$DefaultImpls", classLoader);
            if (sheetActionDefault != null) {
                XposedBridge.hookAllMethods(sheetActionDefault, "enable", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (MainHook.isBypassDownloadRestrictionEnabled() || MainHook.isNoWatermarkEnabled()) {
                            Object target = (param.args != null && param.args.length > 0) ? param.args[0] : null;
                            if (target != null && target.getClass().getName().contains("0xEd")) {
                                // In ACListener: if (enable(r8) == 0) goto execute. Wrapper 0xEd must return false to execute!
                                param.setResult(false);
                            } else {
                                param.setResult(true);
                            }
                        }
                    }
                });
                XposedBridge.hookAllMethods(sheetActionDefault, "dismissForDisableAction", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (MainHook.isBypassDownloadRestrictionEnabled() || MainHook.isNoWatermarkEnabled()) {
                            param.setResult(false);
                        }
                    }
                });
            }
        } catch (Throwable ignored) {}
    }

    public static void hookActionClass(Class<?> actionClass) {
        if (actionClass == null) return;
        try {
            final boolean isWrapper = actionClass.getName().contains("0xEd");
            XposedBridge.hookAllMethods(actionClass, "enable", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (MainHook.isBypassDownloadRestrictionEnabled() || MainHook.isNoWatermarkEnabled()) {
                        // For 0xEd wrapper: ACListener checks if (enable(r8) == 0) goto execute.
                        // So 0xEd wrapper must return false (0) to jump to execute!
                        // For 0xMa download action itself: enable() must return true.
                        param.setResult(!isWrapper);
                    }
                }
            });

            XposedBridge.hookAllMethods(actionClass, "dismissForDisableAction", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (MainHook.isBypassDownloadRestrictionEnabled() || MainHook.isNoWatermarkEnabled()) {
                        param.setResult(false);
                    }
                }
            });

            XposedBridge.hookAllMethods(actionClass, "LIZ", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (MainHook.isBypassDownloadRestrictionEnabled() || MainHook.isNoWatermarkEnabled()) {
                        param.setResult(true);
                    }
                }
            });
            XposedBridge.log(TAG + ": Hooked " + actionClass.getName() + " actions (enable=" + (!isWrapper) + ", dismissForDisableAction=false, LIZ=true)");
        } catch (Throwable ignored) {}
    }

    private static void hookMultiStateDownload(ClassLoader classLoader) {
        if (sMultiStateDownloadHooked || classLoader == null) return;
        try {
            Class<?> holderClass = XposedHelpers.findClassIfExists(
                    "com.ss.android.ugc.aweme.share.socialpanel.viewholder.MultiStateDownloadViewHolder", classLoader);
            if (holderClass != null) {
                sMultiStateDownloadHooked = true;
                hookMultiStateDownloadClass(holderClass);
            }
        } catch (Throwable t) {
            Log.d(TAG, "hookMultiStateDownload error: " + t.getMessage());
        }
    }

    public static void hookMultiStateDownloadClass(Class<?> holderClass) {
        if (holderClass == null) return;

        XC_MethodHook stateChangeHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (MainHook.isBypassDownloadRestrictionEnabled() || MainHook.isNoWatermarkEnabled()) {
                    if (param.args != null && param.args.length >= 2) {
                        param.args[0] = 0;
                        param.args[1] = 0;
                    }
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                makeMultiStateDownloadViewHolderEnabled(param.thisObject);
            }
        };

        try {
            // Hook F3 (Douyin 40.6.0) and B3 (older)
            XposedBridge.hookAllMethods(holderClass, "F3", stateChangeHook);
            XposedBridge.hookAllMethods(holderClass, "B3", stateChangeHook);
            XposedBridge.log(TAG + ": Hooked MultiStateDownloadViewHolder.F3/B3 for Douyin");
        } catch (Throwable t) {
            Log.d(TAG, "hookMultiStateDownloadClass methods error: " + t.getMessage());
        }

        try {
            XposedBridge.hookAllConstructors(holderClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    makeMultiStateDownloadViewHolderEnabled(param.thisObject);
                }
            });
        } catch (Throwable ignored) {}
    }

    private static void hookSocialActionsPanelVM(ClassLoader classLoader) {
        if (sSocialVMHooked || classLoader == null) return;
        try {
            Class<?> vmClass = XposedHelpers.findClassIfExists(
                    "com.ss.android.ugc.aweme.share.socialpanel.viewmodel.SocialActionsPanelVM", classLoader);
            if (vmClass != null) {
                sSocialVMHooked = true;
                hookSocialActionsPanelVMClass(vmClass);
            }
        } catch (Throwable t) {
            Log.d(TAG, "hookSocialActionsPanelVM error: " + t.getMessage());
        }
    }

    public static void hookSocialActionsPanelVMClass(Class<?> vmClass) {
        if (vmClass == null) return;

        XC_MethodHook vmHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (!MainHook.isBypassDownloadRestrictionEnabled() && !MainHook.isNoWatermarkEnabled()) return;
                Object res = param.getResult();
                if (res != null) {
                    try {
                        Object mapObj = XposedHelpers.getObjectField(res, "LIZ");
                        if (mapObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<Object, Object> map = (Map<Object, Object>) mapObj;
                            map.put("download", 0);
                            map.put("share", 0);
                            map.put("share_offsite", 0);
                            map.put("share_offsite_direct", 0);
                            for (Object k : new ArrayList<>(map.keySet())) {
                                map.put(k, 0);
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            }
        };

        try {
            // Hook rA (Douyin 40.6.0) and lv (older Douyin)
            XposedBridge.hookAllMethods(vmClass, "rA", vmHook);
            XposedBridge.hookAllMethods(vmClass, "lv", vmHook);
            XposedBridge.log(TAG + ": Hooked SocialActionsPanelVM.rA/lv state map");
        } catch (Throwable t) {
            Log.d(TAG, "hookSocialActionsPanelVMClass error: " + t.getMessage());
        }
    }

    public static void makeMultiStateDownloadViewHolderEnabled(final Object holder) {
        if (holder == null) return;
        if (!MainHook.isBypassDownloadRestrictionEnabled() && !MainHook.isNoWatermarkEnabled()) return;

        try { XposedHelpers.setIntField(holder, "i", 0); } catch (Throwable ignored) {}

        try {
            Object wrapper = XposedHelpers.getObjectField(holder, "j");
            if (wrapper != null) {
                // wrapper.b must be 1 for "保存本地" (Save Local) action!
                try { XposedHelpers.setIntField(wrapper, "b", 1); } catch (Throwable ignored) {}
                hookActionObject(wrapper);
                try {
                    Object action = XposedHelpers.getObjectField(wrapper, "e");
                    if (action != null) {
                        hookActionObject(action);
                    }
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        try {
            final View itemView = (View) XposedHelpers.getObjectField(holder, "itemView");
            if (itemView != null) {
                itemView.setAlpha(1.0f);
                itemView.setEnabled(true);
                itemView.setClickable(true);
            }
        } catch (Throwable ignored) {}

        try {
            View iconView = (View) XposedHelpers.getObjectField(holder, "b");
            if (iconView != null) {
                iconView.setAlpha(1.0f);
                iconView.setVisibility(View.VISIBLE);
                iconView.setEnabled(true);
            }
        } catch (Throwable ignored) {}

        try {
            View labelView = (View) XposedHelpers.getObjectField(holder, "f");
            if (labelView != null) {
                labelView.setAlpha(1.0f);
                labelView.setVisibility(View.VISIBLE);
                labelView.setEnabled(true);
                if (labelView instanceof TextView) {
                    ((TextView) labelView).setText("保存本地");
                }
            }
        } catch (Throwable ignored) {}

        try {
            View disabledIcon = (View) XposedHelpers.getObjectField(holder, "e");
            if (disabledIcon != null) disabledIcon.setVisibility(View.GONE);
        } catch (Throwable ignored) {}

        try {
            View disabledLabel = (View) XposedHelpers.getObjectField(holder, "h");
            if (disabledLabel != null) disabledLabel.setVisibility(View.GONE);
        } catch (Throwable ignored) {}

        try {
            View ring = (View) XposedHelpers.getObjectField(holder, "d");
            if (ring != null) ring.setVisibility(View.GONE);
        } catch (Throwable ignored) {}

        try {
            View progLabel = (View) XposedHelpers.getObjectField(holder, "g");
            if (progLabel != null) progLabel.setVisibility(View.GONE);
        } catch (Throwable ignored) {}
    }

    private static void hookActionObject(Object obj) {
        if (obj == null) return;
        try {
            hookActionClass(obj.getClass());
        } catch (Throwable ignored) {}
    }

    private static boolean copyFile(File src, File dst) {
        FileChannel in = null;
        FileChannel out = null;
        try {
            in = new FileInputStream(src).getChannel();
            out = new FileOutputStream(dst).getChannel();
            in.transferTo(0, in.size(), out);
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "copyFile failed", t);
            return false;
        } finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
            } catch (Throwable ignored) {}
        }
    }
}
