package com.ash.tiktokregion.core;

import android.os.Bundle;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * High-speed in-memory configuration store.
 * Replaces the native Rust engine with zero-JNI, thread-safe memory reads.
 */
public final class ModuleConfig {

    public static volatile boolean enabled = true;
    public static volatile String countryIso = "us";
    public static volatile String operatorMccMnc = "310260";
    public static volatile String operatorName = "T-Mobile";
    public static volatile String mcc = "310";
    public static volatile String mnc = "260";
    public static volatile boolean spoofLocale = false;
    public static volatile String localeLanguage = "en";
    public static volatile String localeCountry = "US";

    // Media & Downloads
    public static volatile boolean noWatermark = true;
    public static volatile boolean bypassDownloadRestriction = true;
    public static volatile boolean downloadStory = true;
    public static volatile boolean forceHighestQuality = true;

    // Feed & Content
    public static volatile boolean hideAds = true;
    public static volatile boolean forceRegion = true;
    public static volatile boolean strictForceRegion = false;
    public static volatile boolean filterNonHd = true;
    public static volatile boolean blockCountries = true;
    public static volatile boolean blockCountrySongs = true;
    public static volatile Set<String> blockedCountries = Collections.emptySet();

    // Social & Privacy
    public static volatile boolean ghostStoryView = true;
    public static volatile boolean ghostReadReceipts = true;
    public static volatile boolean ghostTyping = true;
    public static volatile boolean antiDelete = true;

    // UI Tweaks
    public static volatile boolean suppressPopups = true;
    public static volatile boolean masterUpload = true;

    private ModuleConfig() {}

    /**
     * Atomically refreshes configuration parameters from IPC bundle.
     */
    public static void updateFromBundle(Bundle b) {
        if (b == null) return;

        enabled = b.getBoolean("enabled", enabled);

        // Support both "sim_country" and "country_iso"
        if (b.containsKey("sim_country")) {
            countryIso = b.getString("sim_country", countryIso);
        } else if (b.containsKey("country_iso")) {
            countryIso = b.getString("country_iso", countryIso);
        }
        if (countryIso != null) {
            countryIso = countryIso.toLowerCase(Locale.ROOT).trim();
        }

        // Support both "sim_operator" and "operator_mcc_mnc"
        if (b.containsKey("sim_operator")) {
            operatorMccMnc = b.getString("sim_operator", operatorMccMnc);
        } else if (b.containsKey("operator_mcc_mnc")) {
            operatorMccMnc = b.getString("operator_mcc_mnc", operatorMccMnc);
        }

        operatorName = b.getString("operator_name", operatorName);
        mcc = b.getString("mcc", mcc);
        mnc = b.getString("mnc", mnc);
        spoofLocale = b.getBoolean("spoof_locale", spoofLocale);
        localeLanguage = b.getString("locale_lang", localeLanguage);
        localeCountry = b.getString("locale_country", localeCountry);

        noWatermark = b.getBoolean("no_watermark", noWatermark);
        bypassDownloadRestriction = b.getBoolean("bypass_download_restriction", bypassDownloadRestriction);
        downloadStory = b.getBoolean("download_story", downloadStory);
        forceHighestQuality = b.getBoolean("force_highest_quality", forceHighestQuality);

        hideAds = b.getBoolean("hide_ads", hideAds);
        forceRegion = b.getBoolean("force_region", forceRegion);
        strictForceRegion = b.getBoolean("strict_force_region", strictForceRegion);
        filterNonHd = b.getBoolean("filter_non_hd", filterNonHd);
        blockCountries = b.getBoolean("block_countries", blockCountries);
        blockCountrySongs = b.getBoolean("block_country_songs", blockCountrySongs);

        String blockedCsv = b.containsKey("blocked_country_list")
                ? b.getString("blocked_country_list", "")
                : b.getString("blocked_countries_csv", "");
        if (blockedCsv != null && !blockedCsv.isEmpty()) {
            Set<String> set = new HashSet<>();
            for (String part : blockedCsv.split("[,;]")) {
                String clean = part.trim().toLowerCase(Locale.ROOT);
                if (!clean.isEmpty()) {
                    set.add(clean);
                }
            }
            blockedCountries = Collections.unmodifiableSet(set);
            if (!set.isEmpty()) {
                blockCountries = true;
            }
        } else {
            blockedCountries = Collections.emptySet();
        }

        if (b.containsKey("anonymous_view")) {
            ghostStoryView = b.getBoolean("anonymous_view", ghostStoryView);
        } else {
            ghostStoryView = b.getBoolean("ghost_story_view", ghostStoryView);
        }

        ghostReadReceipts = b.getBoolean("ghost_read_receipts", ghostReadReceipts);
        ghostTyping = b.getBoolean("ghost_typing", ghostTyping);

        if (b.containsKey("anti_delete_msg")) {
            antiDelete = b.getBoolean("anti_delete_msg", antiDelete);
        } else {
            antiDelete = b.getBoolean("anti_delete", antiDelete);
        }

        if (b.containsKey("disable_popups")) {
            suppressPopups = b.getBoolean("disable_popups", suppressPopups);
        } else {
            suppressPopups = b.getBoolean("suppress_popups", suppressPopups);
        }

        if (b.containsKey("force_master_upload")) {
            masterUpload = b.getBoolean("force_master_upload", masterUpload);
        } else {
            masterUpload = b.getBoolean("master_upload", masterUpload);
        }
    }

    public static boolean isCountryBlocked(String iso) {
        if (!enabled || iso == null) {
            return false;
        }
        return blockedCountries.contains(iso.trim().toLowerCase(Locale.ROOT));
    }

    public static String getTelephonyString(String key, String fallback) {
        if (!enabled) {
            return fallback;
        }
        switch (key) {
            case "simCountryIso":
            case "networkCountryIso":
                return countryIso;
            case "simOperator":
            case "networkOperator":
                return operatorMccMnc;
            case "simOperatorName":
            case "networkOperatorName":
                return operatorName;
            case "mcc":
                return mcc;
            case "mnc":
                return mnc;
            case "subscriberId":
                return operatorMccMnc + "123456789";
            case "simSerialNumber":
                return "8901260" + operatorMccMnc + "12345";
            case "line1Number":
                return "";
            default:
                return fallback;
        }
    }

    public static int getTelephonyInt(String key, int fallback) {
        if (!enabled) {
            return fallback;
        }
        switch (key) {
            case "simState":
                return 5; // SIM_STATE_READY
            case "phoneType":
                return 1; // PHONE_TYPE_GSM
            case "hasIccCard":
                return 1; // true
            case "isNetworkRoaming":
                return 0; // false
            case "activeSubscriptionInfoCount":
            case "defaultSubscriptionId":
            case "defaultDataSubscriptionId":
                return 1;
            default:
                return fallback;
        }
    }

    public static String getSystemProperty(String key, String fallback) {
        if (!enabled || key == null) {
            return fallback;
        }
        if (key.startsWith("gsm.sim.operator.iso-country") || key.startsWith("gsm.operator.iso-country")) {
            return countryIso;
        }
        if (key.startsWith("gsm.sim.operator.numeric") || key.startsWith("gsm.operator.numeric")) {
            return operatorMccMnc;
        }
        if (key.startsWith("gsm.sim.operator.alpha") || key.startsWith("gsm.operator.alpha")) {
            return operatorName;
        }
        if (key.startsWith("gsm.sim.state")) {
            return "LOADED";
        }
        return fallback;
    }

    public static String getCronetParam(String key, String fallback) {
        if (!enabled) {
            return fallback;
        }
        switch (key) {
            case "getRegion":
            case "getCarrierRegion":
            case "getSysRegion":
            case "getAppInitialRegionInfo":
                return countryIso.toUpperCase(Locale.ROOT);
            case "getSimOperator":
                return operatorMccMnc;
            default:
                return fallback;
        }
    }
}
