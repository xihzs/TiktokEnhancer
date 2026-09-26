package com.ash.tiktokregion;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class NearbyTabHook {

    private static final String TAG = "TikTokEnhancer-Nearby";
    private static volatile boolean sEnabled = false;

    public static void setEnabled(boolean enabled) {
        sEnabled = enabled;
    }

    public static boolean isEnabled() {
        return sEnabled || MainHook.isHideNearbyTabEnabled();
    }

    public static boolean isNearby(Object obj) {
        if (obj == null) return false;
        try {
            if (obj instanceof String) {
                String str = (String) obj;
                return "Nearby".equalsIgnoreCase(str)
                        || "homepage_nearby".equalsIgnoreCase(str)
                        || "Sleman".equalsIgnoreCase(str)
                        || str.toLowerCase().contains("nearby");
            }

            String clsName = obj.getClass().getName();
            if (clsName.toLowerCase().contains("nearby")) {
                return true;
            }

            // TopTabNode contains LLJJL which is TopTabProtocol
            try {
                Field fProtocol = XposedHelpers.findFieldIfExists(obj.getClass(), "LLJJL");
                if (fProtocol != null) {
                    Object protocol = fProtocol.get(obj);
                    if (protocol != null && protocol.getClass().getName().toLowerCase().contains("nearby")) {
                        return true;
                    }
                }
            } catch (Throwable ignored) {}

            // Check LLJJJIL field (TopTabProtocol.LLJJJIL is "Nearby")
            try {
                Field fTag = XposedHelpers.findFieldIfExists(obj.getClass(), "LLJJJIL");
                if (fTag != null) {
                    Object tag = fTag.get(obj);
                    if (tag instanceof String && isNearby(tag)) return true;
                }
            } catch (Throwable ignored) {}

            // Check LLJJJJLIIL field (TopTabProtocol.LLJJJJLIIL is "homepage_nearby")
            try {
                Field fMob = XposedHelpers.findFieldIfExists(obj.getClass(), "LLJJJJLIIL");
                if (fMob != null) {
                    Object mob = fMob.get(obj);
                    if (mob instanceof String && isNearby(mob)) return true;
                }
            } catch (Throwable ignored) {}

        } catch (Throwable ignored) {}
        return false;
    }

    public static List<?> filterTabList(List<?> original) {
        if (original == null || original.isEmpty()) return original;
        boolean hasNearby = false;
        for (Object item : original) {
            if (isNearby(item)) {
                hasNearby = true;
                break;
            }
        }
        if (!hasNearby) return original;

        List<Object> filtered = new ArrayList<>(original.size());
        for (Object item : original) {
            if (!isNearby(item)) {
                filtered.add(item);
            }
        }

        if (original instanceof CopyOnWriteArrayList) {
            return new CopyOnWriteArrayList<>(filtered);
        }
        return filtered;
    }

    public static Object filterPairResult(Object pairResult, ClassLoader classLoader) {
        if (pairResult == null) return null;
        try {
            Object first = XposedHelpers.callMethod(pairResult, "getFirst");
            Object second = XposedHelpers.callMethod(pairResult, "getSecond");

            List<?> filteredFirst = (first instanceof List) ? filterTabList((List<?>) first) : null;
            List<?> filteredSecond = (second instanceof List) ? filterTabList((List<?>) second) : null;

            if (filteredFirst != null && filteredSecond != null) {
                Class<?> pairClass = XposedHelpers.findClassIfExists("kotlin.Pair", classLoader);
                if (pairClass != null) {
                    return XposedHelpers.newInstance(pairClass, filteredFirst, filteredSecond);
                }
            }
        } catch (Throwable t) {
            Log.d(TAG, "filterPairResult error: " + t.getMessage());
        }
        return pairResult;
    }

    public static void hook(ClassLoader classLoader) {
        if (classLoader == null) return;

        // 1. Hook NearbyTabProtocol
        Class<?> nearbyProtoClass = XposedHelpers.findClassIfExists("com.ss.android.ugc.nearby.tab.NearbyTabProtocol", classLoader);
        if (nearbyProtoClass != null) {
            hookNearbyTabProtocolClass(nearbyProtoClass);
        }

        // 2. Hook NearbyServiceImpl
        Class<?> nearbyServiceClass = XposedHelpers.findClassIfExists("com.ss.android.ugc.nearby.service.NearbyServiceImpl", classLoader);
        if (nearbyServiceClass != null) {
            hookNearbyServiceImplClass(nearbyServiceClass);
        }

        // 3. Hook HomeTabViewModel
        Class<?> viewModelClass = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.homepage.ui.view.tab.HomeTabViewModel", classLoader);
        if (viewModelClass != null) {
            hookHomeTabViewModelClass(viewModelClass);
        }

        // 4. Hook TabAbilityAssem
        Class<?> tabAbilityClass = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.main.assems.tabs.TabAbilityAssem", classLoader);
        if (tabAbilityClass != null) {
            hookTabAbilityAssemClass(tabAbilityClass);
        }

        // 5. Hook MainTabStrip
        Class<?> mainTabStripClass = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.homepage.ui.view.tab.top.MainTabStrip", classLoader);
        if (mainTabStripClass != null) {
            hookMainTabStripClass(mainTabStripClass);
        }

        // 6. Hook C205503nG (LX/03nG;) TopTabOperator
        Class<?> tabOperatorClass = XposedHelpers.findClassIfExists("X.03nG", classLoader);
        if (tabOperatorClass != null) {
            hookTabOperatorClass(tabOperatorClass, classLoader);
        }
    }

    public static void onClassLoaded(Class<?> clazz) {
        if (clazz == null) return;
        String name = clazz.getName();
        if ("com.ss.android.ugc.nearby.tab.NearbyTabProtocol".equals(name)) {
            hookNearbyTabProtocolClass(clazz);
        } else if ("com.ss.android.ugc.nearby.service.NearbyServiceImpl".equals(name)) {
            hookNearbyServiceImplClass(clazz);
        } else if ("com.ss.android.ugc.aweme.homepage.ui.view.tab.HomeTabViewModel".equals(name)) {
            hookHomeTabViewModelClass(clazz);
        } else if ("com.ss.android.ugc.aweme.main.assems.tabs.TabAbilityAssem".equals(name)) {
            hookTabAbilityAssemClass(clazz);
        } else if ("com.ss.android.ugc.aweme.homepage.ui.view.tab.top.MainTabStrip".equals(name)) {
            hookMainTabStripClass(clazz);
        } else if ("X.03nG".equals(name)) {
            hookTabOperatorClass(clazz, clazz.getClassLoader());
        }
    }

    public static void hookNearbyTabProtocolClass(Class<?> clazz) {
        if (clazz == null) return;
        try {
            XposedHelpers.findAndHookMethod(clazz, "enable", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (isEnabled()) {
                        param.setResult(false);
                    }
                }
            });
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(clazz, "LJIILL", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (isEnabled()) {
                        param.setResult(false);
                    }
                }
            });
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(clazz, "bd", Context.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (isEnabled()) {
                        param.setResult("");
                    }
                }
            });
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(clazz, "getTag", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (isEnabled()) {
                        param.setResult("DISABLED_NEARBY");
                    }
                }
            });
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(clazz, "getMob", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (isEnabled()) {
                        param.setResult("disabled_homepage_nearby");
                    }
                }
            });
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(clazz, "Ia", Context.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (isEnabled()) {
                        param.setResult(new Bundle());
                    }
                }
            });
        } catch (Throwable ignored) {}
    }

    public static void hookNearbyServiceImplClass(Class<?> clazz) {
        if (clazz == null) return;
        try {
            XposedHelpers.findAndHookMethod(clazz, "LJIIIIZZ", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (isEnabled()) {
                        param.setResult(false);
                    }
                }
            });
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(clazz, "LIZ", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (isEnabled()) {
                        param.setResult(false);
                    }
                }
            });
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(clazz, "LJFF", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (isEnabled()) {
                        param.setResult(false);
                    }
                }
            });
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(clazz, "LJIJ", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (isEnabled()) {
                        param.setResult(false);
                    }
                }
            });
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(clazz, "LJIIZILJ", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (isEnabled()) {
                        param.setResult(null);
                    }
                }
            });
        } catch (Throwable ignored) {}
    }

    public static void hookHomeTabViewModelClass(Class<?> clazz) {
        if (clazz == null) return;
        try {
            XposedHelpers.findAndHookMethod(clazz, "xb2", boolean.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (isEnabled()) {
                        Object result = param.getResult();
                        if (result instanceof List) {
                            param.setResult(filterTabList((List<?>) result));
                        }
                    }
                }
            });
        } catch (Throwable ignored) {}
    }

    public static void hookTabAbilityAssemClass(Class<?> clazz) {
        if (clazz == null) return;
        try {
            XposedHelpers.findAndHookMethod(clazz, "K9", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (isEnabled()) {
                        Object result = param.getResult();
                        if (result instanceof List) {
                            param.setResult(filterTabList((List<?>) result));
                        }
                    }
                }
            });
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(clazz, "fg2", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (isEnabled()) {
                        Object result = param.getResult();
                        if (result instanceof List) {
                            param.setResult(filterTabList((List<?>) result));
                        }
                    }
                }
            });
        } catch (Throwable ignored) {}

        try {
            for (Method m : clazz.getDeclaredMethods()) {
                if (Modifier.isStatic(m.getModifiers())) {
                    Class<?>[] pts = m.getParameterTypes();
                    if (pts.length == 4 && List.class.isAssignableFrom(pts[2]) && List.class.isAssignableFrom(pts[3])) {
                        XposedBridge.hookMethod(m, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) {
                                if (isEnabled()) {
                                    if (param.args[2] instanceof List) {
                                        param.args[2] = filterTabList((List<?>) param.args[2]);
                                    }
                                    if (param.args[3] instanceof List) {
                                        param.args[3] = filterTabList((List<?>) param.args[3]);
                                    }
                                }
                            }
                        });
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    public static void hookMainTabStripClass(Class<?> clazz) {
        if (clazz == null) return;
        try {
            XposedHelpers.findAndHookMethod(clazz, "LJIIZILJ", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (isEnabled()) {
                        Object result = param.getResult();
                        if (result instanceof List) {
                            param.setResult(filterTabList((List<?>) result));
                        }
                    }
                }
            });
        } catch (Throwable ignored) {}
    }

    public static void hookTabOperatorClass(Class<?> clazz, ClassLoader classLoader) {
        if (clazz == null) return;
        try {
            XposedHelpers.findAndHookMethod(clazz, "LIZIZ", boolean.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (isEnabled()) {
                        Object result = param.getResult();
                        if (result instanceof List) {
                            param.setResult(filterTabList((List<?>) result));
                        }
                    }
                }
            });
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(clazz, "LJII", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (isEnabled()) {
                        Object result = param.getResult();
                        if (result != null) {
                            param.setResult(filterPairResult(result, classLoader));
                        }
                    }
                }
            });
        } catch (Throwable ignored) {}
    }
}
