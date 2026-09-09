package com.ash.tiktokregion;

import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class WatermarkHook {

    private static final String TAG = "TikTokWatermark";
    private static volatile Object sCurrentAweme = null;
    private static volatile Object sLastPlayedPlayAddr = null;
    private static final List<Object> sCurrentStoryList = new ArrayList<>();
    private static final java.util.Map<Object, Object> sVideoToAwemeMap = new java.util.WeakHashMap<>();
    private static final Set<String> sKnownStoryAids = Collections.synchronizedSet(new HashSet<>());
    private static final Set<Object> sKnownStoryAwemes = Collections.newSetFromMap(new java.util.WeakHashMap<>());
    private static final ThreadLocal<Boolean> sIsUnlocking = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public static void hook(ClassLoader classLoader) {
        hookAwemeDownloadFlags(classLoader);
        hookVideoModel(classLoader);
        hookUrlModel(classLoader);
        hookWatermarkService(classLoader);
        hookACLShare(classLoader);
        hookDownloadRestrictions(classLoader);
        hookStoryDownload(classLoader);
        hookShareSheetDialog(classLoader);
        hookDouyinDownload(classLoader);
    }

    public static Object getCleanPlayAddr(Object video) {
        if (video == null) return null;
        Object playAddr = null;
        try { playAddr = XposedHelpers.getObjectField(video, "playAddr"); } catch (Throwable ignored) {}
        if (playAddr == null) {
            try { playAddr = XposedHelpers.callMethod(video, "getPlayAddr"); } catch (Throwable ignored) {}
        }
        if (playAddr == null) {
            try { playAddr = XposedHelpers.getObjectField(video, "a"); } catch (Throwable ignored) {}
        }
        if (playAddr == null) {
            try { playAddr = XposedHelpers.callMethod(video, "LJIILJJIL"); } catch (Throwable ignored) {}
        }
        if (playAddr == null) {
            try { playAddr = XposedHelpers.getObjectField(video, "d"); } catch (Throwable ignored) {}
        }
        if (playAddr == null) {
            try { playAddr = XposedHelpers.callMethod(video, "LJIIIIZZ"); } catch (Throwable ignored) {}
        }
        if (playAddr == null) {
            try { playAddr = XposedHelpers.getObjectField(video, "b"); } catch (Throwable ignored) {}
        }
        if (playAddr == null) {
            try { playAddr = XposedHelpers.callMethod(video, "LJIILIIL"); } catch (Throwable ignored) {}
        }
        if (playAddr != null) {
            sLastPlayedPlayAddr = playAddr;
            cleanUrlModel(playAddr);
        }
        return playAddr;
    }

    public static Object getCleanPlayAddrFromAweme(Object aweme) {
        if (aweme == null) return null;
        try {
            Object video = null;
            try { video = XposedHelpers.getObjectField(aweme, "video"); } catch (Throwable ignored) {}
            if (video == null) {
                try { video = XposedHelpers.callMethod(aweme, "getVideo"); } catch (Throwable ignored) {}
            }
            if (video != null) {
                return getCleanPlayAddr(video);
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public static void cleanVideo(Object video) {
        if (video == null) return;
        if (!MainHook.isNoWatermarkEnabled()) return;

        try {
            Object playAddr = getCleanPlayAddr(video);
            if (playAddr != null) {
                try { XposedHelpers.setObjectField(video, "playAddr", playAddr); } catch (Throwable ignored) {}
                try { XposedHelpers.setObjectField(video, "a", playAddr); } catch (Throwable ignored) {}
                try { XposedHelpers.setObjectField(video, "downloadAddr", playAddr); } catch (Throwable ignored) {}
                try { XposedHelpers.setObjectField(video, "v", playAddr); } catch (Throwable ignored) {}
                try { XposedHelpers.setObjectField(video, "newDownloadAddr", playAddr); } catch (Throwable ignored) {}
                try { XposedHelpers.setObjectField(video, "z", playAddr); } catch (Throwable ignored) {}
                try { XposedHelpers.setObjectField(video, "downloadNoWatermarkAddr", playAddr); } catch (Throwable ignored) {}
                try { XposedHelpers.setObjectField(video, "ui_alike_download_addr", playAddr); } catch (Throwable ignored) {}
                try { XposedHelpers.setObjectField(video, "B", playAddr); } catch (Throwable ignored) {}
                try { XposedHelpers.setObjectField(video, "n", playAddr); } catch (Throwable ignored) {}
                try { XposedHelpers.setObjectField(video, "A", null); } catch (Throwable ignored) {}
                try { XposedHelpers.setObjectField(video, "suffixLogoAddr", null); } catch (Throwable ignored) {}
                try { XposedHelpers.setObjectField(video, "downloadSuffixLogoAddr", null); } catch (Throwable ignored) {}
                try { XposedHelpers.setObjectField(video, "D", null); } catch (Throwable ignored) {}
                try { XposedHelpers.setObjectField(video, "caption_download_addr", null); } catch (Throwable ignored) {}
                try { XposedHelpers.setObjectField(video, "captionDownloadAddr", null); } catch (Throwable ignored) {}
                try { XposedHelpers.setObjectField(video, "H", null); } catch (Throwable ignored) {}
                try { XposedHelpers.setObjectField(video, "misc_download_addrs", null); } catch (Throwable ignored) {}
                try { XposedHelpers.setObjectField(video, "miscDownloadAddrs", null); } catch (Throwable ignored) {}
                try { XposedHelpers.setBooleanField(video, "hasWaterMark", false); } catch (Throwable ignored) {}
                try { XposedHelpers.setBooleanField(video, "w", false); } catch (Throwable ignored) {}
                try { XposedHelpers.setBooleanField(video, "hasSuffixWaterMark", false); } catch (Throwable ignored) {}
                try { XposedHelpers.setBooleanField(video, "C", false); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }

    public static void cleanAweme(Object aweme) {
        if (aweme == null) return;
        try {
            Object video = null;
            try { video = XposedHelpers.getObjectField(aweme, "video"); } catch (Throwable ignored) {}
            if (video == null) {
                try { video = XposedHelpers.callMethod(aweme, "getVideo"); } catch (Throwable ignored) {}
            }
            if (video != null) {
                cleanVideo(video);
            }
            try {
                Object images = XposedHelpers.getObjectField(aweme, "images");
                if (images instanceof List) {
                    for (Object img : (List<?>) images) {
                        if (img != null) {
                            try {
                                Object imgVideo = XposedHelpers.getObjectField(img, "video");
                                if (imgVideo != null) {
                                    cleanVideo(imgVideo);
                                }
                            } catch (Throwable ignored) {}
                        }
                    }
                }
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }

    public static void cleanUrlModel(Object urlModel) {
        if (urlModel == null) return;
        try {
            List<?> list = null;
            try {
                list = (List<?>) XposedHelpers.getObjectField(urlModel, "urlList");
            } catch (Throwable ignored) {}
            if (list == null) {
                try {
                    list = (List<?>) XposedHelpers.callMethod(urlModel, "getUrlList");
                } catch (Throwable ignored) {}
            }
            if (list != null && !list.isEmpty()) {
                List<String> cleanList = new ArrayList<>(list.size());
                for (Object item : list) {
                    if (item instanceof String) {
                        cleanList.add(cleanDownloadUrl((String) item));
                    } else if (item != null) {
                        cleanList.add(cleanDownloadUrl(item.toString()));
                    }
                }
                try {
                    XposedHelpers.setObjectField(urlModel, "urlList", cleanList);
                } catch (Throwable ignored) {}
            }
            try {
                String uri = (String) XposedHelpers.getObjectField(urlModel, "uri");
                if (uri != null) {
                    XposedHelpers.setObjectField(urlModel, "uri", cleanDownloadUrl(uri));
                }
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }

    private static void hookAwemeDownloadFlags(ClassLoader classLoader) {
        final String awemeClass = "com.ss.android.ugc.aweme.feed.model.Aweme";

        try {
            XposedHelpers.findAndHookMethod(
                    awemeClass,
                    classLoader,
                    "getDownloadWithoutWatermark",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (MainHook.isNoWatermarkEnabled()) {
                                param.setResult(true);
                            }
                        }
                    }
            );
            XposedBridge.log(TAG + ": Hooked Aweme.getDownloadWithoutWatermark() -> true");
        } catch (Throwable t) {
            Log.d(TAG, "getDownloadWithoutWatermark hook failed: " + t.getMessage());
        }

        try {
            XposedHelpers.findAndHookMethod(
                    awemeClass,
                    classLoader,
                    "getVideo",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Object video = param.getResult();
                            if (param.thisObject != null) {
                                sCurrentAweme = param.thisObject;
                                unlockAwemeRestrictions(param.thisObject, video);
                                if (video != null) {
                                    synchronized (sVideoToAwemeMap) {
                                        sVideoToAwemeMap.put(video, param.thisObject);
                                    }
                                }
                            }

                            if (!MainHook.isNoWatermarkEnabled()) return;
                            if (video == null) return;

                            cleanVideo(video);
                        }
                    }
            );
        } catch (Throwable t) {
            Log.d(TAG, "Aweme.getVideo() hook failed: " + t.getMessage());
        }

        try {
            XposedHelpers.findAndHookMethod(
                    awemeClass,
                    classLoader,
                    "getFieldVideo",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Object video = param.getResult();
                            if (video != null && MainHook.isNoWatermarkEnabled()) {
                                cleanVideo(video);
                            }
                        }
                    }
            );
        } catch (Throwable ignored) {}

        try {
            Class<?> videoClass = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.feed.model.Video", classLoader);
            if (videoClass != null) {
                XposedHelpers.findAndHookMethod(
                        awemeClass,
                        classLoader,
                        "setVideo",
                        videoClass,
                        new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                if (param.args != null && param.args.length > 0 && param.args[0] != null && MainHook.isNoWatermarkEnabled()) {
                                    cleanVideo(param.args[0]);
                                }
                            }
                        }
                );
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> feedItemListClass = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.feed.model.FeedItemList", classLoader);
            if (feedItemListClass != null) {
                XC_MethodHook feedHook = new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!MainHook.isNoWatermarkEnabled()) return;
                        Object res = param.getResult();
                        if (res instanceof List) {
                            for (Object item : (List<?>) res) {
                                if (item != null) {
                                    cleanAweme(item);
                                }
                            }
                        }
                    }
                };
                for (Method m : feedItemListClass.getDeclaredMethods()) {
                    if (("getItems".equals(m.getName()) || "getAwemeList".equals(m.getName())) && m.getParameterTypes().length == 0) {
                        XposedBridge.hookMethod(m, feedHook);
                    }
                }
            }
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(
                    awemeClass,
                    classLoader,
                    "getVideoControl",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Object vc = param.getResult();
                            if (vc != null && (MainHook.isBypassDownloadRestrictionEnabled() || MainHook.isNoWatermarkEnabled())) {
                                try { XposedHelpers.setObjectField(vc, "allowDownload", Boolean.TRUE); } catch (Throwable ignored) {}
                                try { XposedHelpers.setIntField(vc, "preventDownloadType", 0); } catch (Throwable ignored) {}
                                try { XposedHelpers.setObjectField(vc, "shareGrayed", Boolean.FALSE); } catch (Throwable ignored) {}
                                try { XposedHelpers.setObjectField(vc, "downloadIgnoreVisibility", Boolean.FALSE); } catch (Throwable ignored) {}
                                try { XposedHelpers.setObjectField(vc, "shareIgnoreVisibility", Boolean.FALSE); } catch (Throwable ignored) {}
                                try { XposedHelpers.setObjectField(vc, "downloadInfo", null); } catch (Throwable ignored) {}
                                try { XposedHelpers.setIntField(vc, "downloadSetting", 0); } catch (Throwable ignored) {}
                                try { XposedHelpers.setObjectField(vc, "allowShare", Boolean.TRUE); } catch (Throwable ignored) {}
                                try { XposedHelpers.setBooleanField(vc, "showWatermark", false); } catch (Throwable ignored) {}
                            }
                        }
                    }
            );
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(
                    awemeClass,
                    classLoader,
                    "getAwemeControl",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Object control = param.getResult();
                            if (control == null && param.thisObject != null) {
                                try {
                                    control = XposedHelpers.newInstance(
                                            XposedHelpers.findClass("com.ss.android.ugc.aweme.feed.model.AwemeControl", classLoader)
                                    );
                                    param.setResult(control);
                                    try { XposedHelpers.setObjectField(param.thisObject, "awemeControl", control); } catch (Throwable ignored) {}
                                } catch (Throwable ignored) {}
                            }
                            if (control != null) {
                                try { XposedHelpers.setBooleanField(control, "b", true); } catch (Throwable ignored) {}
                                try { XposedHelpers.setBooleanField(control, "d", true); } catch (Throwable ignored) {}
                                try { XposedHelpers.setBooleanField(control, "a", true); } catch (Throwable ignored) {}
                                try { XposedHelpers.setBooleanField(control, "e", true); } catch (Throwable ignored) {}
                                try { XposedHelpers.setBooleanField(control, "canShare", true); } catch (Throwable ignored) {}
                            }
                        }
                    }
            );
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(
                    awemeClass,
                    classLoader,
                    "getImageInfos",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            if (param.thisObject != null) {
                                sCurrentAweme = param.thisObject;
                                unlockAwemeRestrictions(param.thisObject);
                            }
                        }
                    }
            );
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(
                    awemeClass,
                    classLoader,
                    "getPhotoModeImageInfo",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            if (param.thisObject != null) {
                                sCurrentAweme = param.thisObject;
                                unlockAwemeRestrictions(param.thisObject);
                            }
                        }
                    }
            );
        } catch (Throwable ignored) {}
    }

    private static void hookVideoModel(ClassLoader classLoader) {
        final String videoClass = "com.ss.android.ugc.aweme.feed.model.Video";

        XC_MethodHook trackPlayHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object res = param.getResult();
                if (res != null) {
                    sLastPlayedPlayAddr = res;
                    cleanUrlModel(res);
                }
                if (param.thisObject != null && MainHook.isNoWatermarkEnabled()) {
                    cleanVideo(param.thisObject);
                }
            }
        };

        String[] playMethods = {
                "getPlayAddr", "getProperPlayAddr", "getPlayAddrH264", "getH264PlayAddr",
                "getPlayAddrBytevc1", "LJIILJJIL", "LJIIIIZZ", "LJIILIIL", "LJIILL", "LJIILLIIL"
        };
        for (String m : playMethods) {
            try {
                XposedHelpers.findAndHookMethod(videoClass, classLoader, m, trackPlayHook);
            } catch (Throwable ignored) {}
        }

        XC_MethodHook redirectHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!MainHook.isNoWatermarkEnabled()) return;

                Object video = param.thisObject;
                if (video == null) return;

                cleanVideo(video);
                Object playAddr = getCleanPlayAddr(video);
                if (playAddr != null) {
                    param.setResult(playAddr);
                }
            }
        };

        String[] methods = {"getDownloadAddr", "getNewDownloadAddr", "getDownloadNoWatermarkAddr", "getUIAlikeDownloadAddr"};
        for (String method : methods) {
            try {
                XposedHelpers.findAndHookMethod(videoClass, classLoader, method, redirectHook);
                XposedBridge.log(TAG + ": Hooked Video." + method + "() -> redirect to playAddr");
            } catch (Throwable t) {
                Log.d(TAG, "Video." + method + " hook failed: " + t.getMessage());
            }
        }
    }

    private static void hookUrlModel(ClassLoader classLoader) {
        final String urlModelClass = "com.ss.android.ugc.aweme.base.model.UrlModel";

        try {
            XposedHelpers.findAndHookMethod(
                    urlModelClass,
                    classLoader,
                    "getUrlList",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            if (!MainHook.isNoWatermarkEnabled()) return;

                            Object result = param.getResult();
                            if (result instanceof List) {
                                List<?> originalList = (List<?>) result;
                                if (originalList.isEmpty()) return;

                                boolean modified = false;
                                List<String> cleanList = new ArrayList<>(originalList.size());

                                for (Object item : originalList) {
                                    if (item instanceof String) {
                                        String url = (String) item;
                                        String cleaned = cleanDownloadUrl(url);
                                        if (!cleaned.equals(url)) {
                                            modified = true;
                                        }
                                        cleanList.add(cleaned);
                                    } else if (item != null) {
                                        cleanList.add(item.toString());
                                    }
                                }

                                if (modified) {
                                    param.setResult(cleanList);
                                }
                            }
                        }
                    }
            );
            XposedBridge.log(TAG + ": Hooked UrlModel.getUrlList() for stream cleaning");
        } catch (Throwable t) {
            Log.d(TAG, "UrlModel.getUrlList() hook failed: " + t.getMessage());
        }

        try {
            XposedHelpers.findAndHookMethod(
                    urlModelClass,
                    classLoader,
                    "getUri",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            if (!MainHook.isNoWatermarkEnabled()) return;
                            Object res = param.getResult();
                            if (res instanceof String) {
                                String uri = (String) res;
                                String cleaned = cleanDownloadUrl(uri);
                                if (!cleaned.equals(uri)) {
                                    param.setResult(cleaned);
                                }
                            }
                        }
                    }
            );
        } catch (Throwable ignored) {}
    }

    private static void hookWatermarkService(ClassLoader classLoader) {
        final String serviceClass = "com.ss.android.ugc.aweme.watermark.WaterMarkServiceImpl";
        final String builderClass = "com.ss.android.ugc.aweme.services.watermark.WaterMarkBuilder";

        XC_MethodHook globalWaterMarkHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!MainHook.isNoWatermarkEnabled()) return;
                if (param.args == null || param.args.length == 0) return;

                Object builder = param.args[0];
                if (builder == null) return;

                try {
                    XposedHelpers.callMethod(builder, "setAddEndMark", false);
                } catch (Throwable ignored) {}
                try {
                    XposedHelpers.callMethod(builder, "setAddInterMark", false);
                } catch (Throwable ignored) {}

                try {
                    XposedHelpers.setBooleanField(builder, "addEndMark", false);
                } catch (Throwable ignored) {}
                try {
                    XposedHelpers.setBooleanField(builder, "addInterMark", false);
                } catch (Throwable ignored) {}

                try {
                    String inputPath = (String) XposedHelpers.getObjectField(builder, "inputPath");
                    String outPath = (String) XposedHelpers.getObjectField(builder, "outPath");
                    Object listener = XposedHelpers.getObjectField(builder, "listener");

                    if (inputPath != null && outPath != null && listener != null) {
                        File inputFile = new File(inputPath);
                        if (inputFile.exists() && inputFile.length() > 0) {
                            File outFile = new File(outPath);
                            if (outFile.exists()) {
                                outFile.delete();
                            }
                            boolean copied = copyFile(inputFile, outFile);
                            if (copied) {
                                try {
                                    Method onProgress = listener.getClass().getMethod("onProgress", int.class);
                                    onProgress.invoke(listener, 100);
                                } catch (Throwable ignored) {}
                                try {
                                    Method onSuccess = listener.getClass().getMethod("onSuccess");
                                    onSuccess.invoke(listener);
                                    param.setResult(null);
                                    return;
                                } catch (Throwable ignored) {}
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
            XposedHelpers.findAndHookMethod(serviceClass, classLoader, "waterMark", builderClazz, globalWaterMarkHook);
            XposedBridge.log(TAG + ": Hooked WaterMarkServiceImpl.waterMark() [Global]");
        } catch (Throwable ignored) {}

        try {
            Class<?> builderClazz = XposedHelpers.findClass(builderClass, classLoader);
            XposedHelpers.findAndHookMethod(serviceClass, classLoader, "watermarkForTikTokNow", builderClazz, globalWaterMarkHook);
        } catch (Throwable ignored) {}

        try {
            Class<?> serviceClazz = XposedHelpers.findClass(serviceClass, classLoader);
            for (Method method : serviceClazz.getDeclaredMethods()) {
                if (method.getName().equals("i5") && method.getParameterTypes().length == 1) {
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (!MainHook.isNoWatermarkEnabled()) return;
                            Object builder = param.args[0];
                            if (builder == null) return;

                            try { XposedHelpers.setBooleanField(builder, "LJ", false); } catch (Throwable ignored) {}
                            try { XposedHelpers.setBooleanField(builder, "LJFF", false); } catch (Throwable ignored) {}
                            try { XposedHelpers.setBooleanField(builder, "LJII", false); } catch (Throwable ignored) {}
                            try { XposedHelpers.callMethod(builder, "setAddEndMark", false); } catch (Throwable ignored) {}
                            try { XposedHelpers.callMethod(builder, "setAddInterMark", false); } catch (Throwable ignored) {}

                            String inPath = null;
                            String outPath = null;
                            Object listener = null;

                            try { inPath = (String) XposedHelpers.getObjectField(builder, "LIZ"); } catch (Throwable ignored) {}
                            try { outPath = (String) XposedHelpers.getObjectField(builder, "LIZIZ"); } catch (Throwable ignored) {}
                            try { listener = XposedHelpers.getObjectField(builder, "LJI"); } catch (Throwable ignored) {}

                            if (inPath == null || outPath == null) {
                                for (java.lang.reflect.Field f : builder.getClass().getDeclaredFields()) {
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
                                for (java.lang.reflect.Field f : builder.getClass().getDeclaredFields()) {
                                    try {
                                        f.setAccessible(true);
                                        Object obj = f.get(builder);
                                        if (obj != null) {
                                            for (Method m : obj.getClass().getDeclaredMethods()) {
                                                if (m.getName().equals("onSuccess") || m.getName().equals("LIZ")) {
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
                                    if (dst.exists()) dst.delete();
                                    boolean copied = copyFile(src, dst);
                                    if (copied) {
                                        XposedBridge.log(TAG + ": Douyin i5 watermark bypassed via direct clean video copy!");
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
                                        param.setResult(null);
                                        return;
                                    }
                                }
                            }
                        }
                    });
                    XposedBridge.log(TAG + ": Hooked WaterMarkServiceImpl.i5() [Douyin]");
                    break;
                }
            }
        } catch (Throwable t) {
            Log.d(TAG, "WaterMarkServiceImpl.i5() hook failed: " + t.getMessage());
        }

        try {
            Class<?> serviceClazz = XposedHelpers.findClass(serviceClass, classLoader);
            for (Method method : serviceClazz.getDeclaredMethods()) {
                if (method.getName().equals("s2") && method.getParameterTypes().length == 5) {
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (!MainHook.isNoWatermarkEnabled()) return;
                            String inPath = (String) param.args[1];
                            String outPath = (String) param.args[2];
                            Object callback = param.args[4];

                            if (inPath != null && outPath != null) {
                                File src = new File(inPath);
                                if (src.exists() && src.length() > 0) {
                                    File dst = new File(outPath);
                                    if (dst.exists()) dst.delete();
                                    boolean copied = copyFile(src, dst);
                                    if (copied) {
                                        XposedBridge.log(TAG + ": Douyin s2 watermark bypassed via direct clean video copy!");
                                        if (callback != null) {
                                            try {
                                                Method m = callback.getClass().getMethod("LIZ", int.class);
                                                m.invoke(callback, 0);
                                            } catch (Throwable t) {
                                                for (Method m : callback.getClass().getDeclaredMethods()) {
                                                    if (m.getParameterTypes().length == 1 &&
                                                            (m.getParameterTypes()[0] == int.class || m.getParameterTypes()[0] == Integer.class)) {
                                                        m.setAccessible(true);
                                                        m.invoke(callback, 0);
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                        param.setResult(null);
                                        return;
                                    }
                                }
                            }
                        }
                    });
                    XposedBridge.log(TAG + ": Hooked WaterMarkServiceImpl.s2() [Douyin]");
                    break;
                }
            }
        } catch (Throwable t) {
            Log.d(TAG, "WaterMarkServiceImpl.s2() hook failed: " + t.getMessage());
        }

        try {
            Class<?> serviceClazz = XposedHelpers.findClass(serviceClass, classLoader);
            for (Method method : serviceClazz.getDeclaredMethods()) {
                if (method.getName().equals("LIZIZ") && method.getParameterTypes().length == 5
                        && method.getParameterTypes()[0] == String.class
                        && method.getParameterTypes()[1] == String.class) {
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (!MainHook.isNoWatermarkEnabled()) return;
                            String inPath = (String) param.args[0];
                            String outPath = (String) param.args[1];
                            Object callback = param.args[4];

                            if (inPath != null && outPath != null) {
                                File src = new File(inPath);
                                if (src.exists() && src.length() > 0) {
                                    File dst = new File(outPath);
                                    if (dst.exists()) dst.delete();
                                    boolean copied = copyFile(src, dst);
                                    if (copied) {
                                        XposedBridge.log(TAG + ": Douyin LIZIZ watermark bypassed via direct clean video copy!");
                                        if (callback != null) {
                                            try {
                                                Method m = callback.getClass().getMethod("LIZ", int.class);
                                                m.invoke(callback, 0);
                                            } catch (Throwable t) {
                                                for (Method m : callback.getClass().getDeclaredMethods()) {
                                                    if (m.getParameterTypes().length == 1 &&
                                                            (m.getParameterTypes()[0] == int.class || m.getParameterTypes()[0] == Integer.class)) {
                                                        m.setAccessible(true);
                                                        m.invoke(callback, 0);
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                        param.setResult(null);
                                        return;
                                    }
                                }
                            }
                        }
                    });
                    XposedBridge.log(TAG + ": Hooked WaterMarkServiceImpl.LIZIZ() [Douyin]");
                    break;
                }
            }
        } catch (Throwable t) {
            Log.d(TAG, "WaterMarkServiceImpl.LIZIZ() hook failed: " + t.getMessage());
        }
    }

    private static void hookACLShare(ClassLoader classLoader) {
        final String aclCommonShareClass = "com.ss.android.ugc.aweme.feed.model.ACLCommonShare";
        final String awemeAclShareClass = "com.ss.android.ugc.aweme.feed.model.AwemeACLShare";

        try {
            XposedHelpers.findAndHookMethod(
                    aclCommonShareClass,
                    classLoader,
                    "getCode",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (MainHook.isBypassDownloadRestrictionEnabled() || MainHook.isDownloadStoryEnabled()) {
                                param.setResult(0);
                            }
                        }
                    }
            );
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(
                    aclCommonShareClass,
                    classLoader,
                    "getShowType",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (MainHook.isBypassDownloadRestrictionEnabled() || MainHook.isDownloadStoryEnabled()) {
                                param.setResult(2);
                            }
                        }
                    }
            );
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(
                    aclCommonShareClass,
                    classLoader,
                    "getTranscode",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (MainHook.isBypassDownloadRestrictionEnabled() || MainHook.isDownloadStoryEnabled()) {
                                param.setResult(1);
                            }
                        }
                    }
            );
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(
                    aclCommonShareClass,
                    classLoader,
                    "getMute",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (MainHook.isBypassDownloadRestrictionEnabled() || MainHook.isDownloadStoryEnabled()) {
                                param.setResult(false);
                            }
                        }
                    }
            );
        } catch (Throwable ignored) {}

        String[] aclGetters = {"getDownloadMaskPanel", "getDownloadGeneral", "getDownloadSharePanel"};
        for (String getter : aclGetters) {
            try {
                XposedHelpers.findAndHookMethod(
                        awemeAclShareClass,
                        classLoader,
                        getter,
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                Object acl = param.getResult();
                                if (acl == null) {
                                    try {
                                        acl = XposedHelpers.newInstance(
                                                XposedHelpers.findClass(aclCommonShareClass, classLoader)
                                        );
                                        param.setResult(acl);
                                    } catch (Throwable ignored) {}
                                }
                                if (acl != null) {
                                    try { XposedHelpers.callMethod(acl, "setTranscode", 1); } catch (Throwable ignored) {}
                                    try { XposedHelpers.callMethod(acl, "setCode", 0); } catch (Throwable ignored) {}
                                    try { XposedHelpers.callMethod(acl, "setShowType", 2); } catch (Throwable ignored) {}
                                    try { XposedHelpers.callMethod(acl, "setMute", false); } catch (Throwable ignored) {}
                                }
                            }
                        }
                    );
            } catch (Throwable ignored) {}
        }

        try {
            XposedHelpers.findAndHookMethod(
                    "com.ss.android.ugc.aweme.feed.model.Aweme",
                    classLoader,
                    "getAwemeACLShareInfo",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Object acl = param.getResult();
                            if (acl == null) {
                                try {
                                    acl = XposedHelpers.newInstance(
                                            XposedHelpers.findClass(awemeAclShareClass, classLoader)
                                    );
                                    param.setResult(acl);
                                } catch (Throwable ignored) {}
                            }
                        }
                    }
            );
        } catch (Throwable ignored) {}
    }

    private static void hookDownloadRestrictions(ClassLoader classLoader) {
        final String awemeClass = "com.ss.android.ugc.aweme.feed.model.Aweme";
        final String awemeControlClass = "com.ss.android.ugc.aweme.feed.model.AwemeControl";

        XC_MethodHook returnFalseHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (MainHook.isBypassDownloadRestrictionEnabled()) {
                    param.setResult(false);
                }
            }
        };

        XC_MethodHook returnZeroHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (MainHook.isBypassDownloadRestrictionEnabled()) {
                    param.setResult(0);
                }
            }
        };

        try {
            XposedHelpers.findAndHookMethod(awemeClass, classLoader, "isPreventDownload", returnFalseHook);
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(awemeClass, classLoader, "getPreventDownload", returnFalseHook);
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(awemeClass, classLoader, "getPreventDownloadType", returnZeroHook);
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(awemeClass, classLoader, "getDownloadSetting", returnZeroHook);
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(
                    awemeControlClass,
                    classLoader,
                    "isCanDownload",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (MainHook.isBypassDownloadRestrictionEnabled()) {
                                param.setResult(true);
                            }
                        }
                    }
            );
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(
                    awemeControlClass,
                    classLoader,
                    "canShare",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (MainHook.isBypassDownloadRestrictionEnabled() || MainHook.isDownloadStoryEnabled()) {
                                param.setResult(true);
                            }
                        }
                    }
            );
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(awemeControlClass, classLoader, "getPreventDownloadType", returnZeroHook);
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(awemeControlClass, classLoader, "getDownloadSetting", returnZeroHook);
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(awemeClass, classLoader, "getDownloadStatus", returnZeroHook);
        } catch (Throwable ignored) {}

        final String videoControlClass = "com.ss.android.ugc.aweme.feed.model.VideoControl";
        try {
            XposedHelpers.findAndHookMethod(videoControlClass, classLoader, "getPreventDownloadType", returnZeroHook);
        } catch (Throwable ignored) {}
        try {
            XposedHelpers.findAndHookMethod(videoControlClass, classLoader, "getDownloadSetting", returnZeroHook);
        } catch (Throwable ignored) {}
        try {
            XposedHelpers.findAndHookMethod(videoControlClass, classLoader, "getShareGrayed", returnFalseHook);
        } catch (Throwable ignored) {}
        try {
            XposedHelpers.findAndHookMethod(videoControlClass, classLoader, "isShareGrayed", returnFalseHook);
        } catch (Throwable ignored) {}
        try {
            XposedHelpers.findAndHookMethod(videoControlClass, classLoader, "getShareIgnoreVisibility", returnFalseHook);
        } catch (Throwable ignored) {}
        try {
            XposedHelpers.findAndHookMethod(videoControlClass, classLoader, "isShareIgnoreVisibility", returnFalseHook);
        } catch (Throwable ignored) {}
        try {
            XposedHelpers.findAndHookMethod(videoControlClass, classLoader, "getDownloadIgnoreVisibility", returnFalseHook);
        } catch (Throwable ignored) {}
        try {
            XposedHelpers.findAndHookMethod(videoControlClass, classLoader, "isDownloadIgnoreVisibility", returnFalseHook);
        } catch (Throwable ignored) {}
        try {
            XposedHelpers.findAndHookMethod(videoControlClass, classLoader, "getAllowShare", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(Boolean.TRUE);
                }
            });
        } catch (Throwable ignored) {}
        try {
            XposedHelpers.findAndHookMethod(videoControlClass, classLoader, "getAllowDownload", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (MainHook.isBypassDownloadRestrictionEnabled()) {
                        param.setResult(Boolean.TRUE);
                    }
                }
            });
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(awemeClass, classLoader, "isShareIgnoreVisibility", returnFalseHook);
        } catch (Throwable ignored) {}
        try {
            XposedHelpers.findAndHookMethod(awemeClass, classLoader, "isDownloadIgnoreVisibility", returnFalseHook);
        } catch (Throwable ignored) {}
        try {
            XposedHelpers.findAndHookMethod(awemeClass, classLoader, "canShare", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });
        } catch (Throwable ignored) {}
        try {
            XposedHelpers.findAndHookMethod(awemeClass, classLoader, "isCanShare", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });
        } catch (Throwable ignored) {}
        try {
            XposedHelpers.findAndHookMethod(awemeClass, classLoader, "canPlay", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });
        } catch (Throwable ignored) {}
        try {
            XposedHelpers.findAndHookMethod(awemeClass, classLoader, "isCanPlay", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });
        } catch (Throwable ignored) {}
        try {
            XposedHelpers.findAndHookMethod(awemeClass, classLoader, "isPaidLiveVideo", returnFalseHook);
        } catch (Throwable ignored) {}

        try {
            Class<?> icClass = XposedHelpers.findClassIfExists("com.ss.ugc.aweme.InteractControl", classLoader);
            if (icClass != null) {
                for (Method m : icClass.getDeclaredMethods()) {
                    if ("getCanShare".equals(m.getName()) || "canShare".equals(m.getName()) || "isCanShare".equals(m.getName())) {
                        XposedBridge.hookMethod(m, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                param.setResult(true);
                            }
                        });
                    }
                }
            }
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(
                    "com.ss.android.ugc.aweme.utils.PrivacyPolicyAgreementUtils",
                    classLoader,
                    "isUserAgreePrivacyPolicy",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            param.setResult(true);
                        }
                    }
            );
        } catch (Throwable ignored) {}
    }

    private static void hookStoryDownload(ClassLoader classLoader) {
        final String userStoryClass = "com.ss.android.ugc.aweme.feed.model.story.UserStory";

        try {
            XposedHelpers.findAndHookMethod(
                    userStoryClass,
                    classLoader,
                    "getStories",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            if (!MainHook.isDownloadStoryEnabled()) return;
                            Object result = param.getResult();
                            if (result instanceof List) {
                                List<?> list = (List<?>) result;
                                synchronized (sCurrentStoryList) {
                                    sCurrentStoryList.clear();
                                    sCurrentStoryList.addAll(list);
                                }
                                for (Object item : list) {
                                    if (item != null) {
                                        unlockStoryAweme(item);
                                        trackStoryAweme(item);
                                    }
                                }
                                if (!list.isEmpty() && list.get(0) != null) {
                                    sCurrentAweme = list.get(0);
                                }
                            }
                        }
                    }
            );
            XposedBridge.log(TAG + ": Hooked UserStory.getStories() for story download unlocking");
        } catch (Throwable t) {
            Log.d(TAG, "UserStory.getStories hook failed: " + t.getMessage());
        }

        try {
            XposedHelpers.findAndHookMethod(
                    "com.ss.android.ugc.aweme.feed.model.Aweme",
                    classLoader,
                    "getUserStory",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            if (!MainHook.isDownloadStoryEnabled()) return;
                            Object userStory = param.getResult();
                            if (userStory != null) {
                                try {
                                    List<?> stories = (List<?>) XposedHelpers.callMethod(userStory, "getStories");
                                    if (stories != null && !stories.isEmpty()) {
                                        for (Object item : stories) {
                                            if (item != null) {
                                                unlockStoryAweme(item);
                                                trackStoryAweme(item);
                                            }
                                        }
                                    }
                                } catch (Throwable ignored) {}
                            }
                        }
                    }
            );
        } catch (Throwable ignored) {}
    }

    private static void trackStoryAweme(Object aweme) {
        if (aweme == null) return;
        sKnownStoryAwemes.add(aweme);
        try {
            String aid = (String) XposedHelpers.callMethod(aweme, "getAid");
            if (aid != null && !aid.isEmpty()) {
                sKnownStoryAids.add(aid);
            }
        } catch (Throwable ignored) {}
    }

    public static boolean isStoryAweme(Object aweme) {
        if (aweme == null) return false;

        if (sKnownStoryAwemes.contains(aweme)) return true;
        try {
            String aid = (String) XposedHelpers.callMethod(aweme, "getAid");
            if (aid != null && sKnownStoryAids.contains(aid)) return true;
        } catch (Throwable ignored) {}

        synchronized (sCurrentStoryList) {
            if (sCurrentStoryList.contains(aweme)) return true;
        }

        try {
            Object isStory = XposedHelpers.callMethod(aweme, "isStory");
            if (Boolean.TRUE.equals(isStory) || (isStory instanceof Number && ((Number) isStory).intValue() == 1)) {
                return true;
            }
        } catch (Throwable ignored) {}

        try {
            Object isStory = XposedHelpers.getObjectField(aweme, "isStory");
            if (Boolean.TRUE.equals(isStory) || (isStory instanceof Number && ((Number) isStory).intValue() == 1)) {
                return true;
            }
        } catch (Throwable ignored) {}

        try {
            Object typeObj = XposedHelpers.callMethod(aweme, "getAwemeType");
            if (typeObj instanceof Number) {
                int type = ((Number) typeObj).intValue();
                if (type == 40 || type == 43 || type == 150) {
                    return true;
                }
            }
        } catch (Throwable ignored) {}

        try {
            Boolean fake = (Boolean) XposedHelpers.callMethod(aweme, "isStoryFakeAweme");
            if (Boolean.TRUE.equals(fake)) return true;
        } catch (Throwable ignored) {}

        return false;
    }

    private static Activity getActivityFromContext(Context context) {
        if (context instanceof Activity) {
            return (Activity) context;
        }
        if (context instanceof android.content.ContextWrapper) {
            Context base = ((android.content.ContextWrapper) context).getBaseContext();
            if (base instanceof Activity) {
                return (Activity) base;
            }
        }
        return null;
    }

    private static boolean isSavedVideoList(Activity activity) {
        if (activity == null) return false;

        String activityName = activity.getClass().getName();
        if (activityName.contains("Favorite") || activityName.contains("Bookmark")
                || activityName.contains("Collection")) {
            return true;
        }

        Intent intent = activity.getIntent();
        if (intent != null) {
            if (intent.getBooleanExtra("is_story", false)
                    || intent.getBooleanExtra("is_from_story", false)
                    || intent.hasExtra("story_id")) {
                return false;
            }

            Bundle extras = intent.getExtras();
            if (extras != null) {
                for (String key : extras.keySet()) {
                    if (key.toLowerCase(Locale.US).contains("story")) return false;
                    try {
                        Object obj = extras.get(key);
                        if (obj instanceof String && ((String) obj).toLowerCase(Locale.US).contains("story")) {
                            return false;
                        }
                    } catch (Throwable ignored) {}
                }

                String[] checkKeys = {
                        "enter_from", "from_page", "previous_page", "event_type",
                        "feed_type", "page_type", "detail_type", "tab_name"
                };
                for (String key : checkKeys) {
                    try {
                        String val = intent.getStringExtra(key);
                        if (val != null) {
                            String lower = val.toLowerCase(Locale.US);
                            if (lower.contains("favorite") || lower.contains("collection_video")
                                    || lower.contains("bookmark") || lower.contains("saved_video")) {
                                return true;
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            }
        }
        return false;
    }

    private static boolean isStoryDetailActivity(Activity activity) {
        if (activity == null) return false;

        if (isSavedVideoList(activity)) {
            return false;
        }

        String activityName = activity.getClass().getName();
        if (activityName.contains("Story") || activityName.contains("story")) {
            return true;
        }

        Intent intent = activity.getIntent();
        if (intent != null) {
            if (intent.getBooleanExtra("is_story", false)
                    || intent.getBooleanExtra("is_from_story", false)
                    || intent.hasExtra("story_id")) {
                return true;
            }
            Bundle extras = intent.getExtras();
            if (extras != null) {
                for (String key : extras.keySet()) {
                    if (key.toLowerCase(Locale.US).contains("story")) {
                        return true;
                    }
                    try {
                        Object val = extras.get(key);
                        if (val instanceof String && ((String) val).toLowerCase(Locale.US).contains("story")) {
                            return true;
                        }
                    } catch (Throwable ignored) {}
                }
            }
        }

        return false;
    }

    public static void unlockAwemeRestrictions(Object aweme) {
        unlockAwemeRestrictions(aweme, null);
    }

    public static void unlockAwemeRestrictions(Object aweme, Object video) {
        if (aweme == null) return;
        if (Boolean.TRUE.equals(sIsUnlocking.get())) return;
        sIsUnlocking.set(Boolean.TRUE);
        try {

            try {
                XposedHelpers.setBooleanField(aweme, "preventDownload", false);
            } catch (Throwable ignored) {}
            try {
                XposedHelpers.callMethod(aweme, "setPreventDownload", false);
            } catch (Throwable ignored) {}
            try {
                XposedHelpers.setBooleanField(aweme, "canPlay", true);
            } catch (Throwable ignored) {}

            if (video == null) {
                try {
                    video = XposedHelpers.getObjectField(aweme, "video");
                } catch (Throwable ignored) {}
            }

            if (video != null) {
                try {
                    Object playAddr = null;
                    try {
                        playAddr = XposedHelpers.getObjectField(video, "playAddr");
                    } catch (Throwable ignored) {}
                    if (playAddr == null) {
                        try {
                            playAddr = XposedHelpers.callMethod(video, "getPlayAddr");
                        } catch (Throwable ignored) {}
                    }
                    if (playAddr != null) {
                        try { XposedHelpers.setObjectField(video, "downloadAddr", playAddr); } catch (Throwable ignored) {}
                        try { XposedHelpers.setObjectField(video, "downloadNoWatermarkAddr", playAddr); } catch (Throwable ignored) {}
                        try { XposedHelpers.setObjectField(video, "newDownloadAddr", playAddr); } catch (Throwable ignored) {}
                    }
                } catch (Throwable ignored) {}
            }

            try {
                Object videoControl = null;
                try {
                    videoControl = XposedHelpers.getObjectField(aweme, "videoControl");
                } catch (Throwable ignored) {}
                if (videoControl == null) {
                    try {
                        videoControl = XposedHelpers.callMethod(aweme, "getVideoControl");
                    } catch (Throwable ignored) {}
                }
                if (videoControl != null) {
                    try { XposedHelpers.setObjectField(videoControl, "allowDownload", Boolean.TRUE); } catch (Throwable ignored) {}
                    try { XposedHelpers.setIntField(videoControl, "preventDownloadType", 0); } catch (Throwable ignored) {}
                    try { XposedHelpers.setObjectField(videoControl, "shareGrayed", Boolean.FALSE); } catch (Throwable ignored) {}
                    try { XposedHelpers.setObjectField(videoControl, "downloadIgnoreVisibility", Boolean.FALSE); } catch (Throwable ignored) {}
                    try { XposedHelpers.setObjectField(videoControl, "shareIgnoreVisibility", Boolean.FALSE); } catch (Throwable ignored) {}
                    try { XposedHelpers.setObjectField(videoControl, "downloadInfo", null); } catch (Throwable ignored) {}
                    try { XposedHelpers.setIntField(videoControl, "downloadSetting", 0); } catch (Throwable ignored) {}
                    try { XposedHelpers.setObjectField(videoControl, "allowShare", Boolean.TRUE); } catch (Throwable ignored) {}
                    try { XposedHelpers.setBooleanField(videoControl, "showWatermark", false); } catch (Throwable ignored) {}
                    try { XposedHelpers.setIntField(videoControl, "timerStatus", 1); } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}

            try {
                Object acl = null;
                try {
                    acl = XposedHelpers.getObjectField(aweme, "awemeACLShareInfo");
                } catch (Throwable ignored) {}
                if (acl == null) {
                    try {
                        acl = XposedHelpers.callMethod(aweme, "getAwemeACLShareInfo");
                    } catch (Throwable ignored) {}
                }
                if (acl != null) {
                    try { XposedHelpers.callMethod(acl, "setTranscode", 1); } catch (Throwable ignored) {}
                    try { XposedHelpers.callMethod(acl, "setCode", 0); } catch (Throwable ignored) {}
                    try { XposedHelpers.callMethod(acl, "setShowType", 2); } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}

            try {
                Object control = null;
                try {
                    control = XposedHelpers.getObjectField(aweme, "awemeControl");
                } catch (Throwable ignored) {}
                if (control == null) {
                    try {
                        control = XposedHelpers.callMethod(aweme, "getAwemeControl");
                    } catch (Throwable ignored) {}
                }
                if (control == null) {
                    try {
                        Class<?> acClass = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.feed.model.AwemeControl", aweme.getClass().getClassLoader());
                        if (acClass != null) {
                            control = XposedHelpers.newInstance(acClass);
                            XposedHelpers.setObjectField(aweme, "awemeControl", control);
                        }
                    } catch (Throwable ignored) {}
                }
                if (control != null) {
                    try { XposedHelpers.setBooleanField(control, "b", true); } catch (Throwable ignored) {}
                    try { XposedHelpers.setBooleanField(control, "d", true); } catch (Throwable ignored) {}
                    try { XposedHelpers.setBooleanField(control, "a", true); } catch (Throwable ignored) {}
                    try { XposedHelpers.setBooleanField(control, "e", true); } catch (Throwable ignored) {}
                    try { XposedHelpers.callMethod(control, "setCanShare", true); } catch (Throwable ignored) {}
                    try { XposedHelpers.setBooleanField(control, "canShare", true); } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}

            try {
                Object ic = XposedHelpers.getObjectField(aweme, "interactControl");
                if (ic != null) {
                    try { XposedHelpers.setBooleanField(ic, "canShare", true); } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {
        } finally {
            sIsUnlocking.set(Boolean.FALSE);
        }
    }

    private static void unlockStoryAweme(Object aweme) {
        unlockAwemeRestrictions(aweme);
    }

    private static void hookShareSheetDialog(ClassLoader classLoader) {
        XC_MethodHook dialogHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (!MainHook.isDownloadStoryEnabled() && !MainHook.isNoWatermarkEnabled()) return;
                if (param.thisObject instanceof Dialog) {
                    final Dialog dialog = (Dialog) param.thisObject;
                    attachSaveButtonToShareDialog(dialog);
                }
            }
        };

        try {
            XposedHelpers.findAndHookMethod(
                    "android.app.Dialog",
                    classLoader,
                    "show",
                    dialogHook
            );
        } catch (Throwable ignored) {}
    }

    private static void hookDouyinDownload(ClassLoader classLoader) {
        final String multiStateHolderClass = "com.ss.android.ugc.aweme.share.socialpanel.viewholder.MultiStateDownloadViewHolder";
        final String socialAdapterClass = "com.ss.android.ugc.aweme.share.socialpanel.adapter.SocialActionsAdapter";
        final String socialVMClass = "com.ss.android.ugc.aweme.share.socialpanel.viewmodel.SocialActionsPanelVM";

        try {
            Class<?> gczClass = XposedHelpers.findClassIfExists("X.0GcZ", classLoader);
            if (gczClass == null) {
                gczClass = XposedHelpers.findClassIfExists("LX.0GcZ", classLoader);
            }
            if (gczClass != null) {
                for (Method m : gczClass.getDeclaredMethods()) {
                    if ("LIZ".equals(m.getName())) {
                        XposedBridge.hookMethod(m, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                if (MainHook.isNoWatermarkEnabled()) {
                                    param.setResult(null);
                                }
                            }
                        });
                    }
                }
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> nyoClass = XposedHelpers.findClassIfExists("X.1Nyo", classLoader);
            if (nyoClass == null) {
                nyoClass = XposedHelpers.findClassIfExists("LX.1Nyo", classLoader);
            }
            if (nyoClass != null) {
                for (Method m : nyoClass.getDeclaredMethods()) {
                    if ("LIZ".equals(m.getName()) && m.getParameterTypes().length == 1 && m.getParameterTypes()[0] == boolean.class) {
                        XposedBridge.hookMethod(m, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                if (!MainHook.isNoWatermarkEnabled()) return;
                                try {
                                    Object aweme = XposedHelpers.getObjectField(param.thisObject, "LIZ");
                                    if (aweme != null) {
                                        cleanAweme(aweme);
                                    }
                                } catch (Throwable ignored) {}
                                try {
                                    Object nym = XposedHelpers.getObjectField(param.thisObject, "LIZIZ");
                                    if (nym != null) {
                                        try { XposedHelpers.setBooleanField(nym, "LJJJJLL", false); } catch (Throwable ignored) {}
                                        try { XposedHelpers.setBooleanField(nym, "LJJJJZ", false); } catch (Throwable ignored) {}
                                        try { XposedHelpers.setBooleanField(nym, "LJJJJZI", false); } catch (Throwable ignored) {}
                                        try {
                                            Object nymAweme = XposedHelpers.getObjectField(nym, "LJIJI");
                                            if (nymAweme != null) {
                                                cleanAweme(nymAweme);
                                            }
                                        } catch (Throwable ignored) {}
                                    }
                                } catch (Throwable ignored) {}
                            }

                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                if (!MainHook.isNoWatermarkEnabled()) return;
                                try {
                                    Object nym = XposedHelpers.getObjectField(param.thisObject, "LIZIZ");
                                    if (nym != null) {
                                        try { XposedHelpers.setBooleanField(nym, "LJJJJLL", false); } catch (Throwable ignored) {}
                                        try { XposedHelpers.setBooleanField(nym, "LJJJJZ", false); } catch (Throwable ignored) {}
                                        try { XposedHelpers.setBooleanField(nym, "LJJJJZI", false); } catch (Throwable ignored) {}
                                        Object aweme = XposedHelpers.getObjectField(param.thisObject, "LIZ");
                                        Object cleanPlay = getCleanPlayAddrFromAweme(aweme);
                                        if (cleanPlay != null) {
                                            cleanUrlModel(cleanPlay);
                                            try { XposedHelpers.setObjectField(nym, "LJJIL", cleanPlay); } catch (Throwable ignored) {}
                                        }
                                    }
                                } catch (Throwable ignored) {}
                            }
                        });
                    }
                }
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> nymClass = XposedHelpers.findClassIfExists("X.1Nym", classLoader);
            if (nymClass == null) {
                nymClass = XposedHelpers.findClassIfExists("LX.1Nym", classLoader);
            }
            if (nymClass != null) {
                for (Method m : nymClass.getDeclaredMethods()) {
                    if ("LJIIIIZZ".equals(m.getName()) && m.getParameterTypes().length == 0) {
                        XposedBridge.hookMethod(m, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                if (!MainHook.isNoWatermarkEnabled()) return;
                                try {
                                    Object nym = param.thisObject;
                                    Object aweme = XposedHelpers.getObjectField(nym, "LJIJI");
                                    if (aweme != null) {
                                        cleanAweme(aweme);
                                    }
                                    Object cleanPlay = getCleanPlayAddrFromAweme(aweme);
                                    if (cleanPlay != null) {
                                        cleanUrlModel(cleanPlay);
                                        try { XposedHelpers.setObjectField(nym, "LJJIL", cleanPlay); } catch (Throwable ignored) {}
                                    }
                                    try {
                                        String dlUrl = (String) XposedHelpers.getObjectField(nym, "LJJIZ");
                                        if (dlUrl != null) {
                                            XposedHelpers.setObjectField(nym, "LJJIZ", cleanDownloadUrl(dlUrl));
                                        }
                                    } catch (Throwable ignored) {}
                                    try {
                                        String origUrl = (String) XposedHelpers.getObjectField(nym, "LJJIIJ");
                                        if (origUrl != null) {
                                            XposedHelpers.setObjectField(nym, "LJJIIJ", cleanDownloadUrl(origUrl));
                                        }
                                    } catch (Throwable ignored) {}
                                    try { XposedHelpers.setBooleanField(nym, "LJJJJLL", false); } catch (Throwable ignored) {}
                                    try { XposedHelpers.setBooleanField(nym, "LJJJJZ", false); } catch (Throwable ignored) {}
                                    try { XposedHelpers.setBooleanField(nym, "LJJJJZI", false); } catch (Throwable ignored) {}
                                } catch (Throwable ignored) {}
                            }
                        });
                    }
                }
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> nypClass = XposedHelpers.findClassIfExists("X.1Nyp", classLoader);
            if (nypClass == null) {
                nypClass = XposedHelpers.findClassIfExists("LX.1Nyp", classLoader);
            }
            if (nypClass != null) {
                for (Method m : nypClass.getDeclaredMethods()) {
                    if ("LIZ".equals(m.getName()) && m.getParameterTypes().length == 0) {
                        XposedBridge.hookMethod(m, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                if (!MainHook.isNoWatermarkEnabled()) return;
                                try {
                                    Object nym = XposedHelpers.getObjectField(param.thisObject, "LIZIZ");
                                    if (nym != null) {
                                        try { XposedHelpers.setBooleanField(nym, "LJJJJLL", false); } catch (Throwable ignored) {}
                                        try { XposedHelpers.setBooleanField(nym, "LJJJJZ", false); } catch (Throwable ignored) {}
                                        try { XposedHelpers.setBooleanField(nym, "LJJJJZI", false); } catch (Throwable ignored) {}
                                    }
                                } catch (Throwable ignored) {}
                            }
                        });
                    }
                }
            }
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(
                    multiStateHolderClass,
                    classLoader,
                    "B3",
                    int.class,
                    int.class,
                    boolean.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (MainHook.isBypassDownloadRestrictionEnabled() || MainHook.isNoWatermarkEnabled()) {

                                param.args[1] = 0;
                            }
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            makeMultiStateDownloadViewHolderEnabled(param.thisObject);
                        }
                    }
            );
            XposedBridge.log(TAG + ": Hooked MultiStateDownloadViewHolder.B3() for Douyin");
        } catch (Throwable t) {
            Log.d(TAG, "MultiStateDownloadViewHolder.B3 hook failed: " + t.getMessage());
        }

        try {
            Class<?> holderClass = XposedHelpers.findClassIfExists(multiStateHolderClass, classLoader);
            if (holderClass != null) {
                XposedBridge.hookAllConstructors(holderClass, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        makeMultiStateDownloadViewHolderEnabled(param.thisObject);
                    }
                });
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> adapterClass = XposedHelpers.findClassIfExists(socialAdapterClass, classLoader);
            if (adapterClass != null) {
                for (Method m : adapterClass.getDeclaredMethods()) {
                    if ("onBindViewHolder".equals(m.getName())) {
                        XposedBridge.hookMethod(m, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                if (param.args != null && param.args.length > 0) {
                                    Object vh = param.args[0];
                                    if (vh != null) {
                                        try {
                                            View itemView = (View) XposedHelpers.getObjectField(vh, "itemView");
                                            if (itemView != null) {
                                                itemView.setAlpha(1.0f);
                                                itemView.setEnabled(true);
                                                itemView.setClickable(true);
                                            }
                                        } catch (Throwable ignored) {}
                                        try {
                                            View a = (View) XposedHelpers.getObjectField(vh, "a");
                                            if (a != null) a.setAlpha(1.0f);
                                        } catch (Throwable ignored) {}
                                        try {
                                            View b = (View) XposedHelpers.getObjectField(vh, "b");
                                            if (b != null) b.setAlpha(1.0f);
                                        } catch (Throwable ignored) {}
                                        if (vh.getClass().getName().contains("MultiStateDownloadViewHolder")) {
                                            makeMultiStateDownloadViewHolderEnabled(vh);
                                        }
                                    }
                                }
                            }
                        });
                    }
                }
                XposedBridge.log(TAG + ": Hooked SocialActionsAdapter.onBindViewHolder for Douyin");
            }
        } catch (Throwable t) {
            Log.d(TAG, "SocialActionsAdapter.onBindViewHolder hook failed: " + t.getMessage());
        }

        try {
            Class<?> vmClass = XposedHelpers.findClassIfExists(socialVMClass, classLoader);
            if (vmClass != null) {
                for (Method m : vmClass.getDeclaredMethods()) {
                    if ("lv".equals(m.getName()) && m.getParameterTypes().length == 0) {
                        XposedBridge.hookMethod(m, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                if (!MainHook.isBypassDownloadRestrictionEnabled() && !MainHook.isNoWatermarkEnabled()) return;
                                Object muZ = param.getResult();
                                if (muZ != null) {
                                    try {
                                        Object mapObj = XposedHelpers.getObjectField(muZ, "LIZ");
                                        if (mapObj instanceof Map) {
                                            Map map = (Map) mapObj;
                                            map.put("download", 0);
                                            map.put("share", 0);
                                            for (Object k : new ArrayList<>(map.keySet())) {
                                                if (Integer.valueOf(2).equals(map.get(k))) {
                                                    map.put(k, 0);
                                                }
                                            }
                                        }
                                    } catch (Throwable ignored) {}
                                }
                            }
                        });
                    }
                }
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> holderClass = XposedHelpers.findClassIfExists(multiStateHolderClass, classLoader);
            if (holderClass != null) {
                Field jField = null;
                try { jField = holderClass.getDeclaredField("j"); } catch (Throwable ignored) {}
                Class<?> wrapperClass = (jField != null) ? jField.getType() : XposedHelpers.findClassIfExists("LX.0muS", classLoader);

                if (wrapperClass != null) {
                    Field eField = null;
                    try { eField = wrapperClass.getDeclaredField("e"); } catch (Throwable ignored) {}
                    Class<?> downloadActionClass = (eField != null) ? eField.getType() : XposedHelpers.findClassIfExists("LX.1Ny3", classLoader);

                    if (downloadActionClass != null) {
                        for (Method m : downloadActionClass.getDeclaredMethods()) {
                            if ("enable".equals(m.getName()) && m.getReturnType() == boolean.class) {
                                XposedBridge.hookMethod(m, new XC_MethodHook() {
                                    @Override
                                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                        if (MainHook.isBypassDownloadRestrictionEnabled() || MainHook.isNoWatermarkEnabled()) {
                                            param.setResult(true);
                                        }
                                    }
                                });
                            } else if ("dismissForDisableAction".equals(m.getName()) && m.getReturnType() == boolean.class) {
                                XposedBridge.hookMethod(m, new XC_MethodHook() {
                                    @Override
                                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                        if (MainHook.isBypassDownloadRestrictionEnabled() || MainHook.isNoWatermarkEnabled()) {
                                            param.setResult(false);
                                        }
                                    }
                                });
                            }
                        }
                        XposedBridge.log(TAG + ": Hooked Douyin DownloadAction (" + downloadActionClass.getName() + ").enable()");
                    }

                    for (Method m : wrapperClass.getDeclaredMethods()) {
                        if ("enable".equals(m.getName()) && m.getReturnType() == boolean.class) {
                            XposedBridge.hookMethod(m, new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    if (MainHook.isBypassDownloadRestrictionEnabled() || MainHook.isNoWatermarkEnabled()) {
                                        param.setResult(true);
                                    }
                                }
                            });
                        } else if ("LIZ".equals(m.getName()) && m.getReturnType() == boolean.class) {
                            XposedBridge.hookMethod(m, new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    if (MainHook.isBypassDownloadRestrictionEnabled() || MainHook.isNoWatermarkEnabled()) {
                                        param.setResult(true);
                                    }
                                }
                            });
                        }
                    }

                    XposedBridge.hookAllConstructors(wrapperClass, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            if (MainHook.isBypassDownloadRestrictionEnabled() || MainHook.isNoWatermarkEnabled()) {
                                try {
                                    XposedHelpers.setIntField(param.thisObject, "b", 0);
                                } catch (Throwable ignored) {}
                            }
                        }
                    });
                }
            }
        } catch (Throwable t) {
            Log.d(TAG, "DownloadAction/Wrapper hook error: " + t.getMessage());
        }

        try {
            Class<?> saClass = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.share.ShareActionImpl", classLoader);
            if (saClass != null) {
                XC_MethodHook returnTrueHook = new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (MainHook.isBypassDownloadRestrictionEnabled() || MainHook.isNoWatermarkEnabled()) {
                            param.setResult(true);
                        }
                    }
                };
                for (Method m : saClass.getDeclaredMethods()) {
                    if ("LJ".equals(m.getName()) || "LJJIJIIJIL".equals(m.getName())) {
                        XposedBridge.hookMethod(m, returnTrueHook);
                    }
                }
                XposedBridge.log(TAG + ": Hooked ShareActionImpl.LJ/LJJIJIIJIL for Douyin");
            }
        } catch (Throwable t) {
            Log.d(TAG, "ShareActionImpl hook error: " + t.getMessage());
        }

        try {
            Class<?> sheetActionDefault = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.sharer.ui.SheetAction$DefaultImpls", classLoader);
            if (sheetActionDefault != null) {
                for (Method m : sheetActionDefault.getDeclaredMethods()) {
                    if ("enable".equals(m.getName())) {
                        XposedBridge.hookMethod(m, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                if (MainHook.isBypassDownloadRestrictionEnabled() || MainHook.isNoWatermarkEnabled()) {
                                    param.setResult(true);
                                }
                            }
                        });
                    }
                }
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> paramsClass = XposedHelpers.findClassIfExists(
                    "com.ss.android.ugc.aweme.feed.long_press_panel.model.LongPressPanelParams",
                    classLoader
            );
            if (paramsClass != null) {
                XposedHelpers.findAndHookMethod(
                        "com.ss.android.ugc.aweme.feed.long_press_panel.modules.business.homepage.LppDownloadModule",
                        classLoader,
                        "LJJ",
                        paramsClass,
                        new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                if (MainHook.isBypassDownloadRestrictionEnabled() || MainHook.isNoWatermarkEnabled()) {
                                    param.setResult(0);
                                }
                            }
                        }
                );
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> normalVhClass = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.share.socialpanel.viewholder.NormalActionViewHolder", classLoader);
            if (normalVhClass != null) {
                for (Method m : normalVhClass.getDeclaredMethods()) {
                    if ("A3".equals(m.getName())) {
                        XposedBridge.hookMethod(m, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                try {
                                    View itemView = (View) XposedHelpers.getObjectField(param.thisObject, "itemView");
                                    if (itemView != null) {
                                        itemView.setAlpha(1.0f);
                                        itemView.setEnabled(true);
                                        itemView.setClickable(true);
                                    }
                                    View a = (View) XposedHelpers.getObjectField(param.thisObject, "a");
                                    if (a != null) a.setAlpha(1.0f);
                                    View b = (View) XposedHelpers.getObjectField(param.thisObject, "b");
                                    if (b != null) b.setAlpha(1.0f);
                                } catch (Throwable ignored) {}
                            }
                        });
                    }
                }
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> vsvClass = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.feed.ui.VideoShareView", classLoader);
            if (vsvClass != null) {
                for (Method m : vsvClass.getDeclaredMethods()) {
                    if ("LJ".equals(m.getName())) {
                        XposedBridge.hookMethod(m, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                if (param.args != null && param.args.length > 0 && param.args[0] != null) {
                                    try {
                                        Object data = XposedHelpers.callMethod(param.args[0], "getData");
                                        if (data instanceof Map) {
                                            ((Map) data).put("share_enable_state", Boolean.FALSE);
                                        }
                                    } catch (Throwable ignored) {}
                                }
                            }

                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                try {
                                    View v = (View) XposedHelpers.getObjectField(param.thisObject, "v");
                                    if (v != null) {
                                        v.setAlpha(1.0f);
                                        v.setEnabled(true);
                                        v.setClickable(true);
                                    }
                                } catch (Throwable ignored) {}
                            }
                        });
                    } else if ("LJIJI".equals(m.getName())) {
                        XposedBridge.hookMethod(m, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                if (param.args != null && param.args.length > 0 && param.args[0] != null) {
                                    try {
                                        Object data = XposedHelpers.callMethod(param.args[0], "getData");
                                        if (data instanceof Map) {
                                            ((Map) data).put("share_enable_state", Boolean.FALSE);
                                        }
                                    } catch (Throwable ignored) {}
                                }
                            }

                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                try {
                                    Object res = param.getResult();
                                    if (res != null) {
                                        Object data = XposedHelpers.callMethod(res, "getData");
                                        if (data instanceof Map) {
                                            ((Map) data).put("share_enable_state", Boolean.FALSE);
                                        }
                                    }
                                } catch (Throwable ignored) {}
                            }
                        });
                    }
                }
                XposedBridge.log(TAG + ": Hooked VideoShareView.LJ/LJIJI for Douyin feed share button");
            }
        } catch (Throwable ignored) {}
    }

    private static void makeMultiStateDownloadViewHolderEnabled(final Object holder) {
        if (holder == null) return;
        if (!MainHook.isBypassDownloadRestrictionEnabled() && !MainHook.isNoWatermarkEnabled()) return;

        try {
            XposedHelpers.setIntField(holder, "i", 0);
        } catch (Throwable ignored) {}

        try {
            Object wrapper = XposedHelpers.getObjectField(holder, "j");
            if (wrapper != null) {
                XposedHelpers.setIntField(wrapper, "b", 0);
            }
        } catch (Throwable ignored) {}

        final View itemView = (View) XposedHelpers.getObjectField(holder, "itemView");
        if (itemView != null) {
            itemView.setAlpha(1.0f);
            itemView.setEnabled(true);
            itemView.setClickable(true);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismissDouyinSharePanel(holder, v);

                    Object aweme = extractAwemeFromHolder(holder);
                    if (aweme == null) {
                        aweme = sCurrentAweme;
                    }

                    downloadAweme(v.getContext(), aweme);
                }
            });
        }

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
            if (disabledIcon != null) {
                disabledIcon.setVisibility(View.GONE);
            }
        } catch (Throwable ignored) {}

        try {
            View disabledLabel = (View) XposedHelpers.getObjectField(holder, "h");
            if (disabledLabel != null) {
                disabledLabel.setVisibility(View.GONE);
            }
        } catch (Throwable ignored) {}

        try {
            View ring = (View) XposedHelpers.getObjectField(holder, "d");
            if (ring != null) {
                ring.setVisibility(View.GONE);
            }
        } catch (Throwable ignored) {}

        try {
            View progLabel = (View) XposedHelpers.getObjectField(holder, "g");
            if (progLabel != null) {
                progLabel.setVisibility(View.GONE);
            }
        } catch (Throwable ignored) {}
    }

    private static void dismissDouyinSharePanel(Object holder, View v) {

        try {
            Object muS = XposedHelpers.getObjectField(holder, "j");
            if (muS != null) {
                Object vm = XposedHelpers.getObjectField(muS, "g");
                if (vm != null) {
                    Object pObj = XposedHelpers.getObjectField(vm, "p");
                    if (pObj != null) {
                        Object panel = XposedHelpers.getObjectField(pObj, "b");
                        if (panel instanceof Dialog) {
                            ((Dialog) panel).dismiss();
                            return;
                        } else if (panel != null) {
                            Method dm = panel.getClass().getMethod("dismiss");
                            dm.invoke(panel);
                            return;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        try {
            android.view.ViewParent p = (v != null) ? v.getParent() : null;
            while (p != null) {
                String pName = p.getClass().getName();
                if (pName.contains("SocialActionsPanel") || pName.contains("BottomSheet") || pName.contains("Dialog")) {
                    try {
                        Method dismiss = p.getClass().getMethod("dismiss");
                        dismiss.invoke(p);
                        return;
                    } catch (Throwable ignored) {}
                }
                p = p.getParent();
            }
        } catch (Throwable ignored) {}

        if (v != null) {
            Activity act = getActivityFromContext(v.getContext());
            if (act instanceof androidx.fragment.app.FragmentActivity) {
                try {
                    androidx.fragment.app.FragmentManager fm = ((androidx.fragment.app.FragmentActivity) act).getSupportFragmentManager();
                    for (androidx.fragment.app.Fragment f : fm.getFragments()) {
                        if (f instanceof androidx.fragment.app.DialogFragment) {
                            ((androidx.fragment.app.DialogFragment) f).dismissAllowingStateLoss();
                        }
                    }
                } catch (Throwable ignored) {}
            }
        }
    }

    private static Object extractAwemeFromHolder(Object holder) {
        if (holder == null) return null;
        try {
            Object muS = XposedHelpers.getObjectField(holder, "j");
            if (muS != null) {
                Object action = XposedHelpers.getObjectField(muS, "e");
                if (action != null) {
                    Object aweme = XposedHelpers.getObjectField(action, "b");
                    if (aweme != null) {
                        sCurrentAweme = aweme;
                        return aweme;
                    }
                }
            }
        } catch (Throwable ignored) {}

        try {
            for (java.lang.reflect.Field f : holder.getClass().getDeclaredFields()) {
                if (f.getType().getName().contains("Aweme")) {
                    f.setAccessible(true);
                    Object val = f.get(holder);
                    if (val != null) {
                        sCurrentAweme = val;
                        return val;
                    }
                }
            }
        } catch (Throwable ignored) {}

        return sCurrentAweme;
    }

    private static void attachSaveButtonToShareDialog(final Dialog dialog) {
        if (dialog == null) return;
        final Window window = dialog.getWindow();
        if (window == null) return;
        final View decor = window.getDecorView();
        if (decor == null) return;

        Runnable attachTask = () -> {
            try {
                if (!(decor instanceof ViewGroup)) return;
                final ViewGroup decorGroup = (ViewGroup) decor;

                final String tag = "tiktok_enhancer_share_dl_btn";
                if (decorGroup.findViewWithTag(tag) != null) return;

                TextView reportTv = findActionTextView(decorGroup);
                if (reportTv == null) return;

                if (hasNativeDownloadButton(decorGroup)) return;

                Context ctx = dialog.getContext();
                Activity activity = getActivityFromContext(ctx);
                if (activity == null) activity = dialog.getOwnerActivity();

                boolean isSavedList = isSavedVideoList(activity);
                if (isSavedList) return;

                boolean isStory = isStoryAweme(sCurrentAweme) || !sCurrentStoryList.isEmpty()
                        || isStoryDetailActivity(activity);
                boolean isChina = MainHook.isChinaPackage();

                String buttonText;
                if (isStory) {
                    if (!MainHook.isDownloadStoryEnabled()) return;
                    buttonText = isChina ? "⬇  保存日常" : "⬇  Save Story";
                } else {
                    if (!MainHook.isBypassDownloadRestrictionEnabled() && !MainHook.isNoWatermarkEnabled()) return;
                    buttonText = isChina ? "⬇  保存视频" : "⬇  Save Video";
                }

                TextView saveBtn = new TextView(ctx);
                saveBtn.setTag(tag);
                saveBtn.setText(buttonText);
                saveBtn.setTextSize(12.5f);
                saveBtn.setTextColor(Color.WHITE);
                saveBtn.setGravity(Gravity.CENTER);
                saveBtn.setPadding(dpToPx(ctx, 16), dpToPx(ctx, 6), dpToPx(ctx, 16), dpToPx(ctx, 6));
                saveBtn.setElevation(dpToPx(ctx, 6));

                GradientDrawable bg = new GradientDrawable();
                bg.setShape(GradientDrawable.RECTANGLE);
                bg.setCornerRadius(dpToPx(ctx, 17));
                bg.setColor(Color.parseColor("#E6181920"));
                bg.setStroke(dpToPx(ctx, 1), Color.parseColor("#40FFFFFF"));
                saveBtn.setBackground(bg);

                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        dpToPx(ctx, 34)
                );
                lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                lp.bottomMargin = dpToPx(ctx, 10);

                saveBtn.setOnClickListener(v -> {
                    try {
                        dialog.dismiss();
                    } catch (Throwable ignored) {}
                    downloadAweme(ctx, sCurrentAweme);
                });

                decorGroup.addView(saveBtn, lp);
                Log.i(TAG, "Successfully injected compact " + buttonText + " button into Share Sheet!");
            } catch (Throwable t) {
                Log.e(TAG, "attachSaveButtonToShareDialog error: " + t.getMessage());
            }
        };

        decor.post(attachTask);
        decor.postDelayed(attachTask, 200);
        decor.postDelayed(attachTask, 500);
    }

    private static boolean hasNativeDownloadButton(View view) {
        if (view == null) return false;

        if (view instanceof TextView && view.getVisibility() == View.VISIBLE) {
            CharSequence text = ((TextView) view).getText();
            if (text != null) {
                String s = text.toString().trim().toLowerCase(Locale.US);
                if (s.equals("download") || s.equals("save video") || s.equals("unduh")
                        || s.equals("herunterladen") || s.equals("télécharger") || s.equals("descargar")
                        || s.equals("simpan video") || s.equals("lưu video") || s.equals("salvar vídeo")
                        || s.equals("salvar video") || s.equals("scarica video") || s.equals("video speichern")
                        || s.equals("保存本地") || s.equals("下载") || s.equals("保存视频") || s.equals("保存")
                        || s.contains("保存本地")) {

                    float alpha = view.getAlpha();
                    View current = view;
                    while (current.getParent() instanceof View) {
                        current = (View) current.getParent();
                        alpha *= current.getAlpha();
                    }
                    if (alpha < 0.8f || !view.isEnabled()) {
                        return false;
                    }
                    return true;
                }
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                if (hasNativeDownloadButton(vg.getChildAt(i))) return true;
            }
        }
        return false;
    }

    private static TextView findActionTextView(View view) {
        if (view instanceof TextView) {
            CharSequence text = ((TextView) view).getText();
            if (text != null) {
                String s = text.toString().trim().toLowerCase();
                if (s.equals("report") || s.equals("reportar") || s.equals("signaler")
                        || s.equals("laporkan") || s.equals("melden") || s.equals("báo cáo")
                        || s.equals("举报") || s.equals("投诉") || s.contains("举报") || s.contains("投诉")
                        || s.equals("分享给朋友") || s.equals("复制链接") || s.equals("合拍") || s.equals("一起看")
                        || s.equals("不感兴趣") || s.equals("生成图片") || s.equals("加桌面伙伴") || s.equals("播放反馈")) {
                    return (TextView) view;
                }
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                TextView found = findActionTextView(vg.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private static View findFirstImageViewOrCircle(View parent) {
        if (parent instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) parent;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View child = vg.getChildAt(i);
                if (child instanceof android.widget.ImageView || (child.getWidth() > 0 && child.getWidth() == child.getHeight())) {
                    return child;
                }
            }
        }
        return null;
    }

    private static class StoryMedia {
        boolean isVideo;
        List<String> urls = new ArrayList<>();
        String aid;
        String desc;
    }

    public static void downloadAweme(Context ctx, Object aweme) {
        if (ctx == null) return;

        Object targetAweme = (aweme != null) ? aweme : sCurrentAweme;
        boolean isStory = isStoryAweme(targetAweme);

        StoryMedia media = extractStoryMedia(targetAweme);

        if (media == null && isStory) {
            synchronized (sCurrentStoryList) {
                for (Object item : sCurrentStoryList) {
                    if (item != null) {
                        media = extractStoryMedia(item);
                        if (media != null) {
                            Log.i(TAG, "Found story media from sCurrentStoryList");
                            break;
                        }
                    }
                }
            }
        }

        if (media == null && sLastPlayedPlayAddr != null) {
            List<String> liveUrls = getUrlsFromUrlModel(sLastPlayedPlayAddr);
            if (!liveUrls.isEmpty()) {
                media = new StoryMedia();
                media.isVideo = true;
                media.urls.addAll(liveUrls);
                media.aid = String.valueOf(System.currentTimeMillis());
                media.desc = isStory ? "Story Video" : "TikTok Video";
                Log.i(TAG, "Found media directly from live sLastPlayedPlayAddr: " + liveUrls.get(0));
            }
        }

        if (media == null || media.urls.isEmpty()) {
            Log.e(TAG, "Could not find media stream URL. aweme=" + (aweme != null ? aweme.getClass().getName() : "null")
                    + ", sCurrentAweme=" + (sCurrentAweme != null ? sCurrentAweme.getClass().getName() : "null")
                    + ", isStory=" + isStory);
            Toast.makeText(ctx, "Could not find video stream URL", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isChina = MainHook.isChinaPackage();
        String prefix = isChina ? (isStory ? "douyin_story_" : "douyin_") : (isStory ? "story_" : "tiktok_");
        if (media.isVideo) {
            String bestUrl = cleanDownloadUrl(media.urls.get(0));
            enqueueDownload(ctx, bestUrl, prefix + "video_" + media.aid + ".mp4", media.desc, "video/mp4");
        } else {
            int count = 0;
            for (int i = 0; i < media.urls.size(); i++) {
                String imgUrl = media.urls.get(i);
                enqueueDownload(ctx, imgUrl, prefix + "photo_" + media.aid + "_" + (i + 1) + ".jpg", media.desc, "image/jpeg");
                count++;
            }
            String msg;
            if (isChina) {
                msg = isStory ? ("正在保存 " + count + " 张日常照片...") : ("正在下载 " + count + " 张图片...");
            } else {
                msg = isStory ? ("Downloading " + count + " story photo(s)...") : ("Downloading " + count + " photo(s)...");
            }
            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
        }
    }

    private static StoryMedia extractStoryMedia(Object aweme) {
        if (aweme == null) return null;
        StoryMedia media = new StoryMedia();

        try {
            media.aid = (String) XposedHelpers.callMethod(aweme, "getAid");
        } catch (Throwable ignored) {}
        if (media.aid == null || media.aid.isEmpty()) {
            media.aid = String.valueOf(System.currentTimeMillis());
        }
        try {
            media.desc = (String) XposedHelpers.callMethod(aweme, "getDesc");
        } catch (Throwable ignored) {}

        try {
            Object video = null;
            try {
                video = XposedHelpers.getObjectField(aweme, "video");
            } catch (Throwable ignored) {}
            if (video == null) {
                try {
                    video = XposedHelpers.callMethod(aweme, "getVideo");
                } catch (Throwable ignored) {}
            }
            if (video != null) {
                cleanVideo(video);
                Object cleanPlay = getCleanPlayAddr(video);
                if (cleanPlay != null) {
                    List<String> extracted = getUrlsFromUrlModel(cleanPlay);
                    if (!extracted.isEmpty()) {
                        media.isVideo = true;
                        media.urls.addAll(extracted);
                        Log.i(TAG, "Extracted clean video URL from Video: " + extracted.get(0));
                        return media;
                    }
                }

                if (sLastPlayedPlayAddr != null) {
                    List<String> extracted = getUrlsFromUrlModel(sLastPlayedPlayAddr);
                    if (!extracted.isEmpty()) {
                        media.isVideo = true;
                        media.urls.addAll(extracted);
                        Log.i(TAG, "Extracted clean video URL from sLastPlayedPlayAddr: " + extracted.get(0));
                        return media;
                    }
                }

                String[] addrMethods = {
                        "getPlayAddr", "getProperPlayAddr", "LJIILJJIL", "LJIIIIZZ", "LJIILIIL",
                        "LJIILL", "LJIILLIIL", "getPlayAddrH264", "getH264PlayAddr", "getPlayAddrBytevc1",
                        "getDownloadNoWatermarkAddr"
                };

                for (String m : addrMethods) {
                    try {
                        Object urlModel = XposedHelpers.callMethod(video, m);
                        if (urlModel != null) {
                            cleanUrlModel(urlModel);
                            List<String> extracted = getUrlsFromUrlModel(urlModel);
                            if (!extracted.isEmpty()) {
                                media.isVideo = true;
                                media.urls.addAll(extracted);
                                Log.i(TAG, "Extracted story video URL from Video." + m + "(): " + extracted.get(0));
                                return media;
                            }
                        }
                    } catch (Throwable ignored) {}
                }

                for (String fieldName : new String[]{"a", "d", "b", "playAddr"}) {
                    try {
                        Object urlModel = XposedHelpers.getObjectField(video, fieldName);
                        if (urlModel != null) {
                            cleanUrlModel(urlModel);
                            List<String> extracted = getUrlsFromUrlModel(urlModel);
                            if (!extracted.isEmpty()) {
                                media.isVideo = true;
                                media.urls.addAll(extracted);
                                Log.i(TAG, "Extracted story video URL from Video." + fieldName + ": " + extracted.get(0));
                                return media;
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable t) {
            Log.d(TAG, "Video extraction failed: " + t.getMessage());
        }

        try {
            Object photoModeInfo = XposedHelpers.callMethod(aweme, "getPhotoModeImageInfo");
            if (photoModeInfo != null) {
                List<?> imageList = (List<?>) XposedHelpers.callMethod(photoModeInfo, "getImageList");
                if (imageList != null && !imageList.isEmpty()) {
                    for (Object item : imageList) {
                        if (item == null) continue;
                        String[] modelFields = {"displayImageNoWatermark", "targetMultiRateImageUrl", "userWatermarkImage", "ownerWatermarkImage", "thumbnail"};
                        for (String f : modelFields) {
                            try {
                                Object urlModel = XposedHelpers.getObjectField(item, f);
                                if (urlModel != null) {
                                    List<String> extracted = getUrlsFromUrlModel(urlModel);
                                    if (!extracted.isEmpty()) {
                                        media.urls.add(extracted.get(0));
                                        break;
                                    }
                                }
                            } catch (Throwable ignored) {}
                        }
                    }
                    if (!media.urls.isEmpty()) {
                        media.isVideo = false;
                        Log.i(TAG, "Extracted " + media.urls.size() + " story photos from PhotoModeImageInfo");
                        return media;
                    }
                }
            }
        } catch (Throwable t) {
            Log.d(TAG, "PhotoModeImageInfo extraction failed: " + t.getMessage());
        }

        try {
            List<?> imageInfos = (List<?>) XposedHelpers.callMethod(aweme, "getImageInfos");
            if (imageInfos != null && !imageInfos.isEmpty()) {
                for (Object img : imageInfos) {
                    if (img == null) continue;
                    String[] imgMethods = {"getLabelLarge", "getLabelThumb", "getUrlModel"};
                    for (String m : imgMethods) {
                        try {
                            Object urlModel = XposedHelpers.callMethod(img, m);
                            if (urlModel != null) {
                                List<String> extracted = getUrlsFromUrlModel(urlModel);
                                if (!extracted.isEmpty()) {
                                    media.urls.add(extracted.get(0));
                                    break;
                                }
                            }
                        } catch (Throwable ignored) {}
                    }
                }
                if (!media.urls.isEmpty()) {
                    media.isVideo = false;
                    Log.i(TAG, "Extracted " + media.urls.size() + " story photos from ImageInfos");
                    return media;
                }
            }
        } catch (Throwable t) {
            Log.d(TAG, "ImageInfos extraction failed: " + t.getMessage());
        }

        return null;
    }

    private static List<String> getUrlsFromUrlModel(Object urlModel) {
        List<String> res = new ArrayList<>();
        if (urlModel == null) return res;
        try {
            List<?> list = (List<?>) XposedHelpers.callMethod(urlModel, "getUrlList");
            if (list == null || list.isEmpty()) {
                try {
                    list = (List<?>) XposedHelpers.getObjectField(urlModel, "urlList");
                } catch (Throwable ignored) {}
            }
            if (list != null) {
                for (Object item : list) {
                    if (item instanceof String) {
                        String s = (String) item;
                        if (!s.isEmpty() && (s.startsWith("http://") || s.startsWith("https://"))) {
                            res.add(cleanDownloadUrl(s));
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        if (res.isEmpty()) {
            try {
                String uri = (String) XposedHelpers.callMethod(urlModel, "getUri");
                if (uri == null) {
                    try {
                        uri = (String) XposedHelpers.getObjectField(urlModel, "uri");
                    } catch (Throwable ignored) {}
                }
                if (uri != null && (uri.startsWith("http://") || uri.startsWith("https://"))) {
                    res.add(cleanDownloadUrl(uri));
                }
            } catch (Throwable ignored) {}
        }
        return res;
    }

    private static void enqueueDownload(Context ctx, String url, String fileName, String desc, String mimeType) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle(fileName);
            request.setDescription(desc != null && !desc.isEmpty() ? desc : "Downloading from story");
            request.setMimeType(mimeType);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            request.addRequestHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14)");

            DownloadManager dm = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm != null) {
                dm.enqueue(request);
                String msg;
                if (MainHook.isChinaPackage()) {
                    msg = fileName.contains("story") ? ("正在保存日常: " + fileName) : ("正在下载无水印视频: " + fileName);
                } else {
                    msg = fileName.contains("story") ? ("Saving story: " + fileName) : ("Downloading video: " + fileName);
                }
                Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
            }
        } catch (Throwable t) {
            Log.e(TAG, "enqueueDownload failed", t);
            Toast.makeText(ctx, "Download request failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public static String cleanDownloadUrl(String url) {
        if (url == null) return null;
        String clean = url.replace("/playwm/", "/play/").replace("playwm", "play");
        if (clean.contains("watermark=")) {
            clean = clean.replaceAll("([&?])watermark=[^&]*", "$1watermark=0");
        }
        if (clean.contains("is_watermark=")) {
            clean = clean.replaceAll("([&?])is_watermark=[^&]*", "$1is_watermark=0");
        }
        if (clean.contains("logo_name=")) {
            clean = clean.replaceAll("([&?])logo_name=[^&]*", "");
        }
        return clean;
    }

    private static int dpToPx(Context ctx, int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                ctx.getResources().getDisplayMetrics()
        );
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

