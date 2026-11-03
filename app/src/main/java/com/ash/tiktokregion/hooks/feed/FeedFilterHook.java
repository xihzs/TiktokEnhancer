package com.ash.tiktokregion.hooks.feed;

import android.util.Log;

import com.ash.tiktokregion.core.BaseHook;
import com.ash.tiktokregion.core.ModuleConfig;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Presentation-layer feed filtering hook for FullFeedFragmentPanel.
 * Strictly filters only the For You Page ("homepage_hot") recommendation feed,
 * guaranteeing 1:1 index alignment on profile grids, sound pages, and search.
 */
public class FeedFilterHook implements BaseHook {

    private static final String TAG = "TikTokFeedFilter";

    @Override
    public void hook(ClassLoader classLoader) {
        hookFullFeedPanel(classLoader);
        hookAwemeModel(classLoader);
    }

    private void hookFullFeedPanel(ClassLoader classLoader) {
        final String panelClass = "com.ss.android.ugc.aweme.feed.panel.FullFeedFragmentPanel";
        final String basePanelClass = "com.ss.android.ugc.aweme.feed.panel.BaseListFragmentPanel";

        XC_MethodHook feedListMethodHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args != null && param.args.length > 0 && param.args[0] instanceof List) {
                    List<?> list = (List<?>) param.args[0];
                    if (list.isEmpty()) return;

                    // Strictly filter only if confirmed to be Recommend / FYP feed ("homepage_hot")
                    if (!isRecommendFeed(param.thisObject)) return;

                    List<Object> clean = filterList(list, param.thisObject);
                    if (clean.size() != list.size()) {
                        param.args[0] = clean;
                    }
                }
            }
        };

        // Hook onRefreshResult (x), onLoadMoreResult (Lk), and init/restore (LLLFF) in FullFeedFragmentPanel
        try {
            XposedHelpers.findAndHookMethod(panelClass, classLoader, "x", List.class, boolean.class, feedListMethodHook);
            XposedHelpers.findAndHookMethod(panelClass, classLoader, "Lk", List.class, boolean.class, feedListMethodHook);
            XposedHelpers.findAndHookMethod(panelClass, classLoader, "LLLFF", List.class, boolean.class, feedListMethodHook);
            XposedBridge.log(TAG + ": Hooked FullFeedFragmentPanel feed pipeline (x, Lk, LLLFF)");
        } catch (Throwable t) {
            Log.d(TAG, "Hook FullFeedFragmentPanel failed: " + t.getMessage());
        }

        // Hook dynamic card insertion in BaseListFragmentPanel (LLJILJIL)
        try {
            Class<?> baseClass = XposedHelpers.findClassIfExists(basePanelClass, classLoader);
            if (baseClass != null) {
                for (java.lang.reflect.Method m : baseClass.getDeclaredMethods()) {
                    if ("LLJILJIL".equals(m.getName()) && m.getParameterTypes().length == 1) {
                        XposedBridge.hookMethod(m, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                if (!isRecommendFeed(param.thisObject)) return;

                                Object holder = param.args[0];
                                if (holder != null) {
                                    try {
                                        Object listObj = XposedHelpers.getObjectField(holder, "LIZ");
                                        if (listObj instanceof List) {
                                            List<?> list = (List<?>) listObj;
                                            List<Object> clean = filterList(list, param.thisObject);
                                            if (clean.size() != list.size()) {
                                                XposedHelpers.setObjectField(holder, "LIZ", clean);
                                            }
                                        }
                                    } catch (Throwable ignored) {}
                                }
                            }
                        });
                        XposedBridge.log(TAG + ": Hooked BaseListFragmentPanel.LLJILJIL(insertItemList)");
                        break;
                    }
                }
            }
        } catch (Throwable t) {
            Log.d(TAG, "Hook BaseListFragmentPanel.LLJILJIL failed: " + t.getMessage());
        }
    }

    private List<Object> filterList(List<?> sourceList, Object context) {
        boolean isFyp = isRecommendFeed(context);
        boolean isColdStart = isColdStartFeed(context);

        List<Object> kept = new ArrayList<>(sourceList.size());

        for (Object item : sourceList) {
            if (item == null) continue;
            int decision = AwemeEvaluator.evaluate(item, isFyp, isColdStart);
            if (decision == AwemeEvaluator.DECISION_KEEP) {
                kept.add(item);
            }
        }

        // Buffer starvation protection for Smart Force Region mode
        int minHealthy = Math.min(sourceList.size(), 6);
        boolean smart = !isColdStart && !ModuleConfig.strictForceRegion && ModuleConfig.forceRegion && isFyp;

        if (smart && kept.size() < minHealthy && !sourceList.isEmpty()) {
            for (Object item : sourceList) {
                if (item != null && !kept.contains(item)) {
                    // Only restore non-ad and strictly non-blocked items (never restore blocked country or song items)
                    boolean isAd = AwemeEvaluator.isAdAweme(item);
                    boolean isBlockedCountry = (!ModuleConfig.blockedCountries.isEmpty())
                            && AwemeEvaluator.isBlockedCountry(item, ModuleConfig.blockedCountries);
                    boolean isBlockedSong = ModuleConfig.blockCountrySongs
                            && (!ModuleConfig.blockedCountries.isEmpty())
                            && AwemeEvaluator.isBlockedSong(item, ModuleConfig.blockedCountries);

                    if (!isAd && !isBlockedCountry && !isBlockedSong) {
                        kept.add(item);
                        if (kept.size() >= minHealthy) break;
                    }
                }
            }
        }

        return kept;
    }

    private boolean isRecommendFeed(Object context) {
        if (context == null) return false;

        // 1. Panels / Fragments / Adapters with getEventType()
        try {
            Object eventType = XposedHelpers.callMethod(context, "getEventType");
            if (eventType instanceof String) {
                // Strictly "homepage_hot" is the For You Page recommendation feed
                return "homepage_hot".equals(eventType);
            }
        } catch (Throwable ignored) {}

        // 2. Objects with eventType field (e.g. BaseFeedPageParams)
        try {
            Object eventType = XposedHelpers.getObjectField(context, "eventType");
            if (eventType instanceof String) {
                return "homepage_hot".equals(eventType);
            }
        } catch (Throwable ignored) {}

        return false;
    }

    private boolean isColdStartFeed(Object context) {
        if (context == null) return false;
        try {
            if (!isRecommendFeed(context)) return false;

            Object ft = XposedHelpers.getObjectField(context, "fetchType");
            if (ft instanceof Number && ((Number) ft).intValue() == 0) {
                return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private void hookAwemeModel(ClassLoader classLoader) {
        final String awemeClass = "com.ss.android.ugc.aweme.feed.model.Aweme";

        try {
            XposedHelpers.findAndHookMethod(awemeClass, classLoader, "isAd", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (ModuleConfig.hideAds) {
                        param.setResult(false);
                    }
                }
            });

            XposedHelpers.findAndHookMethod(awemeClass, classLoader, "getAwemeRawAd", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (ModuleConfig.hideAds) {
                        param.setResult(null);
                    }
                }
            });
        } catch (Throwable ignored) {}
    }
}
