package com.ash.tiktokregion;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

public class ConfigProvider extends ContentProvider {

    public static final String AUTHORITY = "com.ash.tiktokregion.provider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
    public static final String METHOD_GET_CONFIG = "get_config";

    public static final String KEY_ENABLED = "enabled";
    public static final String KEY_SIM_COUNTRY = "sim_country";
    public static final String KEY_SIM_OPERATOR = "sim_operator";
    public static final String KEY_OPERATOR_NAME = "operator_name";
    public static final String KEY_MCC = "mcc";
    public static final String KEY_MNC = "mnc";
    public static final String KEY_SPOOF_LOCALE = "spoof_locale";
    public static final String KEY_LOCALE_LANG = "locale_lang";
    public static final String KEY_LOCALE_COUNTRY = "locale_country";
    public static final String KEY_NO_WATERMARK = "no_watermark";
    public static final String KEY_BYPASS_DOWNLOAD_RESTRICTION = "bypass_download_restriction";
    public static final String KEY_HIDE_ADS = "hide_ads";
    public static final String KEY_FORCE_REGION = "force_region";
    public static final String KEY_STRICT_FORCE_REGION = "strict_force_region";
    public static final String KEY_DOWNLOAD_STORY = "download_story";
    public static final String KEY_BLOCK_COUNTRIES = "block_countries";
    public static final String KEY_BLOCKED_COUNTRY_LIST = "blocked_country_list";
    public static final String KEY_ACTIVE_PROFILE = "active_profile";
    public static final String PREF_LAST_PING = "last_hook_ping";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (METHOD_GET_CONFIG.equals(method)) {
            Context context = getContext();
            if (context != null) {
                SharedPreferences sp = context.getSharedPreferences(MainActivity.PREF_NAME, Context.MODE_PRIVATE);

                sp.edit().putLong(PREF_LAST_PING, System.currentTimeMillis()).apply();

                CountryPreset defPreset = CountryPreset.getDefault();
                String presetId = sp.getString(MainActivity.KEY_PRESET_ID, defPreset.getId());
                CountryPreset preset = CountryPreset.getById(presetId);

                boolean enabled = sp.getBoolean(MainActivity.KEY_ENABLED, true);
                boolean isCustom = sp.getBoolean(MainActivity.KEY_IS_CUSTOM, false);
                boolean spoofLocale = sp.getBoolean(MainActivity.KEY_SPOOF_LOCALE, false);
                boolean noWatermark = sp.getBoolean(MainActivity.KEY_NO_WATERMARK, true);
                boolean bypassDownload = sp.getBoolean(MainActivity.KEY_BYPASS_DOWNLOAD_RESTRICTION, true);
                boolean hideAds = sp.getBoolean(MainActivity.KEY_HIDE_ADS, true);
                boolean forceRegion = sp.getBoolean(MainActivity.KEY_FORCE_REGION, true);
                boolean strictForceRegion = sp.getBoolean(MainActivity.KEY_STRICT_FORCE_REGION, false);
                boolean downloadStory = sp.getBoolean(MainActivity.KEY_DOWNLOAD_STORY, true);
                boolean blockCountries = sp.getBoolean(MainActivity.KEY_BLOCK_COUNTRIES, false);
                String blockedCountryList = sp.getString(MainActivity.KEY_BLOCKED_COUNTRY_LIST, "");
                String activeProfile = sp.getString(MainActivity.KEY_ACTIVE_PROFILE, MainActivity.PROFILE_GLOBAL);

                String countryIso = isCustom
                        ? sp.getString(MainActivity.KEY_CUSTOM_ISO, preset.getCountryIso())
                        : preset.getCountryIso();
                String operatorMccMnc = isCustom
                        ? sp.getString(MainActivity.KEY_CUSTOM_OPERATOR, preset.getOperatorMccMnc())
                        : preset.getOperatorMccMnc();
                String operatorName = isCustom
                        ? sp.getString(MainActivity.KEY_CUSTOM_NAME, preset.getOperatorName())
                        : preset.getOperatorName();

                String mcc = preset.getMcc();
                String mnc = preset.getMnc();
                if (operatorMccMnc != null && operatorMccMnc.length() >= 5) {
                    mcc = operatorMccMnc.substring(0, 3);
                    mnc = operatorMccMnc.substring(3);
                }

                Bundle bundle = new Bundle();
                bundle.putBoolean(KEY_ENABLED, enabled);
                bundle.putString(KEY_ACTIVE_PROFILE, activeProfile);
                bundle.putString(KEY_SIM_COUNTRY, countryIso.toLowerCase());
                bundle.putString(KEY_SIM_OPERATOR, operatorMccMnc);
                bundle.putString(KEY_OPERATOR_NAME, operatorName);
                bundle.putString(KEY_MCC, mcc);
                bundle.putString(KEY_MNC, mnc);
                bundle.putBoolean(KEY_SPOOF_LOCALE, spoofLocale);
                bundle.putString(KEY_LOCALE_LANG, preset.getLocaleLanguage());
                bundle.putString(KEY_LOCALE_COUNTRY, preset.getLocaleCountry());
                bundle.putBoolean(KEY_NO_WATERMARK, noWatermark);
                bundle.putBoolean(KEY_BYPASS_DOWNLOAD_RESTRICTION, bypassDownload);
                bundle.putBoolean(KEY_HIDE_ADS, hideAds);
                bundle.putBoolean(KEY_FORCE_REGION, forceRegion);
                bundle.putBoolean(KEY_STRICT_FORCE_REGION, strictForceRegion);
                bundle.putBoolean(KEY_DOWNLOAD_STORY, downloadStory);
                bundle.putBoolean(KEY_BLOCK_COUNTRIES, blockCountries);
                bundle.putString(KEY_BLOCKED_COUNTRY_LIST, blockedCountryList);

                return bundle;
            }
        }
        return super.call(method, arg, extras);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}

