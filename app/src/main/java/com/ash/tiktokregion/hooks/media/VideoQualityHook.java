package com.ash.tiktokregion.hooks.media;

import android.util.Log;

import com.ash.tiktokregion.core.BaseHook;
import com.ash.tiktokregion.core.ModuleConfig;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Enforces highest bitrate and resolution playback (1080p / 720p 60fps) by sorting bitrate profiles
 * descending and pointing playback streams to maximum quality.
 */
public class VideoQualityHook implements BaseHook {

    private static final String TAG = "TikTokVideoQuality";
    private static volatile Object sLastPlayedPlayAddr = null;

    @Override
    public void hook(ClassLoader classLoader) {
        hookVideoQualityStreams(classLoader);
    }

    private void hookVideoQualityStreams(ClassLoader classLoader) {
        final String videoClass = "com.ss.android.ugc.aweme.feed.model.Video";

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

        try {
            XposedHelpers.findAndHookMethod(videoClass, classLoader, "getBitRate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!ModuleConfig.forceHighestQuality) return;

                    Object result = param.getResult();
                    if (result instanceof List) {
                        List<?> list = (List<?>) result;
                        if (list.size() > 1) {
                            try {
                                Collections.sort((List) list, new Comparator<Object>() {
                                    @Override
                                    public int compare(Object a, Object b) {
                                        try {
                                            int rateA = (int) XposedHelpers.callMethod(a, "getBitRate");
                                            int rateB = (int) XposedHelpers.callMethod(b, "getBitRate");
                                            return Integer.compare(rateB, rateA); // Descending
                                        } catch (Throwable ignored) {
                                            return 0;
                                        }
                                    }
                                });
                            } catch (Throwable ignored) {}
                        }
                    }
                }
            });
            XposedBridge.log(TAG + ": Hooked Video.getBitRate() for highest bitrate sorting");
        } catch (Throwable t) {
            Log.d(TAG, "Video.getBitRate hook failed: " + t.getMessage());
        }
    }

    public static Object getHighestQualityPlayAddr(Object video) {
        if (video == null) return null;
        try {
            Object bitRatesObj = XposedHelpers.callMethod(video, "getBitRate");
            if (bitRatesObj instanceof List) {
                List<?> list = (List<?>) bitRatesObj;
                if (list.isEmpty()) return null;

                Object bestBitRate = null;
                int maxRate = -1;

                for (Object item : list) {
                    if (item == null) continue;
                    try {
                        int rate = (int) XposedHelpers.callMethod(item, "getBitRate");
                        Object playAddr = XposedHelpers.callMethod(item, "getPlayAddr");
                        if (playAddr != null && rate > maxRate) {
                            maxRate = rate;
                            bestBitRate = item;
                        }
                    } catch (Throwable ignored) {}
                }

                if (bestBitRate != null) {
                    return XposedHelpers.callMethod(bestBitRate, "getPlayAddr");
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public static Object getLastPlayedPlayAddr() {
        return sLastPlayedPlayAddr;
    }
}
