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

    /**
     * Returns the highest bitrate playback stream compatible with Android MediaStore / Gallery (H.264 preferred,
     * HEVC fallback), strictly avoiding ByteVC2 (bvc2) which Android MediaExtractor misclassifies as audio/mp4.
     */
    public static Object getHighestCompatibleDownloadPlayAddr(Object video) {
        if (video == null) return null;
        try {
            Object bitRatesObj = XposedHelpers.callMethod(video, "getBitRate");
            if (bitRatesObj instanceof List) {
                List<?> list = (List<?>) bitRatesObj;
                Object bestH264 = null;
                int maxH264Rate = -1;
                Object bestHevc = null;
                int maxHevcRate = -1;

                for (Object item : list) {
                    if (item == null) continue;
                    try {
                        Object playAddr = XposedHelpers.callMethod(item, "getPlayAddr");
                        if (playAddr == null) continue;

                        int rate = (int) XposedHelpers.callMethod(item, "getBitRate");
                        int bytevc1 = 0;
                        try {
                            Object b = XposedHelpers.callMethod(item, "isBytevc1");
                            if (b instanceof Number) bytevc1 = ((Number) b).intValue();
                        } catch (Throwable ignored) {}

                        String gear = "";
                        try {
                            Object g = XposedHelpers.callMethod(item, "getGearName");
                            if (g != null) gear = g.toString().toLowerCase(java.util.Locale.ROOT);
                        } catch (Throwable ignored) {}

                        String urlKey = "";
                        try {
                            Object u = XposedHelpers.callMethod(item, "getUrlKey");
                            if (u != null) urlKey = u.toString().toLowerCase(java.util.Locale.ROOT);
                        } catch (Throwable ignored) {}

                        if (bytevc1 >= 2 || gear.contains("bytevc2") || gear.contains("bvc2")
                                || urlKey.contains("bytevc2") || urlKey.contains("bvc2")) {
                            continue;
                        }

                        if (bytevc1 == 0) {
                            if (rate > maxH264Rate) {
                                maxH264Rate = rate;
                                bestH264 = playAddr;
                            }
                        } else {
                            if (rate > maxHevcRate) {
                                maxHevcRate = rate;
                                bestHevc = playAddr;
                            }
                        }
                    } catch (Throwable ignored) {}
                }

                if (bestH264 != null) return bestH264;
                if (bestHevc != null) return bestHevc;
            }
        } catch (Throwable ignored) {}

        try {
            Object h264 = XposedHelpers.callMethod(video, "getPlayAddrH264");
            if (isCompatiblePlayAddr(h264)) return h264;
        } catch (Throwable ignored) {}
        try {
            Object h264 = XposedHelpers.callMethod(video, "getH264PlayAddr");
            if (isCompatiblePlayAddr(h264)) return h264;
        } catch (Throwable ignored) {}
        try {
            Object bytevc1 = XposedHelpers.callMethod(video, "getPlayAddrBytevc1");
            if (isCompatiblePlayAddr(bytevc1)) return bytevc1;
        } catch (Throwable ignored) {}
        return null;
    }

    public static boolean isCompatiblePlayAddr(Object urlModel) {
        if (urlModel == null) return false;
        try {
            Object ct = XposedHelpers.callMethod(urlModel, "getCodecType");
            if (ct instanceof Number && ((Number) ct).intValue() >= 2) {
                return false;
            }
        } catch (Throwable ignored) {}
        try {
            Object uk = XposedHelpers.callMethod(urlModel, "getUrlKey");
            if (uk != null) {
                String s = uk.toString().toLowerCase(java.util.Locale.ROOT);
                if (s.contains("bytevc2") || s.contains("bvc2")) return false;
            }
        } catch (Throwable ignored) {}
        return true;
    }

    public static Object getLastPlayedPlayAddr() {
        return sLastPlayedPlayAddr;
    }
}
