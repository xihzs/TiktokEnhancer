package com.ash.tiktokregion.hooks.ui;

import com.ash.tiktokregion.core.BaseHook;
import com.ash.tiktokregion.core.ModuleConfig;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * 4K / Master Video Ingestion Uncapper Hook.
 * <p>
 * Bypasses TikTok's client-side upload downscaling pipeline:
 * 1. Overrides {@code com.ss.android.ugc.aweme.property.AVSettingsWrapper} methods
 *    so Gallery/Album import never clamps 4K/1080p source video dimensions to 576p.
 * 2. Injects an expanded resolution category ladder into {@code SettingsManager}
 *    with safe valid indices to prevent out-of-bounds fallback.
 * 3. Enforces high-bitrate encoding parameters (50 Mbps, 60fps, CRF 14) in VESDK.
 */
public class MasterUploadHook implements BaseHook {

    private static final String TAG = "TikTokRegion-MasterUpload";

    private static volatile boolean sEnabled = true;

    private static final int[] MASTER_DIMENSIONS = new int[]{2160, 3840};
    private static final String MASTER_RESOLUTION_STR = "2160*3840";
    private static final String MASTER_SIZE_STR = "2160x3840";

    private static final String[] EXTENDED_UPLOAD_CATEGORY = new String[]{
            "576x1024", "720x1280", "720x1280", "720x1280", "1080x1920", "1440x2560", "2160x3840"
    };

    private static final String[] EXTENDED_VIDEO_SIZE_CATEGORY = new String[]{
            "576x1024", "720x1280", "720x1280", "720x1280", "480x848", "544x960", "944x1680", "1080x1920", "1440x2560", "2160x3840"
    };

    public static void setEnabled(boolean enabled) {
        sEnabled = enabled;
        XposedBridge.log(TAG + ": master video upload uncapper status -> enabled=" + enabled);
    }

    public static boolean isEnabled() {
        return sEnabled;
    }

    @Override
    public void hook(ClassLoader cl) {
        hookAVSettingsWrapper(cl);
        hookSettingsManager(cl);
        hookVEVideoEncodeSettings(cl);
    }

    private static void hookAVSettingsWrapper(ClassLoader cl) {
        Class<?> clazz = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.property.AVSettingsWrapper", cl);
        if (clazz == null) {
            XposedBridge.log(TAG + ": AVSettingsWrapper not found, skipping");
            return;
        }

        try {
            // Import dimension bounds (prevents 576p clamping on import)
            XposedHelpers.findAndHookMethod(clazz, "getImportVideoSize", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (sEnabled) {
                        param.setResult(MASTER_DIMENSIONS.clone());
                    }
                }
            });

            XposedHelpers.findAndHookMethod(clazz, "getImportVideoResolution", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (sEnabled) {
                        param.setResult(MASTER_RESOLUTION_STR);
                    }
                }
            });

            // Recording and encode size targets
            XposedHelpers.findAndHookMethod(clazz, "getNowEncodeSize", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (sEnabled) {
                        param.setResult(MASTER_DIMENSIONS.clone());
                    }
                }
            });

            XposedHelpers.findAndHookMethod(clazz, "getNowShotScreenEncodeSize", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (sEnabled) {
                        param.setResult(MASTER_DIMENSIONS.clone());
                    }
                }
            });

            XposedHelpers.findAndHookMethod(clazz, "getRecordVideoSize", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (sEnabled) {
                        param.setResult(MASTER_DIMENSIONS.clone());
                    }
                }
            });

            XposedHelpers.findAndHookMethod(clazz, "getRecordVideoResolution", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (sEnabled) {
                        param.setResult(MASTER_RESOLUTION_STR);
                    }
                }
            });

            XposedHelpers.findAndHookMethod(clazz, "getCompileVideoSize", int.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (sEnabled) {
                        param.setResult(MASTER_DIMENSIONS.clone());
                    }
                }
            });

            // Hardware encoding acceleration
            XposedHelpers.findAndHookMethod(clazz, "enableHardEncodeForRecord", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (sEnabled) param.setResult(Boolean.TRUE);
                }
            });

            XposedHelpers.findAndHookMethod(clazz, "enableHardEncodeForSynthetic", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (sEnabled) param.setResult(Boolean.TRUE);
                }
            });

            XposedHelpers.findAndHookMethod(clazz, "enableHardEncodeForWaterMark", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (sEnabled) param.setResult(Boolean.TRUE);
                }
            });

            // Maximum bitrate & quality level
            XposedHelpers.findAndHookMethod(clazz, "getRecordBitrate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (sEnabled) param.setResult(35.0f);
                }
            });

            XposedHelpers.findAndHookMethod(clazz, "getRecordQuality", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (sEnabled) param.setResult(3);
                }
            });

            XposedBridge.log(TAG + ": Hooked AVSettingsWrapper for 4K master ingestion");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Error hooking AVSettingsWrapper: " + t);
        }
    }

    private static void hookSettingsManager(ClassLoader cl) {
        Class<?> clazz = XposedHelpers.findClassIfExists("com.bytedance.ies.abmock.SettingsManager", cl);
        if (clazz == null) {
            XposedBridge.log(TAG + ": SettingsManager not found, skipping");
            return;
        }

        try {
            // String settings: "video_size"
            XC_MethodHook stringHook = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!sEnabled || param.args == null || param.args.length == 0) return;
                    String key = String.valueOf(param.args[0]);
                    if ("video_size".equals(key)) {
                        param.setResult(MASTER_SIZE_STR);
                    }
                }
            };
            for (Method m : clazz.getDeclaredMethods()) {
                if (("getStringValue".equals(m.getName()) || "LJI".equals(m.getName()))
                        && m.getParameterTypes().length >= 1
                        && m.getParameterTypes()[0] == String.class
                        && m.getReturnType() == String.class) {
                    XposedBridge.hookMethod(m, stringHook);
                }
            }

            // String array settings: "upload_video_size_category", "video_size_category"
            XC_MethodHook strArrHook = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!sEnabled || param.args == null || param.args.length == 0) return;
                    String key = String.valueOf(param.args[0]);
                    if ("upload_video_size_category".equals(key)) {
                        param.setResult(EXTENDED_UPLOAD_CATEGORY.clone());
                    } else if ("video_size_category".equals(key)) {
                        param.setResult(EXTENDED_VIDEO_SIZE_CATEGORY.clone());
                    }
                }
            };
            for (Method m : clazz.getDeclaredMethods()) {
                if (("getStringArrayValue".equals(m.getName()) || "LJIIIIZZ".equals(m.getName()))
                        && m.getParameterTypes().length >= 1
                        && m.getParameterTypes()[0] == String.class
                        && m.getReturnType() == String[].class) {
                    XposedBridge.hookMethod(m, strArrHook);
                }
            }

            // Integer index settings: "upload_video_size_index", "compile_video_size_index"
            XC_MethodHook intHook = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!sEnabled || param.args == null || param.args.length == 0) return;
                    String key = String.valueOf(param.args[0]);
                    if ("upload_video_size_index".equals(key)) {
                        // Index 6 corresponds to "2160x3840" in EXTENDED_UPLOAD_CATEGORY
                        param.setResult(6);
                    } else if ("compile_video_size_index".equals(key)) {
                        // Index 9 corresponds to "2160x3840" in EXTENDED_VIDEO_SIZE_CATEGORY
                        param.setResult(9);
                    }
                }
            };
            for (Method m : clazz.getDeclaredMethods()) {
                if (("getIntValue".equals(m.getName()) || "LJIIJ".equals(m.getName()))
                        && m.getParameterTypes().length >= 1
                        && m.getParameterTypes()[0] == String.class
                        && (m.getReturnType() == int.class || m.getReturnType() == Integer.class)) {
                    XposedBridge.hookMethod(m, intHook);
                }
            }

            XposedBridge.log(TAG + ": Hooked SettingsManager upload categories");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Error hooking SettingsManager: " + t);
        }
    }

    private static void hookVEVideoEncodeSettings(ClassLoader cl) {
        Class<?> clazz = XposedHelpers.findClassIfExists("com.ss.android.vesdk.VEVideoEncodeSettings", cl);
        if (clazz == null) {
            XposedBridge.log(TAG + ": VEVideoEncodeSettings not found, skipping");
            return;
        }

        try {
            // Hook all constructors to upgrade bitrate, framerate, and remux flags
            XposedBridge.hookAllConstructors(clazz, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!sEnabled) return;
                    Object settings = param.thisObject;
                    trySetIntField(settings, "bps", 50_000_000); // 50 Mbps
                    trySetLongField(settings, "swMaxrate", 75_000_000L); // 75 Mbps
                    trySetIntField(settings, "swCRF", 14);
                    trySetIntField(settings, "fps", 60);
                    trySetBooleanField(settings, "enableUploadDirectly", true);
                    trySetBooleanField(settings, "enableByteVCRemuxVideo", true);
                    trySetBooleanField(settings, "enableVideoAndAudioRemux", true);
                    trySetBooleanField(settings, "mOptRemuxWithCopy", true);
                    trySetIntField(settings, "enableRemuxVideoRes", -1);
                    trySetDoubleField(settings, "m4kBitrateRatio", 1.0d);
                }
            });
            XposedBridge.log(TAG + ": Hooked VEVideoEncodeSettings constructor");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Error hooking VEVideoEncodeSettings: " + t);
        }
    }

    private static void trySetIntField(Object obj, String fieldName, int value) {
        try {
            XposedHelpers.setIntField(obj, fieldName, value);
        } catch (Throwable ignored) {}
    }

    private static void trySetLongField(Object obj, String fieldName, long value) {
        try {
            XposedHelpers.setLongField(obj, fieldName, value);
        } catch (Throwable ignored) {}
    }

    private static void trySetBooleanField(Object obj, String fieldName, boolean value) {
        try {
            XposedHelpers.setBooleanField(obj, fieldName, value);
        } catch (Throwable ignored) {}
    }

    private static void trySetDoubleField(Object obj, String fieldName, double value) {
        try {
            XposedHelpers.setDoubleField(obj, fieldName, value);
        } catch (Throwable ignored) {}
    }
}
