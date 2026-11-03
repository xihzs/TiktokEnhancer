package com.ash.tiktokregion.hooks.feed;

import com.ash.tiktokregion.core.ModuleConfig;
import com.ash.tiktokregion.hooks.media.StoryDownloadHook;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import de.robv.android.xposed.XposedHelpers;

/**
 * Pure-Java, zero-JNI Aweme feed evaluator.
 * Evaluates videos for ads, blocked countries, blocked country songs, region enforcement, and HD status.
 */
public final class AwemeEvaluator {

    public static final int DECISION_KEEP = 0;
    public static final int DECISION_DISCARD_AD = 1;
    public static final int DECISION_DISCARD_BLOCKED_COUNTRY = 2;
    public static final int DECISION_DISCARD_WRONG_REGION = 3;
    public static final int DECISION_DISCARD_NON_HD = 4;
    public static final int DECISION_DISCARD_BLOCKED_SONG = 5;

    private static volatile BlockedMatcher sCachedMatcher = null;

    private AwemeEvaluator() {}

    public static int evaluate(Object aweme, boolean isFyp, boolean isColdStart) {
        if (aweme == null || !isFyp) {
            return DECISION_KEEP;
        }

        // 1. Check Ad / Commercial (hard discard)
        if (ModuleConfig.hideAds && isAdAweme(aweme)) {
            return DECISION_DISCARD_AD;
        }

        // 2. Check Blocked Countries & Blocked Songs (hard blacklist, evaluated before story bypass!)
        boolean hasBlockedCountries = !ModuleConfig.blockedCountries.isEmpty();
        if (hasBlockedCountries) {
            if (isBlockedCountry(aweme, ModuleConfig.blockedCountries)) {
                return DECISION_DISCARD_BLOCKED_COUNTRY;
            }
            if (ModuleConfig.blockCountrySongs && isBlockedSong(aweme, ModuleConfig.blockedCountries)) {
                return DECISION_DISCARD_BLOCKED_SONG;
            }
        }

        // 3. Preserve Story Awemes so user profile story viewing works without being dropped by region/HD filters
        if (StoryDownloadHook.isStoryAweme(aweme)) {
            return DECISION_KEEP;
        }

        // 4. Check Force Region (Strict vs Smart)
        boolean strict = ModuleConfig.strictForceRegion && isFyp;
        boolean smart = !isColdStart && !ModuleConfig.strictForceRegion && ModuleConfig.forceRegion && isFyp;
        if (strict || smart) {
            if (shouldFilterForRegion(aweme, ModuleConfig.countryIso, strict)) {
                return DECISION_DISCARD_WRONG_REGION;
            }
        }

        // 5. Check Non-HD Filtering
        if (!isColdStart && ModuleConfig.filterNonHd && isFyp) {
            if (isNonHdVideo(aweme)) {
                return DECISION_DISCARD_NON_HD;
            }
        }

        return DECISION_KEEP;
    }

    public static boolean isAdAweme(Object aweme) {
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

        return false;
    }

    public static boolean isBlockedCountry(Object aweme, Set<String> blockedCountries) {
        if (blockedCountries == null || blockedCountries.isEmpty()) return false;
        BlockedMatcher matcher = getMatcher(blockedCountries);
        if (matcher == null) return false;

        // 1. Video region (getter & direct field)
        String itemRegion = getAwemeRegion(aweme);
        if (!itemRegion.isEmpty() && matcher.matchesCountryOrIso(itemRegion)) {
            return true;
        }
        try {
            Object rf = XposedHelpers.getObjectField(aweme, "region");
            if (rf instanceof String && matcher.matchesCountryOrIso((String) rf)) {
                return true;
            }
        } catch (Throwable ignored) {}

        // 2. Video country code if present on Aweme
        try {
            Object c = XposedHelpers.callMethod(aweme, "getCountryCode");
            if (c instanceof String && matcher.matchesCountryOrIso((String) c)) {
                return true;
            }
        } catch (Throwable ignored) {}

        // 3. Geofencing regions (getter & direct field)
        try {
            Object g = XposedHelpers.callMethod(aweme, "getGeofencingRegions");
            if (g instanceof List) {
                for (Object item : (List<?>) g) {
                    if (item instanceof String && matcher.matchesCountryOrIso((String) item)) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {}
        try {
            Object gf = XposedHelpers.getObjectField(aweme, "geofencingRegions");
            if (gf instanceof List) {
                for (Object item : (List<?>) gf) {
                    if (item instanceof String && matcher.matchesCountryOrIso((String) item)) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {}

        // 4. Aweme extra JSON metadata (getter & direct field)
        try {
            Object extra = XposedHelpers.callMethod(aweme, "getExtra");
            if (extra instanceof String && matcher.matchesMetadataJson((String) extra)) {
                return true;
            }
        } catch (Throwable ignored) {}
        try {
            Object extraField = XposedHelpers.getObjectField(aweme, "extra");
            if (extraField instanceof String && matcher.matchesMetadataJson((String) extraField)) {
                return true;
            }
        } catch (Throwable ignored) {}

        // 5. Aweme Description / Captions & Hashtags & Flag Emojis
        try {
            Object desc = XposedHelpers.callMethod(aweme, "getDesc");
            if (desc instanceof String && matcher.matchesContentText((String) desc)) {
                return true;
            }
        } catch (Throwable ignored) {}
        try {
            Object descField = XposedHelpers.getObjectField(aweme, "desc");
            if (descField instanceof String && matcher.matchesContentText((String) descField)) {
                return true;
            }
        } catch (Throwable ignored) {}

        // 6. Author location, country, bio, nickname, and account info
        try {
            Object author = XposedHelpers.callMethod(aweme, "getAuthor");
            if (author == null) {
                author = XposedHelpers.getObjectField(aweme, "author");
            }
            if (author != null && checkUserCountry(author, matcher)) {
                return true;
            }
        } catch (Throwable ignored) {}

        // 7. POI (Point of Interest) address & description
        try {
            Object poi = XposedHelpers.callMethod(aweme, "getPoiDataStruct");
            if (poi == null) {
                poi = XposedHelpers.callMethod(aweme, "getSearchPoiDataStruct");
            }
            if (poi == null) {
                poi = XposedHelpers.getObjectField(aweme, "_poiDataStruct");
            }
            if (poi != null) {
                Object addr = XposedHelpers.getObjectField(poi, "addressInfo");
                if (addr != null) {
                    Object rc = XposedHelpers.getObjectField(addr, "regionCode");
                    if (rc instanceof String && matcher.matchesCountryOrIso((String) rc)) return true;

                    Object cc = XposedHelpers.getObjectField(addr, "cityCode");
                    if (cc instanceof String && matcher.matchesCountryOrIso((String) cc)) return true;

                    Object cn = XposedHelpers.getObjectField(addr, "cityName");
                    if (cn instanceof String && matcher.matchesCountryOrIso((String) cn)) return true;

                    Object ad = XposedHelpers.getObjectField(addr, "address");
                    if (ad instanceof String && matcher.containsCountryName((String) ad)) return true;
                }
                Object desc = XposedHelpers.getObjectField(poi, "locationDesc");
                if (desc instanceof String && matcher.containsCountryName((String) desc)) return true;
            }
        } catch (Throwable ignored) {}

        // 8. Aweme language code (for languages uniquely tied to blocked country)
        try {
            Object lang = XposedHelpers.callMethod(aweme, "getDescLanguage");
            if (lang instanceof String && matcher.matchesUniqueLanguage((String) lang)) {
                return true;
            }
            Object pLang = XposedHelpers.callMethod(aweme, "getPhotoTitleLanguageCode");
            if (pLang instanceof String && matcher.matchesUniqueLanguage((String) pLang)) {
                return true;
            }
            Object sLang = XposedHelpers.callMethod(aweme, "getTextStickerMajorityLang");
            if (sLang instanceof String && matcher.matchesUniqueLanguage((String) sLang)) {
                return true;
            }
        } catch (Throwable ignored) {}

        return false;
    }

    public static boolean isBlockedSong(Object aweme, Set<String> blockedCountries) {
        if (blockedCountries == null || blockedCountries.isEmpty()) return false;
        BlockedMatcher matcher = getMatcher(blockedCountries);
        if (matcher == null) return false;

        // Retrieve Music model
        Object music = null;
        try {
            music = XposedHelpers.callMethod(aweme, "getMusic");
        } catch (Throwable ignored) {}
        if (music == null) {
            try {
                music = XposedHelpers.callMethod(aweme, "getAddedSoundMusicInfo");
            } catch (Throwable ignored) {}
        }
        if (music == null) return false;

        // 1. Original sound check
        boolean isOriginal = false;
        try {
            isOriginal = (boolean) XposedHelpers.callMethod(music, "isOriginalSound");
        } catch (Throwable ignored) {}
        if (!isOriginal) {
            try {
                isOriginal = (boolean) XposedHelpers.callMethod(music, "isOriginMusic");
            } catch (Throwable ignored) {}
        }
        if (!isOriginal) {
            try {
                isOriginal = XposedHelpers.getBooleanField(music, "isOriginalSound");
            } catch (Throwable ignored) {}
        }
        if (!isOriginal) {
            try {
                isOriginal = XposedHelpers.getBooleanField(music, "isOriginMusic");
            } catch (Throwable ignored) {}
        }

        if (isOriginal) {
            // For original sound, check if the video author is from a blocked country
            try {
                Object author = XposedHelpers.callMethod(aweme, "getAuthor");
                if (author == null) {
                    author = XposedHelpers.getObjectField(aweme, "author");
                }
                if (author != null && checkUserCountry(author, matcher)) {
                    return true;
                }
            } catch (Throwable ignored) {}
        }

        // 2. Extract song title and author
        String musicName = "";
        try {
            Object n = XposedHelpers.callMethod(music, "getMusicName");
            if (n instanceof String) musicName = (String) n;
        } catch (Throwable ignored) {}
        if (musicName.isEmpty()) {
            try {
                Object n = XposedHelpers.getObjectField(music, "musicName");
                if (n instanceof String) musicName = (String) n;
            } catch (Throwable ignored) {}
        }

        String authorName = "";
        try {
            Object a = XposedHelpers.callMethod(music, "getAuthorName");
            if (a instanceof String) authorName = (String) a;
        } catch (Throwable ignored) {}
        if (authorName.isEmpty()) {
            try {
                Object a = XposedHelpers.getObjectField(music, "authorName");
                if (a instanceof String) authorName = (String) a;
            } catch (Throwable ignored) {}
        }

        // 3. Localized sound phrase check (e.g., "suara asli", "bunyi asal", "âm thanh gốc", etc.)
        if (!musicName.isEmpty() && matcher.matchesLocalizedSound(musicName)) {
            return true;
        }

        // 4. Script detection on title and author (Thai, Cyrillic, Japanese Kana, Korean Hangul, Arabic, Devanagari, Hebrew, Greek)
        if (!musicName.isEmpty() && matcher.matchesScript(musicName)) {
            return true;
        }
        if (!authorName.isEmpty() && matcher.matchesScript(authorName)) {
            return true;
        }

        // 5. Flag emojis in music title or author name
        if (!musicName.isEmpty() && matcher.matchesContentText(musicName)) {
            return true;
        }
        if (!authorName.isEmpty() && matcher.matchesContentText(authorName)) {
            return true;
        }

        // 6. Demonyms, localized genre, and sound keywords
        if (!musicName.isEmpty() && matcher.matchesSongKeywords(musicName)) {
            return true;
        }
        if (!authorName.isEmpty() && matcher.matchesSongKeywords(authorName)) {
            return true;
        }

        // 7. Matched Sound Info & Matched PGC Sound Info
        try {
            Object matched = XposedHelpers.callMethod(music, "getMatchedSongInfo");
            if (matched == null) {
                matched = XposedHelpers.getObjectField(music, "mMatchedSongInfo");
            }
            if (matched != null) {
                Object mt = XposedHelpers.callMethod(matched, "getTitle");
                if (mt instanceof String) {
                    String s = (String) mt;
                    if (matcher.matchesLocalizedSound(s) || matcher.matchesScript(s) || matcher.matchesSongKeywords(s) || matcher.matchesContentText(s)) {
                        return true;
                    }
                }
                Object ma = XposedHelpers.callMethod(matched, "getAuthor");
                if (ma instanceof String) {
                    String s = (String) ma;
                    if (matcher.matchesScript(s) || matcher.matchesSongKeywords(s) || matcher.matchesContentText(s)) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {}

        try {
            Object matchedPgc = XposedHelpers.callMethod(music, "getMatchedPGCSoundInfo");
            if (matchedPgc == null) {
                matchedPgc = XposedHelpers.getObjectField(music, "matchedPGCSoundInfo");
            }
            if (matchedPgc != null) {
                Object mt = XposedHelpers.callMethod(matchedPgc, "getTitle");
                if (mt instanceof String) {
                    String s = (String) mt;
                    if (matcher.matchesLocalizedSound(s) || matcher.matchesScript(s) || matcher.matchesSongKeywords(s) || matcher.matchesContentText(s)) {
                        return true;
                    }
                }
                Object ma = XposedHelpers.callMethod(matchedPgc, "getAuthor");
                if (ma instanceof String) {
                    String s = (String) ma;
                    if (matcher.matchesScript(s) || matcher.matchesSongKeywords(s) || matcher.matchesContentText(s)) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {}

        // 8. Music extra and logExtra JSON region fields
        try {
            Object extra = XposedHelpers.callMethod(music, "getExtra");
            if (extra instanceof String && matcher.matchesMetadataJson((String) extra)) {
                return true;
            }
            Object logExtra = XposedHelpers.callMethod(music, "getLogExtra");
            if (logExtra instanceof String && matcher.matchesMetadataJson((String) logExtra)) {
                return true;
            }
        } catch (Throwable ignored) {}

        return false;
    }

    public static boolean checkUserCountry(Object author, BlockedMatcher matcher) {
        if (author == null || matcher == null) return false;
        try {
            // 1. Region getters
            Object r = XposedHelpers.callMethod(author, "getRegion");
            if (r instanceof String && matcher.matchesCountryOrIso((String) r)) return true;

            Object iso = XposedHelpers.callMethod(author, "getIsoCountryCode");
            if (iso instanceof String && matcher.matchesCountryOrIso((String) iso)) return true;

            Object c = XposedHelpers.callMethod(author, "getCountry");
            if (c instanceof String && matcher.matchesCountryOrIso((String) c)) return true;

            Object ar = XposedHelpers.callMethod(author, "getAccountRegion");
            if (ar instanceof String && matcher.matchesCountryOrIso((String) ar)) return true;

            Object lr = XposedHelpers.callMethod(author, "getLemon8StoreRegion");
            if (lr instanceof String && matcher.matchesCountryOrIso((String) lr)) return true;

            // 2. Direct field checks for region
            try {
                Object rf = XposedHelpers.getObjectField(author, "region");
                if (rf instanceof String && matcher.matchesCountryOrIso((String) rf)) return true;
            } catch (Throwable ignored) {}
            try {
                Object isof = XposedHelpers.getObjectField(author, "isoCountryCode");
                if (isof instanceof String && matcher.matchesCountryOrIso((String) isof)) return true;
            } catch (Throwable ignored) {}
            try {
                Object cf = XposedHelpers.getObjectField(author, "country");
                if (cf instanceof String && matcher.matchesCountryOrIso((String) cf)) return true;
            } catch (Throwable ignored) {}
            try {
                Object arf = XposedHelpers.getObjectField(author, "accountRegion");
                if (arf instanceof String && matcher.matchesCountryOrIso((String) arf)) return true;
            } catch (Throwable ignored) {}

            // 3. Register from
            try {
                Object regFrom = XposedHelpers.getObjectField(author, "registerFrom");
                if (regFrom instanceof String && matcher.matchesCountryOrIso((String) regFrom)) return true;
            } catch (Throwable ignored) {}

            // 4. City, province, district, bio location
            Object city = XposedHelpers.callMethod(author, "getCityName");
            if (city instanceof String && matcher.containsCountryName((String) city)) return true;

            Object prov = XposedHelpers.callMethod(author, "getProvince");
            if (prov instanceof String && matcher.containsCountryName((String) prov)) return true;

            try {
                Object dist = XposedHelpers.getObjectField(author, "district");
                if (dist instanceof String && matcher.containsCountryName((String) dist)) return true;
            } catch (Throwable ignored) {}

            Object bio = XposedHelpers.callMethod(author, "getBioLocation");
            if (bio instanceof String && matcher.containsCountryName((String) bio)) return true;

            // 5. Bio signature & nickname (flag emoji or country names)
            try {
                Object sig = XposedHelpers.callMethod(author, "getSignature");
                if (sig instanceof String && matcher.matchesContentText((String) sig)) return true;
            } catch (Throwable ignored) {}
            try {
                Object nick = XposedHelpers.callMethod(author, "getNickname");
                if (nick instanceof String && matcher.matchesContentText((String) nick)) return true;
            } catch (Throwable ignored) {}

            // 6. Language
            Object lang = XposedHelpers.callMethod(author, "getLanguage");
            if (lang instanceof String && matcher.matchesUniqueLanguage((String) lang)) return true;

            Object sigLang = XposedHelpers.callMethod(author, "getSignatureLanguage");
            if (sigLang instanceof String && matcher.matchesUniqueLanguage((String) sigLang)) return true;
        } catch (Throwable ignored) {}
        return false;
    }

    public static boolean shouldFilterForRegion(Object aweme, String targetRegion, boolean strict) {
        if (targetRegion == null || targetRegion.trim().isEmpty()) return false;
        String target = targetRegion.trim().toLowerCase(Locale.ROOT);

        String itemRegion = getAwemeRegion(aweme);
        String authorRegion = getAwemeAuthorRegion(aweme);

        boolean matches = (!itemRegion.isEmpty() && itemRegion.equals(target))
                || (!authorRegion.isEmpty() && authorRegion.equals(target));

        if (matches) return false;

        if (strict) {
            return true;
        }

        return !itemRegion.isEmpty() || !authorRegion.isEmpty();
    }

    public static String getAwemeRegion(Object aweme) {
        try {
            Object r = XposedHelpers.callMethod(aweme, "getRegion");
            if (r instanceof String) {
                return ((String) r).trim().toLowerCase(Locale.ROOT);
            }
        } catch (Throwable ignored) {}
        return "";
    }

    public static String getAwemeAuthorRegion(Object aweme) {
        try {
            Object author = XposedHelpers.callMethod(aweme, "getAuthor");
            if (author == null) {
                author = XposedHelpers.getObjectField(aweme, "author");
            }
            if (author != null) {
                Object r = XposedHelpers.callMethod(author, "getRegion");
                if (r instanceof String) {
                    String s = ((String) r).trim().toLowerCase(Locale.ROOT);
                    if (!s.isEmpty()) return s;
                }
                Object iso = XposedHelpers.callMethod(author, "getIsoCountryCode");
                if (iso instanceof String) {
                    String s = ((String) iso).trim().toLowerCase(Locale.ROOT);
                    if (!s.isEmpty()) return s;
                }
                Object ar = XposedHelpers.callMethod(author, "getAccountRegion");
                if (ar instanceof String) {
                    String s = ((String) ar).trim().toLowerCase(Locale.ROOT);
                    if (!s.isEmpty()) return s;
                }
                Object c = XposedHelpers.callMethod(author, "getCountry");
                if (c instanceof String) {
                    String s = ((String) c).trim().toLowerCase(Locale.ROOT);
                    if (s.length() == 2) return s;
                    String mapped = BlockedMatcher.iso2FromName(s);
                    if (!mapped.isEmpty()) return mapped;
                }
            }
        } catch (Throwable ignored) {}
        return "";
    }

    public static boolean isNonHdVideo(Object aweme) {
        try {
            Object video = XposedHelpers.callMethod(aweme, "getVideo");
            if (video == null) return false;

            int width = (int) XposedHelpers.callMethod(video, "getWidth");
            int height = (int) XposedHelpers.callMethod(video, "getHeight");
            int maxBitrate = 0;

            Object brList = XposedHelpers.callMethod(video, "getBitRate");
            if (brList instanceof java.util.List) {
                for (Object b : (java.util.List<?>) brList) {
                    if (b != null) {
                        int r = (int) XposedHelpers.callMethod(b, "getBitRate");
                        if (r > maxBitrate) maxBitrate = r;
                    }
                }
            }

            if (width > 0 && height > 0 && Math.min(width, height) < 540) {
                if (maxBitrate < 750_000) return true;
            }
            if (maxBitrate > 0 && maxBitrate < 350_000) return true;
        } catch (Throwable ignored) {}

        return false;
    }

    private static BlockedMatcher getMatcher(Set<String> blockedCountries) {
        if (blockedCountries == null || blockedCountries.isEmpty()) return null;
        BlockedMatcher m = sCachedMatcher;
        if (m != null && (m.sourceSet == blockedCountries || m.sourceSet.equals(blockedCountries))) {
            return m;
        }
        m = new BlockedMatcher(blockedCountries);
        sCachedMatcher = m;
        return m;
    }

    public static final class BlockedMatcher {
        final Set<String> sourceSet;
        final Set<String> isoCodes = new HashSet<>();
        final Set<String> countryNames = new HashSet<>();
        final Set<String> uniqueLanguages = new HashSet<>();
        final List<String> localizedSounds = new ArrayList<>();
        final List<String> songKeywords = new ArrayList<>();
        final List<String> flagEmojis = new ArrayList<>();
        final List<String> contentHashtags = new ArrayList<>();
        int scriptMask = 0;

        static final int SCRIPT_THAI = 1 << 0;
        static final int SCRIPT_CYRILLIC = 1 << 1;
        static final int SCRIPT_JAPANESE = 1 << 2;
        static final int SCRIPT_KOREAN = 1 << 3;
        static final int SCRIPT_ARABIC = 1 << 4;
        static final int SCRIPT_DEVANAGARI = 1 << 5;
        static final int SCRIPT_HEBREW = 1 << 6;
        static final int SCRIPT_GREEK = 1 << 7;

        private static final Map<String, String> NAME_TO_ISO2 = new HashMap<>();

        static {
            for (String iso : Locale.getISOCountries()) {
                Locale loc = new Locale("", iso);
                String iso2 = iso.toLowerCase(Locale.ROOT);
                NAME_TO_ISO2.put(iso2, iso2);
                try {
                    String iso3 = loc.getISO3Country().toLowerCase(Locale.ROOT);
                    if (!iso3.isEmpty()) NAME_TO_ISO2.put(iso3, iso2);
                } catch (Throwable ignored) {}
                String enName = loc.getDisplayCountry(Locale.ENGLISH).toLowerCase(Locale.ROOT);
                if (!enName.isEmpty()) NAME_TO_ISO2.put(enName, iso2);
                String selfName = loc.getDisplayCountry(loc).toLowerCase(Locale.ROOT);
                if (!selfName.isEmpty()) NAME_TO_ISO2.put(selfName, iso2);
            }
            // Explicit common aliases
            NAME_TO_ISO2.put("indo", "id");
            NAME_TO_ISO2.put("indonesia", "id");
            NAME_TO_ISO2.put("malaysia", "my");
            NAME_TO_ISO2.put("thai", "th");
            NAME_TO_ISO2.put("thailand", "th");
            NAME_TO_ISO2.put("vietnam", "vn");
            NAME_TO_ISO2.put("viet nam", "vn");
            NAME_TO_ISO2.put("philippines", "ph");
            NAME_TO_ISO2.put("pilipinas", "ph");
            NAME_TO_ISO2.put("russia", "ru");
            NAME_TO_ISO2.put("россия", "ru");
            NAME_TO_ISO2.put("ukraine", "ua");
            NAME_TO_ISO2.put("украина", "ua");
            NAME_TO_ISO2.put("україна", "ua");
            NAME_TO_ISO2.put("belarus", "by");
            NAME_TO_ISO2.put("беларусь", "by");
            NAME_TO_ISO2.put("kazakhstan", "kz");
            NAME_TO_ISO2.put("казахстан", "kz");
            NAME_TO_ISO2.put("japan", "jp");
            NAME_TO_ISO2.put("nihon", "jp");
            NAME_TO_ISO2.put("nippon", "jp");
            NAME_TO_ISO2.put("日本", "jp");
            NAME_TO_ISO2.put("korea", "kr");
            NAME_TO_ISO2.put("south korea", "kr");
            NAME_TO_ISO2.put("한국", "kr");
            NAME_TO_ISO2.put("china", "cn");
            NAME_TO_ISO2.put("taiwan", "tw");
            NAME_TO_ISO2.put("hong kong", "hk");
            NAME_TO_ISO2.put("brazil", "br");
            NAME_TO_ISO2.put("brasil", "br");
            NAME_TO_ISO2.put("turkey", "tr");
            NAME_TO_ISO2.put("turkiye", "tr");
            NAME_TO_ISO2.put("türkiye", "tr");
            NAME_TO_ISO2.put("germany", "de");
            NAME_TO_ISO2.put("deutschland", "de");
            NAME_TO_ISO2.put("france", "fr");
            NAME_TO_ISO2.put("italy", "it");
            NAME_TO_ISO2.put("italia", "it");
            NAME_TO_ISO2.put("poland", "pl");
            NAME_TO_ISO2.put("polska", "pl");
            NAME_TO_ISO2.put("united states", "us");
            NAME_TO_ISO2.put("usa", "us");
            NAME_TO_ISO2.put("united kingdom", "gb");
            NAME_TO_ISO2.put("uk", "gb");
            NAME_TO_ISO2.put("great britain", "gb");
        }

        public static String iso2FromName(String name) {
            if (name == null || name.isEmpty()) return "";
            String res = NAME_TO_ISO2.get(name.trim().toLowerCase(Locale.ROOT));
            return res != null ? res : "";
        }

        public static String getFlagEmoji(String iso2) {
            if (iso2 == null || iso2.length() != 2) return "";
            int c1 = Character.toLowerCase(iso2.charAt(0)) - 'a';
            int c2 = Character.toLowerCase(iso2.charAt(1)) - 'a';
            if (c1 < 0 || c1 > 25 || c2 < 0 || c2 > 25) return "";
            return new String(Character.toChars(0x1F1E6 + c1)) + new String(Character.toChars(0x1F1E6 + c2));
        }

        public BlockedMatcher(Set<String> blocked) {
            this.sourceSet = blocked;
            for (String b : blocked) {
                if (b == null) continue;
                String iso = b.trim().toLowerCase(Locale.ROOT);
                if (iso.isEmpty()) continue;
                isoCodes.add(iso);

                // Add ISO3 and country names via Locale
                Locale loc = new Locale("", iso);
                try {
                    String iso3 = loc.getISO3Country().toLowerCase(Locale.ROOT);
                    if (!iso3.isEmpty()) isoCodes.add(iso3);
                } catch (Throwable ignored) {}

                String enName = loc.getDisplayCountry(Locale.ENGLISH).toLowerCase(Locale.ROOT);
                if (!enName.isEmpty()) countryNames.add(enName);
                String selfName = loc.getDisplayCountry(loc).toLowerCase(Locale.ROOT);
                if (!selfName.isEmpty()) countryNames.add(selfName);

                // Flag emoji
                String flag = getFlagEmoji(iso);
                if (!flag.isEmpty()) flagEmojis.add(flag);

                // Common hashtag patterns
                contentHashtags.add("#fyp" + iso);
                contentHashtags.add("#tiktok" + iso);
                contentHashtags.add("#" + iso + "tiktok");

                // Populate country-specific rules: localized sounds, script flags, unique languages, keywords
                populateCountrySpecifics(iso);
            }
        }

        private void populateCountrySpecifics(String iso) {
            switch (iso) {
                case "id":
                    countryNames.add("indonesia");
                    countryNames.add("indo");
                    countryNames.add("jakarta");
                    countryNames.add("surabaya");
                    countryNames.add("bandung");
                    countryNames.add("medan");
                    countryNames.add("bali");
                    countryNames.add("semarang");
                    countryNames.add("makassar");
                    countryNames.add("palembang");
                    countryNames.add("yogyakarta");
                    countryNames.add("jogja");
                    countryNames.add("bekasi");
                    countryNames.add("tangerang");
                    countryNames.add("depok");
                    countryNames.add("bogor");
                    countryNames.add("jawa barat");
                    countryNames.add("jawa timur");
                    countryNames.add("jawa tengah");
                    uniqueLanguages.add("id");
                    uniqueLanguages.add("in");
                    uniqueLanguages.add("ind");
                    localizedSounds.add("suara asli");
                    contentHashtags.add("#indonesia");
                    contentHashtags.add("#tiktokindonesia");
                    contentHashtags.add("#fypindonesia");
                    contentHashtags.add("#warga62");
                    contentHashtags.add("#viralindonesia");
                    contentHashtags.add("#jakartahits");
                    contentHashtags.add("#dagelan");
                    songKeywords.add("dangdut");
                    songKeywords.add("koplo");
                    songKeywords.add("dj tiktok indonesia");
                    songKeywords.add("dj indonesia");
                    songKeywords.add("indo remix");
                    songKeywords.add("lagu batak");
                    songKeywords.add("lagu minang");
                    songKeywords.add("lagu sunda");
                    songKeywords.add("lagu jawa");
                    songKeywords.add("tarling");
                    songKeywords.add("campursari");
                    songKeywords.add("pop sunda");
                    songKeywords.add("pop jawa");
                    songKeywords.add("dj desa");
                    songKeywords.add("dj brewog");
                    songKeywords.add("jedag jedug");
                    break;
                case "my":
                    countryNames.add("malaysia");
                    countryNames.add("kuala lumpur");
                    countryNames.add("selangor");
                    countryNames.add("johor");
                    countryNames.add("penang");
                    countryNames.add("sabah");
                    countryNames.add("sarawak");
                    countryNames.add("melaka");
                    countryNames.add("perak");
                    countryNames.add("kedah");
                    countryNames.add("putrajaya");
                    uniqueLanguages.add("ms");
                    uniqueLanguages.add("zsm");
                    uniqueLanguages.add("may");
                    localizedSounds.add("bunyi asal");
                    contentHashtags.add("#malaysia");
                    contentHashtags.add("#tiktokmalaysia");
                    contentHashtags.add("#fypmalaysia");
                    contentHashtags.add("#malaysiatiktok");
                    contentHashtags.add("#viralmalaysia");
                    songKeywords.add("lagu melayu");
                    songKeywords.add("dikir barat");
                    songKeywords.add("nasyid");
                    songKeywords.add("lagu raya");
                    songKeywords.add("lagu sarawak");
                    songKeywords.add("lagu sabah");
                    break;
                case "th":
                    countryNames.add("thailand");
                    countryNames.add("siam");
                    countryNames.add("bangkok");
                    countryNames.add("chiang mai");
                    countryNames.add("phuket");
                    countryNames.add("pattaya");
                    uniqueLanguages.add("th");
                    uniqueLanguages.add("tha");
                    scriptMask |= SCRIPT_THAI;
                    localizedSounds.add("เสียงต้นฉบับ");
                    contentHashtags.add("#thailand");
                    contentHashtags.add("#tiktokthailand");
                    contentHashtags.add("#fypthailand");
                    contentHashtags.add("#ไทย");
                    songKeywords.add("mor lam");
                    songKeywords.add("luk thung");
                    songKeywords.add("thai pop");
                    songKeywords.add("thai remix");
                    songKeywords.add("t-pop");
                    songKeywords.add("sai yoh");
                    break;
                case "vn":
                    countryNames.add("vietnam");
                    countryNames.add("viet nam");
                    countryNames.add("hanoi");
                    countryNames.add("saigon");
                    countryNames.add("ho chi minh");
                    countryNames.add("da nang");
                    uniqueLanguages.add("vi");
                    uniqueLanguages.add("vie");
                    localizedSounds.add("âm thanh gốc");
                    contentHashtags.add("#vietnam");
                    contentHashtags.add("#tiktokvietnam");
                    contentHashtags.add("#fypvietnam");
                    songKeywords.add("v-pop");
                    songKeywords.add("vpop");
                    songKeywords.add("vinahouse");
                    songKeywords.add("vina house");
                    songKeywords.add("nhac tre");
                    songKeywords.add("nhac song");
                    songKeywords.add("nhac vang");
                    songKeywords.add("bolero vietnam");
                    songKeywords.add("nhac che");
                    break;
                case "ph":
                    countryNames.add("philippines");
                    countryNames.add("pilipinas");
                    countryNames.add("manila");
                    countryNames.add("quezon");
                    countryNames.add("davao");
                    countryNames.add("cebu");
                    uniqueLanguages.add("tl");
                    uniqueLanguages.add("fil");
                    uniqueLanguages.add("tgl");
                    localizedSounds.add("orihinal na tunog");
                    contentHashtags.add("#philippines");
                    contentHashtags.add("#tiktokphilippines");
                    contentHashtags.add("#fypphilippines");
                    contentHashtags.add("#pinoy");
                    songKeywords.add("opm");
                    songKeywords.add("tagalog");
                    songKeywords.add("budots");
                    songKeywords.add("pinoy");
                    songKeywords.add("bisaya");
                    break;
                case "ru":
                    countryNames.add("russia");
                    countryNames.add("россия");
                    countryNames.add("moscow");
                    countryNames.add("москва");
                    countryNames.add("saint petersburg");
                    countryNames.add("питер");
                    countryNames.add("спб");
                    uniqueLanguages.add("ru");
                    uniqueLanguages.add("rus");
                    scriptMask |= SCRIPT_CYRILLIC;
                    localizedSounds.add("оригинальный звук");
                    contentHashtags.add("#россия");
                    contentHashtags.add("#тиктокроссия");
                    contentHashtags.add("#fypроссия");
                    songKeywords.add("русский");
                    songKeywords.add("русская");
                    songKeywords.add("русские");
                    songKeywords.add("шансон");
                    songKeywords.add("русский фонк");
                    songKeywords.add("russian drill");
                    break;
                case "ua":
                    countryNames.add("ukraine");
                    countryNames.add("украина");
                    countryNames.add("україна");
                    countryNames.add("kyiv");
                    countryNames.add("kiev");
                    uniqueLanguages.add("uk");
                    uniqueLanguages.add("ukr");
                    scriptMask |= SCRIPT_CYRILLIC;
                    localizedSounds.add("оригінальний звук");
                    break;
                case "by":
                    countryNames.add("belarus");
                    countryNames.add("беларусь");
                    countryNames.add("minsk");
                    uniqueLanguages.add("be");
                    uniqueLanguages.add("bel");
                    scriptMask |= SCRIPT_CYRILLIC;
                    localizedSounds.add("оригинальный звук");
                    break;
                case "kz":
                    countryNames.add("kazakhstan");
                    countryNames.add("казахстан");
                    countryNames.add("almaty");
                    countryNames.add("astana");
                    uniqueLanguages.add("kk");
                    uniqueLanguages.add("kaz");
                    scriptMask |= SCRIPT_CYRILLIC;
                    localizedSounds.add("оригинальный звук");
                    break;
                case "jp":
                    countryNames.add("japan");
                    countryNames.add("nihon");
                    countryNames.add("nippon");
                    countryNames.add("日本");
                    countryNames.add("tokyo");
                    countryNames.add("osaka");
                    countryNames.add("kyoto");
                    uniqueLanguages.add("ja");
                    uniqueLanguages.add("jpn");
                    scriptMask |= SCRIPT_JAPANESE;
                    localizedSounds.add("オリジナル楽曲");
                    contentHashtags.add("#tiktokjapan");
                    contentHashtags.add("#fypjapan");
                    contentHashtags.add("#日本");
                    songKeywords.add("j-pop");
                    songKeywords.add("jpop");
                    songKeywords.add("anime song");
                    songKeywords.add("vocaloid");
                    break;
                case "kr":
                    countryNames.add("korea");
                    countryNames.add("south korea");
                    countryNames.add("한국");
                    countryNames.add("seoul");
                    countryNames.add("busan");
                    uniqueLanguages.add("ko");
                    uniqueLanguages.add("kor");
                    scriptMask |= SCRIPT_KOREAN;
                    localizedSounds.add("오리지널 사운드");
                    contentHashtags.add("#tiktokkorea");
                    contentHashtags.add("#fypkorea");
                    contentHashtags.add("#한국");
                    songKeywords.add("k-pop");
                    songKeywords.add("kpop");
                    songKeywords.add("trot");
                    break;
                case "cn":
                case "tw":
                case "hk":
                    countryNames.add("china");
                    countryNames.add("taiwan");
                    countryNames.add("hong kong");
                    localizedSounds.add("原声");
                    localizedSounds.add("原聲");
                    songKeywords.add("c-pop");
                    songKeywords.add("cpop");
                    songKeywords.add("mandopop");
                    songKeywords.add("cantopop");
                    songKeywords.add("douyin");
                    break;
                case "in":
                    countryNames.add("india");
                    countryNames.add("bharat");
                    countryNames.add("hindustan");
                    countryNames.add("mumbai");
                    countryNames.add("delhi");
                    countryNames.add("bangalore");
                    uniqueLanguages.add("hi");
                    uniqueLanguages.add("hin");
                    uniqueLanguages.add("ta");
                    uniqueLanguages.add("te");
                    uniqueLanguages.add("bn");
                    uniqueLanguages.add("mr");
                    uniqueLanguages.add("gu");
                    uniqueLanguages.add("pa");
                    scriptMask |= SCRIPT_DEVANAGARI;
                    localizedSounds.add("मूल ध्वनि");
                    songKeywords.add("bollywood");
                    songKeywords.add("hindi song");
                    songKeywords.add("punjabi song");
                    songKeywords.add("bhojpuri");
                    songKeywords.add("desi");
                    break;
                case "sa":
                case "ae":
                case "eg":
                case "iq":
                case "ma":
                case "dz":
                case "qa":
                case "kw":
                case "om":
                case "jo":
                case "lb":
                case "ly":
                case "tn":
                case "ye":
                case "sy":
                case "sd":
                case "bh":
                    scriptMask |= SCRIPT_ARABIC;
                    localizedSounds.add("صوت أصلي");
                    songKeywords.add("mahraganat");
                    songKeywords.add("dabke");
                    songKeywords.add("khaleeji");
                    songKeywords.add("sheilat");
                    songKeywords.add("shilat");
                    songKeywords.add("شيلات");
                    songKeywords.add("مهرجانات");
                    break;
                case "tr":
                    countryNames.add("turkey");
                    countryNames.add("turkiye");
                    countryNames.add("türkiye");
                    countryNames.add("istanbul");
                    countryNames.add("ankara");
                    uniqueLanguages.add("tr");
                    uniqueLanguages.add("tur");
                    localizedSounds.add("orijinal ses");
                    songKeywords.add("turkce pop");
                    songKeywords.add("arabesk");
                    songKeywords.add("halay");
                    break;
                case "br":
                    countryNames.add("brazil");
                    countryNames.add("brasil");
                    countryNames.add("sao paulo");
                    countryNames.add("rio de janeiro");
                    localizedSounds.add("som original");
                    contentHashtags.add("#tiktokbrasil");
                    contentHashtags.add("#brasil");
                    contentHashtags.add("#fypbrasil");
                    songKeywords.add("funk rj");
                    songKeywords.add("funk sp");
                    songKeywords.add("funk brasil");
                    songKeywords.add("sertanejo");
                    songKeywords.add("pagode");
                    songKeywords.add("forro");
                    songKeywords.add("arrocha");
                    songKeywords.add("piseiro");
                    songKeywords.add("brega funk");
                    break;
                case "pt":
                    countryNames.add("portugal");
                    countryNames.add("lisbon");
                    countryNames.add("porto");
                    localizedSounds.add("som original");
                    break;
                case "es":
                case "mx":
                case "co":
                case "ar":
                case "pe":
                case "cl":
                case "ec":
                case "ve":
                case "gt":
                case "do":
                case "cu":
                    localizedSounds.add("sonido original");
                    songKeywords.add("reggaeton");
                    songKeywords.add("cumbia");
                    songKeywords.add("corridos");
                    songKeywords.add("bachata");
                    songKeywords.add("mariachi");
                    songKeywords.add("norteno");
                    songKeywords.add("banda");
                    break;
                case "de":
                case "at":
                case "ch":
                    countryNames.add("berlin");
                    countryNames.add("munich");
                    localizedSounds.add("originalton");
                    break;
                case "fr":
                    countryNames.add("paris");
                    localizedSounds.add("son original");
                    break;
                case "it":
                    countryNames.add("rome");
                    countryNames.add("milan");
                    localizedSounds.add("audio originale");
                    break;
                case "pl":
                    countryNames.add("warsaw");
                    uniqueLanguages.add("pl");
                    uniqueLanguages.add("pol");
                    localizedSounds.add("dźwięk oryginalny");
                    break;
                case "nl":
                    countryNames.add("amsterdam");
                    uniqueLanguages.add("nl");
                    uniqueLanguages.add("nld");
                    localizedSounds.add("origineel geluid");
                    break;
                case "il":
                    countryNames.add("tel aviv");
                    countryNames.add("jerusalem");
                    uniqueLanguages.add("he");
                    uniqueLanguages.add("iw");
                    uniqueLanguages.add("heb");
                    scriptMask |= SCRIPT_HEBREW;
                    break;
                case "gr":
                    countryNames.add("athens");
                    uniqueLanguages.add("el");
                    uniqueLanguages.add("ell");
                    scriptMask |= SCRIPT_GREEK;
                    localizedSounds.add("πρωτότυπος ήχος");
                    break;
                case "se":
                    countryNames.add("stockholm");
                    uniqueLanguages.add("sv");
                    uniqueLanguages.add("swe");
                    localizedSounds.add("originalljud");
                    break;
            }
        }

        public boolean matchesCountryOrIso(String val) {
            if (val == null || val.isEmpty()) return false;
            String clean = val.trim().toLowerCase(Locale.ROOT);
            if (clean.isEmpty()) return false;
            if (isoCodes.contains(clean)) return true;
            if (countryNames.contains(clean)) return true;
            String iso2 = NAME_TO_ISO2.get(clean);
            return iso2 != null && isoCodes.contains(iso2);
        }

        public boolean containsCountryName(String text) {
            if (text == null || text.isEmpty()) return false;
            String clean = text.toLowerCase(Locale.ROOT);
            for (String name : countryNames) {
                if (clean.contains(name)) return true;
            }
            return false;
        }

        public boolean matchesContentText(String text) {
            if (text == null || text.isEmpty()) return false;
            // 1. Flag emojis
            for (String flag : flagEmojis) {
                if (text.contains(flag)) return true;
            }
            // 2. Hashtags & country names
            String lower = text.toLowerCase(Locale.ROOT);
            for (String tag : contentHashtags) {
                if (lower.contains(tag)) return true;
            }
            for (String name : countryNames) {
                if (name.length() >= 4 && lower.contains(name)) return true;
            }
            // 3. Script detection
            if (matchesScript(text)) {
                return true;
            }
            return false;
        }

        public boolean matchesUniqueLanguage(String lang) {
            if (lang == null || lang.isEmpty() || uniqueLanguages.isEmpty()) return false;
            String clean = lang.trim().toLowerCase(Locale.ROOT);
            return uniqueLanguages.contains(clean);
        }

        public boolean matchesLocalizedSound(String musicName) {
            if (musicName == null || musicName.isEmpty()) return false;
            String lower = musicName.toLowerCase(Locale.ROOT);
            for (String sound : localizedSounds) {
                if (lower.contains(sound)) return true;
            }
            return false;
        }

        public boolean matchesSongKeywords(String text) {
            if (text == null || text.isEmpty() || songKeywords.isEmpty()) return false;
            String lower = text.toLowerCase(Locale.ROOT);
            for (String kw : songKeywords) {
                if (lower.contains(kw)) return true;
            }
            return false;
        }

        public boolean matchesScript(String text) {
            if (text == null || text.isEmpty() || scriptMask == 0) return false;
            int len = text.length();
            for (int i = 0; i < len; i++) {
                char ch = text.charAt(i);
                if ((scriptMask & SCRIPT_THAI) != 0 && ch >= 0x0E00 && ch <= 0x0E7F) return true;
                if ((scriptMask & SCRIPT_CYRILLIC) != 0 && ((ch >= 0x0400 && ch <= 0x04FF) || (ch >= 0x0500 && ch <= 0x052F))) return true;
                if ((scriptMask & SCRIPT_JAPANESE) != 0 && ((ch >= 0x3040 && ch <= 0x309F) || (ch >= 0x30A0 && ch <= 0x30FF))) return true;
                if ((scriptMask & SCRIPT_KOREAN) != 0 && ((ch >= 0xAC00 && ch <= 0xD7AF) || (ch >= 0x1100 && ch <= 0x11FF) || (ch >= 0x3130 && ch <= 0x318F))) return true;
                if ((scriptMask & SCRIPT_ARABIC) != 0 && ((ch >= 0x0600 && ch <= 0x06FF) || (ch >= 0x0750 && ch <= 0x077F) || (ch >= 0x08A0 && ch <= 0x08FF))) return true;
                if ((scriptMask & SCRIPT_DEVANAGARI) != 0 && ch >= 0x0900 && ch <= 0x097F) return true;
                if ((scriptMask & SCRIPT_HEBREW) != 0 && ch >= 0x0590 && ch <= 0x05FF) return true;
                if ((scriptMask & SCRIPT_GREEK) != 0 && ch >= 0x0370 && ch <= 0x03FF) return true;
            }
            return false;
        }

        public boolean matchesMetadataJson(String json) {
            if (json == null || json.isEmpty()) return false;
            String lower = json.toLowerCase(Locale.ROOT);
            for (String iso : isoCodes) {
                if (lower.contains("\"region\":\"" + iso + "\"")
                        || lower.contains("\"country\":\"" + iso + "\"")
                        || lower.contains("\"country_code\":\"" + iso + "\"")
                        || lower.contains("\"source_region\":\"" + iso + "\"")
                        || lower.contains("\"owner_region\":\"" + iso + "\"")
                        || lower.contains("\"music_region\":\"" + iso + "\"")
                        || lower.contains("\"artist_region\":\"" + iso + "\"")
                        || lower.contains("\"user_region\":\"" + iso + "\"")
                        || lower.contains("\"author_region\":\"" + iso + "\"")
                        || lower.contains("\"distribute_region\":\"" + iso + "\"")
                        || lower.contains("\"song_country\":\"" + iso + "\"")
                        || lower.contains("\"language\":\"" + iso + "\"")) {
                    return true;
                }
            }
            return false;
        }
    }
}
