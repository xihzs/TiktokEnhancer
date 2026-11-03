package com.ash.tiktokregion.hooks.network;

import android.util.Log;

import com.ash.tiktokregion.core.BaseHook;
import com.ash.tiktokregion.core.DexKitManager;
import com.ash.tiktokregion.core.ModuleConfig;

import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.ClassDataList;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Hooks TikTok's core TTNet / Cronet network engine (backed by libsscronet.so).
 * Injects target country region codes and MCC/MNC into request metadata and API parameters.
 */
public class TTNetHook implements BaseHook {

    private static final String TAG = "TikTokTTNet";

    @Override
    public void hook(ClassLoader classLoader) {
        hookStandardCronetAdapters(classLoader);
        findAndHookObfuscatedProviders(classLoader);
    }

    private void hookStandardCronetAdapters(ClassLoader classLoader) {
        final String cronetAdapter = "com.bytedance.ttnet.cronet.AbsCronetDependAdapter";

        hookMethod(cronetAdapter, classLoader, "getRegion", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String val = ModuleConfig.getCronetParam("getRegion", null);
                if (val != null) param.setResult(val);
            }
        });

        hookMethod(cronetAdapter, classLoader, "getCarrierRegion", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String val = ModuleConfig.getCronetParam("getCarrierRegion", null);
                if (val != null) param.setResult(val);
            }
        });

        hookMethod(cronetAdapter, classLoader, "getSysRegion", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String val = ModuleConfig.getCronetParam("getSysRegion", null);
                if (val != null) param.setResult(val);
            }
        });

        hookMethod(cronetAdapter, classLoader, "getSimOperator", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String val = ModuleConfig.getCronetParam("getSimOperator", null);
                if (val != null) param.setResult(val);
            }
        });

        hookMethod(cronetAdapter, classLoader, "getAppInitialRegionInfo", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String val = ModuleConfig.getCronetParam("getAppInitialRegionInfo", null);
                if (val != null) param.setResult(val);
            }
        });
    }

    /**
     * Uses DexKit 2.3.0 to find and hook any dynamic or obfuscated implementations of ICronetAppProvider.
     */
    private void findAndHookObfuscatedProviders(ClassLoader classLoader) {
        if (!DexKitManager.isAvailable()) {
            return;
        }

        try {
            ClassDataList list = DexKitManager.findClass(
                    FindClass.create().searchPackages("com.bytedance.ttnet", "com.ss.android.ugc.aweme")
            );

            if (list == null || list.isEmpty()) return;

            for (ClassData cd : list) {
                if (cd.getName().contains("CronetAppProvider") || cd.getName().contains("TTNetAppProvider")) {
                    try {
                        Class<?> clazz = cd.getInstance(classLoader);
                        if (clazz != null) {
                            hookMethodOnClass(clazz, "getRegion");
                            hookMethodOnClass(clazz, "getCarrierRegion");
                            hookMethodOnClass(clazz, "getSysRegion");
                            hookMethodOnClass(clazz, "getSimOperator");
                        }
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable t) {
            Log.d(TAG, "DexKit provider scan skipped: " + t.getMessage());
        }
    }

    private void hookMethodOnClass(Class<?> clazz, final String methodName) {
        try {
            XposedHelpers.findAndHookMethod(clazz, methodName, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    String val = ModuleConfig.getCronetParam(methodName, null);
                    if (val != null) param.setResult(val);
                }
            });
        } catch (Throwable ignored) {}
    }

    private void hookMethod(String className, ClassLoader classLoader, final String methodName, XC_MethodHook hook) {
        try {
            XposedHelpers.findAndHookMethod(className, classLoader, methodName, hook);
            XposedBridge.log(TAG + ": Hooked " + className + "." + methodName + "()");
        } catch (Throwable t) {
            Log.d(TAG, "Hook " + className + "." + methodName + "() failed: " + t.getMessage());
        }
    }
}
