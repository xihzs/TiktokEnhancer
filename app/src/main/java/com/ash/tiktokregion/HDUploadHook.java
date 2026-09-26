package com.ash.tiktokregion;

import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class HDUploadHook {

    private static final String TAG = "TikTokHDUpload";

    private static final Set<String> sHookedClasses = ConcurrentHashMap.newKeySet();
    private static volatile boolean sInitialized = false;

    public static final int HD_WIDTH = 1080;
    public static final int HD_HEIGHT = 1920;
    public static final int HD_TARGET_BPS = 18_000_000;
    public static final int HD_MAX_BPS = 35_000_000;
    public static final int HD_TARGET_FPS = 60;
    public static final int HD_SW_CRF = 16;
    public static final int HD_ENCODE_PROFILE_HIGH = 3;
    public static final int HD_COMPILE_SIZE_INDEX_1080P = 7;

    public static final int HD_WIDTH_4K = 2160;
    public static final int HD_HEIGHT_4K = 3840;
    public static final int HD_TARGET_BPS_4K = 50_000_000;
    public static final int HD_MAX_BPS_4K = 75_000_000;
    public static final int HD_SW_CRF_4K = 14;

    public static int getTargetWidth() {
        return MainHook.isUpload4KEnabled() ? HD_WIDTH_4K : HD_WIDTH;
    }

    public static int getTargetHeight() {
        return MainHook.isUpload4KEnabled() ? HD_HEIGHT_4K : HD_HEIGHT;
    }

    public static int getTargetBps() {
        return MainHook.isUpload4KEnabled() ? HD_TARGET_BPS_4K : HD_TARGET_BPS;
    }

    public static int getMaxBps() {
        return MainHook.isUpload4KEnabled() ? HD_MAX_BPS_4K : HD_MAX_BPS;
    }

    public static int getSwCrf() {
        return MainHook.isUpload4KEnabled() ? HD_SW_CRF_4K : HD_SW_CRF;
    }

    public static long getDirectUploadThresholdMb() {
        return MainHook.isUpload4KEnabled() ? 1500L : 500L;
    }

    public static void log(String msg) {
        Log.i(TAG, msg);
        try {
            XposedBridge.log(TAG + ": " + msg);
        } catch (Throwable ignored) {}
    }

    public static void logD(String msg) {
        Log.d(TAG, msg);
    }

    public static void hook(ClassLoader classLoader) {
        if (classLoader == null) return;
        if (!MainHook.isHDUploadEnabled()) return;

        try {
            hookKevaPublishRepo(classLoader);
        } catch (Throwable t) {
            logD("hookKevaPublishRepo error: " + t.getMessage());
        }

        try {
            hookSettingsManager(classLoader);
        } catch (Throwable t) {
            logD("hookSettingsManager error: " + t.getMessage());
        }

        try {
            hookAVSettingsWrapper(classLoader);
        } catch (Throwable t) {
            logD("hookAVSettingsWrapper error: " + t.getMessage());
        }

        try {
            hookVEEditor(classLoader);
        } catch (Throwable t) {
            logD("hookVEEditor error: " + t.getMessage());
        }

        try {
            hookVEVideoEncodeSettings(classLoader);
        } catch (Throwable t) {
            logD("hookVEVideoEncodeSettings error: " + t.getMessage());
        }

        try {
            hookPublishHDSettings(classLoader);
        } catch (Throwable t) {
            logD("hookPublishHDSettings error: " + t.getMessage());
        }

        sInitialized = true;
    }

    private static final ThreadLocal<Boolean> sInHDUpload = new ThreadLocal<>();

    public static void onClassLoaded(Class<?> clazz) {
        if (clazz == null) return;
        if (Boolean.TRUE.equals(sInHDUpload.get())) return;
        if (!MainHook.isHDUploadEnabled()) return;

        String name = clazz.getName();
        if (sHookedClasses.contains(name)) return;

        sInHDUpload.set(true);
        try {
            if ("com.ss.android.vesdk.VEEditor".equals(name)) {
                hookVEEditorClass(clazz);
            } else if ("com.ss.android.vesdk.VEVideoEncodeSettings".equals(name)) {
                hookVEVideoEncodeSettingsClass(clazz);
            } else if ("com.ss.android.ugc.aweme.property.AVSettingsWrapper".equals(name)) {
                hookAVSettingsWrapperClass(clazz);
            } else if ("com.bytedance.ies.abmock.SettingsManager".equals(name)) {
                hookSettingsManagerClass(clazz);
            } else if ("com.bytedance.keva.Keva".equals(name)) {
                hookKevaClass(clazz);
            }
        } catch (Throwable t) {
            logD("onClassLoaded error on " + name + ": " + t.getMessage());
        } finally {
            sInHDUpload.set(false);
        }
    }

    private static void hookKevaPublishRepo(ClassLoader classLoader) {
        try {
            Class<?> kevaClass = XposedHelpers.findClassIfExists("com.bytedance.keva.Keva", classLoader);
            if (kevaClass != null) {
                hookKevaClass(kevaClass);
            }
        } catch (Throwable ignored) {}
    }

    public static void hookKevaClass(Class<?> kevaClass) {
        if (kevaClass == null || !sHookedClasses.add(kevaClass.getName() + "_hd")) return;

        try {

            XposedBridge.hookAllMethods(kevaClass, "getInt", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!MainHook.isHDUploadEnabled()) return;
                    if (param.args != null && param.args.length >= 1 && "USER_HD_VIDEO_SWITCH_SETTING".equals(param.args[0])) {
                        param.setResult(1);
                    }
                }
            });

            XposedBridge.hookAllMethods(kevaClass, "storeInt", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!MainHook.isHDUploadEnabled()) return;
                    if (param.args != null && param.args.length >= 2 && "USER_HD_VIDEO_SWITCH_SETTING".equals(param.args[0])) {
                        param.args[1] = 1;
                    }
                }
            });

            log("Hooked Keva for USER_HD_VIDEO_SWITCH_SETTING");
        } catch (Throwable t) {
            logD("hookKevaClass error: " + t.getMessage());
        }
    }

    private static void hookSettingsManager(ClassLoader classLoader) {
        try {
            Class<?> smClass = XposedHelpers.findClassIfExists("com.bytedance.ies.abmock.SettingsManager", classLoader);
            if (smClass != null) {
                hookSettingsManagerClass(smClass);
            }
        } catch (Throwable ignored) {}
    }

    public static void hookSettingsManagerClass(Class<?> smClass) {
        if (smClass == null || !sHookedClasses.add(smClass.getName() + "_hd")) return;

        try {

            XposedBridge.hookAllMethods(smClass, "LIZ", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!MainHook.isHDUploadEnabled()) return;
                    if (param.args == null || param.args.length < 1 || !(param.args[0] instanceof String)) return;
                    String key = (String) param.args[0];
                    if ("enable_high_quality_video".equals(key)
                            || "enable_default_open_hd_video_switch".equals(key)
                            || "high_quality_use_smart_compile".equals(key)
                            || "use_synthetic_hardcode".equals(key)
                            || "use_hardcode".equals(key)
                            || "studio_enable_upload_source_file_directly".equals(key)) {
                        param.setResult(true);
                    }
                }
            });

            XposedBridge.hookAllMethods(smClass, "LJ", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!MainHook.isHDUploadEnabled()) return;
                    if (param.args == null || param.args.length < 1 || !(param.args[0] instanceof String)) return;
                    String key = (String) param.args[0];
                    if ("high_quality_compile_video_size_index".equals(key)
                            || "compile_video_size_index".equals(key)
                            || "upload_video_size_index".equals(key)) {
                        param.setResult(HD_COMPILE_SIZE_INDEX_1080P);
                    } else if ("video_bitrate_category_index".equals(key)) {
                        param.setResult(10);
                    } else if ("video_quality".equals(key)) {
                        param.setResult(51);
                    }
                }
            });

            if (hasMethod(smClass, "getIntValue")) {
                XposedBridge.hookAllMethods(smClass, "getIntValue", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (!MainHook.isHDUploadEnabled()) return;
                        if (param.args == null || param.args.length < 1 || !(param.args[0] instanceof String)) return;
                        String key = (String) param.args[0];
                        if ("high_quality_compile_video_size_index".equals(key) || "compile_video_size_index".equals(key)) {
                            param.setResult(HD_COMPILE_SIZE_INDEX_1080P);
                        }
                    }
                });
            }

            log("Hooked SettingsManager for HD compile and upload parameters");
        } catch (Throwable t) {
            logD("hookSettingsManagerClass error: " + t.getMessage());
        }
    }

    public static void hookABExperimentManagerClass(Class<?> abClass) {
        if (abClass == null || !sHookedClasses.add(abClass.getName() + "_hd")) return;

        try {

            XposedBridge.hookAllMethods(abClass, "LIZJ", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!MainHook.isHDUploadEnabled()) return;
                    if (param.args == null) return;
                    for (Object arg : param.args) {
                        if (arg instanceof String) {
                            String key = (String) arg;
                            if ("studio_enable_upload_source_file_directly".equals(key)
                                    || "high_quality_upload".equals(key)
                                    || "upload_save_local".equals(key)) {
                                param.setResult(true);
                                break;
                            }
                        }
                    }
                }
            });

            XposedBridge.hookAllMethods(abClass, "LJIIJJI", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!MainHook.isHDUploadEnabled()) return;
                    if (param.args == null) return;
                    for (Object arg : param.args) {
                        if (arg instanceof String) {
                            String key = (String) arg;
                            if ("compile_video_size_index".equals(key) || "upload_video_size_index".equals(key)) {
                                param.setResult(HD_COMPILE_SIZE_INDEX_1080P);
                                break;
                            } else if ("video_bitrate_category_index".equals(key)) {
                                param.setResult(10);
                                break;
                            }
                        }
                    }
                }
            });

            XposedBridge.hookAllMethods(abClass, "LJIILIIL", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!MainHook.isHDUploadEnabled()) return;
                    if (param.args == null) return;
                    for (Object arg : param.args) {
                        if (arg instanceof String) {
                            String key = (String) arg;
                            if ("studio_upload_direct_long_video_threshold_mb".equals(key)) {
                                param.setResult(getDirectUploadThresholdMb());
                                break;
                            }
                        }
                    }
                }
            });

            log("Hooked AB experiment manager for source upload & size index");
        } catch (Throwable t) {
            logD("hookABExperimentManagerClass error: " + t.getMessage());
        }
    }

    private static void hookAVSettingsWrapper(ClassLoader classLoader) {
        try {
            Class<?> avClass = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.property.AVSettingsWrapper", classLoader);
            if (avClass != null) {
                hookAVSettingsWrapperClass(avClass);
            }
        } catch (Throwable ignored) {}
    }

    public static void hookAVSettingsWrapperClass(Class<?> avClass) {
        if (avClass == null || !sHookedClasses.add(avClass.getName() + "_hd")) return;

        try {

            XposedBridge.hookAllMethods(avClass, "getCompileVideoSize", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!MainHook.isHDUploadEnabled()) return;
                    int targetW = getTargetWidth();
                    int targetH = getTargetHeight();
                    int[] orig = (int[]) param.getResult();
                    if (orig != null && orig.length == 2) {
                        int w = orig[0];
                        int h = orig[1];
                        if (h >= w) {
                            param.setResult(new int[]{targetW, targetH});
                        } else {
                            param.setResult(new int[]{targetH, targetW});
                        }
                    } else {
                        param.setResult(new int[]{targetW, targetH});
                    }
                }
            });

            XposedBridge.hookAllMethods(avClass, "getRecordVideoSize", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!MainHook.isHDUploadEnabled()) return;
                    int targetW = getTargetWidth();
                    int targetH = getTargetHeight();
                    int[] orig = (int[]) param.getResult();
                    if (orig != null && orig.length == 2 && orig[1] < orig[0]) {
                        param.setResult(new int[]{targetH, targetW});
                    } else {
                        param.setResult(new int[]{targetW, targetH});
                    }
                }
            });

            XposedBridge.hookAllMethods(avClass, "getRecordBitrate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!MainHook.isHDUploadEnabled()) return;
                    Object res = param.getResult();
                    float minBitrate = MainHook.isUpload4KEnabled() ? 6.0f : 2.5f;
                    if (res instanceof Float) {
                        float val = (Float) res;
                        if (val < minBitrate) {
                            param.setResult(minBitrate);
                        }
                    }
                }
            });

            XposedBridge.hookAllMethods(avClass, "enableHardEncodeForSynthetic", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!MainHook.isHDUploadEnabled()) return;
                    param.setResult(true);
                }
            });

            log("Hooked AVSettingsWrapper for 1080p compile size and synthetic encoding");
        } catch (Throwable t) {
            logD("hookAVSettingsWrapperClass error: " + t.getMessage());
        }
    }

    private static void hookVEEditor(ClassLoader classLoader) {
        try {
            Class<?> editorClass = XposedHelpers.findClassIfExists("com.ss.android.vesdk.VEEditor", classLoader);
            if (editorClass != null) {
                hookVEEditorClass(editorClass);
            }
        } catch (Throwable ignored) {}
    }

    public static void hookVEEditorClass(Class<?> editorClass) {
        if (editorClass == null || !sHookedClasses.add(editorClass.getName() + "_hd")) return;

        try {

            Method[] methods = editorClass.getDeclaredMethods();
            for (Method method : methods) {
                Class<?>[] paramTypes = method.getParameterTypes();
                boolean hasEncodeSettings = false;
                for (Class<?> p : paramTypes) {
                    if (p != null && "com.ss.android.vesdk.VEVideoEncodeSettings".equals(p.getName())) {
                        hasEncodeSettings = true;
                        break;
                    }
                }

                if (hasEncodeSettings) {
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!MainHook.isHDUploadEnabled()) return;
                            if (param.args == null) return;
                            for (Object arg : param.args) {
                                if (arg != null && "com.ss.android.vesdk.VEVideoEncodeSettings".equals(arg.getClass().getName())) {
                                    upgradeVEVideoEncodeSettings(arg);
                                }
                            }
                        }
                    });
                }
            }

            log("Hooked VEEditor compilation methods for HD synthesis upgrade");
        } catch (Throwable t) {
            logD("hookVEEditorClass error: " + t.getMessage());
        }
    }

    private static void hookVEVideoEncodeSettings(ClassLoader classLoader) {
        try {
            Class<?> settingsClass = XposedHelpers.findClassIfExists("com.ss.android.vesdk.VEVideoEncodeSettings", classLoader);
            if (settingsClass != null) {
                hookVEVideoEncodeSettingsClass(settingsClass);
            }
        } catch (Throwable ignored) {}
    }

    public static void hookVEVideoEncodeSettingsClass(Class<?> settingsClass) {
        if (settingsClass == null || !sHookedClasses.add(settingsClass.getName() + "_hd")) return;

        try {

            XposedBridge.hookAllConstructors(settingsClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!MainHook.isHDUploadEnabled()) return;
                    upgradeVEVideoEncodeSettings(param.thisObject);
                }
            });

            if (hasMethod(settingsClass, "setVideoRes")) {
                XposedBridge.hookAllMethods(settingsClass, "setVideoRes", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (!MainHook.isHDUploadEnabled()) return;
                        int targetW = getTargetWidth();
                        if (param.args != null && param.args.length >= 2) {
                            int w = ((Number) param.args[0]).intValue();
                            int h = ((Number) param.args[1]).intValue();
                            if (w > 0 && h > 0) {
                                if (h >= w && w < targetW) {
                                    float aspect = (float) h / (float) w;
                                    param.args[0] = targetW;
                                    param.args[1] = ((int) (targetW * aspect) / 16) * 16;
                                } else if (w > h && h < targetW) {
                                    float aspect = (float) w / (float) h;
                                    param.args[1] = targetW;
                                    param.args[0] = ((int) (targetW * aspect) / 16) * 16;
                                }
                            }
                        }
                    }
                });
            }

            if (hasMethod(settingsClass, "setBps")) {
                XposedBridge.hookAllMethods(settingsClass, "setBps", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (!MainHook.isHDUploadEnabled()) return;
                        int targetBps = getTargetBps();
                        if (param.args != null && param.args.length >= 1) {
                            int bps = ((Number) param.args[0]).intValue();
                            if (bps > 0 && bps < targetBps) {
                                param.args[0] = targetBps;
                            }
                        }
                    }
                });
            }

            log("Hooked VEVideoEncodeSettings constructors and resolution setters");
        } catch (Throwable t) {
            logD("hookVEVideoEncodeSettingsClass error: " + t.getMessage());
        }
    }

    public static void upgradeVEVideoEncodeSettings(Object encodeSettings) {
        if (encodeSettings == null) return;
        try {
            Class<?> clazz = encodeSettings.getClass();
            int targetW = getTargetWidth();
            int targetH = getTargetHeight();
            int targetBps = getTargetBps();
            int maxBps = getMaxBps();
            int swCrf = getSwCrf();

            Field outputSizeField = findFieldSafely(clazz, "outputSize");
            if (outputSizeField != null) {
                Object outputSize = outputSizeField.get(encodeSettings);
                if (outputSize != null) {
                    Field wField = findFieldSafely(outputSize.getClass(), "width");
                    Field hField = findFieldSafely(outputSize.getClass(), "height");
                    if (wField != null && hField != null) {
                        int width = wField.getInt(outputSize);
                        int height = hField.getInt(outputSize);

                        if (width > 0 && height > 0) {
                            if (height >= width) {
                                if (width < targetW) {
                                    float aspect = (float) height / (float) width;
                                    int newWidth = targetW;
                                    int newHeight = ((int) (targetW * aspect) / 16) * 16;
                                    wField.setInt(outputSize, newWidth);
                                    hField.setInt(outputSize, newHeight);
                                    log("Upgraded portrait outputSize: " + width + "x" + height + " -> " + newWidth + "x" + newHeight);
                                }
                            } else {
                                if (height < targetW) {
                                    float aspect = (float) width / (float) height;
                                    int newHeight = targetW;
                                    int newWidth = ((int) (targetW * aspect) / 16) * 16;
                                    wField.setInt(outputSize, newWidth);
                                    hField.setInt(outputSize, newHeight);
                                    log("Upgraded landscape outputSize: " + width + "x" + height + " -> " + newWidth + "x" + newHeight);
                                }
                            }
                        } else {
                            wField.setInt(outputSize, targetW);
                            hField.setInt(outputSize, targetH);
                        }
                    }
                }
            }

            Field bpsField = findFieldSafely(clazz, "bps");
            if (bpsField != null) {
                int currentBps = bpsField.getInt(encodeSettings);
                if (currentBps < targetBps) {
                    bpsField.setInt(encodeSettings, targetBps);
                }
            }

            Field hwBpsField = findFieldSafely(clazz, "HwBps");
            if (hwBpsField != null) {
                int currentHwBps = hwBpsField.getInt(encodeSettings);
                if (currentHwBps < targetBps) {
                    hwBpsField.setInt(encodeSettings, targetBps);
                }
            }

            Field swMaxrateField = findFieldSafely(clazz, "swMaxrate");
            if (swMaxrateField != null) {
                long currentMax = swMaxrateField.getLong(encodeSettings);
                if (currentMax < maxBps) {
                    swMaxrateField.setLong(encodeSettings, (long) maxBps);
                }
            }

            Field swCRFField = findFieldSafely(clazz, "swCRF");
            if (swCRFField != null) {
                swCRFField.setInt(encodeSettings, swCrf);
            }

            Field fpsField = findFieldSafely(clazz, "fps");
            if (fpsField != null) {
                int fps = fpsField.getInt(encodeSettings);
                if (fps > 0 && fps < HD_TARGET_FPS) {
                    fpsField.setInt(encodeSettings, HD_TARGET_FPS);
                }
            }

            Field frameRateField = findFieldSafely(clazz, "frameRate");
            if (frameRateField != null) {
                int fr = frameRateField.getInt(encodeSettings);
                if (fr < HD_TARGET_FPS) {
                    frameRateField.setInt(encodeSettings, HD_TARGET_FPS);
                }
            }

            Field pubFpsField = findFieldSafely(clazz, "publishFps");
            if (pubFpsField != null) {
                int pf = pubFpsField.getInt(encodeSettings);
                if (pf > 0 && pf < HD_TARGET_FPS) {
                    pubFpsField.setInt(encodeSettings, HD_TARGET_FPS);
                }
            }

            Field encodeProfileField = findFieldSafely(clazz, "encodeProfile");
            if (encodeProfileField != null) {
                encodeProfileField.setInt(encodeSettings, HD_ENCODE_PROFILE_HIGH);
            }

            Field isSupportHWEncoderField = findFieldSafely(clazz, "isSupportHWEncoder");
            if (isSupportHWEncoderField != null) {
                isSupportHWEncoderField.setBoolean(encodeSettings, true);
            }

            Field enableRemuxVideoField = findFieldSafely(clazz, "enableRemuxVideo");
            if (enableRemuxVideoField != null) {
                enableRemuxVideoField.setBoolean(encodeSettings, true);
            }

            Field imageCompileQualityField = findFieldSafely(clazz, "image_compile_quality");
            if (imageCompileQualityField != null) {
                imageCompileQualityField.setFloat(encodeSettings, 100.0f);
            }

            Field enableCopyWithMetadataField = findFieldSafely(clazz, "enableCopyWithMetadata");
            if (enableCopyWithMetadataField != null) {
                enableCopyWithMetadataField.setBoolean(encodeSettings, true);
            }

            Field resolutionAlignField = findFieldSafely(clazz, "mResolutionAlign");
            if (resolutionAlignField != null) {
                resolutionAlignField.setInt(encodeSettings, 16);
            }

            log("Applied Douyin-quality HD synthesis settings to VEVideoEncodeSettings");
        } catch (Throwable t) {
            logD("upgradeVEVideoEncodeSettings error: " + t.getMessage());
        }
    }

    private static void hookPublishHDSettings(ClassLoader classLoader) {
        try {
            Class<?> w0Class = XposedHelpers.findClassIfExists("zni.w0", classLoader);
            if (w0Class != null) {
                hookPublishHDHelperClass(w0Class);
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> c1hrsClass = XposedHelpers.findClassIfExists("X.1HRS", classLoader);
            if (c1hrsClass != null) {
                XposedBridge.hookAllMethods(c1hrsClass, "LIZ", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (MainHook.isHDUploadEnabled()) {
                            param.setResult(true);
                        }
                    }
                });
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> c1hrtClass = XposedHelpers.findClassIfExists("X.1HRT", classLoader);
            if (c1hrtClass != null) {
                XposedBridge.hookAllMethods(c1hrtClass, "LIZ", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (MainHook.isHDUploadEnabled()) {
                            param.setResult(true);
                        }
                    }
                });
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> rvkFClass = XposedHelpers.findClassIfExists("rvk.f", classLoader);
            if (rvkFClass != null) {
                XposedBridge.hookAllMethods(rvkFClass, "LIZIZ", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (!MainHook.isHDUploadEnabled()) return;

                        if (param.args != null && param.args.length >= 1 && param.args[0] != null) {
                            try {
                                Object model = param.args[0];
                                Method getMeta = model.getClass().getMethod("getMetadataMap");
                                Object meta = getMeta.invoke(model);
                                if (meta instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> map = (Map<String, Object>) meta;
                                    map.put("upload_HD", Boolean.TRUE);
                                    map.put("upload_HD_button", 1);
                                }
                            } catch (Throwable ignored) {}
                        }
                    }
                });
            }
        } catch (Throwable ignored) {}
    }

    public static void hookPublishHDHelperClass(Class<?> helperClass) {
        if (helperClass == null || !sHookedClasses.add(helperClass.getName() + "_hd")) return;

        try {

            if (hasMethod(helperClass, "LJI")) {
                XposedBridge.hookAllMethods(helperClass, "LJI", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (MainHook.isHDUploadEnabled()) {
                            param.setResult(true);
                        }
                    }
                });
            }

            if (hasMethod(helperClass, "LIZJ")) {
                XposedBridge.hookAllMethods(helperClass, "LIZJ", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (MainHook.isHDUploadEnabled()) {
                            param.setResult(true);
                        }
                    }
                });
            }

            if (hasMethod(helperClass, "LIZLLL")) {
                XposedBridge.hookAllMethods(helperClass, "LIZLLL", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (MainHook.isHDUploadEnabled()) {
                            param.setResult("1");
                        }
                    }
                });
            }

            log("Hooked publish HD helper class: " + helperClass.getName());
        } catch (Throwable t) {
            logD("hookPublishHDHelperClass error: " + t.getMessage());
        }
    }

    private static boolean hasMethod(Class<?> clazz, String methodName) {
        if (clazz == null || methodName == null) return false;
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(methodName)) return true;
        }
        return false;
    }

    private static Field findFieldSafely(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                Field f = current.getDeclaredField(fieldName);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}

