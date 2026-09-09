package com.ash.tiktokregion;

import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
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

    private static volatile boolean sFeedItemListHooked = false;
    private static volatile boolean sFeedApisHooked = false;
    private static volatile boolean sFeedPanelHooked = false;
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
                    }
                }
            }
        };

        try {
            XposedHelpers.findAndHookMethod(feedItemListClass, "getItems", listHook);
            Log.i(TAG, "Hooked FeedItemList.getItems()");
        } catch (Throwable t) {
            Log.d(TAG, "FeedItemList.getItems() hook failed: " + t.getMessage());
        }

        try {
            XposedHelpers.findAndHookMethod(feedItemListClass, "getAwemeList", listHook);
            Log.i(TAG, "Hooked FeedItemList.getAwemeList()");
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
            Log.i(TAG, "Hooked FeedItemList.setItems(List)");
        } catch (Throwable t) {
            Log.d(TAG, "FeedItemList.setItems(List) hook failed: " + t.getMessage());
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

    private static void hookFeedItemList(ClassLoader classLoader) {
        if (sFeedItemListHooked || classLoader == null) return;
        Class<?> clazz = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.feed.model.FeedItemList", classLoader);
        if (clazz != null) {
            hookFeedItemListClass(clazz);
        }
    }

    public static void hookFeedPanel(ClassLoader classLoader) {
        if (sFeedPanelHooked || classLoader == null) return;
        sFeedPanelHooked = true;

        String[] panelClasses = {
                "com.ss.android.ugc.aweme.feed.panel.BaseListFragmentPanel",
                "com.ss.android.ugc.aweme.feed.panel.FullFeedFragmentPanel"
        };

        for (String panelClass : panelClasses) {
            try {
                Class<?> clazz = XposedHelpers.findClassIfExists(panelClass, classLoader);
                if (clazz == null) continue;

                XposedHelpers.findAndHookMethod(clazz, "getAwemeList", new XC_MethodHook() {
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
                            }
                        }
                    }
                });
                Log.i(TAG, "Hooked " + panelClass + ".getAwemeList()");
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

    public static List<Object> filterAwemeList(List<?> sourceList, Object feedItemList) {
        if (sourceList == null || sourceList.isEmpty()) {
            return new ArrayList<>();
        }

        if (Boolean.TRUE.equals(XposedHelpers.getAdditionalInstanceField(sourceList, "tiktok_enhancer_clean"))) {
            @SuppressWarnings("unchecked")
            List<Object> alreadyClean = (List<Object>) sourceList;
            return alreadyClean;
        }

        MainHook.checkConfigRefreshAsync(null);

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
        BlockedCountryMatcher matcher = hasBlockedCountries ? getMatcher(blockedCountries) : null;

        for (Object item : sourceList) {
            if (item == null) continue;

            if (hideAds && isAdAweme(item)) {
                continue;
            }

            if (matcher != null && matcher.matchesAweme(item)) {
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
                        && !(matcher != null && matcher.matchesAweme(item))) {
                    kept.add(item);
                }
            }
            // CRITICAL: NEVER kept.addAll(sourceList) if blocked countries or ads exist!
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

        String region = getSafeString(aweme, "getRegion", "region");
        if (region != null && !region.isEmpty()) {
            return !region.equalsIgnoreCase(target);
        }

        Object author = getSafeObject(aweme, "getAuthor", "author");
        if (author != null) {
            String aRegion = getSafeString(author, "getRegion", "region");
            if (aRegion != null && !aRegion.isEmpty()) {
                return !aRegion.equalsIgnoreCase(target);
            }
            String aIso = getSafeString(author, "getIsoCountryCode", "isoCountryCode");
            if (aIso != null && !aIso.isEmpty()) {
                return !aIso.equalsIgnoreCase(target);
            }
            String aAccount = getSafeString(author, "getAccountRegion", "accountRegion");
            if (aAccount != null && !aAccount.isEmpty()) {
                return !aAccount.equalsIgnoreCase(target);
            }
        }

        return false;
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
            KNOWN_ALIASES.put("id", new String[]{"indonesia", "jakarta", "surabaya", "bandung", "medan"});
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
                if (iso.length() == 2 && containsWord(clean, iso)) {
                    return true;
                }
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

        public boolean matchesAweme(Object aweme) {
            if (aweme == null) return false;

            // 1. Direct Aweme region
            String region = getSafeString(aweme, "getRegion", "region");
            if (matchesCode(region)) return true;

            // 2. Aweme geofencing regions (List<String>)
            Object geoObj = getSafeObject(aweme, "getGeofencingRegions", "geofencingRegions");
            if (geoObj instanceof List) {
                for (Object g : (List<?>) geoObj) {
                    if (g instanceof String && matchesCode((String) g)) {
                        return true;
                    }
                }
            }

            // 3. Nearby info
            Object nearby = getSafeObject(aweme, "getNearbyInfo", "nearbyInfo");
            if (nearby != null) {
                String nearbyRegion = getSafeString(nearby, "getNearbyRegion", "nearbyRegion");
                if (matchesCode(nearbyRegion)) return true;
                String eventRegion = getSafeString(nearby, "getEventRegion", "eventRegion");
                if (matchesCode(eventRegion)) return true;
            }

            // 4. POI data struct
            Object poi = getSafeObject(aweme, "getPoiDataStruct", "poiDataStruct");
            if (poi != null) {
                String locDesc = getSafeString(poi, "getLocationDesc", "locationDesc");
                if (matchesText(locDesc)) return true;

                Object addrInfo = getSafeObject(poi, "getAddressInfo", "addressInfo");
                if (addrInfo != null) {
                    String regCode = getSafeString(addrInfo, "getRegionCode", "regionCode");
                    if (matchesCode(regCode)) return true;
                    String cityName = getSafeString(addrInfo, "getCityName", "cityName");
                    if (matchesText(cityName)) return true;
                    String address = getSafeString(addrInfo, "getAddress", "address");
                    if (matchesText(address)) return true;
                    String country = getSafeString(addrInfo, "getCountry", "country");
                    if (matchesText(country)) return true;
                }
            }

            // 5. Author profile
            Object author = getSafeObject(aweme, "getAuthor", "author");
            if (checkUser(author)) return true;

            // 6. Origin author profile (duet / stitch / repost)
            Object originAuthor = getSafeObject(aweme, "getOriginAuthor", "originAuthor");
            if (checkUser(originAuthor)) return true;

            return false;
        }

        private boolean checkUser(Object user) {
            if (user == null) return false;

            String region = getSafeString(user, "getRegion", "region");
            if (matchesCode(region)) return true;

            String isoCountryCode = getSafeString(user, "getIsoCountryCode", "isoCountryCode");
            if (matchesCode(isoCountryCode)) return true;

            String accountRegion = getSafeString(user, "getAccountRegion", "accountRegion");
            if (matchesCode(accountRegion)) return true;

            String lemon8Region = getSafeString(user, "getLemon8StoreRegion", "lemon8StoreRegion");
            if (matchesCode(lemon8Region)) return true;

            String country = getSafeString(user, "getCountry", "country");
            if (matchesText(country)) return true;

            String bioLocation = getSafeString(user, "getBioLocation", "bioLocation");
            if (matchesText(bioLocation)) return true;

            String cityName = getSafeString(user, "getCityName", "cityName");
            if (matchesText(cityName)) return true;

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
