package com.ash.tiktokregion;

import android.os.Bundle;
import android.util.Log;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class FeedLandingHook {

    private static final String TAG = "TikTokFeedLanding";
    private static volatile boolean sAssemHooked = false;
    private static volatile boolean sExServiceHooked = false;
    private static volatile boolean sHoxHooked = false;
    private static volatile boolean sMainTabStripHooked = false;

    public static void hook(ClassLoader classLoader) {
        if (classLoader == null) return;

        Class<?> assemClass = XposedHelpers.findClassIfExists(
                "com.ss.android.ugc.aweme.main.assems.mainfragment.HomeViewPagerAssem", classLoader);
        if (assemClass != null) {
            hookHomeViewPagerAssem(assemClass);
        }

        Class<?> exServiceClass = XposedHelpers.findClassIfExists(
                "com.ss.android.ugc.aweme.homepage.HomePageExServiceImpl", classLoader);
        if (exServiceClass != null) {
            hookHomePageExService(exServiceClass);
        }

        Class<?> hoxClass = XposedHelpers.findClassIfExists(
                "com.bytedance.hox.Hox", classLoader);
        if (hoxClass != null) {
            hookHox(hoxClass);
        }

        Class<?> mainTabStripClass = XposedHelpers.findClassIfExists(
                "com.ss.android.ugc.aweme.homepage.ui.view.tab.top.MainTabStrip", classLoader);
        if (mainTabStripClass != null) {
            hookMainTabStrip(mainTabStripClass);
        }
    }

    public static void onClassLoaded(Class<?> clazz) {
        if (clazz == null) return;
        String name = clazz.getName();
        if ("com.ss.android.ugc.aweme.main.assems.mainfragment.HomeViewPagerAssem".equals(name)) {
            hookHomeViewPagerAssem(clazz);
        } else if ("com.ss.android.ugc.aweme.homepage.HomePageExServiceImpl".equals(name)) {
            hookHomePageExService(clazz);
        } else if ("com.bytedance.hox.Hox".equals(name)) {
            hookHox(clazz);
        } else if ("com.ss.android.ugc.aweme.homepage.ui.view.tab.top.MainTabStrip".equals(name)) {
            hookMainTabStrip(clazz);
        }
    }

    public static int getForYouTabIndex(Object assem) {
        if (assem == null) return 1;
        try {
            Object tabStrip = XposedHelpers.callMethod(assem, "Br");
            if (tabStrip != null) {
                int idx = (int) XposedHelpers.callMethod(tabStrip, "rq0", "For You");
                if (idx >= 0) return idx;
                idx = (int) XposedHelpers.callMethod(tabStrip, "rq0", "homepage_hot");
                if (idx >= 0) return idx;
            }
        } catch (Throwable ignored) {}

        try {
            Object adapter = XposedHelpers.callMethod(assem, "Ar");
            if (adapter != null) {
                int idx = (int) XposedHelpers.callMethod(adapter, "Xn", 1, 0);
                if (idx >= 0) return idx;
            }
        } catch (Throwable ignored) {}

        return 1;
    }

    public static void hookHomeViewPagerAssem(Class<?> clazz) {
        if (clazz == null || sAssemHooked) return;
        sAssemHooked = true;

        try {
            XposedHelpers.findAndHookMethod(clazz, "fG0", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    param.setResult(getForYouTabIndex(param.thisObject));
                }
            });
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(clazz, "zh1", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    param.setResult(getForYouTabIndex(param.thisObject));
                }
            });
        } catch (Throwable ignored) {}

        try {
            XposedBridge.hookAllMethods(clazz, "onViewCreated", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        int idx = getForYouTabIndex(param.thisObject);
                        Object viewPager = XposedHelpers.callMethod(param.thisObject, "Cr");
                        if (viewPager != null) {
                            XposedHelpers.callMethod(viewPager, "setCurrentItem", idx, false);
                        }
                    } catch (Throwable ignored) {}
                }
            });
        } catch (Throwable ignored) {}
    }

    public static void hookHomePageExService(Class<?> clazz) {
        if (clazz == null || sExServiceHooked) return;
        sExServiceHooked = true;

        try {
            XposedHelpers.findAndHookMethod(clazz, "LJIJJLI", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    param.setResult(1);
                }
            });
        } catch (Throwable ignored) {}
    }

    public static void hookHox(Class<?> clazz) {
        if (clazz == null || sHoxHooked) return;
        sHoxHooked = true;

        try {
            XposedBridge.hookAllMethods(clazz, "d73", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.args == null || param.args.length < 2) return;
                    if (param.args[0] instanceof Bundle) {
                        Bundle bundle = (Bundle) param.args[0];
                        if (bundle.containsKey("fromStart")) {
                            Object target = param.args[1];
                            if (target instanceof String) {
                                String tag = (String) target;
                                if ("Following".equalsIgnoreCase(tag) || "homepage_follow".equalsIgnoreCase(tag)) {
                                    param.args[1] = "For You";
                                    if (bundle.containsKey("toPage")) {
                                        bundle.putString("toPage", "For You");
                                    }
                                }
                            }
                        }
                    }
                }
            });
        } catch (Throwable ignored) {}
    }

    public static void hookMainTabStrip(Class<?> clazz) {
        if (clazz == null || sMainTabStripHooked) return;
        sMainTabStripHooked = true;

        try {
            XposedBridge.hookAllMethods(clazz, "Zb1", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Object res = param.getResult();
                    if (res == null || "".equals(res)) {
                        param.setResult("For You");
                    }
                }
            });
        } catch (Throwable ignored) {}
    }
}
