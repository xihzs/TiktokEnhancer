package com.ash.tiktokregion;

import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class AdsHook {

    private static final String TAG = "TikTokFeedFilter";

    public static void log(String msg) {
        Log.i(TAG, msg);
        try {
            XposedBridge.log(TAG + ": " + msg);
        } catch (Throwable ignored) {}
    }

    private static volatile boolean sFeedItemListHooked = false;
    private static volatile boolean sFeedApisHooked = false;
    private static volatile boolean sAwemeModelHooked = false;

    private static volatile BlockedCountryMatcher sCachedMatcher = null;
    private static volatile Set<String> sLastBlockedSet = null;

    public static void hook(ClassLoader classLoader) {
        if (classLoader == null) return;
        hookFeedItemList(classLoader);
        hookFeedApis(classLoader);
        hookFeedPanel(classLoader);
        hookAwemeModel(classLoader);
    }

    public static synchronized void hookFeedItemListClass(Class<?> feedItemListClass) {
        if (feedItemListClass == null || sFeedItemListHooked) return;
        sFeedItemListHooked = true;

        XC_MethodHook listHook = new XC_MethodHook() {
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

                    List<Object> cleanList = filterAwemeList(list, feedItemList);

                    if (cleanList.size() != list.size()) {
                        try {
                            XposedHelpers.setBooleanField(feedItemList, "hasAd", false);
                        } catch (Throwable ignored) {}
                        try {
                            XposedHelpers.setObjectField(feedItemList, "preloadAds", null);
                        } catch (Throwable ignored) {}
                        try {
                            XposedHelpers.setObjectField(feedItemList, "items", cleanList);
                        } catch (Throwable ignored) {}

                        param.setResult(cleanList);
                    } else {
                        XposedHelpers.setAdditionalInstanceField(list, "tiktok_enhancer_clean", Boolean.TRUE);
                    }
                }
            }
        };

        try {
            XposedHelpers.findAndHookMethod(feedItemListClass, "getItems", listHook);
            log("Hooked FeedItemList.getItems()");
        } catch (Throwable t) {
            log("FeedItemList.getItems() hook failed: " + t.getMessage());
        }

        try {
            XposedHelpers.findAndHookMethod(feedItemListClass, "getAwemeList", listHook);
            log("Hooked FeedItemList.getAwemeList()");
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(feedItemListClass, "setItems", List.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.args != null && param.args.length > 0 && param.args[0] instanceof List) {
                        List<?> incoming = (List<?>) param.args[0];
                        if (incoming != null && !incoming.isEmpty()) {
                            if (Boolean.TRUE.equals(XposedHelpers.getAdditionalInstanceField(incoming, "tiktok_enhancer_clean"))) {
                                return;
                            }
                            List<Object> cleanList = filterAwemeList(incoming, param.thisObject);
                            param.args[0] = cleanList;
                        }
                    }
                }
            });
            log("Hooked FeedItemList.setItems(List)");
        } catch (Throwable t) {
            log("FeedItemList.setItems(List) hook failed: " + t.getMessage());
        }

        try {
            XposedHelpers.findAndHookMethod(feedItemListClass, "isHasAd", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (MainHook.isHideAdsEnabled()) {
                        param.setResult(false);
                    }
                }
            });
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(feedItemListClass, "getPreloadAds", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (MainHook.isHideAdsEnabled()) {
                        param.setResult(null);
                    }
                }
            });
        } catch (Throwable ignored) {}
    }

    private static void hookSetAwemeList(Class<?> feedItemListClass) {
        try {
            XposedHelpers.findAndHookMethod(feedItemListClass, "setAwemeList", List.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.args != null && param.args.length > 0 && param.args[0] instanceof List) {
                        List<?> incoming = (List<?>) param.args[0];
                        if (incoming != null && !incoming.isEmpty()) {
                            if (Boolean.TRUE.equals(XposedHelpers.getAdditionalInstanceField(incoming, "tiktok_enhancer_clean"))) {
                                return;
                            }
                            List<Object> cleanList = filterAwemeList(incoming, param.thisObject);
                            param.args[0] = cleanList;
                        }
                    }
                }
            });
            log("Hooked FeedItemList.setAwemeList(List)");
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(feedItemListClass, "appendItems", List.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.args != null && param.args.length > 0 && param.args[0] instanceof List) {
                        List<?> incoming = (List<?>) param.args[0];
                        if (incoming != null && !incoming.isEmpty()) {
                            if (Boolean.TRUE.equals(XposedHelpers.getAdditionalInstanceField(incoming, "tiktok_enhancer_clean"))) {
                                return;
                            }
                            List<Object> cleanList = filterAwemeList(incoming, param.thisObject);
                            param.args[0] = cleanList;
                        }
                    }
                }
            });
            log("Hooked FeedItemList.appendItems(List)");
        } catch (Throwable ignored) {}
    }

    private static void hookFeedItemList(ClassLoader classLoader) {
        if (sFeedItemListHooked || classLoader == null) return;
        Class<?> clazz = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.feed.model.FeedItemList", classLoader);
        if (clazz != null) {
            hookFeedItemListClass(clazz);
            hookSetAwemeList(clazz);
        }
    }

    private static final Set<String> sHookedPanelClasses = new HashSet<>();

    public static void hookFeedPanel(ClassLoader classLoader) {
        if (classLoader == null) return;

        String[] panelClasses = {
                "com.ss.android.ugc.aweme.feed.panel.FullFeedFragmentPanel",
                "com.ss.android.ugc.aweme.feed.panel.RecommendFeedFragmentPanel",
                "com.ss.android.ugc.aweme.feed.panel.FollowFeedFragmentPanelMT",
                "com.ss.android.ugc.aweme.stemfeed.panel.StemFeedFragmentPanel",
                "com.ss.android.ugc.aweme.repostfeed.feed.RepostFeedPanel",
        };

        for (String panelClass : panelClasses) {
            if (sHookedPanelClasses.contains(panelClass)) continue;
            try {
                Class<?> clazz = XposedHelpers.findClassIfExists(panelClass, classLoader);
                if (clazz == null) continue;

                hookPanelGetAwemeList(clazz, panelClass);
            } catch (Throwable ignored) {}
        }
    }

    private static void hookPanelGetAwemeList(Class<?> clazz, String panelClass) {
        if (sHookedPanelClasses.contains(panelClass)) return;
        try {
            XposedHelpers.findAndHookMethod(clazz, "getAwemeList", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.thisObject != null && isProfileOrDetailContext(param.thisObject)) {
                        return;
                    }
                    Object result = param.getResult();
                    if (result instanceof List) {
                        List<?> list = (List<?>) result;
                        if (list.isEmpty()) return;
                        if (Boolean.TRUE.equals(XposedHelpers.getAdditionalInstanceField(list, "tiktok_enhancer_clean"))) {
                            return;
                        }

                        List<Object> clean = filterAwemeList(list, param.thisObject);
                        if (clean.size() != list.size()) {
                            param.setResult(clean);
                        } else {
                            XposedHelpers.setAdditionalInstanceField(list, "tiktok_enhancer_clean", Boolean.TRUE);
                        }
                    }
                }
            });
            sHookedPanelClasses.add(panelClass);
            log("Hooked " + panelClass + ".getAwemeList()");
        } catch (Throwable t) {
            log("Failed hooking " + panelClass + ".getAwemeList(): " + t.getMessage());
        }
    }

    private static volatile boolean sFollowFeedListHooked = false;

    public static synchronized void hookFollowFeedListClass(Class<?> followFeedListClass) {
        if (followFeedListClass == null || sFollowFeedListHooked) return;
        sFollowFeedListHooked = true;

        try {
            XposedHelpers.findAndHookMethod(followFeedListClass, "getAwemeList", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object result = param.getResult();
                    if (result instanceof List) {
                        List<?> list = (List<?>) result;
                        if (list.isEmpty()) return;
                        if (Boolean.TRUE.equals(XposedHelpers.getAdditionalInstanceField(list, "tiktok_enhancer_clean"))) {
                            return;
                        }
                        List<Object> clean = filterAwemeList(list, param.thisObject);
                        if (clean.size() != list.size()) {
                            param.setResult(clean);
                        } else {
                            XposedHelpers.setAdditionalInstanceField(list, "tiktok_enhancer_clean", Boolean.TRUE);
                        }
                    }
                }
            });
            log("Hooked FollowFeedList.getAwemeList()");
        } catch (Throwable t) {
            log("FollowFeedList.getAwemeList() hook failed: " + t.getMessage());
        }
    }

    private static volatile boolean sFriendsFeedResponseHooked = false;

    public static synchronized void hookFriendsFeedResponseClass(Class<?> friendsFeedResponseClass) {
        if (friendsFeedResponseClass == null || sFriendsFeedResponseHooked) return;
        sFriendsFeedResponseHooked = true;

        String[] listMethods = {"getAwemeList", "getItems", "getFriendsFeedList"};
        for (String methodName : listMethods) {
            try {
                XposedHelpers.findAndHookMethod(friendsFeedResponseClass, methodName, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object result = param.getResult();
                        if (result instanceof List) {
                            List<?> list = (List<?>) result;
                            if (list.isEmpty()) return;
                            if (Boolean.TRUE.equals(XposedHelpers.getAdditionalInstanceField(list, "tiktok_enhancer_clean"))) {
                                return;
                            }
                            List<Object> clean = filterAwemeList(list, param.thisObject);
                            if (clean.size() != list.size()) {
                                param.setResult(clean);
                            } else {
                                XposedHelpers.setAdditionalInstanceField(list, "tiktok_enhancer_clean", Boolean.TRUE);
                            }
                        }
                    }
                });
                log("Hooked FriendsFeedResponse." + methodName + "()");
            } catch (Throwable ignored) {}
        }
    }

    private static void hookFeedApis(ClassLoader classLoader) {
        if (sFeedApisHooked || classLoader == null) return;
        sFeedApisHooked = true;

        String[] apiClasses = {
                "com.ss.android.ugc.aweme.feed.FeedApiService",
                "com.ss.android.ugc.aweme.feed.api.FeedApi",
                "com.ss.android.ugc.aweme.feed.cache.IFeedApi"
        };

        for (String className : apiClasses) {
            try {
                Class<?> clazz = XposedHelpers.findClassIfExists(className, classLoader);
                if (clazz == null) continue;

                for (Method method : clazz.getDeclaredMethods()) {
                    if ("com.ss.android.ugc.aweme.feed.model.FeedItemList".equals(method.getReturnType().getName())) {
                        XposedBridge.hookMethod(method, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                Object feedItemList = param.getResult();
                                if (feedItemList == null) return;

                                try {
                                    Object itemsObj = XposedHelpers.callMethod(feedItemList, "getItems");
                                    if (itemsObj instanceof List) {
                                        List<?> list = (List<?>) itemsObj;
                                        if (Boolean.TRUE.equals(XposedHelpers.getAdditionalInstanceField(list, "tiktok_enhancer_clean"))) {
                                            return;
                                        }

                                        List<Object> clean = filterAwemeList(list, feedItemList);

                                        if (clean.size() != list.size()) {
                                            try {
                                                XposedHelpers.setBooleanField(feedItemList, "hasAd", false);
                                            } catch (Throwable ignored) {}
                                            try {
                                                XposedHelpers.setObjectField(feedItemList, "items", clean);
                                            } catch (Throwable ignored) {}
                                        }
                                    }
                                } catch (Throwable ignored) {}
                            }
                        });
                    }
                }
            } catch (Throwable ignored) {}
        }
    }

    public static boolean isProfileOrDetailContext(Object contextObj) {
        if (contextObj == null) return false;
        String className = contextObj.getClass().getName();
        if (className.contains("Profile")
                || className.contains("User")
                || className.contains("Detail")
                || className.contains("AwemeList")
                || className.contains("Favorite")
                || className.contains("Music")
                || className.contains("Challenge")
                || className.contains("Search")) {
            return true;
        }
        try {
            String dataUserId = getSafeString(contextObj, "getDataUserId", "dataUserId");
            if (dataUserId != null && !dataUserId.trim().isEmpty()) {
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
            Object author = getSafeObject(aweme, "getAuthor", "author");
            if (author == null) continue;
            String uid = getSafeString(author, "getUid", "uid");
            if (uid == null || uid.isEmpty()) continue;
            if (firstUid == null) {
                firstUid = uid;
            } else if (!firstUid.equals(uid)) {
                return false;
            }
            checked++;
            if (checked >= 3) break;
        }
        return checked >= 2;
    }

    private static boolean isRecommendFeed(Object feedItemList) {
        if (feedItemList == null) return false;
        if (isProfileOrDetailContext(feedItemList)) return false;
        String className = feedItemList.getClass().getName();
        if (className.contains("RecommendFeedFragmentPanel") || className.contains("FullFeedFragmentPanel")) {
            return true;
        }
        try {
            Object ft = XposedHelpers.callMethod(feedItemList, "getFeedType");
            if (ft instanceof Number) {
                return ((Number) ft).intValue() == 0;
            }
        } catch (Throwable ignored) {}
        try {
            Object ft = XposedHelpers.getObjectField(feedItemList, "feedType");
            if (ft instanceof Number) {
                return ((Number) ft).intValue() == 0;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static List<Object> filterAwemeList(List<?> sourceList, Object feedItemList) {
        if (sourceList == null || sourceList.isEmpty()) {
            return new ArrayList<>();
        }

        if (Boolean.TRUE.equals(XposedHelpers.getAdditionalInstanceField(sourceList, "tiktok_enhancer_clean"))) {
            @SuppressWarnings("unchecked")
            List<Object> alreadyClean = (List<Object>) sourceList;
            return alreadyClean;
        }

        if (!MainHook.isEnabled()) {
            @SuppressWarnings("unchecked")
            List<Object> raw = (List<Object>) sourceList;
            return raw;
        }

        MainHook.checkConfigRefreshAsync(null);

        List<Object> kept = new ArrayList<>(sourceList.size());
        boolean isChina = MainHook.isChinaPackage();
        boolean isProfile = isProfileOrDetailContext(feedItemList) || isSingleAuthorProfileFeed(sourceList);
        if (isProfile) {
            log("Detected profile/creator feed (" + sourceList.size() + " items) - bypassing recommendation and country/language filters");
        }

        boolean hideAds = MainHook.isHideAdsEnabled();
        boolean strictRegion = !isProfile && !isChina && MainHook.isStrictForceRegionEnabled();
        boolean smartRegion = !isProfile && !isChina && !strictRegion && MainHook.isForceRegionEnabled() && isRecommendFeed(feedItemList);
        boolean anyRegionFilter = strictRegion || smartRegion;
        String targetRegion = MainHook.getTargetCountryIso();
        boolean blockCountries = !isProfile && !isChina && MainHook.isBlockCountriesEnabled();
        Set<String> blockedCountries = blockCountries ? MainHook.getBlockedCountries() : Collections.emptySet();
        boolean hasBlockedCountries = blockCountries && blockedCountries != null && !blockedCountries.isEmpty();
        BlockedCountryMatcher matcher = hasBlockedCountries ? getMatcher(blockedCountries) : null;

        boolean lockedRegionFilter = !isProfile && !isChina && MainHook.isLockedRegionFilterEnabled();
        boolean languageFilter = !isProfile && !isChina && MainHook.isLanguageFilterEnabled();
        Set<String> allowedLanguages = languageFilter ? MainHook.getAllowedLanguages() : Collections.emptySet();
        boolean hasAllowedLanguages = languageFilter && allowedLanguages != null && !allowedLanguages.isEmpty();

        int totalBefore = sourceList.size();
        int blockedCount = 0;
        int adCount = 0;
        int regionFilterCount = 0;
        int lockedRegionCount = 0;
        int languageFilterCount = 0;

        for (Object item : sourceList) {
            if (item == null) continue;

            if (hideAds && isAdAweme(item)) {
                adCount++;
                continue;
            }

            if (matcher != null && matcher.matchesAweme(item)) {
                blockedCount++;
                continue;
            }

            if (lockedRegionFilter && shouldFilterForLockedRegion(item, targetRegion)) {
                lockedRegionCount++;
                continue;
            }

            if (hasAllowedLanguages && shouldFilterForLanguage(item, allowedLanguages)) {
                languageFilterCount++;
                continue;
            }

            if (anyRegionFilter && shouldFilterForRegion(item, targetRegion)) {
                regionFilterCount++;
                continue;
            }

            kept.add(item);
        }

        if (blockedCount > 0 || adCount > 0 || regionFilterCount > 0 || lockedRegionCount > 0 || languageFilterCount > 0) {
            log("Feed filtered: " + totalBefore + " -> " + kept.size() + " kept (blocked=" + blockedCount
                    + ", ads=" + adCount + ", nonTargetRegion=" + regionFilterCount
                    + ", nonTargetLockedRegion=" + lockedRegionCount
                    + ", nonAllowedLang=" + languageFilterCount + ")");
        }

        if (!strictRegion && kept.isEmpty() && !sourceList.isEmpty()) {
            // Priority 1: rescue items that pass ad, blocked country, locked region, and language filters
            for (Object item : sourceList) {
                if (item != null
                        && !(hideAds && isAdAweme(item))
                        && !(matcher != null && matcher.matchesAweme(item))
                        && !(lockedRegionFilter && shouldFilterForLockedRegion(item, targetRegion))
                        && !(hasAllowedLanguages && shouldFilterForLanguage(item, allowedLanguages))) {
                    kept.add(item);
                }
            }

            // Priority 2: rescue items that are not ads and not from blocked countries
            if (kept.isEmpty()) {
                for (Object item : sourceList) {
                    if (item != null
                            && !(hideAds && isAdAweme(item))
                            && !(matcher != null && matcher.matchesAweme(item))) {
                        kept.add(item);
                        if (kept.size() >= 2) break;
                    }
                }
            }

            // Priority 3: rescue non-ad items
            if (kept.isEmpty()) {
                for (Object item : sourceList) {
                    if (item != null && !(hideAds && isAdAweme(item))) {
                        kept.add(item);
                        if (kept.size() >= 2) break;
                    }
                }
            }

            // Priority 4: absolute fallback to preserve feed continuity (strictly excluding ads & full-screen card views)
            if (kept.isEmpty() && !sourceList.isEmpty()) {
                for (Object item : sourceList) {
                    if (item != null && !(hideAds && isAdAweme(item))) {
                        int at = 0;
                        try {
                            Number n = (Number) getSafeObject(item, "getAwemeType", "awemeType");
                            if (n != null) at = n.intValue();
                        } catch (Throwable ignored) {}

                        // Exclude pure recommendation card placeholders (4004, 101, 102, 103, 301, 302)
                        if (at != 4004 && at != 101 && at != 102 && at != 103 && at != 301 && at != 302) {
                            kept.add(item);
                            if (kept.size() >= 2) break;
                        }
                    }
                }
            }

            if (!kept.isEmpty()) {
                log("Feed starvation safety floor activated: preserved " + kept.size() + " items to prevent feed reload error");
            }
        }

        // Absolute emergency fallback: ensure TikTok feed state machine never starves into "Something went wrong"
        if (kept.isEmpty() && !sourceList.isEmpty()) {
            for (Object item : sourceList) {
                if (item != null && !(hideAds && isAdAweme(item))) {
                    kept.add(item);
                    if (kept.size() >= 1) break;
                }
            }
            if (!kept.isEmpty()) {
                log("Feed emergency continuity guard: preserved " + kept.size() + " non-ad video(s)");
            }
        }

        XposedHelpers.setAdditionalInstanceField(kept, "tiktok_enhancer_clean", Boolean.TRUE);

        for (Object item : kept) {
            try {
                WatermarkHook.unlockAwemeRestrictions(item);
                WatermarkHook.cleanAweme(item);
            } catch (Throwable ignored) {}
        }

        return kept;
    }

    private static BlockedCountryMatcher getMatcher(Set<String> blockedCountries) {
        if (blockedCountries == null || blockedCountries.isEmpty()) return null;
        BlockedCountryMatcher matcher = sCachedMatcher;
        if (matcher != null && blockedCountries.equals(sLastBlockedSet)) {
            return matcher;
        }
        matcher = new BlockedCountryMatcher(blockedCountries);
        sLastBlockedSet = new HashSet<>(blockedCountries);
        sCachedMatcher = matcher;
        return matcher;
    }

    private static boolean isAdAweme(Object aweme) {
        try {
            boolean isAd = (boolean) XposedHelpers.callMethod(aweme, "isAd");
            if (isAd) return true;
        } catch (Throwable ignored) {}

        try {
            Object rawAd = XposedHelpers.callMethod(aweme, "getAwemeRawAd");
            if (rawAd != null) return true;
        } catch (Throwable ignored) {}

        try {
            int type = (int) XposedHelpers.callMethod(aweme, "getAwemeType");
            if (type == 2 || type == 101) return true;
        } catch (Throwable ignored) {}

        try {
            boolean fakeUser = (boolean) XposedHelpers.callMethod(aweme, "withFakeUser");
            if (fakeUser) return true;
        } catch (Throwable ignored) {}

        try {
            boolean promoMusic = (boolean) XposedHelpers.callMethod(aweme, "isWithPromotionalMusic");
            if (promoMusic) return true;
        } catch (Throwable ignored) {}

        try {
            Object isLiveAd = XposedHelpers.callMethod(aweme, "isLiveAd");
            if (Boolean.TRUE.equals(isLiveAd)) return true;
        } catch (Throwable ignored) {}

        try {
            Object commInfo = XposedHelpers.callMethod(aweme, "getCommercialToolInfo");
            if (commInfo != null) {
                Boolean hasAd = (Boolean) XposedHelpers.callMethod(commInfo, "hasAdTag");
                if (Boolean.TRUE.equals(hasAd)) return true;
            }
        } catch (Throwable ignored) {}

        return false;
    }



    private static boolean shouldFilterForRegion(Object aweme, String targetRegion) {
        if (targetRegion == null || targetRegion.trim().isEmpty()) return false;
        String target = targetRegion.trim();

        // 1. Author locked account region takes priority
        Object author = getSafeObject(aweme, "getAuthor", "author");
        if (author != null) {
            String aAccount = getSafeString(author, "getAccountRegion", "accountRegion");
            if (aAccount != null && !aAccount.isEmpty()) {
                return !aAccount.equalsIgnoreCase(target);
            }
            String aIso = getSafeString(author, "getIsoCountryCode", "isoCountryCode");
            if (aIso != null && !aIso.isEmpty()) {
                return !aIso.equalsIgnoreCase(target);
            }
            String aRegion = getSafeString(author, "getRegion", "region");
            if (aRegion != null && !aRegion.isEmpty()) {
                return !aRegion.equalsIgnoreCase(target);
            }
        }

        // 2. Check forwardItem (repost) author
        Object forwardItem = getSafeObject(aweme, "getForwardItem", "forwardItem");
        if (forwardItem != null) {
            Object fAuthor = getSafeObject(forwardItem, "getAuthor", "author");
            if (fAuthor != null) {
                String fAccount = getSafeString(fAuthor, "getAccountRegion", "accountRegion");
                if (fAccount != null && !fAccount.isEmpty()) {
                    return !fAccount.equalsIgnoreCase(target);
                }
                String fIso = getSafeString(fAuthor, "getIsoCountryCode", "isoCountryCode");
                if (fIso != null && !fIso.isEmpty()) {
                    return !fIso.equalsIgnoreCase(target);
                }
            }
        }

        // 3. Fallback to Aweme region
        String region = getSafeString(aweme, "getRegion", "region");
        if (region != null && !region.isEmpty()) {
            return !region.equalsIgnoreCase(target);
        }

        return false;
    }

    private static boolean shouldFilterForLockedRegion(Object aweme, String targetRegion) {
        if (targetRegion == null || targetRegion.trim().isEmpty()) return false;
        String target = targetRegion.trim();

        // 1. Author locked account region
        Object author = getSafeObject(aweme, "getAuthor", "author");
        if (author != null) {
            String accountRegion = getSafeString(author, "getAccountRegion", "accountRegion");
            if (accountRegion != null && !accountRegion.isEmpty()) {
                return !accountRegion.equalsIgnoreCase(target);
            }
            String iso = getSafeString(author, "getIsoCountryCode", "isoCountryCode");
            if (iso != null && !iso.isEmpty()) {
                return !iso.equalsIgnoreCase(target);
            }
        }

        // 2. Forwarded / reposted item original author
        Object forwardItem = getSafeObject(aweme, "getForwardItem", "forwardItem");
        if (forwardItem != null) {
            Object fAuthor = getSafeObject(forwardItem, "getAuthor", "author");
            if (fAuthor != null) {
                String fAccount = getSafeString(fAuthor, "getAccountRegion", "accountRegion");
                if (fAccount != null && !fAccount.isEmpty()) {
                    return !fAccount.equalsIgnoreCase(target);
                }
            }
        }

        // 3. Picked users (duets/stitches)
        Object pickedUsers = getSafeObject(aweme, "getPickedUsers", "pickedUsers");
        if (pickedUsers instanceof List) {
            for (Object u : (List<?>) pickedUsers) {
                if (u != null) {
                    String pAccount = getSafeString(u, "getAccountRegion", "accountRegion");
                    if (pAccount != null && !pAccount.isEmpty() && !pAccount.equalsIgnoreCase(target)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean shouldFilterForLanguage(Object aweme, Set<String> allowedLanguages) {
        if (allowedLanguages == null || allowedLanguages.isEmpty()) return false;

        Set<String> detectedLangs = extractLanguages(aweme);
        if (detectedLangs.isEmpty()) {
            return false;
        }

        for (String lang : detectedLangs) {
            if (allowedLanguages.contains(lang)) {
                return false;
            }
            // Indonesian code aliasing: 'in' is legacy 'id'
            if ("in".equals(lang) && allowedLanguages.contains("id")) return false;
            if ("id".equals(lang) && allowedLanguages.contains("in")) return false;
            // Hebrew code aliasing: 'iw' is legacy 'he'
            if ("iw".equals(lang) && allowedLanguages.contains("he")) return false;
            if ("he".equals(lang) && allowedLanguages.contains("iw")) return false;
            // Chinese prefix match: 'zh-hant' vs 'zh'
            if (lang.startsWith("zh") && (allowedLanguages.contains("zh") || allowedLanguages.contains("zh-hant"))) return false;
        }

        return true;
    }

    private static Set<String> extractLanguages(Object aweme) {
        if (aweme == null) return Collections.emptySet();
        Set<String> langs = new HashSet<>();

        addNormalizedLang(langs, getSafeString(aweme, "getDescLanguage", "descLanguage"));
        addNormalizedLang(langs, getSafeString(aweme, "getPhotoTitleLanguageCode", "photoTitleLanguageCode"));
        addNormalizedLang(langs, getSafeString(aweme, "getContentLanguage", "contentLanguage"));
        addNormalizedLang(langs, getSafeString(aweme, "getTextStickerMajorityLang", "textStickerMajorityLang"));

        Object video = getSafeObject(aweme, "getVideo", "video");
        if (video != null) {
            Object captionModel = getSafeObject(video, "getCaptionModel", "captionModel");
            if (captionModel != null) {
                Object origCaptionLang = getSafeObject(captionModel, "getOriginalCaptionLanguage", "originalCaptionLanguage");
                if (origCaptionLang != null) {
                    addNormalizedLang(langs, getSafeString(origCaptionLang, "getLanguageCode", "languageCode"));
                }
                Object captionList = getSafeObject(captionModel, "getCaptionList", "captionList");
                if (captionList instanceof List) {
                    for (Object capItem : (List<?>) captionList) {
                        if (capItem != null) {
                            addNormalizedLang(langs, getSafeString(capItem, "getLanguageCode", "languageCode"));
                        }
                    }
                }
            }
        }

        Object author = getSafeObject(aweme, "getAuthor", "author");
        if (author != null) {
            addNormalizedLang(langs, getSafeString(author, "getLanguage", "language"));
            addNormalizedLang(langs, getSafeString(author, "getSignatureLanguage", "signatureLanguage"));
        }

        Object forwardItem = getSafeObject(aweme, "getForwardItem", "forwardItem");
        if (forwardItem != null && forwardItem != aweme) {
            addNormalizedLang(langs, getSafeString(forwardItem, "getDescLanguage", "descLanguage"));
            addNormalizedLang(langs, getSafeString(forwardItem, "getPhotoTitleLanguageCode", "photoTitleLanguageCode"));
            addNormalizedLang(langs, getSafeString(forwardItem, "getTextStickerMajorityLang", "textStickerMajorityLang"));
            Object fAuthor = getSafeObject(forwardItem, "getAuthor", "author");
            if (fAuthor != null) {
                addNormalizedLang(langs, getSafeString(fAuthor, "getLanguage", "language"));
            }
        }

        return langs;
    }

    private static void addNormalizedLang(Set<String> set, String code) {
        if (code == null) return;
        String clean = code.trim().toLowerCase(Locale.ROOT);
        if (clean.isEmpty() || "und".equals(clean) || "un".equals(clean)) return;
        if (clean.contains("-")) {
            set.add(clean);
            String prefix = clean.split("-")[0];
            if (!prefix.isEmpty()) set.add(prefix);
        } else if (clean.contains("_")) {
            set.add(clean);
            String prefix = clean.split("_")[0];
            if (!prefix.isEmpty()) set.add(prefix);
        } else {
            set.add(clean);
        }
        if ("in".equals(clean)) set.add("id");
        if ("id".equals(clean)) set.add("in");
    }

    private static String getSafeString(Object obj, String getterName, String fieldName) {
        if (obj == null) return null;
        if (getterName != null) {
            try {
                Object res = XposedHelpers.callMethod(obj, getterName);
                if (res instanceof String) {
                    String s = ((String) res).trim();
                    if (!s.isEmpty()) return s;
                }
            } catch (Throwable ignored) {}
        }
        if (fieldName != null) {
            try {
                Object res = XposedHelpers.getObjectField(obj, fieldName);
                if (res instanceof String) {
                    String s = ((String) res).trim();
                    if (!s.isEmpty()) return s;
                }
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static Object getSafeObject(Object obj, String getterName, String fieldName) {
        if (obj == null) return null;
        if (getterName != null) {
            try {
                Object res = XposedHelpers.callMethod(obj, getterName);
                if (res != null) return res;
            } catch (Throwable ignored) {}
        }
        if (fieldName != null) {
            try {
                Object res = XposedHelpers.getObjectField(obj, fieldName);
                if (res != null) return res;
            } catch (Throwable ignored) {}
        }
        return null;
    }

    public static class BlockedCountryMatcher {
        private final Set<String> mIsoCodes = new HashSet<>();
        private final Set<String> mCountryNames = new HashSet<>();
        private final List<String> mTextMatchTerms = new ArrayList<>();

        private static final Map<String, String[]> KNOWN_ALIASES = new HashMap<>();

        static {
            KNOWN_ALIASES.put("ru", new String[]{"russia", "russian federation", "россия", "москва", "moscow", "saint petersburg", "st. petersburg", "st petersburg", "санкт-петербург"});
            KNOWN_ALIASES.put("pk", new String[]{"pakistan", "پاکستان", "karachi", "lahore", "islamabad", "rawalpindi", "faisalabad", "peshawar"});
            KNOWN_ALIASES.put("in", new String[]{"india", "bharat", "hindustan", "भारत", "mumbai", "delhi", "bangalore", "bengaluru", "hyderabad", "kolkata"});
            KNOWN_ALIASES.put("id", new String[]{
                    "indonesia", "indonesian", "jakarta", "surabaya", "bandung", "medan",
                    "bali", "semarang", "makassar", "palembang", "tangerang", "depok",
                    "bekasi", "bogor", "yogyakarta", "jogja", "malang", "solo", "surakarta",
                    "batam", "pekanbaru", "lampung", "padang", "denpasar", "samarinda",
                    "banjarmasin", "balikpapan", "pontianak", "manado", "mataram", "cirebon",
                    "aceh", "papua", "jawa", "sumatra", "kalimantan", "sulawesi",
                    "fypindonesia", "tiktokindonesia", "viralindonesia", "fypindo",
                    "wkwk", "wkwkwk", "ngakak", "banget", "gimana", "kalian", "nggak", "ngga",
                    "receh", "dagelan", "selebtwit"
            });
            KNOWN_ALIASES.put("ua", new String[]{"ukraine", "україна", "украина", "kyiv", "kiev", "kharkiv", "odesa"});
            KNOWN_ALIASES.put("by", new String[]{"belarus", "беларусь", "minsk"});
            KNOWN_ALIASES.put("kz", new String[]{"kazakhstan", "казахстан", "almaty", "astana"});
            KNOWN_ALIASES.put("uz", new String[]{"uzbekistan", "ўзбекистон", "tashkent"});
            KNOWN_ALIASES.put("cn", new String[]{"china", "中国", "beijing", "shanghai", "guangzhou", "shenzhen"});
            KNOWN_ALIASES.put("ir", new String[]{"iran", "ایران", "tehran"});
            KNOWN_ALIASES.put("bd", new String[]{"bangladesh", "বাংলাদেশ", "dhaka"});
            KNOWN_ALIASES.put("ph", new String[]{"philippines", "pilipinas", "manila"});
            KNOWN_ALIASES.put("vn", new String[]{"vietnam", "việt nam", "hanoi", "saigon", "ho chi minh"});
            KNOWN_ALIASES.put("tr", new String[]{"turkey", "türkiye", "istanbul", "ankara"});
            KNOWN_ALIASES.put("sa", new String[]{"saudi arabia", "riyadh", "jeddah"});
            KNOWN_ALIASES.put("ae", new String[]{"united arab emirates", "emirates", "dubai", "abu dhabi"});
            KNOWN_ALIASES.put("eg", new String[]{"egypt", "cairo", "alexandria"});
            KNOWN_ALIASES.put("il", new String[]{"israel", "jerusalem", "tel aviv"});
            KNOWN_ALIASES.put("br", new String[]{"brazil", "brasil", "sao paulo", "rio de janeiro"});
            KNOWN_ALIASES.put("mx", new String[]{"mexico", "méxico"});
            KNOWN_ALIASES.put("de", new String[]{"germany", "deutschland", "berlin", "munich"});
            KNOWN_ALIASES.put("fr", new String[]{"france", "paris"});
            KNOWN_ALIASES.put("gb", new String[]{"united kingdom", "great britain", "britain", "england", "scotland", "wales", "london"});
            KNOWN_ALIASES.put("us", new String[]{"united states", "united states of america", "america", "usa"});
        }

        public BlockedCountryMatcher(Set<String> blockedIsoSet) {
            if (blockedIsoSet == null) return;

            for (String iso : blockedIsoSet) {
                if (iso == null) continue;
                String cleanIso = iso.trim().toLowerCase(Locale.ROOT);
                if (cleanIso.isEmpty()) continue;

                mIsoCodes.add(cleanIso);

                try {
                    Locale loc = new Locale("", cleanIso.toUpperCase(Locale.ROOT));
                    try {
                        String iso3 = loc.getISO3Country();
                        if (iso3 != null && !iso3.trim().isEmpty()) {
                            mIsoCodes.add(iso3.trim().toLowerCase(Locale.ROOT));
                        }
                    } catch (Throwable ignored) {}

                    String engName = loc.getDisplayCountry(Locale.ENGLISH);
                    if (engName != null && !engName.trim().isEmpty() && !engName.equalsIgnoreCase(cleanIso)) {
                        mCountryNames.add(engName.trim().toLowerCase(Locale.ROOT));
                    }

                    String nativeName = loc.getDisplayCountry(loc);
                    if (nativeName != null && !nativeName.trim().isEmpty() && !nativeName.equalsIgnoreCase(cleanIso)) {
                        mCountryNames.add(nativeName.trim().toLowerCase(Locale.ROOT));
                    }
                } catch (Throwable ignored) {}

                CountryPreset preset = CountryPreset.findByIso(cleanIso);
                if (preset != null && preset.getCountryName() != null) {
                    mCountryNames.add(preset.getCountryName().trim().toLowerCase(Locale.ROOT));
                }

                String[] aliases = KNOWN_ALIASES.get(cleanIso);
                if (aliases != null) {
                    for (String alias : aliases) {
                        mCountryNames.add(alias.trim().toLowerCase(Locale.ROOT));
                    }
                }
            }

            for (String name : mCountryNames) {
                if (name != null && name.length() >= 3 && !mTextMatchTerms.contains(name)) {
                    mTextMatchTerms.add(name);
                }
            }
            Collections.sort(mTextMatchTerms, (a, b) -> Integer.compare(b.length(), a.length()));
        }

        public boolean matchesCode(String code) {
            if (code == null) return false;
            String clean = code.trim().toLowerCase(Locale.ROOT);
            if (clean.isEmpty()) return false;

            if (mIsoCodes.contains(clean) || mCountryNames.contains(clean)) {
                return true;
            }

            if (clean.contains("-") || clean.contains("_") || clean.contains(".")) {
                String[] tokens = clean.split("[-_.]");
                for (String token : tokens) {
                    String t = token.trim();
                    if (!t.isEmpty() && (mIsoCodes.contains(t) || mCountryNames.contains(t))) {
                        return true;
                    }
                }
            }
            return false;
        }

        public boolean matchesLanguageCode(String langCode) {
            if (langCode == null) return false;
            String clean = langCode.trim().toLowerCase(Locale.ROOT);
            if (clean.isEmpty()) return false;

            if (mIsoCodes.contains(clean)) return true;

            if ("in".equals(clean) && (mIsoCodes.contains("id") || mIsoCodes.contains("idn"))) return true;

            if ("iw".equals(clean) && (mIsoCodes.contains("il") || mIsoCodes.contains("isr"))) return true;

            if ("uk".equals(clean) && (mIsoCodes.contains("ua") || mIsoCodes.contains("ukr"))) return true;

            if (clean.contains("-") || clean.contains("_")) {
                String[] parts = clean.split("[-_]");
                for (String part : parts) {
                    String p = part.trim();
                    if (p.isEmpty()) continue;
                    if (mIsoCodes.contains(p)) return true;
                    if ("in".equals(p) && (mIsoCodes.contains("id") || mIsoCodes.contains("idn"))) return true;
                    if ("uk".equals(p) && (mIsoCodes.contains("ua") || mIsoCodes.contains("ukr"))) return true;
                }
            }

            return false;
        }

        public boolean matchesText(String text) {
            if (text == null) return false;
            String clean = text.trim().toLowerCase(Locale.ROOT);
            if (clean.isEmpty()) return false;

            if (mIsoCodes.contains(clean) || mCountryNames.contains(clean)) {
                return true;
            }

            for (String term : mTextMatchTerms) {
                if (containsWord(clean, term)) {
                    return true;
                }
            }

            for (String iso : mIsoCodes) {
                if (iso.length() == 2 && containsHashtag(clean, iso)) {
                    return true;
                }
            }

            if (mIsoCodes.contains("id") || mIsoCodes.contains("idn")) {
                if (clean.contains("+62") || containsWord(clean, "idr") || containsWord(clean, "rupiah")) {
                    return true;
                }
            }

            return false;
        }

        private static boolean containsHashtag(String text, String tag) {
            if (text == null || tag == null || tag.isEmpty()) return false;
            String target = "#" + tag;
            int idx = 0;
            int len = target.length();
            int textLen = text.length();
            while ((idx = text.indexOf(target, idx)) != -1) {
                boolean endBoundary = (idx + len == textLen) || !Character.isLetterOrDigit(text.charAt(idx + len));
                if (endBoundary) return true;
                idx += len;
            }
            return false;
        }

        private static boolean containsWord(String text, String word) {
            if (text == null || word == null || word.isEmpty()) return false;
            int idx = 0;
            int wordLen = word.length();
            int textLen = text.length();
            while ((idx = text.indexOf(word, idx)) != -1) {
                boolean startBoundary = (idx == 0) || !Character.isLetterOrDigit(text.charAt(idx - 1));
                boolean endBoundary = (idx + wordLen == textLen) || !Character.isLetterOrDigit(text.charAt(idx + wordLen));
                if (startBoundary && endBoundary) {
                    return true;
                }
                idx += wordLen;
            }
            return false;
        }

        private boolean matchesCommerce(String data) {
            if (data == null || data.isEmpty()) return false;
            String lower = data.toLowerCase(Locale.ROOT);
            for (String iso : mIsoCodes) {
                if (iso.length() == 2) {
                    if (lower.contains("\"region\":\"" + iso + "\"")
                            || lower.contains("\"country\":\"" + iso + "\"")
                            || lower.contains("\"country_code\":\"" + iso + "\"")) {
                        return true;
                    }
                }
            }
            if (mIsoCodes.contains("id") || mIsoCodes.contains("idn")) {
                if (lower.contains("\"currency\":\"idr\"")
                        || lower.contains("\"currency_code\":\"idr\"")
                        || lower.contains("currency=idr")) {
                    return true;
                }
            }
            return false;
        }

        public boolean matchesAweme(Object aweme) {
            if (aweme == null) return false;

            String aid = getSafeString(aweme, "getAid", "aid");

            String region = getSafeString(aweme, "getRegion", "region");
            if (matchesCode(region)) {
                AdsHook.log("Blocked Aweme [" + aid + "] by region: " + region);
                return true;
            }

            Object geoObj = getSafeObject(aweme, "getGeofencingRegions", "geofencingRegions");
            if (geoObj instanceof List) {
                for (Object g : (List<?>) geoObj) {
                    if (g instanceof String && matchesCode((String) g)) {
                        AdsHook.log("Blocked Aweme [" + aid + "] by geofencing region: " + g);
                        return true;
                    }
                }
            }

            String descLang = getSafeString(aweme, "getDescLanguage", "descLanguage");
            if (matchesLanguageCode(descLang)) {
                AdsHook.log("Blocked Aweme [" + aid + "] by descLanguage: " + descLang);
                return true;
            }

            String photoTitleLang = getSafeString(aweme, "getPhotoTitleLanguageCode", "photoTitleLanguageCode");
            if (matchesLanguageCode(photoTitleLang)) {
                AdsHook.log("Blocked Aweme [" + aid + "] by photoTitleLanguageCode: " + photoTitleLang);
                return true;
            }

            String contentLang = getSafeString(aweme, "getContentLanguage", "contentLanguage");
            if (matchesLanguageCode(contentLang)) {
                AdsHook.log("Blocked Aweme [" + aid + "] by contentLanguage: " + contentLang);
                return true;
            }

            String textStickerLang = getSafeString(aweme, "getTextStickerMajorityLang", "textStickerMajorityLang");
            if (matchesLanguageCode(textStickerLang)) {
                AdsHook.log("Blocked Aweme [" + aid + "] by textStickerMajorityLang: " + textStickerLang);
                return true;
            }

            Object video = getSafeObject(aweme, "getVideo", "video");
            if (video != null) {
                Object captionModel = getSafeObject(video, "getCaptionModel", "captionModel");
                if (captionModel != null) {
                    Object origCaptionLang = getSafeObject(captionModel, "getOriginalCaptionLanguage", "originalCaptionLanguage");
                    if (origCaptionLang != null) {
                        String capLangCode = getSafeString(origCaptionLang, "getLanguageCode", "languageCode");
                        if (matchesLanguageCode(capLangCode)) {
                            AdsHook.log("Blocked Aweme [" + aid + "] by originalCaptionLanguage: " + capLangCode);
                            return true;
                        }
                    }
                    Object captionList = getSafeObject(captionModel, "getCaptionList", "captionList");
                    if (captionList instanceof List) {
                        for (Object capItem : (List<?>) captionList) {
                            if (capItem != null) {
                                String clCode = getSafeString(capItem, "getLanguageCode", "languageCode");
                                if (matchesLanguageCode(clCode)) {
                                    AdsHook.log("Blocked Aweme [" + aid + "] by captionList language: " + clCode);
                                    return true;
                                }
                            }
                        }
                    }
                }
            }

            String desc = getSafeString(aweme, "getDesc", "desc");
            if (matchesText(desc)) {
                String preview = desc != null && desc.length() > 60 ? desc.substring(0, 60) + "..." : desc;
                AdsHook.log("Blocked Aweme [" + aid + "] by desc text: " + preview);
                return true;
            }

            Object author = getSafeObject(aweme, "getAuthor", "author");
            if (checkUser(author, aid, "author")) return true;

            Object forwardItem = getSafeObject(aweme, "getForwardItem", "forwardItem");
            if (forwardItem != null && forwardItem != aweme) {
                Object fAuthor = getSafeObject(forwardItem, "getAuthor", "author");
                if (checkUser(fAuthor, aid, "forwardItem.author")) return true;
                String fDescLang = getSafeString(forwardItem, "getDescLanguage", "descLanguage");
                if (matchesLanguageCode(fDescLang)) {
                    AdsHook.log("Blocked Aweme [" + aid + "] by forwardItem.descLanguage: " + fDescLang);
                    return true;
                }
                String fDesc = getSafeString(forwardItem, "getDesc", "desc");
                if (matchesText(fDesc)) {
                    AdsHook.log("Blocked Aweme [" + aid + "] by forwardItem.desc text");
                    return true;
                }
            }

            Object pickedUsers = getSafeObject(aweme, "getPickedUsers", "pickedUsers");
            if (pickedUsers instanceof List) {
                for (Object u : (List<?>) pickedUsers) {
                    if (checkUser(u, aid, "pickedUser")) return true;
                }
            }

            Object nearby = getSafeObject(aweme, "getNearbyInfo", "nearbyInfo");
            if (nearby != null) {
                String nearbyRegion = getSafeString(nearby, "getNearbyRegion", "nearbyRegion");
                if (matchesCode(nearbyRegion)) {
                    AdsHook.log("Blocked Aweme [" + aid + "] by nearbyRegion: " + nearbyRegion);
                    return true;
                }
                String eventRegion = getSafeString(nearby, "getEventRegion", "eventRegion");
                if (matchesCode(eventRegion)) {
                    AdsHook.log("Blocked Aweme [" + aid + "] by eventRegion: " + eventRegion);
                    return true;
                }
            }

            Object poi = getSafeObject(aweme, "getPoiDataStruct", "poiDataStruct");
            if (poi != null) {
                String locDesc = getSafeString(poi, "getLocationDesc", "locationDesc");
                if (matchesText(locDesc)) {
                    AdsHook.log("Blocked Aweme [" + aid + "] by POI locDesc: " + locDesc);
                    return true;
                }

                Object addrInfo = getSafeObject(poi, "getAddressInfo", "addressInfo");
                if (addrInfo != null) {
                    String regCode = getSafeString(addrInfo, "getRegionCode", "regionCode");
                    if (matchesCode(regCode)) {
                        AdsHook.log("Blocked Aweme [" + aid + "] by POI regionCode: " + regCode);
                        return true;
                    }
                    String cityName = getSafeString(addrInfo, "getCityName", "cityName");
                    if (matchesText(cityName)) {
                        AdsHook.log("Blocked Aweme [" + aid + "] by POI cityName: " + cityName);
                        return true;
                    }
                    String address = getSafeString(addrInfo, "getAddress", "address");
                    if (matchesText(address)) {
                        AdsHook.log("Blocked Aweme [" + aid + "] by POI address: " + address);
                        return true;
                    }
                    String country = getSafeString(addrInfo, "getCountry", "country");
                    if (matchesText(country)) {
                        AdsHook.log("Blocked Aweme [" + aid + "] by POI country: " + country);
                        return true;
                    }
                }

                String poiRegion = getSafeString(poi, "getRegion", "region");
                if (matchesCode(poiRegion)) {
                    AdsHook.log("Blocked Aweme [" + aid + "] by poiRegion: " + poiRegion);
                    return true;
                }
                String poiCountry = getSafeString(poi, "getCountry", "country");
                if (matchesText(poiCountry)) {
                    AdsHook.log("Blocked Aweme [" + aid + "] by poiCountry: " + poiCountry);
                    return true;
                }
            }

            String regionOfResidence = getSafeString(aweme, "getRegionOfResidence", "regionOfResidence");
            if (matchesCode(regionOfResidence)) {
                AdsHook.log("Blocked Aweme [" + aid + "] by regionOfResidence: " + regionOfResidence);
                return true;
            }

            String regionBlock = getSafeString(aweme, "getRegionBlock", "regionBlock");
            if (matchesCode(regionBlock)) {
                AdsHook.log("Blocked Aweme [" + aid + "] by regionBlock: " + regionBlock);
                return true;
            }

            String anchorsExtras = getSafeString(aweme, "getAnchorsExtras", "anchorsExtras");
            if (anchorsExtras != null && matchesCommerce(anchorsExtras)) {
                AdsHook.log("Blocked Aweme [" + aid + "] by commerce anchorsExtras");
                return true;
            }

            String ecomView = getSafeString(aweme, "getEcomAnchorViewObject", "ecomAnchorViewObject");
            if (ecomView != null && matchesCommerce(ecomView)) {
                AdsHook.log("Blocked Aweme [" + aid + "] by commerce ecomAnchorViewObject");
                return true;
            }

            Object music = getSafeObject(aweme, "getMusic", "music");
            if (music != null) {
                String musicAuthor = getSafeString(music, "getAuthorName", "authorName");
                if (matchesText(musicAuthor)) {
                    AdsHook.log("Blocked Aweme [" + aid + "] by musicAuthor: " + musicAuthor);
                    return true;
                }
                String musicOwnerNick = getSafeString(music, "getOwnerNickName", "ownerNickName");
                if (matchesText(musicOwnerNick)) {
                    AdsHook.log("Blocked Aweme [" + aid + "] by musicOwner: " + musicOwnerNick);
                    return true;
                }
            }

            return false;
        }

        private boolean checkUser(Object user, String aid, String source) {
            if (user == null) return false;

            String accountRegion = getSafeString(user, "getAccountRegion", "accountRegion");
            if (matchesCode(accountRegion)) {
                AdsHook.log("Blocked Aweme [" + aid + "] by " + source + ".accountRegion: " + accountRegion);
                return true;
            }

            String region = getSafeString(user, "getRegion", "region");
            if (matchesCode(region)) {
                AdsHook.log("Blocked Aweme [" + aid + "] by " + source + ".region: " + region);
                return true;
            }

            String isoCountryCode = getSafeString(user, "getIsoCountryCode", "isoCountryCode");
            if (matchesCode(isoCountryCode)) {
                AdsHook.log("Blocked Aweme [" + aid + "] by " + source + ".isoCountryCode: " + isoCountryCode);
                return true;
            }

            String language = getSafeString(user, "getLanguage", "language");
            if (matchesLanguageCode(language)) {
                AdsHook.log("Blocked Aweme [" + aid + "] by " + source + ".language: " + language);
                return true;
            }

            String lemon8Region = getSafeString(user, "getLemon8StoreRegion", "lemon8StoreRegion");
            if (matchesCode(lemon8Region)) {
                AdsHook.log("Blocked Aweme [" + aid + "] by " + source + ".lemon8StoreRegion: " + lemon8Region);
                return true;
            }

            String country = getSafeString(user, "getCountry", "country");
            if (matchesText(country)) {
                AdsHook.log("Blocked Aweme [" + aid + "] by " + source + ".country: " + country);
                return true;
            }

            String province = getSafeString(user, "getProvince", "province");
            if (matchesText(province)) {
                AdsHook.log("Blocked Aweme [" + aid + "] by " + source + ".province: " + province);
                return true;
            }

            String district = getSafeString(user, "getDistrict", "district");
            if (matchesText(district)) {
                AdsHook.log("Blocked Aweme [" + aid + "] by " + source + ".district: " + district);
                return true;
            }

            String bioLocation = getSafeString(user, "getBioLocation", "bioLocation");
            if (matchesText(bioLocation)) {
                AdsHook.log("Blocked Aweme [" + aid + "] by " + source + ".bioLocation: " + bioLocation);
                return true;
            }

            String cityName = getSafeString(user, "getCityName", "cityName");
            if (matchesText(cityName)) {
                AdsHook.log("Blocked Aweme [" + aid + "] by " + source + ".cityName: " + cityName);
                return true;
            }

            String deviceRegion = getSafeString(user, "getDeviceRegion", "deviceRegion");
            if (matchesCode(deviceRegion)) {
                AdsHook.log("Blocked Aweme [" + aid + "] by " + source + ".deviceRegion: " + deviceRegion);
                return true;
            }

            String storeRegion = getSafeString(user, "getStoreRegion", "storeRegion");
            if (matchesCode(storeRegion)) {
                AdsHook.log("Blocked Aweme [" + aid + "] by " + source + ".storeRegion: " + storeRegion);
                return true;
            }

            String ipRegion = getSafeString(user, "getIpRegion", "ipRegion");
            if (matchesCode(ipRegion)) {
                AdsHook.log("Blocked Aweme [" + aid + "] by " + source + ".ipRegion: " + ipRegion);
                return true;
            }

            String registerFrom = getSafeString(user, "getRegisterFrom", "registerFrom");
            if (matchesCode(registerFrom)) {
                AdsHook.log("Blocked Aweme [" + aid + "] by " + source + ".registerFrom: " + registerFrom);
                return true;
            }

            String sigLang = getSafeString(user, "getSignatureLanguage", "signatureLanguage");
            if (matchesLanguageCode(sigLang)) {
                AdsHook.log("Blocked Aweme [" + aid + "] by " + source + ".signatureLanguage: " + sigLang);
                return true;
            }

            String signature = getSafeString(user, "getSignature", "signature");
            if (matchesText(signature)) {
                AdsHook.log("Blocked Aweme [" + aid + "] by " + source + ".signature text");
                return true;
            }

            return false;
        }
    }

    private static void hookAwemeModel(ClassLoader classLoader) {
        if (sAwemeModelHooked || classLoader == null) return;
        sAwemeModelHooked = true;

        final String awemeClass = "com.ss.android.ugc.aweme.feed.model.Aweme";

        try {
            XposedHelpers.findAndHookMethod(
                    awemeClass,
                    classLoader,
                    "isAd",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (MainHook.isHideAdsEnabled()) {
                                param.setResult(false);
                            }
                        }
                    }
            );
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(
                    awemeClass,
                    classLoader,
                    "isSoftAd",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (MainHook.isHideAdsEnabled()) {
                                param.setResult(false);
                            }
                        }
                    }
            );
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(
                    awemeClass,
                    classLoader,
                    "getAwemeRawAd",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (MainHook.isHideAdsEnabled()) {
                                param.setResult(null);
                            }
                        }
                    }
            );
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(
                    awemeClass,
                    classLoader,
                    "isAdTraffic",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (MainHook.isHideAdsEnabled()) {
                                param.setResult(false);
                            }
                        }
                    }
            );
        } catch (Throwable ignored) {}
    }
}

