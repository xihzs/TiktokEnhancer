package com.ash.tiktokregion.hooks.social;

import com.ash.tiktokregion.core.BaseHook;
import com.ash.tiktokregion.core.ModuleConfig;

import java.lang.reflect.Method;
import java.util.Collections;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Ghost Mode Hook: Anonymous Story & Profile Views.
 * Suppresses view reporting requests at network and dispatcher levels without breaking return contracts.
 */
public class GhostModeHook implements BaseHook {

    private static final String TAG = "TikTokGhostMode";

    @Override
    public void hook(ClassLoader cl) {
        hookNetworkEngine(cl);
        hookStoryDispatchers(cl);
        hookProfileViewReporting(cl);
        hookStoryViewReporting(cl);
    }

    private static boolean isGhostEndpoint(String u) {
        if (u == null) return false;
        return u.contains("/view_record/add/")
                || u.contains("/story/view/report/")
                || u.contains("/story/interaction/report/")
                || u.contains("/story/reveal/report/")
                || u.contains("view_record/add")
                || u.contains("story/view/report");
    }

    private static void hookNetworkEngine(ClassLoader cl) {
        Class<?> ssHttpCallClass = XposedHelpers.findClassIfExists("com.bytedance.retrofit2.SsHttpCall", cl);
        if (ssHttpCallClass == null) return;

        try {
            XposedHelpers.findAndHookMethod(ssHttpCallClass, "getResponseWithInterceptorChain", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!ModuleConfig.ghostStoryView) return;
                    try {
                        Object req = XposedHelpers.callMethod(param.thisObject, "request");
                        if (req != null) {
                            String url = null;
                            try { url = (String) XposedHelpers.callMethod(req, "getUrl"); } catch (Throwable ignored) {}
                            String path = null;
                            try { path = (String) XposedHelpers.callMethod(req, "getPath"); } catch (Throwable ignored) {}

                            if (isGhostEndpoint(url) || isGhostEndpoint(path)) {
                                Class<?> ssResponseClass = XposedHelpers.findClassIfExists("com.bytedance.retrofit2.SsResponse", cl);
                                if (ssResponseClass == null) {
                                    ssResponseClass = XposedHelpers.findClassIfExists("X.055F", cl);
                                }
                                if (ssResponseClass == null) {
                                    ssResponseClass = XposedHelpers.findClassIfExists("X.055g", cl);
                                }
                                Class<?> rawResponseClass = XposedHelpers.findClassIfExists("com.bytedance.retrofit2.client.Response", cl);
                                if (rawResponseClass == null) {
                                    rawResponseClass = XposedHelpers.findClassIfExists("X.055T", cl);
                                }
                                if (rawResponseClass == null) {
                                    rawResponseClass = XposedHelpers.findClassIfExists("X.055s", cl);
                                }
                                Class<?> baseRespClass = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.base.api.BaseResponse", cl);

                                Object rawResp = null;
                                if (rawResponseClass != null) {
                                    try {
                                        rawResp = XposedHelpers.newInstance(rawResponseClass,
                                                url != null ? url : "https://api.tiktokv.com/",
                                                200, "OK", Collections.emptyList(), null);
                                    } catch (Throwable ignored) {}
                                }

                                Object mockBody = null;
                                if (baseRespClass != null) {
                                    try {
                                        mockBody = baseRespClass.newInstance();
                                        XposedHelpers.setIntField(mockBody, "status_code", 0);
                                    } catch (Throwable ignored) {}
                                }

                                if (ssResponseClass != null && rawResp != null) {
                                    Object ssResp = null;
                                    try {
                                        ssResp = XposedHelpers.callStaticMethod(ssResponseClass, "success", mockBody, rawResp);
                                    } catch (Throwable ignored) {}
                                    if (ssResp == null) {
                                        try {
                                            ssResp = XposedHelpers.callStaticMethod(ssResponseClass, "LIZLLL", mockBody, rawResp);
                                        } catch (Throwable ignored) {}
                                    }
                                    if (ssResp != null) {
                                        param.setResult(ssResp);
                                        return;
                                    }
                                }
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            });
            XposedBridge.log(TAG + ": Hooked SsHttpCall for ghost view interception");
        } catch (Throwable ignored) {}
    }

    private static void hookStoryDispatchers(ClassLoader cl) {
        // 1. Hook StoryRevealServiceImpl: LJI (v47.6.3), LJIIZILJ (v47.1.4) (String, String, boolean)
        Class<?> revealService = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.story.reveal.StoryRevealServiceImpl", cl);
        if (revealService != null) {
            for (Method m : revealService.getDeclaredMethods()) {
                if (("LJI".equals(m.getName()) || "LJIIZILJ".equals(m.getName())) && m.getParameterTypes().length == 3) {
                    try {
                        XposedBridge.hookMethod(m, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                if (ModuleConfig.ghostStoryView) {
                                    param.setResult(null); // void return
                                }
                            }
                        });
                        XposedBridge.log(TAG + ": Hooked StoryRevealServiceImpl." + m.getName());
                    } catch (Throwable ignored) {}
                }
            }
        }

        // 2. Legacy fallback: Hook X.07e6 only if void return type
        Class<?> revealClass = XposedHelpers.findClassIfExists("X.07e6", cl);
        if (revealClass != null) {
            for (Method m : revealClass.getDeclaredMethods()) {
                if (("LIZJ".equals(m.getName()) || "LJIIZILJ".equals(m.getName()) || "LJI".equals(m.getName())) && m.getReturnType() == void.class) {
                    try {
                        XposedBridge.hookMethod(m, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                if (ModuleConfig.ghostStoryView) {
                                    param.setResult(null);
                                }
                            }
                        });
                    } catch (Throwable ignored) {}
                }
            }
        }
    }

    private static void hookProfileViewReporting(ClassLoader cl) {
        String[] profileApiClasses = new String[]{
                "com.ss.android.ugc.aweme.user.viewer.service.ProfileViewerApiService",
                "com.ss.android.ugc.profile.business.ci.viewer.api.IProfileViewerApi",
                "com.ss.android.ugc.profile.business.ci.viewer.api.ProfileViewerApiService"
        };
        for (String className : profileApiClasses) {
            Class<?> apiClass = XposedHelpers.findClassIfExists(className, cl);
            if (apiClass == null) continue;
            for (Method m : apiClass.getDeclaredMethods()) {
                if ("reportView".equals(m.getName()) || "reportProfileView".equals(m.getName())) {
                    try {
                        XposedBridge.hookMethod(m, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                if (!ModuleConfig.ghostStoryView || param.args == null || param.args.length < 2) return;
                                param.args[0] = "0";
                                param.args[1] = "0";
                            }
                        });
                    } catch (Throwable ignored) {}
                }
            }
        }
    }

    private static void hookStoryViewReporting(ClassLoader cl) {
        String[] storyApiClasses = new String[]{
                "com.ss.android.ugc.aweme.story.api.StoryApi",
                "com.ss.android.ugc.aweme.story.api.IStoryApi"
        };
        for (String className : storyApiClasses) {
            Class<?> storyApiClass = XposedHelpers.findClassIfExists(className, cl);
            if (storyApiClass == null) continue;
            for (Method m : storyApiClass.getDeclaredMethods()) {
                if ("reportStoryViewed".equals(m.getName())) {
                    try {
                        XposedBridge.hookMethod(m, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                if (!ModuleConfig.ghostStoryView || param.args == null || param.args.length < 2) return;
                                // Neutralize story_id and author_id to prevent server view association without breaking RxJava/Retrofit return contract
                                param.args[0] = "0";
                                param.args[1] = "0";
                            }
                        });
                        XposedBridge.log(TAG + ": Hooked " + className + ".reportStoryViewed");
                    } catch (Throwable ignored) {}
                }
            }
        }
    }
}
