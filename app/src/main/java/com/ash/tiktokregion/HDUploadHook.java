package com.ash.tiktokregion;

import android.app.Activity;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
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
    public static final int HD_UPLOAD_SIZE_INDEX_1080P = 4;

    public static final int UPLOAD_4K_WIDTH = 2160;
    public static final int UPLOAD_4K_HEIGHT = 3840;
    public static final int UPLOAD_4K_TARGET_BPS = 50_000_000;
    public static final int UPLOAD_4K_MAX_BPS = 75_000_000;
    public static final int UPLOAD_4K_COMPILE_SIZE_INDEX = 9;
    public static final int UPLOAD_4K_UPLOAD_SIZE_INDEX = 6;

    public static int getTargetWidth() {
        return MainHook.isUpload4KEnabled() ? UPLOAD_4K_WIDTH : HD_WIDTH;
    }

    public static int getTargetHeight() {
        return MainHook.isUpload4KEnabled() ? UPLOAD_4K_HEIGHT : HD_HEIGHT;
    }

    public static int getTargetMax() {
        return Math.max(getTargetWidth(), getTargetHeight());
    }

    public static int getTargetBps() {
        return MainHook.isUpload4KEnabled() ? UPLOAD_4K_TARGET_BPS : HD_TARGET_BPS;
    }

    public static int getMaxBps() {
        return MainHook.isUpload4KEnabled() ? UPLOAD_4K_MAX_BPS : HD_MAX_BPS;
    }

    public static int getCompileSizeIndex() {
        return MainHook.isUpload4KEnabled() ? UPLOAD_4K_COMPILE_SIZE_INDEX : HD_COMPILE_SIZE_INDEX_1080P;
    }

    public static int getUploadSizeIndex() {
        return MainHook.isUpload4KEnabled() ? UPLOAD_4K_UPLOAD_SIZE_INDEX : HD_UPLOAD_SIZE_INDEX_1080P;
    }

    public static String getTargetResolutionString() {
        return MainHook.isUpload4KEnabled() ? "2160x3840" : "1080x1920";
    }

    public static int getSwCrf() {
        return HD_SW_CRF;
    }

    public static long getDirectUploadThresholdMb() {
        return 500L;
    }

    private static final boolean DEBUG = true;

    public static void log(String msg) {
        Log.i(TAG, msg);
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
            hookCategoryArrays(classLoader);
        } catch (Throwable t) {
            logD("hookCategoryArrays error: " + t.getMessage());
        }

        try {
            hookABExperimentManager(classLoader);
        } catch (Throwable t) {
            logD("hookABExperimentManager error: " + t.getMessage());
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
            hookVEEncodeBuilder(classLoader);
        } catch (Throwable t) {
            logD("hookVEEncodeBuilder error: " + t.getMessage());
        }

        try {
            hookCompileConfig(classLoader);
        } catch (Throwable t) {
            logD("hookCompileConfig error: " + t.getMessage());
        }

        try {
            hookPublishHDSettings(classLoader);
        } catch (Throwable t) {
            logD("hookPublishHDSettings error: " + t.getMessage());
        }

        try {
            hookByteBenchStrategy(classLoader);
        } catch (Throwable t) {
            logD("hookByteBenchStrategy error: " + t.getMessage());
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
            } else if ("X.0Hgg".equals(name) || "X.C14950Hgg".equals(name)) {
                hookHggClass(clazz);
            } else if ("X.0HgR".equals(name) || "X.C14800HgR".equals(name)) {
                hookHgRClass(clazz);
            } else if ("X.0Hgf".equals(name) || "X.C14940Hgf".equals(name)) {
                hookHgfClass(clazz);
            } else if ("X.0Hg2".equals(name) || "X.C14550Hg2".equals(name)) {
                hookHg2Class(clazz);
            } else if ("X.032E".equals(name) || "X.C032E".equals(name)) {
                hookABExperimentManagerClass(clazz);
            } else if ("X.1Lss".equals(name) || "X.C27041Lss".equals(name)) {
                hookVEEncodeBuilderClass(clazz);
            } else if ("X.1LpV".equals(name) || "X.AbstractC24951LpV".equals(name)) {
                hookLpVClass(clazz);
            } else if ("com.ss.android.ugc.aweme.creative.compileconfig.VEVideoEncodeConfigParams".equals(name)) {
                hookCompileParamsClass(clazz);
            } else if ("u4j.w0".equals(name) || "zni.w0".equals(name)) {
                hookPublishHDHelperClass(clazz);
            } else if ("hdl.f".equals(name) || "rvk.f".equals(name)) {
                hookHdlFClass(clazz);
            } else if ("X.1Lp2".equals(name) || "X.C24661Lp2".equals(name)) {
                hook1Lp2Class(clazz);
            } else if ("com.ss.android.ugc.aweme.property.bytebench.ResolutionByteBenchStrategy$$Imp".equals(name)) {
                hookResolutionByteBenchStrategy(clazz);
            } else if ("X.0HgC".equals(name) || "X.C14650HgC".equals(name) || name.endsWith("0HgC")) {
                hookByteBenchResolverClass(clazz);
            }
        } catch (Throwable t) {
            logD("onClassLoaded error on " + name + ": " + t.getMessage());
        } finally {
            sInHDUpload.set(false);
        }
    }

    private static void hookByteBenchStrategy(ClassLoader classLoader) {
        try {
            Class<?> byteBenchStrategy = XposedHelpers.findClassIfExists(
                    "com.ss.android.ugc.aweme.property.bytebench.ResolutionByteBenchStrategy$$Imp", classLoader);
            if (byteBenchStrategy != null) {
                hookResolutionByteBenchStrategy(byteBenchStrategy);
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> byteBenchResolver = XposedHelpers.findClassIfExists("X.0HgC", classLoader);
            if (byteBenchResolver == null) {
                byteBenchResolver = XposedHelpers.findClassIfExists("X.C14650HgC", classLoader);
            }
            if (byteBenchResolver != null) {
                hookByteBenchResolverClass(byteBenchResolver);
            }
        } catch (Throwable ignored) {}
    }

    public static void hookResolutionByteBenchStrategy(Class<?> clazz) {
        if (clazz == null || !sHookedClasses.add(clazz.getName() + "_hd")) return;

        try {
            XposedHelpers.findAndHookMethod(clazz, "uploadVideoSizeIndex", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (MainHook.isHDUploadEnabled()) {
                        param.setResult(getUploadSizeIndex());
                    }
                }
            });
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(clazz, "videoSizeIndex", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (MainHook.isHDUploadEnabled()) {
                        param.setResult(getCompileSizeIndex());
                    }
                }
            });
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(clazz, "compileVideoSizeIndex", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (MainHook.isHDUploadEnabled()) {
                        param.setResult(getCompileSizeIndex());
                    }
                }
            });
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(clazz, "hdCompileVideoSizeIndex", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (MainHook.isHDUploadEnabled()) {
                        param.setResult(getCompileSizeIndex());
                    }
                }
            });
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(clazz, "staticVideoSizeIndex", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (MainHook.isHDUploadEnabled()) {
                        param.setResult(getCompileSizeIndex());
                    }
                }
            });
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(clazz, "enablePreviewResolutionDowngrade", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (MainHook.isHDUploadEnabled()) {
                        param.setResult(false);
                    }
                }
            });
        } catch (Throwable ignored) {}

        log("Hooked ResolutionByteBenchStrategy$$Imp resolution methods");
    }

    public static void hookByteBenchResolverClass(Class<?> clazz) {
        if (clazz == null || !sHookedClasses.add(clazz.getName() + "_hd")) return;

        try {
            XposedHelpers.findAndHookMethod(clazz, "LIZ", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (MainHook.isHDUploadEnabled()) {
                        param.setResult(getCompileSizeIndex());
                    }
                }
            });
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(clazz, "LIZIZ", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (MainHook.isHDUploadEnabled()) {
                        param.setResult(getCompileSizeIndex());
                    }
                }
            });
        } catch (Throwable ignored) {}

        log("Hooked ByteBench resolver (0HgC) methods");
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
            XC_MethodHook getRepoHook = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!MainHook.isHDUploadEnabled()) return;
                    if (isToolsPublishRepoArg(param.args)) {
                        Object result = param.getResult();
                        if (result != null) {
                            try {
                                Method storeInt = result.getClass().getMethod("storeInt", String.class, int.class);
                                storeInt.invoke(result, "USER_HD_VIDEO_SWITCH_SETTING", 1);
                                log("Injected USER_HD_VIDEO_SWITCH_SETTING=1 into TOOLS_PUBLISH_REPO_NAME repo instance");
                            } catch (Throwable t) {
                                logD("Failed storeInt on repo: " + t.getMessage());
                            }
                            hookKevaInstance(result);
                            tryHookAllPublishClasses(result.getClass().getClassLoader());
                            tryHookAllPublishClasses(Thread.currentThread().getContextClassLoader());
                        }
                    }
                }
            };

            for (Method m : kevaClass.getDeclaredMethods()) {
                String mn = m.getName();
                if ("getRepo".equals(mn) || "getRepoSync".equals(mn) || "getRepoFromSp".equals(mn) || "getRepoFromSpSync".equals(mn) || "getRepoWithPath".equals(mn)) {
                    XposedBridge.hookMethod(m, getRepoHook);
                }
            }

            Class<?> kevaImpl = XposedHelpers.findClassIfExists("com.bytedance.keva.KevaImpl", kevaClass.getClassLoader());
            if (kevaImpl != null && sHookedClasses.add(kevaImpl.getName() + "_hd")) {
                for (Method m : kevaImpl.getDeclaredMethods()) {
                    String mn = m.getName();
                    if ("getRepo".equals(mn) || "getRepoSync".equals(mn) || "getRepoFromSpImpl".equals(mn) || "getRepoWithPath".equals(mn)) {
                        XposedBridge.hookMethod(m, getRepoHook);
                    }
                }
            }

            log("Hooked Keva getRepo methods for TOOLS_PUBLISH_REPO_NAME");
        } catch (Throwable t) {
            logD("hookKevaClass error: " + t.getMessage());
        }
    }

    private static boolean isToolsPublishRepoArg(Object[] args) {
        if (args == null) return false;
        for (Object arg : args) {
            if ("TOOLS_PUBLISH_REPO_NAME".equals(arg)) {
                return true;
            }
        }
        return false;
    }

    public static void hookKevaInstance(Object repo) {
        if (repo == null) return;
        Class<?> repoClass = repo.getClass();
        if (!sHookedClasses.add(repoClass.getName() + "_instance_hd")) return;

        try {
            for (Method m : repoClass.getMethods()) {
                if (java.lang.reflect.Modifier.isAbstract(m.getModifiers())) continue;
                String mn = m.getName();
                if ("getInt".equals(mn)) {
                    XposedBridge.hookMethod(m, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!MainHook.isHDUploadEnabled()) return;
                            if (param.args != null && param.args.length >= 1 && "USER_HD_VIDEO_SWITCH_SETTING".equals(param.args[0])) {
                                param.setResult(1);
                            }
                        }
                    });
                } else if ("storeInt".equals(mn)) {
                    XposedBridge.hookMethod(m, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!MainHook.isHDUploadEnabled()) return;
                            if (param.args != null && param.args.length >= 2 && "USER_HD_VIDEO_SWITCH_SETTING".equals(param.args[0])) {
                                param.args[1] = 1;
                            }
                        }
                    });
                } else if ("getBoolean".equals(mn)) {
                    XposedBridge.hookMethod(m, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!MainHook.isHDUploadEnabled()) return;
                            if (param.args != null && param.args.length >= 1 && "upload_HD".equals(param.args[0])) {
                                param.setResult(true);
                            }
                        }
                    });
                }
            }
            log("Hooked concrete Keva repo instance: " + repoClass.getName());
        } catch (Throwable t) {
            logD("hookKevaInstance error: " + t.getMessage());
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
                            || "studio_enable_upload_source_file_directly".equals(key)
                            || "ame_enable_upload_direct".equals(key)
                            || "is_upload_direct_enter".equals(key)
                            || "key_upload_direct_enter".equals(key)
                            || "empty_uid_upload_directly".equals(key)
                            || "studio_enable_continue_compile_on_upload_directly".equals(key)
                            || "ve_enable_pic_upload_directly".equals(key)) {
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
                    if ("upload_video_size_index".equals(key)) {
                        param.setResult(getUploadSizeIndex());
                    } else if ("high_quality_compile_video_size_index".equals(key)
                            || "compile_video_size_index".equals(key)
                            || "video_size_index".equals(key)) {
                        param.setResult(getCompileSizeIndex());
                    } else if ("video_bitrate_category_index".equals(key)) {
                        param.setResult(10);
                    } else if ("video_quality".equals(key)) {
                        param.setResult(51);
                    }
                }
            });

            XposedBridge.hookAllMethods(smClass, "LJIIIIZZ", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!MainHook.isHDUploadEnabled()) return;
                    if (param.args == null || param.args.length < 1 || !(param.args[0] instanceof String)) return;
                    String key = (String) param.args[0];
                    if ("upload_video_size_category".equals(key)) {
                        param.setResult(new String[]{
                                "576x1024", "720x1280", "720x1280", "720x1280", "1080x1920", "1440x2560", "2160x3840"
                        });
                    } else if ("video_size_category".equals(key)) {
                        param.setResult(new String[]{
                                "576x1024", "720x1280", "720x1280", "720x1280", "480x848", "544x960", "944x1680", "1080x1920", "1440x2560", "2160x3840"
                        });
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
                        if ("upload_video_size_index".equals(key)) {
                            param.setResult(getUploadSizeIndex());
                        } else if ("high_quality_compile_video_size_index".equals(key)
                                || "compile_video_size_index".equals(key)
                                || "video_size_index".equals(key)) {
                            param.setResult(getCompileSizeIndex());
                        }
                    }
                });
            }

            if (hasMethod(smClass, "LJI")) {
                XposedBridge.hookAllMethods(smClass, "LJI", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (!MainHook.isHDUploadEnabled()) return;
                        if (param.args == null || param.args.length < 1 || !(param.args[0] instanceof String)) return;
                        String key = (String) param.args[0];
                        if ("video_size".equals(key)) {
                            param.setResult(getTargetResolutionString());
                        }
                    }
                });
            }

            if (hasMethod(smClass, "getStringValue")) {
                XposedBridge.hookAllMethods(smClass, "getStringValue", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (!MainHook.isHDUploadEnabled()) return;
                        if (param.args == null || param.args.length < 1 || !(param.args[0] instanceof String)) return;
                        String key = (String) param.args[0];
                        if ("video_size".equals(key)) {
                            param.setResult(getTargetResolutionString());
                        }
                    }
                });
            }

            log("Hooked SettingsManager for HD compile and upload parameters");
        } catch (Throwable t) {
            logD("hookSettingsManagerClass error: " + t.getMessage());
        }
    }

    private static void hookCategoryArrays(ClassLoader classLoader) {
        try {
            Class<?> hgg = XposedHelpers.findClassIfExists("X.0Hgg", classLoader);
            if (hgg == null) hgg = XposedHelpers.findClassIfExists("X.C14950Hgg", classLoader);
            if (hgg != null) hookHggClass(hgg);
        } catch (Throwable ignored) {}

        try {
            Class<?> hgR = XposedHelpers.findClassIfExists("X.0HgR", classLoader);
            if (hgR == null) hgR = XposedHelpers.findClassIfExists("X.C14800HgR", classLoader);
            if (hgR != null) hookHgRClass(hgR);
        } catch (Throwable ignored) {}

        try {
            Class<?> hgf = XposedHelpers.findClassIfExists("X.0Hgf", classLoader);
            if (hgf == null) hgf = XposedHelpers.findClassIfExists("X.C14940Hgf", classLoader);
            if (hgf != null) hookHgfClass(hgf);
        } catch (Throwable ignored) {}

        try {
            Class<?> hg2 = XposedHelpers.findClassIfExists("X.0Hg2", classLoader);
            if (hg2 == null) hg2 = XposedHelpers.findClassIfExists("X.C14550Hg2", classLoader);
            if (hg2 != null) hookHg2Class(hg2);
        } catch (Throwable ignored) {}
    }

    public static void hookHggClass(Class<?> clazz) {
        if (clazz == null || !sHookedClasses.add(clazz.getName() + "_hd")) return;
        try {
            String[] extended = new String[]{
                    "576x1024", "720x1280", "720x1280", "720x1280", "1080x1920", "1440x2560", "2160x3840"
            };
            Field lField = findFieldSafely(clazz, "LIZ");
            if (lField != null) {
                lField.set(null, extended);
            }
            XposedBridge.hookAllMethods(clazz, "LIZ", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (MainHook.isHDUploadEnabled()) {
                        param.setResult(Arrays.asList(extended));
                    }
                }
            });
            log("Hooked 0Hgg upload_video_size_category array successfully");
        } catch (Throwable t) {
            logD("hookHggClass error: " + t.getMessage());
        }
    }

    public static void hookHgRClass(Class<?> clazz) {
        if (clazz == null || !sHookedClasses.add(clazz.getName() + "_hd")) return;
        try {
            String[] extended = new String[]{
                    "576x1024", "720x1280", "720x1280", "720x1280", "480x848", "544x960", "944x1680", "1080x1920", "1440x2560", "2160x3840"
            };
            Field lField = findFieldSafely(clazz, "LIZ");
            if (lField != null) {
                lField.set(null, extended);
            }
            XposedBridge.hookAllMethods(clazz, "LIZ", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (MainHook.isHDUploadEnabled()) {
                        param.setResult(Arrays.asList(extended));
                    }
                }
            });
            log("Hooked 0HgR video_size_category array successfully");
        } catch (Throwable t) {
            logD("hookHgRClass error: " + t.getMessage());
        }
    }

    public static void hookHgfClass(Class<?> clazz) {
        if (clazz == null || !sHookedClasses.add(clazz.getName() + "_hd")) return;
        try {
            XposedBridge.hookAllMethods(clazz, "LIZ", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (MainHook.isHDUploadEnabled()) {
                        param.setResult(getUploadSizeIndex());
                    }
                }
            });
            log("Hooked 0Hgf upload_video_size_index successfully");
        } catch (Throwable t) {
            logD("hookHgfClass error: " + t.getMessage());
        }
    }

    public static void hookHg2Class(Class<?> clazz) {
        if (clazz == null || !sHookedClasses.add(clazz.getName() + "_hd")) return;
        try {
            XposedBridge.hookAllMethods(clazz, "LIZ", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (MainHook.isHDUploadEnabled()) {
                        param.setResult(getTargetResolutionString());
                    }
                }
            });
            log("Hooked 0Hg2 video_size successfully");
        } catch (Throwable t) {
            logD("hookHg2Class error: " + t.getMessage());
        }
    }

    private static void hookABExperimentManager(ClassLoader classLoader) {
        try {
            Class<?> abClass = XposedHelpers.findClassIfExists("X.032E", classLoader);
            if (abClass == null) abClass = XposedHelpers.findClassIfExists("X.C032E", classLoader);
            if (abClass != null) {
                hookABExperimentManagerClass(abClass);
            }
        } catch (Throwable ignored) {}
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
                                    || "upload_save_local".equals(key)
                                    || "ame_enable_upload_direct".equals(key)
                                    || "is_upload_direct_enter".equals(key)
                                    || "key_upload_direct_enter".equals(key)
                                    || "empty_uid_upload_directly".equals(key)
                                    || "studio_enable_continue_compile_on_upload_directly".equals(key)
                                    || "ve_enable_pic_upload_directly".equals(key)
                                    || "enable_high_quality_video".equals(key)) {
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
                            if ("upload_video_size_index".equals(key)) {
                                param.setResult(getUploadSizeIndex());
                                break;
                            } else if ("compile_video_size_index".equals(key)
                                    || "high_quality_compile_video_size_index".equals(key)
                                    || "video_size_index".equals(key)) {
                                param.setResult(getCompileSizeIndex());
                                break;
                            } else if ("video_bitrate_category_index".equals(key)) {
                                param.setResult(10);
                                break;
                            } else if ("video_quality".equals(key)) {
                                param.setResult(51);
                                break;
                            }
                        }
                    }
                }
            });

            log("Hooked AB experiment manager (032E) for source upload & size index");
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
            XposedBridge.hookAllMethods(avClass, "getImportVideoSize", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!MainHook.isHDUploadEnabled()) return;
                    int maxDim = getTargetMax();
                    param.setResult(new int[]{maxDim, maxDim});
                }
            });

            XposedBridge.hookAllMethods(avClass, "getImportVideoResolution", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!MainHook.isHDUploadEnabled()) return;
                    int maxDim = getTargetMax();
                    param.setResult(maxDim + "*" + maxDim);
                }
            });

            XposedBridge.hookAllMethods(avClass, "getCompileVideoSize", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!MainHook.isHDUploadEnabled()) return;
                    int targetW = getTargetWidth();
                    int targetH = getTargetHeight();
                    int[] orig = (int[]) param.getResult();
                    if (orig != null && orig.length == 2 && orig[0] > orig[1]) {
                        param.setResult(new int[]{targetH, targetW});
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
                    if (orig != null && orig.length == 2 && orig[0] > orig[1]) {
                        param.setResult(new int[]{targetH, targetW});
                    } else {
                        param.setResult(new int[]{targetW, targetH});
                    }
                }
            });

            XposedBridge.hookAllMethods(avClass, "getNowEncodeSize", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!MainHook.isHDUploadEnabled()) return;
                    int targetW = getTargetWidth();
                    int targetH = getTargetHeight();
                    int[] orig = (int[]) param.getResult();
                    if (orig != null && orig.length == 2 && orig[0] > orig[1]) {
                        param.setResult(new int[]{targetH, targetW});
                    } else {
                        param.setResult(new int[]{targetW, targetH});
                    }
                }
            });

            XposedBridge.hookAllMethods(avClass, "getNowShotScreenEncodeSize", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!MainHook.isHDUploadEnabled()) return;
                    int targetW = getTargetWidth();
                    int targetH = getTargetHeight();
                    int[] orig = (int[]) param.getResult();
                    if (orig != null && orig.length == 2 && orig[0] > orig[1]) {
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
                    float minBitrate = 2.5f;
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

            XposedBridge.hookAllMethods(avClass, "enableHardEncodeForRecord", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!MainHook.isHDUploadEnabled()) return;
                    param.setResult(true);
                }
            });

            XposedBridge.hookAllMethods(avClass, "enableHardEncodeForWaterMark", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!MainHook.isHDUploadEnabled()) return;
                    param.setResult(true);
                }
            });

            log("Hooked AVSettingsWrapper for 4K/HD compile, import, and synthetic encoding");
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

    private static void hookVEEncodeBuilder(ClassLoader classLoader) {
        try {
            Class<?> builderClass = XposedHelpers.findClassIfExists("X.1Lss", classLoader);
            if (builderClass == null) builderClass = XposedHelpers.findClassIfExists("X.C27041Lss", classLoader);
            if (builderClass != null) {
                hookVEEncodeBuilderClass(builderClass);
            }
        } catch (Throwable ignored) {}
    }

    public static void hookVEEncodeBuilderClass(Class<?> builderClass) {
        if (builderClass == null || !sHookedClasses.add(builderClass.getName() + "_hd")) return;

        try {
            XposedBridge.hookAllMethods(builderClass, "LIZ", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!MainHook.isHDUploadEnabled()) return;
                    upgradeVEVideoEncodeSettings(param.getResult());
                }
            });

            if (hasMethod(builderClass, "LJIIJJI")) {
                XposedBridge.hookAllMethods(builderClass, "LJIIJJI", new XC_MethodHook() {
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

            log("Hooked 1Lss (VEVideoEncodeSettings.Builder) build and resolution methods");
        } catch (Throwable t) {
            logD("hookVEEncodeBuilderClass error: " + t.getMessage());
        }
    }

    private static void hookCompileConfig(ClassLoader classLoader) {
        try {
            Class<?> lpVClass = XposedHelpers.findClassIfExists("X.1LpV", classLoader);
            if (lpVClass == null) lpVClass = XposedHelpers.findClassIfExists("X.AbstractC24951LpV", classLoader);
            if (lpVClass != null) {
                hookLpVClass(lpVClass);
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> paramsClass = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.creative.compileconfig.VEVideoEncodeConfigParams", classLoader);
            if (paramsClass != null) {
                hookCompileParamsClass(paramsClass);
            }
        } catch (Throwable ignored) {}
    }

    public static void hookLpVClass(Class<?> clazz) {
        if (clazz == null || !sHookedClasses.add(clazz.getName() + "_hd")) return;

        try {
            XposedBridge.hookAllMethods(clazz, "LIZ", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!MainHook.isHDUploadEnabled()) return;
                    upgradeVEVideoEncodeSettings(param.getResult());
                }
            });
            log("Hooked 1LpV (compile orchestrator) build method");
        } catch (Throwable t) {
            logD("hookLpVClass error: " + t.getMessage());
        }
    }

    public static void hookCompileParamsClass(Class<?> clazz) {
        if (clazz == null || !sHookedClasses.add(clazz.getName() + "_hd")) return;

        try {
            XposedBridge.hookAllMethods(clazz, "getOutputSize", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!MainHook.isHDUploadEnabled()) return;
                    Object res = param.getResult();
                    if (res != null) {
                        upgradeCompileConfigResolution(res);
                    }
                }
            });
            log("Hooked VEVideoEncodeConfigParams getOutputSize");
        } catch (Throwable t) {
            logD("hookCompileParamsClass error: " + t.getMessage());
        }
    }

    private static void upgradeCompileConfigResolution(Object res) {
        try {
            Class<?> rClass = res.getClass();
            Field wField = findFieldSafely(rClass, "width");
            Field hField = findFieldSafely(rClass, "height");
            if (wField != null && hField != null) {
                int w = wField.getInt(res);
                int h = hField.getInt(res);
                int targetW = getTargetWidth();
                int targetH = getTargetHeight();
                if (w > h) {
                    wField.setInt(res, targetH);
                    hField.setInt(res, targetW);
                } else {
                    wField.setInt(res, targetW);
                    hField.setInt(res, targetH);
                }
                log("Upgraded CompileConfigResolution: " + w + "x" + h + " -> " + wField.getInt(res) + "x" + hField.getInt(res));
            }
        } catch (Throwable t) {
            logD("upgradeCompileConfigResolution error: " + t.getMessage());
        }
    }

    public static void upgradeVEVideoEncodeSettings(Object encodeSettings) {
        if (encodeSettings == null) return;
        try {
            Class<?> clazz = encodeSettings.getClass();
            if (!"com.ss.android.vesdk.VEVideoEncodeSettings".equals(clazz.getName())) return;

            int targetW = getTargetWidth();
            int targetH = getTargetHeight();
            int targetBps = getTargetBps();
            int maxBps = getMaxBps();
            int swCrf = getSwCrf();

            Field extJsonField = findFieldSafely(clazz, "externalSettingsJsonStr");
            if (extJsonField != null) {
                extJsonField.set(encodeSettings, null);
            }

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
                            if (width > height) {
                                float aspect = (float) width / (float) height;
                                int newHeight = targetW;
                                int newWidth = ((int) (newHeight * aspect) / 16) * 16;
                                wField.setInt(outputSize, newWidth);
                                hField.setInt(outputSize, newHeight);
                                log("Upgraded landscape outputSize: " + width + "x" + height + " -> " + newWidth + "x" + newHeight);
                            } else {
                                float aspect = (float) height / (float) width;
                                int newWidth = targetW;
                                int newHeight = ((int) (newWidth * aspect) / 16) * 16;
                                wField.setInt(outputSize, newWidth);
                                hField.setInt(outputSize, newHeight);
                                log("Upgraded portrait outputSize: " + width + "x" + height + " -> " + newWidth + "x" + newHeight);
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

            Field enableUploadDirectlyField = findFieldSafely(clazz, "enableUploadDirectly");
            if (enableUploadDirectlyField != null) {
                enableUploadDirectlyField.setBoolean(encodeSettings, true);
            }

            Field enableByteVCRemuxVideoField = findFieldSafely(clazz, "enableByteVCRemuxVideo");
            if (enableByteVCRemuxVideoField != null) {
                enableByteVCRemuxVideoField.setBoolean(encodeSettings, true);
            }

            Field enableVideoAndAudioRemuxField = findFieldSafely(clazz, "enableVideoAndAudioRemux");
            if (enableVideoAndAudioRemuxField != null) {
                enableVideoAndAudioRemuxField.setBoolean(encodeSettings, true);
            }

            Field mOptRemuxWithCopyField = findFieldSafely(clazz, "mOptRemuxWithCopy");
            if (mOptRemuxWithCopyField != null) {
                mOptRemuxWithCopyField.setBoolean(encodeSettings, true);
            }

            Field enableRemuxVideoResField = findFieldSafely(clazz, "enableRemuxVideoRes");
            if (enableRemuxVideoResField != null) {
                enableRemuxVideoResField.setInt(encodeSettings, -1);
            }

            Field m2kBitrateRatioField = findFieldSafely(clazz, "m2kBitrateRatio");
            if (m2kBitrateRatioField != null) {
                m2kBitrateRatioField.setDouble(encodeSettings, 1.0d);
            }

            Field m4kBitrateRatioField = findFieldSafely(clazz, "m4kBitrateRatio");
            if (m4kBitrateRatioField != null) {
                m4kBitrateRatioField.setDouble(encodeSettings, 1.0d);
            }

            Field resolutionAlignField = findFieldSafely(clazz, "mResolutionAlign");
            if (resolutionAlignField != null) {
                resolutionAlignField.setInt(encodeSettings, 16);
            }

            log("Applied 4K/HD synthesis settings to VEVideoEncodeSettings");
        } catch (Throwable t) {
            logD("upgradeVEVideoEncodeSettings error: " + t.getMessage());
        }
    }

    private static void hookPublishHDSettings(ClassLoader classLoader) {
        tryHookAllPublishClasses(classLoader);
    }

    public static void tryHookAllPublishClasses(ClassLoader classLoader) {
        if (classLoader == null || !MainHook.isHDUploadEnabled()) return;

        try {
            Class<?> w0Class = XposedHelpers.findClassIfExists("u4j.w0", classLoader);
            if (w0Class == null) {
                w0Class = XposedHelpers.findClassIfExists("zni.w0", classLoader);
            }
            if (w0Class != null) {
                hookPublishHDHelperClass(w0Class);
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> hdlClass = XposedHelpers.findClassIfExists("hdl.f", classLoader);
            if (hdlClass == null) {
                hdlClass = XposedHelpers.findClassIfExists("rvk.f", classLoader);
            }
            if (hdlClass != null) {
                hookHdlFClass(hdlClass);
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> lp2Class = XposedHelpers.findClassIfExists("X.1Lp2", classLoader);
            if (lp2Class == null) {
                lp2Class = XposedHelpers.findClassIfExists("X.C24661Lp2", classLoader);
            }
            if (lp2Class != null) {
                hook1Lp2Class(lp2Class);
            }
        } catch (Throwable ignored) {}
    }

    public static void onActivityLifecycle(Activity activity) {
        if (activity == null || !MainHook.isHDUploadEnabled()) return;
        ClassLoader cl = activity.getClassLoader();
        if (cl != null) {
            tryHookAllPublishClasses(cl);
            try {
                Class<?> kevaClass = XposedHelpers.findClassIfExists("com.bytedance.keva.Keva", cl);
                if (kevaClass != null) {
                    Method getRepo = kevaClass.getMethod("getRepo", String.class);
                    Object repo = getRepo.invoke(null, "TOOLS_PUBLISH_REPO_NAME");
                    if (repo != null) {
                        Method storeInt = repo.getClass().getMethod("storeInt", String.class, int.class);
                        storeInt.invoke(repo, "USER_HD_VIDEO_SWITCH_SETTING", 1);
                        hookKevaInstance(repo);
                    }
                }
            } catch (Throwable ignored) {}
        }
    }

    public static void hookHdlFClass(Class<?> hdlClass) {
        if (hdlClass == null || !sHookedClasses.add(hdlClass.getName() + "_hd")) return;
        try {
            XposedBridge.hookAllMethods(hdlClass, "LIZIZ", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!MainHook.isHDUploadEnabled()) return;
                    if (param.args != null && param.args.length > 0 && param.args[0] != null) {
                        try {
                            Object model = param.args[0];
                            Method getMeta = model.getClass().getMethod("getMetadataMap");
                            Object meta = getMeta.invoke(model);
                            if (meta instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> map = (Map<String, Object>) meta;
                                map.put("upload_HD", Boolean.TRUE);
                                map.put("upload_HD_button", 1);
                                map.put("high_quality_upload", 1);
                            }
                        } catch (Throwable ignored) {}
                    }
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!MainHook.isHDUploadEnabled()) return;
                    Object res = param.getResult();
                    if (res instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) res;
                        map.put("upload_HD", Boolean.TRUE);
                        map.put("upload_HD_button", 1);
                        map.put("high_quality_upload", 1);
                        map.put("is_hd_video", 1);
                        map.put("is_high_quality_upload", 1);
                    }
                }
            });

            XposedBridge.hookAllMethods(hdlClass, "LIZ", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!MainHook.isHDUploadEnabled()) return;
                    Object res = param.getResult();
                    if (res instanceof String) {
                        String json = (String) res;
                        if (json.contains("\"upload_HD\":false")) {
                            json = json.replace("\"upload_HD\":false", "\"upload_HD\":true");
                        }
                        if (json.contains("\"upload_HD_button\":0")) {
                            json = json.replace("\"upload_HD_button\":0", "\"upload_HD_button\":1");
                        }
                        param.setResult(json);
                    }
                }
            });
            log("Hooked hdl.f for upload_HD and high_quality_upload metadata injection");
        } catch (Throwable t) {
            logD("hookHdlFClass error: " + t.getMessage());
        }
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

    public static void hook1Lp2Class(Class<?> lp2Class) {
        if (lp2Class == null || !sHookedClasses.add(lp2Class.getName() + "_hd")) return;

        try {
            if (hasMethod(lp2Class, "LJ")) {
                XposedBridge.hookAllMethods(lp2Class, "LJ", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (MainHook.isHDUploadEnabled()) {
                            param.setResult(true);
                        }
                    }
                });
            }

            if (hasMethod(lp2Class, "LJFF")) {
                XposedBridge.hookAllMethods(lp2Class, "LJFF", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (MainHook.isHDUploadEnabled()) {
                            param.setResult(true);
                        }
                    }
                });
            }

            log("Hooked 1Lp2 helper class: " + lp2Class.getName());
        } catch (Throwable t) {
            logD("hook1Lp2Class error: " + t.getMessage());
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
