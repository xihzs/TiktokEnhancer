package com.ash.tiktokregion.hooks.media;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.ash.tiktokregion.core.BaseHook;
import com.ash.tiktokregion.core.DexKitManager;
import com.ash.tiktokregion.core.ModuleConfig;

import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.MethodDataList;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Handles unwatermarked downloads, client-side transcode bypass, author restriction unlocking,
 * and scoped storage path validation for Android 11+.
 */
public class WatermarkBypassHook implements BaseHook {

    private static final String TAG = "TikTokWatermark";
    private static volatile Object sCurrentAweme = null;
    private static volatile Object sLastPlayedPlayAddr = null;

    @Override
    public void hook(ClassLoader classLoader) {
        hookAwemeDownloadFlags(classLoader);
        hookVideoModel(classLoader);
        hookWatermarkService(classLoader);
        hookACLShare(classLoader);
        hookDownloadRestrictions(classLoader);
        hookDownloaderStorage(classLoader);
    }

    public static Object getCurrentAweme() {
        return sCurrentAweme;
    }

    public static Object getLastPlayedPlayAddr() {
        return sLastPlayedPlayAddr;
    }

    private void hookAwemeDownloadFlags(ClassLoader classLoader) {
        final String awemeClass = "com.ss.android.ugc.aweme.feed.model.Aweme";
        final String videoControlClass = "com.ss.android.ugc.aweme.feed.model.VideoControl";

        // 1. getDownloadWithoutWatermark() -> true
        try {
            XposedHelpers.findAndHookMethod(awemeClass, classLoader, "getDownloadWithoutWatermark", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (ModuleConfig.noWatermark) {
                        param.setResult(true);
                    }
                }
            });
            XposedBridge.log(TAG + ": Hooked Aweme.getDownloadWithoutWatermark() -> true");
        } catch (Throwable ignored) {}

        // 2. needTTSWatermarkWhenDownload() -> false
        try {
            XposedHelpers.findAndHookMethod(awemeClass, classLoader, "needTTSWatermarkWhenDownload", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (ModuleConfig.noWatermark) {
                        param.setResult(false);
                    }
                }
            });
        } catch (Throwable ignored) {}

        // 3. isPreventDownload() -> false
        try {
            XposedHelpers.findAndHookMethod(awemeClass, classLoader, "isPreventDownload", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (ModuleConfig.bypassDownloadRestriction) {
                        param.setResult(false);
                    }
                }
            });
            XposedHelpers.findAndHookMethod(awemeClass, classLoader, "getPreventDownload", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (ModuleConfig.bypassDownloadRestriction) {
                        param.setResult(false);
                    }
                }
            });
            XposedHelpers.findAndHookMethod(awemeClass, classLoader, "getPreventDownload$common_model_release", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (ModuleConfig.bypassDownloadRestriction) {
                        param.setResult(false);
                    }
                }
            });
        } catch (Throwable ignored) {}

        // 4. getIsCommentPostVideo() -> false
        try {
            XposedHelpers.findAndHookMethod(awemeClass, classLoader, "getIsCommentPostVideo", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (ModuleConfig.bypassDownloadRestriction) {
                        param.setResult(false);
                    }
                }
            });
        } catch (Throwable ignored) {}

        // 5. getVideoControl() -> force allowDownload = true, preventDownloadType = 0
        try {
            XposedHelpers.findAndHookMethod(awemeClass, classLoader, "getVideoControl", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!ModuleConfig.bypassDownloadRestriction) return;
                    Object vc = param.getResult();
                    if (vc == null) {
                        try {
                            vc = XposedHelpers.newInstance(XposedHelpers.findClass(videoControlClass, classLoader));
                            param.setResult(vc);
                        } catch (Throwable ignored) {}
                    }
                    if (vc != null) {
                        try { XposedHelpers.setObjectField(vc, "allowDownload", Boolean.TRUE); } catch (Throwable ignored) {}
                        try { XposedHelpers.setIntField(vc, "preventDownloadType", 0); } catch (Throwable ignored) {}
                        try { XposedHelpers.setBooleanField(vc, "isAllowDuet", true); } catch (Throwable ignored) {}
                        try { XposedHelpers.setBooleanField(vc, "isAllowDynamicWallpaper", true); } catch (Throwable ignored) {}
                        try { XposedHelpers.setBooleanField(vc, "isAllowReact", true); } catch (Throwable ignored) {}
                    }
                }
            });
        } catch (Throwable ignored) {}

        // 6. VideoControl direct getters
        try {
            XposedHelpers.findAndHookMethod(videoControlClass, classLoader, "getPreventDownloadType", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (ModuleConfig.bypassDownloadRestriction) {
                        param.setResult(0);
                    }
                }
            });
            XposedHelpers.findAndHookMethod(videoControlClass, classLoader, "isAllowDownload", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (ModuleConfig.bypassDownloadRestriction) {
                        param.setResult(true);
                    }
                }
            });
        } catch (Throwable ignored) {}

        // 7. Aweme.getStatus() -> force downloadStatus = 0, isAllowShare = true
        try {
            XposedHelpers.findAndHookMethod(awemeClass, classLoader, "getStatus", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!ModuleConfig.bypassDownloadRestriction) return;
                    Object status = param.getResult();
                    if (status != null) {
                        try { XposedHelpers.setIntField(status, "downloadStatus", 0); } catch (Throwable ignored) {}
                        try { XposedHelpers.setBooleanField(status, "isAllowShare", true); } catch (Throwable ignored) {}
                    }
                }
            });
        } catch (Throwable ignored) {}

        // 8. Aweme.getAuthor() -> force downloadSetting = 0, preventDownload = false
        try {
            XposedHelpers.findAndHookMethod(awemeClass, classLoader, "getAuthor", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!ModuleConfig.bypassDownloadRestriction) return;
                    Object author = param.getResult();
                    if (author != null) {
                        try { XposedHelpers.setIntField(author, "downloadSetting", 0); } catch (Throwable ignored) {}
                        try { XposedHelpers.setBooleanField(author, "preventDownload", false); } catch (Throwable ignored) {}
                        try { XposedHelpers.setBooleanField(author, "postDefaultDownloadSetting", true); } catch (Throwable ignored) {}
                    }
                }
            });
        } catch (Throwable ignored) {}

        // 9. Aweme.getVideo() / getImageInfos() / getPhotoModeImageInfo() -> track active Aweme
        try {
            XposedHelpers.findAndHookMethod(awemeClass, classLoader, "getVideo", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.thisObject != null) {
                        sCurrentAweme = param.thisObject;
                    }
                }
            });
            XposedHelpers.findAndHookMethod(awemeClass, classLoader, "getImageInfos", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.thisObject != null) {
                        sCurrentAweme = param.thisObject;
                    }
                }
            });
            XposedHelpers.findAndHookMethod(awemeClass, classLoader, "getPhotoModeImageInfo", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.thisObject != null) {
                        sCurrentAweme = param.thisObject;
                    }
                }
            });
        } catch (Throwable ignored) {}
    }

    private void hookVideoModel(ClassLoader classLoader) {
        final String videoClass = "com.ss.android.ugc.aweme.feed.model.Video";

        // Track last played playAddr
        XC_MethodHook trackPlayHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object res = param.getResult();
                if (res != null) {
                    sLastPlayedPlayAddr = res;
                }
            }
        };

        String[] playMethods = {"getPlayAddr", "getProperPlayAddr", "getPlayAddrH264", "getH264PlayAddr", "getPlayAddrBytevc1"};
        for (String m : playMethods) {
            try {
                XposedHelpers.findAndHookMethod(videoClass, classLoader, m, trackPlayHook);
            } catch (Throwable ignored) {}
        }

        XC_MethodHook redirectHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!ModuleConfig.noWatermark) return;

                Object video = param.thisObject;
                if (video == null) return;

                try {
                    Object playAddr = null;
                    if (ModuleConfig.forceHighestQuality) {
                        playAddr = VideoQualityHook.getHighestQualityPlayAddr(video);
                    }
                    if (playAddr == null) {
                        playAddr = XposedHelpers.callMethod(video, "getPlayAddr");
                    }
                    if (playAddr == null) {
                        playAddr = XposedHelpers.callMethod(video, "getPlayAddrH264");
                    }
                    if (playAddr == null) {
                        playAddr = XposedHelpers.callMethod(video, "getPlayAddrBytevc1");
                    }
                    if (playAddr == null) {
                        playAddr = sLastPlayedPlayAddr;
                    }
                    if (playAddr != null) {
                        sLastPlayedPlayAddr = playAddr;
                        param.setResult(playAddr);
                    }
                } catch (Throwable ignored) {}
            }
        };

        String[] methods = {
                "getDownloadAddr", "getNewDownloadAddr", "getDownloadNoWatermarkAddr",
                "getUIAlikeDownloadAddr", "getCaptionDownloadAddr"
        };
        for (String method : methods) {
            try {
                XposedHelpers.findAndHookMethod(videoClass, classLoader, method, redirectHook);
                XposedBridge.log(TAG + ": Hooked Video." + method + "() -> redirect to clean playAddr");
            } catch (Throwable t) {
                Log.d(TAG, "Video." + method + " hook failed: " + t.getMessage());
            }
        }
    }

    private void hookWatermarkService(ClassLoader classLoader) {
        final String serviceClass = "com.ss.android.ugc.aweme.watermark.WaterMarkServiceImpl";
        final String builderClass = "com.ss.android.ugc.aweme.services.watermark.WaterMarkBuilder";

        XC_MethodHook waterMarkHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!ModuleConfig.noWatermark) return;
                if (param.args == null || param.args.length == 0) return;

                Object builder = param.args[0];
                if (builder == null) return;

                // Disable end watermark and corner watermark flags
                try { XposedHelpers.callMethod(builder, "setAddEndMark", false); } catch (Throwable ignored) {}
                try { XposedHelpers.callMethod(builder, "setAddInterMark", false); } catch (Throwable ignored) {}
                try { XposedHelpers.setBooleanField(builder, "addEndMark", false); } catch (Throwable ignored) {}
                try { XposedHelpers.setBooleanField(builder, "addInterMark", false); } catch (Throwable ignored) {}
                try { XposedHelpers.setBooleanField(builder, "forceAIGCWatermark", false); } catch (Throwable ignored) {}
                try { XposedHelpers.setBooleanField(builder, "isAddAiChatWatermark", false); } catch (Throwable ignored) {}
                try { XposedHelpers.setBooleanField(builder, "isAddAiLiveWatermark", false); } catch (Throwable ignored) {}
                try { XposedHelpers.setBooleanField(builder, "containC2PA", false); } catch (Throwable ignored) {}

                // Directly copy input video to outPath and notify listener, skipping CPU transcoding
                try {
                    String inputPath = (String) XposedHelpers.getObjectField(builder, "inputPath");
                    final String outPath = (String) XposedHelpers.getObjectField(builder, "outPath");
                    final Object listener = XposedHelpers.getObjectField(builder, "listener");

                    if (inputPath != null && outPath != null && listener != null) {
                        File inputFile = new File(inputPath);
                        if (inputFile.exists() && inputFile.length() > 0) {
                            File outFile = new File(outPath);
                            if (outFile.exists()) {
                                outFile.delete();
                            }
                            MediaDownloader.copyFile(inputFile, outFile);

                            try {
                                Method onProgress = listener.getClass().getMethod("onProgress", int.class);
                                onProgress.invoke(listener, 100);
                            } catch (Throwable ignored) {}

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
                        }
                    }
                } catch (Throwable t) {
                    Log.d(TAG, "Direct copy bypass fallback: " + t.getMessage());
                }
            }
        };

        try {
            Class<?> builderClazz = XposedHelpers.findClass(builderClass, classLoader);
            XposedHelpers.findAndHookMethod(serviceClass, classLoader, "waterMark", builderClazz, waterMarkHook);
            XposedHelpers.findAndHookMethod(serviceClass, classLoader, "watermarkForTikTokNow", builderClazz, waterMarkHook);
            XposedBridge.log(TAG + ": Hooked WaterMarkServiceImpl.waterMark()");
        } catch (Throwable t) {
            Log.d(TAG, "WaterMarkServiceImpl hook failed: " + t.getMessage());
        }

        // Bypass photo and image watermarking
        XC_MethodHook copyImageHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!ModuleConfig.noWatermark) return;
                if (param.args == null || param.args.length < 2) return;
                String src = null;
                String dst = null;
                for (Object arg : param.args) {
                    if (arg instanceof String) {
                        String s = (String) arg;
                        if (src == null) {
                            src = s;
                        } else if (dst == null) {
                            dst = s;
                            break;
                        }
                    }
                }
                if (src != null && dst != null) {
                    File srcFile = new File(src);
                    if (srcFile.exists() && srcFile.length() > 0) {
                        File dstFile = new File(dst);
                        if (dstFile.exists()) dstFile.delete();
                        try {
                            MediaDownloader.copyFile(srcFile, dstFile);
                            if (param.method instanceof Method && ((Method) param.method).getReturnType() == String.class) {
                                param.setResult(dst);
                            } else {
                                param.setResult(null);
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            }
        };

        String[] imgWatermarkMethods = {
                "addAIPortraitWatermarkToImage",
                "addAITextImageWaterMarkToImage",
                "addAiChatWaterMarkToImage",
                "readAIGCAddAIPortraitWatermarkForSingleImage"
        };
        for (String m : imgWatermarkMethods) {
            try {
                XposedHelpers.findAndHookMethod(serviceClass, classLoader, m, String.class, String.class, copyImageHook);
            } catch (Throwable ignored) {}
        }
    }

    private void hookACLShare(ClassLoader classLoader) {
        final String aclCommonShareClass = "com.ss.android.ugc.aweme.feed.model.ACLCommonShare";
        final String awemeAclShareClass = "com.ss.android.ugc.aweme.feed.model.AwemeACLShare";

        // ACLCommonShare.getCode() -> 0 (allowed)
        try {
            XposedHelpers.findAndHookMethod(aclCommonShareClass, classLoader, "getCode", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (ModuleConfig.bypassDownloadRestriction || ModuleConfig.downloadStory) {
                        param.setResult(0);
                    }
                }
            });
        } catch (Throwable ignored) {}

        // ACLCommonShare.getShowType() -> 2 (show save button)
        try {
            XposedHelpers.findAndHookMethod(aclCommonShareClass, classLoader, "getShowType", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (ModuleConfig.bypassDownloadRestriction || ModuleConfig.downloadStory) {
                        param.setResult(2);
                    }
                }
            });
        } catch (Throwable ignored) {}

        // ACLCommonShare.getTranscode() -> 1 (allow clean stream download)
        try {
            XposedHelpers.findAndHookMethod(aclCommonShareClass, classLoader, "getTranscode", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (ModuleConfig.bypassDownloadRestriction || ModuleConfig.downloadStory) {
                        param.setResult(1);
                    }
                }
            });
        } catch (Throwable ignored) {}

        // ACLCommonShare.getMute() -> false
        try {
            XposedHelpers.findAndHookMethod(aclCommonShareClass, classLoader, "getMute", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (ModuleConfig.bypassDownloadRestriction || ModuleConfig.downloadStory) {
                        param.setResult(false);
                    }
                }
            });
        } catch (Throwable ignored) {}

        // ACLCommonShare.getPopupMsg() / getToastMsg() -> "" (prevent author restriction modal and toast)
        try {
            XposedHelpers.findAndHookMethod(aclCommonShareClass, classLoader, "getPopupMsg", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (ModuleConfig.bypassDownloadRestriction || ModuleConfig.downloadStory) {
                        param.setResult("");
                    }
                }
            });
            XposedHelpers.findAndHookMethod(aclCommonShareClass, classLoader, "getToastMsg", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (ModuleConfig.bypassDownloadRestriction || ModuleConfig.downloadStory) {
                        param.setResult("");
                    }
                }
            });
        } catch (Throwable ignored) {}

        // AwemeACLShare getters
        String[] aclShareGetters = {"getDownloadGeneral", "getDownloadMaskPanel", "getDownloadSharePanel"};
        for (String m : aclShareGetters) {
            try {
                XposedHelpers.findAndHookMethod(awemeAclShareClass, classLoader, m, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!ModuleConfig.bypassDownloadRestriction && !ModuleConfig.downloadStory) return;
                        Object acl = param.getResult();
                        if (acl != null) {
                            try { XposedHelpers.callMethod(acl, "setCode", 0); } catch (Throwable ignored) {}
                            try { XposedHelpers.callMethod(acl, "setShowType", 2); } catch (Throwable ignored) {}
                            try { XposedHelpers.callMethod(acl, "setTranscode", 1); } catch (Throwable ignored) {}
                            try { XposedHelpers.callMethod(acl, "setMute", false); } catch (Throwable ignored) {}
                            try { XposedHelpers.callMethod(acl, "setPopupMsg", ""); } catch (Throwable ignored) {}
                            try { XposedHelpers.callMethod(acl, "setToastMsg", ""); } catch (Throwable ignored) {}
                        }
                    }
                });
            } catch (Throwable ignored) {}
        }

        // Aweme.getAwemeACLShareInfo()
        try {
            XposedHelpers.findAndHookMethod("com.ss.android.ugc.aweme.feed.model.Aweme", classLoader, "getAwemeACLShareInfo", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!ModuleConfig.bypassDownloadRestriction && !ModuleConfig.downloadStory) return;
                    Object acl = param.getResult();
                    if (acl == null) {
                        try {
                            acl = XposedHelpers.newInstance(XposedHelpers.findClass(awemeAclShareClass, classLoader));
                            param.setResult(acl);
                        } catch (Throwable ignored) {}
                    }
                }
            });
        } catch (Throwable ignored) {}

        // FeedPanelStateViewModel.isAclCommonShareShow() -> return true
        final String aclVmClass = "com.ss.android.ugc.aweme.share.viewmodel.FeedPanelStateViewModel";
        try {
            XposedHelpers.findAndHookMethod(aclVmClass, classLoader, "isAclCommonShareShow", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (ModuleConfig.bypassDownloadRestriction || ModuleConfig.downloadStory) {
                        param.setResult(true);
                    }
                }
            });
        } catch (Throwable ignored) {}

        // ShareExtServiceImpl hooks: LJIJI (v47.1.4) & LJIJJ (v47.6.3), LJJZ (v47.1.4) & LJII (v47.6.3)
        try {
            Class<?> shareExtClass = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.share.ShareExtServiceImpl", classLoader);
            if (shareExtClass != null) {
                Class<?> awemeClazz = XposedHelpers.findClass("com.ss.android.ugc.aweme.feed.model.Aweme", classLoader);

                // v47.1.4: LJIJI(Context, Aweme, String)
                try {
                    XposedHelpers.findAndHookMethod(shareExtClass, "LJIJI",
                            Context.class, awemeClazz, String.class, new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    if (ModuleConfig.bypassDownloadRestriction || ModuleConfig.downloadStory) {
                                        param.setResult(true);
                                    }
                                }
                            });
                } catch (Throwable ignored) {}

                // v47.6.3: LJIJJ(Context, Aweme, String)
                try {
                    XposedHelpers.findAndHookMethod(shareExtClass, "LJIJJ",
                            Context.class, awemeClazz, String.class, new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    if (ModuleConfig.bypassDownloadRestriction || ModuleConfig.downloadStory) {
                                        param.setResult(true);
                                    }
                                }
                            });
                    XposedBridge.log(TAG + ": Hooked ShareExtServiceImpl.LJIJJ(Context, Aweme, String)");
                } catch (Throwable ignored) {}

                // v47.1.4: LJJZ
                XposedBridge.hookAllMethods(shareExtClass, "LJJZ", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (param.args != null && param.args.length >= 4) {
                            if (ModuleConfig.bypassDownloadRestriction || ModuleConfig.downloadStory) {
                                param.setResult(true);
                            }
                        }
                    }
                });

                // v47.6.3: LJII(Context, Activity, ...)
                XposedBridge.hookAllMethods(shareExtClass, "LJII", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (param.args != null && param.args.length >= 4) {
                            if (ModuleConfig.bypassDownloadRestriction || ModuleConfig.downloadStory) {
                                param.setResult(true);
                            }
                        }
                    }
                });
                XposedBridge.log(TAG + ": Hooked ShareExtServiceImpl.LJII(Context, Activity, ...)");
            }
        } catch (Throwable ignored) {}

        // Age gate / share check helpers: X.02y7 (v47.1.4) & X.02yn (v47.6.3)
        String[] ageGateClasses = {"X.02y7", "X.02yn"};
        for (String agClass : ageGateClasses) {
            try {
                XposedHelpers.findAndHookMethod(agClass, classLoader, "LJIIIZ", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (ModuleConfig.bypassDownloadRestriction || ModuleConfig.downloadStory) {
                            param.setResult(false);
                        }
                    }
                });
            } catch (Throwable ignored) {}
        }

        // Share action enable and lock bypass (X.0IIy & X.0IJ3 on v47.1.4, X.0G3d on v47.6.3)
        try {
            XposedHelpers.findAndHookMethod("X.0IIy", classLoader, "enable", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (ModuleConfig.bypassDownloadRestriction || ModuleConfig.downloadStory) {
                        param.setResult(true);
                    }
                }
            });
            XposedHelpers.findAndHookMethod("X.0IIy", classLoader, "LJI", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (ModuleConfig.bypassDownloadRestriction || ModuleConfig.downloadStory) {
                        param.setResult(false);
                    }
                }
            });
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod("X.0IJ3", classLoader, "enable", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (ModuleConfig.bypassDownloadRestriction || ModuleConfig.downloadStory) {
                        param.setResult(true);
                    }
                }
            });
            XposedHelpers.findAndHookMethod("X.0IJ3", classLoader, "LJIIL", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (ModuleConfig.bypassDownloadRestriction || ModuleConfig.downloadStory) {
                        param.setResult(0);
                    }
                }
            });
        } catch (Throwable ignored) {}

        // v47.6.3 Download Action: X.0G3d
        try {
            Class<?> g3dClass = XposedHelpers.findClassIfExists("X.0G3d", classLoader);
            if (g3dClass != null) {
                XposedHelpers.findAndHookMethod(g3dClass, "enable", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (ModuleConfig.bypassDownloadRestriction || ModuleConfig.downloadStory) {
                            param.setResult(true);
                        }
                    }
                });
                XposedHelpers.findAndHookMethod(g3dClass, "LJIIL", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (ModuleConfig.bypassDownloadRestriction || ModuleConfig.downloadStory) {
                            param.setResult(0);
                        }
                    }
                });
                XposedBridge.log(TAG + ": Hooked X.0G3d (DownloadAction) for v47.6.3");
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> awemeClazz = XposedHelpers.findClass("com.ss.android.ugc.aweme.feed.model.Aweme", classLoader);
            XposedHelpers.findAndHookMethod("com.ss.android.ugc.aweme.feed.model.AwemeExtKt", classLoader, "isSharedStoryVisible", awemeClazz, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (ModuleConfig.bypassDownloadRestriction || ModuleConfig.downloadStory) {
                        param.setResult(true);
                    }
                }
            });
            XposedHelpers.findAndHookMethod("com.ss.android.ugc.aweme.feed.model.AwemeExtKt", classLoader, "canShare", awemeClazz, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (ModuleConfig.bypassDownloadRestriction || ModuleConfig.downloadStory) {
                        param.setResult(true);
                    }
                }
            });
        } catch (Throwable ignored) {}
    }

    private void hookDownloadRestrictions(ClassLoader classLoader) {
        final String awemeStatusClass = "com.ss.android.ugc.aweme.feed.model.AwemeStatus";
        final String userClass = "com.ss.android.ugc.aweme.profile.model.User";
        final String awemeControlClass = "com.ss.android.ugc.aweme.feed.model.AwemeControl";

        // AwemeStatus.getDownloadStatus() -> 0
        try {
            XposedHelpers.findAndHookMethod(awemeStatusClass, classLoader, "getDownloadStatus", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (ModuleConfig.bypassDownloadRestriction) {
                        param.setResult(0);
                    }
                }
            });
            XposedHelpers.findAndHookMethod(awemeStatusClass, classLoader, "isAllowShare", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (ModuleConfig.bypassDownloadRestriction || ModuleConfig.downloadStory) {
                        param.setResult(true);
                    }
                }
            });
        } catch (Throwable ignored) {}

        // User.getDownloadSetting() -> 0
        try {
            XposedHelpers.findAndHookMethod(userClass, classLoader, "getDownloadSetting", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (ModuleConfig.bypassDownloadRestriction) {
                        param.setResult(0);
                    }
                }
            });
            XposedHelpers.findAndHookMethod(userClass, classLoader, "isPreventDownload", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (ModuleConfig.bypassDownloadRestriction) {
                        param.setResult(false);
                    }
                }
            });
        } catch (Throwable ignored) {}

        // AwemeControl.canShare() / canForward() -> true
        try {
            XposedHelpers.findAndHookMethod(awemeControlClass, classLoader, "canShare", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (ModuleConfig.bypassDownloadRestriction || ModuleConfig.downloadStory) {
                        param.setResult(true);
                    }
                }
            });
            XposedHelpers.findAndHookMethod(awemeControlClass, classLoader, "canForward", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (ModuleConfig.bypassDownloadRestriction || ModuleConfig.downloadStory) {
                        param.setResult(true);
                    }
                }
            });
        } catch (Throwable ignored) {}

        // Hook X.0VYO.LIZ (v47.1.4) and X.0YI0.LIZ (v47.6.3) (context, aweme, str) -> return true
        String[] gatekeeperClasses = {"X.0VYO", "X.0YI0"};
        for (String cls : gatekeeperClasses) {
            try {
                Class<?> awemeClazz = XposedHelpers.findClass("com.ss.android.ugc.aweme.feed.model.Aweme", classLoader);
                XposedHelpers.findAndHookMethod(cls, classLoader, "LIZ", Context.class, awemeClazz, String.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (ModuleConfig.bypassDownloadRestriction || ModuleConfig.downloadStory) {
                            param.setResult(true);
                        }
                    }
                });
                XposedBridge.log(TAG + ": Hooked " + cls + ".LIZ() -> allow download");
            } catch (Throwable ignored) {}
        }

        // Hook X.19ae.LIZ (v47.1.4) and X.10uv.LIZ (v47.6.3) (aweme) -> return 0
        String[] aclCodeClasses = {"X.19ae", "X.10uv"};
        for (String cls : aclCodeClasses) {
            try {
                Class<?> awemeClazz = XposedHelpers.findClass("com.ss.android.ugc.aweme.feed.model.Aweme", classLoader);
                XposedHelpers.findAndHookMethod(cls, classLoader, "LIZ", awemeClazz, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (ModuleConfig.bypassDownloadRestriction || ModuleConfig.downloadStory) {
                            param.setResult(0);
                        }
                    }
                });
                XposedBridge.log(TAG + ": Hooked " + cls + ".LIZ() -> return code 0");
            } catch (Throwable ignored) {}
        }
    }

    /**
     * Fixes Android 11+ Scoped Storage and SocialBase Downloader Directory Failures.
     * Prevents socialbase downloader from freezing at 0% when getExternalFilesDir() returns null
     * or when save_path_security rejects the output directory.
     */
    private void hookDownloaderStorage(ClassLoader classLoader) {
        // 1. Bypass save_path_security check in X.0Meh (v47.1.4) and X.0L8G (v47.6.3)
        String[] dirUtilsClasses = {"X.0Meh", "X.0L8G"};
        for (String cls : dirUtilsClasses) {
            try {
                XposedHelpers.findAndHookMethod(cls, classLoader, "LIZIZ", String.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(true);
                    }
                });
                XposedBridge.log(TAG + ": Hooked " + cls + ".LIZIZ() -> bypass save_path_security");
            } catch (Throwable t) {
                Log.d(TAG, cls + ".LIZIZ direct hook skipped: " + t.getMessage());
            }

            // 2. Ensure valid directory creation in LIZJ(File)
            try {
                XposedHelpers.findAndHookMethod(cls, classLoader, "LIZJ", File.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        File f = (File) param.args[0];
                        if (f != null) {
                            if (!f.exists()) f.mkdirs();
                            if (f.exists() && f.isDirectory()) {
                                param.setResult(true);
                            }
                        }
                    }
                });
            } catch (Throwable ignored) {}

            // 3. Fallback for LIZ(File)
            try {
                XposedHelpers.findAndHookMethod(cls, classLoader, "LIZ", File.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object res = param.getResult();
                        if (res == null || (res instanceof String && TextUtils.isEmpty((String) res))) {
                            File fallback = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "TikTok");
                            if (!fallback.exists()) fallback.mkdirs();
                            param.setResult(fallback.getAbsolutePath());
                        }
                    }
                });
            } catch (Throwable ignored) {}
        }

        // 4. Hook X.00kn (v47.1.4) and X.00kq (v47.6.3) .s2(Context, String) to ensure getExternalFilesDir never returns null
        String[] utilClasses = {"X.00kn", "X.00kq"};
        for (String cls : utilClasses) {
            try {
                XposedHelpers.findAndHookMethod(cls, classLoader, "s2", Context.class, String.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        File res = (File) param.getResult();
                        if (res == null || !res.exists()) {
                            Context ctx = (Context) param.args[0];
                            String type = (String) param.args[1];
                            if (ctx != null) {
                                File fallback = null;
                                try {
                                    fallback = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "TikTok");
                                    if (!fallback.exists()) fallback.mkdirs();
                                } catch (Throwable ignored) {}
                                if (fallback == null || !fallback.exists()) {
                                    fallback = new File(ctx.getFilesDir(), type != null ? type : "Download");
                                    if (!fallback.exists()) fallback.mkdirs();
                                }
                                param.setResult(fallback);
                            }
                        }
                    }
                });
            } catch (Throwable ignored) {}

            // 5. Hook .r2(Context) to ensure getExternalCacheDir never returns null
            try {
                XposedHelpers.findAndHookMethod(cls, classLoader, "r2", Context.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        File res = (File) param.getResult();
                        if (res == null || !res.exists()) {
                            Context ctx = (Context) param.args[0];
                            if (ctx != null) {
                                File cache = ctx.getCacheDir();
                                if (cache != null) {
                                    if (!cache.exists()) cache.mkdirs();
                                    param.setResult(cache);
                                }
                            }
                        }
                    }
                });
            } catch (Throwable ignored) {}
        }

        // 6. Dynamic DexKit 2.3.0 scan for any obfuscated save_path_security methods
        if (DexKitManager.isAvailable()) {
            try {
                MethodDataList matches = DexKitManager.findMethod(
                        FindMethod.create()
                                .searchPackages("X", "com.ss.android.socialbase.downloader")
                                .matcher(MethodMatcher.create().addUsingString("save_path_security"))
                );
                if (matches != null && !matches.isEmpty()) {
                    for (MethodData md : matches) {
                        try {
                            Method m = md.getMethodInstance(classLoader);
                            if (m != null && m.getReturnType() == boolean.class) {
                                XposedBridge.hookMethod(m, new XC_MethodHook() {
                                    @Override
                                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                        param.setResult(true);
                                    }
                                });
                                XposedBridge.log(TAG + ": Dynamically hooked save_path_security via DexKit -> " + md.getDescriptor());
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            } catch (Throwable t) {
                Log.d(TAG, "DexKit save_path_security scan fallback: " + t.getMessage());
            }
        }
    }
}
