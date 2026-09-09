package com.ash.tiktokregion;

import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class AdsHook {

    private static final String TAG = "TikTokFeedFilter";

    public static void hook(ClassLoader classLoader) {
        hookFeedItemList(classLoader);
        hookFeedApis(classLoader);
        hookAwemeModel(classLoader);
    }

    private static void hookFeedItemList(ClassLoader classLoader) {
        final String feedItemListClass = "com.ss.android.ugc.aweme.feed.model.FeedItemList";

        XC_MethodHook listHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object feedItemList = param.thisObject;
                if (feedItemList == null) return;

                if (Boolean.TRUE.equals(XposedHelpers.getAdditionalInstanceField(feedItemList, "tiktok_enhancer_filtered"))) {
                    return;
                }

                Object result = param.getResult();
                if (result instanceof List) {
                    List<?> list = (List<?>) result;
                    if (list.isEmpty()) return;

                    List<Object> cleanList = filterAwemeList(list, feedItemList);

                    XposedHelpers.setAdditionalInstanceField(feedItemList, "tiktok_enhancer_filtered", Boolean.TRUE);

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

                        try {
                            XposedHelpers.callMethod(feedItemList, "setItems", cleanList);
                        } catch (Throwable ignored) {}

                        param.setResult(cleanList);
                    }
                }
            }
        };

        try {
            XposedHelpers.findAndHookMethod(feedItemListClass, classLoader, "getItems", listHook);
            XposedBridge.log(TAG + ": Hooked FeedItemList.getItems()");
        } catch (Throwable t) {
            Log.d(TAG, "FeedItemList.getItems() hook failed: " + t.getMessage());
        }

        try {
            XposedHelpers.findAndHookMethod(feedItemListClass, classLoader, "getAwemeList", listHook);
            XposedBridge.log(TAG + ": Hooked FeedItemList.getAwemeList()");
        } catch (Throwable ignored) {}
    }

    private static void hookFeedApis(ClassLoader classLoader) {
        String[] apiClasses = {
                "com.ss.android.ugc.aweme.feed.FeedApiService",
                "com.ss.android.ugc.aweme.feed.api.FeedApi"
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

                                if (Boolean.TRUE.equals(XposedHelpers.getAdditionalInstanceField(feedItemList, "tiktok_enhancer_filtered"))) {
                                    return;
                                }

                                try {
                                    Object itemsObj = XposedHelpers.callMethod(feedItemList, "getItems");
                                    if (itemsObj instanceof List) {
                                        List<?> list = (List<?>) itemsObj;
                                        List<Object> clean = filterAwemeList(list, feedItemList);

                                        XposedHelpers.setAdditionalInstanceField(feedItemList, "tiktok_enhancer_filtered", Boolean.TRUE);

                                        if (clean.size() != list.size()) {
                                            try {
                                                XposedHelpers.setBooleanField(feedItemList, "hasAd", false);
                                            } catch (Throwable ignored) {}
                                            try {
                                                XposedHelpers.setObjectField(feedItemList, "items", clean);
                                            } catch (Throwable ignored) {}
                                            try {
                                                XposedHelpers.callMethod(feedItemList, "setItems", clean);
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

    private static boolean isRecommendFeed(Object feedItemList) {
        if (feedItemList == null) return true;
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
        return true;
    }

    private static List<Object> filterAwemeList(List<?> sourceList, Object feedItemList) {
        List<Object> kept = new ArrayList<>(sourceList.size());
        boolean isChina = MainHook.isChinaPackage();
        boolean hideAds = MainHook.isHideAdsEnabled();
        boolean strictRegion = !isChina && MainHook.isStrictForceRegionEnabled();
        boolean smartRegion = !isChina && !strictRegion && MainHook.isForceRegionEnabled() && isRecommendFeed(feedItemList);
        boolean anyRegionFilter = strictRegion || smartRegion;
        String targetRegion = MainHook.getTargetCountryIso();
        boolean blockCountries = !isChina && MainHook.isBlockCountriesEnabled();
        Set<String> blockedCountries = blockCountries ? MainHook.getBlockedCountries() : Collections.emptySet();
        boolean hasBlockedCountries = blockCountries && blockedCountries != null && !blockedCountries.isEmpty();

        for (Object item : sourceList) {
            if (item == null) continue;

            if (hideAds && isAdAweme(item)) {
                continue;
            }

            if (hasBlockedCountries && shouldFilterForBlockedCountry(item, blockedCountries)) {
                continue;
            }

            if (anyRegionFilter && shouldFilterForRegion(item, targetRegion)) {
                continue;
            }

            kept.add(item);
        }

        if (!strictRegion && kept.isEmpty() && !sourceList.isEmpty()) {
            for (Object item : sourceList) {
                if (item != null
                        && !(hideAds && isAdAweme(item))
                        && !(hasBlockedCountries && shouldFilterForBlockedCountry(item, blockedCountries))) {
                    kept.add(item);
                }
            }

            if (kept.isEmpty()) {
                kept.addAll(sourceList);
            }
        }

        for (Object item : kept) {
            try {
                WatermarkHook.unlockAwemeRestrictions(item);
            } catch (Throwable ignored) {}
        }

        return kept;
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

        try {
            Object regionObj = XposedHelpers.callMethod(aweme, "getRegion");
            if (regionObj instanceof String) {
                String region = ((String) regionObj).trim();
                if (!region.isEmpty()) {
                    return !region.equalsIgnoreCase(target);
                }
            }
        } catch (Throwable ignored) {}

        try {
            Object author = XposedHelpers.callMethod(aweme, "getAuthor");
            if (author != null) {
                Object authorRegionObj = XposedHelpers.callMethod(author, "getRegion");
                if (authorRegionObj instanceof String) {
                    String aRegion = ((String) authorRegionObj).trim();
                    if (!aRegion.isEmpty()) {
                        return !aRegion.equalsIgnoreCase(target);
                    }
                }
            }
        } catch (Throwable ignored) {}

        return false;
    }

    private static boolean shouldFilterForBlockedCountry(Object aweme, Set<String> blockedCountries) {
        if (blockedCountries == null || blockedCountries.isEmpty()) return false;

        try {
            Object regionObj = XposedHelpers.callMethod(aweme, "getRegion");
            if (regionObj instanceof String) {
                String region = ((String) regionObj).trim().toLowerCase();
                if (!region.isEmpty() && blockedCountries.contains(region)) {
                    return true;
                }
            }
        } catch (Throwable ignored) {}

        try {
            Object author = XposedHelpers.callMethod(aweme, "getAuthor");
            if (author != null) {
                Object authorRegionObj = XposedHelpers.callMethod(author, "getRegion");
                if (authorRegionObj instanceof String) {
                    String aRegion = ((String) authorRegionObj).trim().toLowerCase();
                    if (!aRegion.isEmpty() && blockedCountries.contains(aRegion)) {
                        return true;
                    }
                }
                Object authorCountryObj = XposedHelpers.callMethod(author, "getCountryCode");
                if (authorCountryObj instanceof String) {
                    String aCountry = ((String) authorCountryObj).trim().toLowerCase();
                    if (!aCountry.isEmpty() && blockedCountries.contains(aCountry)) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {}

        try {
            Object countryObj = XposedHelpers.callMethod(aweme, "getCountryCode");
            if (countryObj instanceof String) {
                String country = ((String) countryObj).trim().toLowerCase();
                if (!country.isEmpty() && blockedCountries.contains(country)) {
                    return true;
                }
            }
        } catch (Throwable ignored) {}

        return false;
    }

    private static void hookAwemeModel(ClassLoader classLoader) {
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

