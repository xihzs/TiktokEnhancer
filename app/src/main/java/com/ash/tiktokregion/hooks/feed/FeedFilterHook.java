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
        hookFeedItemList(classLoader);
        hookFullFeedPanel(classLoader);
        hookAwemeModel(classLoader);
    }

    private void hookFeedItemList(ClassLoader classLoader) {
        final String feedItemListClass = "com.ss.android.ugc.aweme.feed.model.FeedItemList";
        try {
            Class<?> clazz = XposedHelpers.findClassIfExists(feedItemListClass, classLoader);
            if (clazz == null) return;

            XC_MethodHook getListHook = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object feedItemList = param.thisObject;
                    if (feedItemList == null) return;

                    Object result = param.getResult();
                    if (result instanceof List) {
                        List<?> list = (List<?>) result;
                        if (list.isEmpty()) return;
                        if (Boolean.TRUE.equals(XposedHelpers.getAdditionalInstanceField(list, "tiktok_enhancer_clean"))) {
                            return;
                        }

                        // Profile feed exclusion: never filter profile grid lists to preserve 1:1 index alignment!
                        if (isProfileFeed(feedItemList, list)) {
                            return;
                        }

                        List<Object> clean = filterList(list, feedItemList);
                        XposedHelpers.setAdditionalInstanceField(clean, "tiktok_enhancer_clean", Boolean.TRUE);
                        XposedHelpers.setAdditionalInstanceField(list, "tiktok_enhancer_clean", Boolean.TRUE);

                        // If clean is empty, DO NOT replace with empty list! Prevents "Something went wrong".
                        if (!clean.isEmpty() && clean.size() != list.size()) {
                            try { XposedHelpers.setBooleanField(feedItemList, "hasAd", false); } catch (Throwable ignored) {}
                            try { XposedHelpers.setObjectField(feedItemList, "preloadAds", null); } catch (Throwable ignored) {}
                            try { XposedHelpers.setObjectField(feedItemList, "items", clean); } catch (Throwable ignored) {}
                            param.setResult(clean);
                        }
                    }
                }
            };

            XposedHelpers.findAndHookMethod(clazz, "getItems", getListHook);
            try {
                XposedHelpers.findAndHookMethod(clazz, "getAwemeList", getListHook);
            } catch (Throwable ignored) {}

            XposedHelpers.findAndHookMethod(clazz, "setItems", List.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.args != null && param.args.length > 0 && param.args[0] instanceof List) {
                        List<?> incoming = (List<?>) param.args[0];
                        if (incoming != null && !incoming.isEmpty()) {
                            if (Boolean.TRUE.equals(XposedHelpers.getAdditionalInstanceField(incoming, "tiktok_enhancer_clean"))) {
                                return;
                            }
                            if (isProfileFeed(param.thisObject, incoming)) {
                                return;
                            }
                            List<Object> clean = filterList(incoming, param.thisObject);
                            XposedHelpers.setAdditionalInstanceField(clean, "tiktok_enhancer_clean", Boolean.TRUE);
                            XposedHelpers.setAdditionalInstanceField(incoming, "tiktok_enhancer_clean", Boolean.TRUE);
                            if (!clean.isEmpty() && clean.size() != incoming.size()) {
                                param.args[0] = clean;
                            }
                        }
                    }
                }
            });
            XposedBridge.log(TAG + ": Hooked FeedItemList (getItems, getAwemeList, setItems)");
        } catch (Throwable t) {
            Log.d(TAG, "Hook FeedItemList failed: " + t.getMessage());
        }
    }

    private void hookFullFeedPanel(ClassLoader classLoader) {
        final String panelClass = "com.ss.android.ugc.aweme.feed.panel.FullFeedFragmentPanel";
        final String basePanelClass = "com.ss.android.ugc.aweme.feed.panel.BaseListFragmentPanel";

        XC_MethodHook feedListMethodHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args != null && param.args.length > 0 && param.args[0] instanceof List) {
                    List<?> list = (List<?>) param.args[0];
                    boolean isFyp = isRecommendFeed(param.thisObject);
                    XposedBridge.log(TAG + ": feedListMethodHook (" + param.method.getName() + ") incoming size=" + list.size() + ", isFyp=" + isFyp + ", target=" + (param.thisObject != null ? param.thisObject.getClass().getName() : "null"));
                    if (list.isEmpty()) return;

                    // Strictly filter only if confirmed to be Recommend / FYP feed ("homepage_hot")
                    if (!isFyp) return;

                    List<Object> clean = filterList(list, param.thisObject);
                    XposedBridge.log(TAG + ": feedListMethodHook (" + param.method.getName() + ") filtered " + list.size() + " -> " + clean.size() + " kept");
                    // Critical: NEVER pass an empty list if source had items!
                    // Passing an empty list to FullFeedFragmentPanel triggers TikTok's fatal "Something went wrong" error screen.
                    if (!clean.isEmpty() && clean.size() != list.size()) {
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
                                            if (!clean.isEmpty() && clean.size() != list.size()) {
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
        if (sourceList == null || sourceList.isEmpty()) {
            return new ArrayList<>();
        }

        if (isProfileOrDetailContext(context) || isSingleAuthorProfileFeed(sourceList)) {
            @SuppressWarnings("unchecked")
            List<Object> raw = (List<Object>) sourceList;
            return raw;
        }

        boolean isFyp = isRecommendFeed(context);
        boolean isColdStart = isColdStartFeed(context);

        List<Object> kept = new ArrayList<>(sourceList.size());

        for (Object item : sourceList) {
            if (item == null) continue;
            int decision = AwemeEvaluator.evaluate(item, isFyp, isColdStart);
            String desc = "";
            try { Object d = XposedHelpers.callMethod(item, "getDesc"); if (d != null) desc = d.toString(); } catch (Throwable ignored) {}
            String author = "";
            try { Object a = XposedHelpers.callMethod(item, "getAuthor"); if (a != null) { Object n = XposedHelpers.callMethod(a, "getNickname"); if (n != null) author = n.toString(); } } catch (Throwable ignored) {}
            XposedBridge.log(TAG + ": evaluate item [" + author + " | " + desc + "] -> decision=" + decision);
            if (decision == AwemeEvaluator.DECISION_KEEP) {
                kept.add(item);
            }
        }

        // Buffer starvation protection for Smart Force Region mode
        int minHealthy = Math.min(sourceList.size(), 6);
        boolean smart = !ModuleConfig.strictForceRegion && ModuleConfig.forceRegion && isFyp;

        if (smart && kept.size() < minHealthy && !sourceList.isEmpty()) {
            for (Object item : sourceList) {
                if (item != null && !kept.contains(item)) {
                    // Only restore non-ad and strictly non-blocked items (never restore blocked country or song items)
                    boolean isAd = AwemeEvaluator.isAdAweme(item);
                    boolean isBlockedCountry = (ModuleConfig.blockCountries
                            && ModuleConfig.blockedCountries != null
                            && !ModuleConfig.blockedCountries.isEmpty())
                            && AwemeEvaluator.isBlockedCountry(item, ModuleConfig.blockedCountries);
                    boolean isBlockedSong = (ModuleConfig.blockCountries
                            && ModuleConfig.blockCountrySongs
                            && ModuleConfig.blockedCountries != null
                            && !ModuleConfig.blockedCountries.isEmpty())
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

    public static boolean isProfileFeed(Object feedItemList, List<?> list) {
        if (feedItemList != null) {
            try {
                Object uid = XposedHelpers.getObjectField(feedItemList, "dataUserId");
                if (uid instanceof String && !((String) uid).trim().isEmpty()) return true;
            } catch (Throwable ignored) {}
            try {
                Object ft = XposedHelpers.getObjectField(feedItemList, "fetchType");
                if (ft instanceof Number && ((Number) ft).intValue() == 24) return true;
            } catch (Throwable ignored) {}
            if (isProfileOrDetailContext(feedItemList)) return true;
        }
        if (list != null && isSingleAuthorProfileFeed(list)) return true;
        return false;
    }

    public static boolean isRecommendFeed(Object context) {
        if (context == null) return false;
        if (isProfileOrDetailContext(context)) return false;

        // FeedItemList support: if context is FeedItemList and NOT a profile feed, it IS FYP / recommend feed!
        if (context.getClass().getName().contains("FeedItemList")) {
            return !isProfileFeed(context, null);
        }

        // 1. Panels / Fragments / Adapters with getEventType()
        try {
            Object eventType = XposedHelpers.callMethod(context, "getEventType");
            if (eventType instanceof String) {
                return "homepage_hot".equals(eventType);
            }
        } catch (Throwable ignored) {}

        // 2. BaseFeedPageParams via getParam()
        try {
            Object param = XposedHelpers.callMethod(context, "getParam");
            if (param != null) {
                Object eventType = XposedHelpers.callMethod(param, "getEventType");
                if (eventType instanceof String) {
                    return "homepage_hot".equals(eventType);
                }
            }
        } catch (Throwable ignored) {}

        // 3. Objects with mParam field
        try {
            Object param = XposedHelpers.getObjectField(context, "mParam");
            if (param != null) {
                Object eventType = XposedHelpers.getObjectField(param, "eventType");
                if (eventType instanceof String) {
                    return "homepage_hot".equals(eventType);
                }
            }
        } catch (Throwable ignored) {}

        // 4. Objects with eventType field (e.g. BaseFeedPageParams)
        try {
            Object eventType = XposedHelpers.getObjectField(context, "eventType");
            if (eventType instanceof String) {
                return "homepage_hot".equals(eventType);
            }
        } catch (Throwable ignored) {}

        // 5. Fallback for FullFeedFragmentPanel / RecommendFeedFragmentPanel
        String className = context.getClass().getName();
        if (className.contains("RecommendFeedFragmentPanel") || className.contains("FullFeedFragmentPanel")) {
            return true;
        }

        return false;
    }

    public static boolean isProfileOrDetailContext(Object contextObj) {
        if (contextObj == null) return false;
        String className = contextObj.getClass().getName();
        if (className.contains("Profile")
                || className.contains("User")
                || className.contains("Detail")
                || className.contains("AwemeList")
                || className.contains("Favorite")
                || className.contains("Bookmark")
                || className.contains("Music")
                || className.contains("Challenge")
                || className.contains("Search")) {
            return true;
        }
        try {
            Object uid = XposedHelpers.callMethod(contextObj, "getDataUserId");
            if (uid instanceof String && !((String) uid).trim().isEmpty()) {
                return true;
            }
        } catch (Throwable ignored) {}
        try {
            Object uid = XposedHelpers.getObjectField(contextObj, "dataUserId");
            if (uid instanceof String && !((String) uid).trim().isEmpty()) {
                return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static boolean isSingleAuthorProfileFeed(List<?> items) {
        if (items == null || items.size() < 2) return false;
        String firstUid = null;
        int checked = 0;
        for (Object aweme : items) {
            if (aweme == null) continue;
            try {
                Object author = XposedHelpers.callMethod(aweme, "getAuthor");
                if (author == null) author = XposedHelpers.getObjectField(aweme, "author");
                if (author == null) continue;
                Object uidObj = XposedHelpers.callMethod(author, "getUid");
                if (uidObj == null) uidObj = XposedHelpers.getObjectField(author, "uid");
                if (uidObj instanceof String) {
                    String uid = (String) uidObj;
                    if (uid.isEmpty()) continue;
                    if (firstUid == null) {
                        firstUid = uid;
                    } else if (!firstUid.equals(uid)) {
                        return false;
                    }
                    checked++;
                    if (checked >= 3) break;
                }
            } catch (Throwable ignored) {}
        }
        return checked >= 2;
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
