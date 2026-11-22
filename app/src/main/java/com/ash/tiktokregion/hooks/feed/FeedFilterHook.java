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
        String[] listClasses = {
                "com.ss.android.ugc.aweme.feed.model.FeedItemList",
                "com.ss.android.ugc.aweme.follow.presenter.FollowFeedList",
                "com.ss.android.ugc.aweme.detail.model.BatchDetailList",
                "com.ss.android.ugc.aweme.explore.data.ExploreAwemeList"
        };

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

                    boolean isProf = isProfileFeed(feedItemList, list);
                    boolean hasBlocked = ModuleConfig.blockCountries
                            && ModuleConfig.blockedCountries != null
                            && !ModuleConfig.blockedCountries.isEmpty();

                    // Profile feed exclusion: only bypass if NO country blocking is configured
                    if (isProf && !hasBlocked) {
                        return;
                    }

                    List<Object> clean = filterList(list, feedItemList);
                    XposedHelpers.setAdditionalInstanceField(clean, "tiktok_enhancer_clean", Boolean.TRUE);
                    XposedHelpers.setAdditionalInstanceField(list, "tiktok_enhancer_clean", Boolean.TRUE);

                    // If clean is empty, DO NOT replace with empty list on FYP (prevents "Something went wrong").
                    // But on profile feeds, an empty list is normal ("No videos yet").
                    if (clean.size() != list.size()) {
                        if (!clean.isEmpty() || isProf) {
                            try { XposedHelpers.setBooleanField(feedItemList, "hasAd", false); } catch (Throwable ignored) {}
                            try { XposedHelpers.setObjectField(feedItemList, "preloadAds", null); } catch (Throwable ignored) {}
                            try { XposedHelpers.setObjectField(feedItemList, "items", clean); } catch (Throwable ignored) {}
                            param.setResult(clean);
                        }
                    }
                }
            }
        };

        XC_MethodHook setListHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args != null && param.args.length > 0 && param.args[0] instanceof List) {
                    List<?> incoming = (List<?>) param.args[0];
                    if (incoming != null && !incoming.isEmpty()) {
                        if (Boolean.TRUE.equals(XposedHelpers.getAdditionalInstanceField(incoming, "tiktok_enhancer_clean"))) {
                            return;
                        }
                        boolean isProf = isProfileFeed(param.thisObject, incoming);
                        boolean hasBlocked = ModuleConfig.blockCountries
                                && ModuleConfig.blockedCountries != null
                                && !ModuleConfig.blockedCountries.isEmpty();

                        if (isProf && !hasBlocked) {
                            return;
                        }
                        List<Object> clean = filterList(incoming, param.thisObject);
                        XposedHelpers.setAdditionalInstanceField(clean, "tiktok_enhancer_clean", Boolean.TRUE);
                        XposedHelpers.setAdditionalInstanceField(incoming, "tiktok_enhancer_clean", Boolean.TRUE);
                        if (clean.size() != incoming.size()) {
                            if (!clean.isEmpty() || isProf) {
                                param.args[0] = clean;
                            }
                        }
                    }
                }
            }
        };

        for (String clsName : listClasses) {
            try {
                Class<?> clazz = XposedHelpers.findClassIfExists(clsName, classLoader);
                if (clazz != null) {
                    try {
                        XposedHelpers.findAndHookMethod(clazz, "getItems", getListHook);
                    } catch (Throwable ignored) {}
                    try {
                        XposedHelpers.findAndHookMethod(clazz, "getAwemeList", getListHook);
                    } catch (Throwable ignored) {}
                    try {
                        XposedHelpers.findAndHookMethod(clazz, "setItems", List.class, setListHook);
                    } catch (Throwable ignored) {}
                    XposedBridge.log(TAG + ": Hooked " + clsName + " (getItems, getAwemeList, setItems)");
                }
            } catch (Throwable t) {
                Log.d(TAG, "Hook " + clsName + " failed: " + t.getMessage());
            }
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
                    boolean isProf = isProfileOrDetailContext(param.thisObject) || isSingleAuthorProfileFeed(list);
                    boolean hasBlocked = ModuleConfig.blockCountries
                            && ModuleConfig.blockedCountries != null
                            && !ModuleConfig.blockedCountries.isEmpty();

                    // Filter FYP feed, or non-FYP when blocked countries / ad hiding is enabled
                    if (!isFyp && !hasBlocked && !ModuleConfig.hideAds) return;

                    XposedBridge.log(TAG + ": feedListMethodHook (" + param.method.getName() + ") incoming size=" + list.size() + ", isFyp=" + isFyp + ", isProf=" + isProf + ", target=" + (param.thisObject != null ? param.thisObject.getClass().getName() : "null"));
                    if (list.isEmpty()) return;

                    List<Object> clean = filterList(list, param.thisObject);
                    XposedBridge.log(TAG + ": feedListMethodHook (" + param.method.getName() + ") filtered " + list.size() + " -> " + clean.size() + " kept");
                    // Critical: On FYP, never pass an empty list if source had items (prevents "Something went wrong").
                    // On profile/detail feeds, empty list is fine ("No videos yet").
                    if (clean.size() != list.size()) {
                        if (!clean.isEmpty() || isProf) {
                            param.args[0] = clean;
                        }
                    }
                }
            }
        };

        // Hook FeedPanel list pipeline across TikTok versions (v47.1.4/47.6.3: x, Lk, LLLFF; v47.9.3: rH, nl, LLLILZJ, w, y)
        String[] panelMethods = {"x", "Lk", "LLLFF", "rH", "nl", "LLLILZJ", "w", "y"};
        List<String> hookedPanelMethods = new ArrayList<>();
        try {
            Class<?> panelClazz = XposedHelpers.findClassIfExists(panelClass, classLoader);
            if (panelClazz != null) {
                for (String mName : panelMethods) {
                    try {
                        XposedHelpers.findAndHookMethod(panelClazz, mName, List.class, boolean.class, feedListMethodHook);
                        hookedPanelMethods.add(mName);
                    } catch (Throwable ignored) {}
                }
            }
            XposedBridge.log(TAG + ": Hooked FullFeedFragmentPanel methods: " + hookedPanelMethods);
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Hook FullFeedFragmentPanel failed: " + t.getMessage());
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
            } else {
                XposedBridge.log(TAG + ": BaseListFragmentPanel not found in classLoader");
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Hook BaseListFragmentPanel.LLJILJIL failed: " + t.getMessage());
        }
    }

    private List<Object> filterList(List<?> sourceList, Object context) {
        if (sourceList == null || sourceList.isEmpty()) {
            return new ArrayList<>();
        }

        boolean isProfile = isProfileOrDetailContext(context) || isSingleAuthorProfileFeed(sourceList);
        if (isProfile) {
            boolean hasBlocked = ModuleConfig.blockCountries
                    && ModuleConfig.blockedCountries != null
                    && !ModuleConfig.blockedCountries.isEmpty();
            if (!hasBlocked) {
                @SuppressWarnings("unchecked")
                List<Object> raw = (List<Object>) sourceList;
                return raw;
            }
        }

        boolean isFyp = isRecommendFeed(context);
        boolean isColdStart = isColdStartFeed(context);

        List<Object> kept = new ArrayList<>(sourceList.size());

        for (Object item : sourceList) {
            if (item == null) continue;
            int decision = AwemeEvaluator.evaluate(item, isFyp, isColdStart, isProfile);
            String desc = "";
            try { Object d = XposedHelpers.callMethod(item, "getDesc"); if (d != null) desc = d.toString(); } catch (Throwable ignored) {}
            String author = "";
            try { Object a = XposedHelpers.callMethod(item, "getAuthor"); if (a != null) { Object n = XposedHelpers.callMethod(a, "getNickname"); if (n != null) author = n.toString(); } } catch (Throwable ignored) {}
            XposedBridge.log(TAG + ": evaluate item [" + author + " | " + desc + "] -> decision=" + decision + (isProfile ? " (profile)" : ""));
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
        Class<?> awemeClazz = XposedHelpers.findClassIfExists(awemeClass, classLoader);
        if (awemeClazz == null) {
            XposedBridge.log(TAG + ": Aweme class not found in classLoader");
            return;
        }

        try {
            XposedHelpers.findAndHookMethod(awemeClazz, "isAd", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (ModuleConfig.hideAds) {
                        param.setResult(false);
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Hook Aweme.isAd failed: " + t.getMessage());
        }

        try {
            XposedHelpers.findAndHookMethod(awemeClazz, "getAwemeRawAd", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (ModuleConfig.hideAds) {
                        param.setResult(null);
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Hook Aweme.getAwemeRawAd failed: " + t.getMessage());
        }

        try {
            XposedHelpers.findAndHookMethod(awemeClazz, "getMusicPromoTagInfo", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (ModuleConfig.hideAds) {
                        param.setResult(null);
                    }
                }
            });
            XposedBridge.log(TAG + ": Hooked Aweme.getMusicPromoTagInfo()");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Hook Aweme.getMusicPromoTagInfo failed: " + t.getMessage());
        }

        try {
            XposedHelpers.findAndHookMethod(awemeClazz, "isWithPromotionalMusic", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (ModuleConfig.hideAds) {
                        param.setResult(false);
                    }
                }
            });
            XposedBridge.log(TAG + ": Hooked Aweme.isWithPromotionalMusic()");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Hook Aweme.isWithPromotionalMusic failed: " + t.getMessage());
        }

        try {
            XposedHelpers.findAndHookMethod(awemeClazz, "getWithPromotionalMusic$common_model_release", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (ModuleConfig.hideAds) {
                        param.setResult(false);
                    }
                }
            });
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(awemeClazz, "getMusicPromoId", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (ModuleConfig.hideAds) {
                        param.setResult(null);
                    }
                }
            });
            XposedBridge.log(TAG + ": Hooked Aweme.getMusicPromoId()");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Hook Aweme.getMusicPromoId failed: " + t.getMessage());
        }

        // Hook MusicPromoTagInfo constructors so Promoted Music badges never render even across version renames
        try {
            Class<?> tagClass = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.music.model.MusicPromoTagInfo", classLoader);
            if (tagClass != null) {
                XposedBridge.hookAllConstructors(tagClass, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object self = param.thisObject;
                        if (self == null) return;
                        try {
                            Object txt = XposedHelpers.getObjectField(self, "text");
                            if (txt instanceof String && !((String) txt).trim().isEmpty()) {
                                XposedHelpers.setAdditionalInstanceField(self, "tiktok_enhancer_is_promo_music", Boolean.TRUE);
                                if (ModuleConfig.hideAds) {
                                    XposedHelpers.setObjectField(self, "text", null);
                                    XposedHelpers.setObjectField(self, "schemaUrl", null);
                                    XposedHelpers.setObjectField(self, "showMusicTitle", Boolean.FALSE);
                                }
                            }
                        } catch (Throwable ignored) {}
                    }
                });
                XposedBridge.log(TAG + ": Hooked MusicPromoTagInfo constructors");
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Hook MusicPromoTagInfo failed: " + t.getMessage());
        }

        // Hook Music promo tags
        try {
            Class<?> musicClass = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.music.model.Music", classLoader);
            if (musicClass != null) {
                try {
                    XposedHelpers.findAndHookMethod(musicClass, "getPromotionTag", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (ModuleConfig.hideAds) {
                                param.setResult(null);
                            }
                        }
                    });
                } catch (Throwable ignored) {}
                try {
                    XposedHelpers.findAndHookMethod(musicClass, "getPromoId", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (ModuleConfig.hideAds) {
                                param.setResult(null);
                            }
                        }
                    });
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        // Hook VideoMusicTitleAssem.os(Aweme) to suppress Promoted Music UI badge entirely
        try {
            Class<?> assemClass = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.feed.assem.music.VideoMusicTitleAssem", classLoader);
            if (assemClass != null) {
                try {
                    XposedHelpers.findAndHookMethod(assemClass, "os", awemeClazz, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (ModuleConfig.hideAds) {
                                param.setResult(false);
                            }
                        }
                    });
                    XposedBridge.log(TAG + ": Hooked VideoMusicTitleAssem.os to suppress Promoted Music badge");
                } catch (Throwable t) {
                    XposedBridge.log(TAG + ": VideoMusicTitleAssem.os not found (" + t.getMessage() + ")");
                }
            } else {
                XposedBridge.log(TAG + ": VideoMusicTitleAssem not found in initial classLoader");
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Hook VideoMusicTitleAssem failed: " + t.getMessage());
        }
    }
}
