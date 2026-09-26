package com.ash.tiktokregion;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import java.io.DataOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    public static final String PREF_NAME = "config";
    public static final String KEY_ENABLED = "enabled";
    public static final String KEY_PRESET_ID = "preset_id";
    public static final String KEY_IS_CUSTOM = "is_custom";
    public static final String KEY_CUSTOM_ISO = "custom_iso";
    public static final String KEY_CUSTOM_OPERATOR = "custom_operator";
    public static final String KEY_CUSTOM_NAME = "custom_name";
    public static final String KEY_SPOOF_LOCALE = "spoof_locale";
    public static final String KEY_HIDE_ICON = "hide_app_icon";
    public static final String KEY_NO_WATERMARK = "no_watermark";
    public static final String KEY_BYPASS_DOWNLOAD_RESTRICTION = "bypass_download_restriction";
    public static final String KEY_HD_UPLOAD = "hd_upload";
    public static final String KEY_UPLOAD_4K = "upload_4k";
    public static final String KEY_FORCE_HIGH_QUALITY = "force_high_quality";
    public static final String KEY_TELEMETRY_HUD = "telemetry_hud";
    public static final String KEY_TELEMETRY_POPUP = "telemetry_popup";
    public static final String KEY_HIDE_ADS = "hide_ads";
    public static final String KEY_FORCE_REGION = "force_region";
    public static final String KEY_STRICT_FORCE_REGION = "strict_force_region";
    public static final String KEY_LOCKED_REGION_FILTER = "locked_region_filter";
    public static final String KEY_BLOCK_COUNTRIES = "block_countries";
    public static final String KEY_BLOCKED_COUNTRY_LIST = "blocked_country_list";
    public static final String KEY_LANGUAGE_FILTER = "language_filter";
    public static final String KEY_ALLOWED_LANGUAGES = "allowed_languages";
    public static final String KEY_DOWNLOAD_STORY = "download_story";
    public static final String KEY_HIDE_NEARBY_TAB = "hide_nearby_tab";

    public static final String KEY_ACTIVE_PROFILE = "active_profile";
    public static final String PROFILE_GLOBAL = "global";
    public static final String PROFILE_ASIA = "asia";
    public static final String PROFILE_CHINA = "china";

    public static final String PACKAGE_GLOBAL = "com.zhiliaoapp.musically";
    public static final String PACKAGE_ASIA = "com.ss.android.ugc.trill";
    public static final String PACKAGE_CHINA = "com.ss.android.ugc.aweme";

    private SharedPreferences mPrefs;
    private String mCurrentProfile = PROFILE_GLOBAL;
    private View mTabGlobal;
    private View mTabAsia;
    private View mTabChina;
    private TextView mTvTabGlobal;
    private TextView mTvTabAsia;
    private TextView mTvTabChina;
    private View mBtnHamburger;
    private View mLayoutHeaderProfile;
    private TextView mTvActiveProfileTitle;
    private TextView mTvActiveProfileBadge;
    private TextView mTvActiveProfilePackage;
    private TextView mTvNoWatermarkTitle;
    private TextView mTvNoWatermarkDesc;
    private View mLayoutChinaInfo;
    private View mLayoutRegionSection;

    private View mRowForceRegion;
    private View mDividerForceRegion;
    private View mRowStrictForceRegion;
    private View mDividerStrictForceRegion;
    private View mRowLockedRegion;
    private View mDividerLockedRegion;
    private View mRowBlockCountries;
    private View mDividerBlockCountries;
    private View mRowLanguageFilter;
    private View mDividerLanguageFilter;
    private View mRowEnableSpoof;
    private View mDividerEnableSpoof;
    private View mRowSpoofLocale;

    private MaterialSwitch mSwitchSpoof;
    private MaterialSwitch mSwitchLocale;
    private MaterialSwitch mSwitchHideIcon;
    private MaterialSwitch mSwitchForceRegion;
    private MaterialSwitch mSwitchStrictForceRegion;
    private MaterialSwitch mSwitchLockedRegion;
    private MaterialSwitch mSwitchBlockCountries;
    private View mBtnManageBlockedCountries;
    private TextView mTvBlockedCountriesSummary;
    private MaterialSwitch mSwitchLanguageFilter;
    private View mBtnManageLanguages;
    private TextView mTvLanguagesSummary;
    private MaterialSwitch mSwitchDownloadStory;
    private View mDividerDownloadStory;
    private MaterialSwitch mSwitchHideAds;
    private MaterialSwitch mSwitchNoWatermark;
    private MaterialSwitch mSwitchBypassDownload;
    private MaterialSwitch mSwitchHDUpload;
    private View mRowHDUpload;
    private MaterialSwitch mSwitchUpload4K;
    private View mRowUpload4K;
    private View mDividerUpload4K;
    private MaterialSwitch mSwitchForceHighQuality;
    private MaterialSwitch mSwitchTelemetryHUD;
    private MaterialSwitch mSwitchTelemetryPopup;
    private MaterialSwitch mSwitchHideNearbyTab;
    private View mRowHideNearbyTab;
    private View mDividerHideNearbyTab;
    private MaterialSwitch mSwitchCustomOverride;
    private LinearLayout mLayoutCustomInputs;
    private TextInputEditText mEtCustomIso;
    private TextInputEditText mEtCustomOperator;
    private TextInputEditText mEtCustomName;
    private TextView mTvCurrentCountry;
    private TextView mTvCurrentCarrierInfo;
    private Button mBtnForceStop;
    private Button mBtnLaunch;

    private static final String[] TIKTOK_PACKAGES = {
            PACKAGE_GLOBAL,
            PACKAGE_ASIA,
            PACKAGE_CHINA,
            "com.zhiliaoapp.musically.go",
            "com.tiktok.business"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPrefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        View dotStatus = findViewById(R.id.dot_status);
        TextView tvStatus = findViewById(R.id.tv_status);

        mBtnHamburger = findViewById(R.id.btn_hamburger_menu);
        mLayoutHeaderProfile = findViewById(R.id.layout_header_profile);
        mTvActiveProfileTitle = findViewById(R.id.tv_active_profile_title);
        mTvActiveProfileBadge = findViewById(R.id.tv_active_profile_badge);
        mTvActiveProfilePackage = findViewById(R.id.tv_active_profile_package);

        mTvNoWatermarkTitle = findViewById(R.id.tv_no_watermark_title);
        mTvNoWatermarkDesc = findViewById(R.id.tv_no_watermark_desc);

        mLayoutChinaInfo = findViewById(R.id.layout_china_info);
        mLayoutRegionSection = findViewById(R.id.layout_region_section);

        mRowForceRegion = findViewById(R.id.row_force_region);
        mDividerForceRegion = findViewById(R.id.divider_force_region);
        mRowStrictForceRegion = findViewById(R.id.row_strict_force_region);
        mDividerStrictForceRegion = findViewById(R.id.divider_strict_force_region);
        mRowLockedRegion = findViewById(R.id.row_locked_region);
        mDividerLockedRegion = findViewById(R.id.divider_locked_region);
        mRowBlockCountries = findViewById(R.id.row_block_countries);
        mDividerBlockCountries = findViewById(R.id.divider_block_countries);
        mRowLanguageFilter = findViewById(R.id.row_language_filter);
        mDividerLanguageFilter = findViewById(R.id.divider_language_filter);
        mRowEnableSpoof = findViewById(R.id.row_enable_spoof);
        mDividerEnableSpoof = findViewById(R.id.divider_enable_spoof);
        mRowSpoofLocale = findViewById(R.id.row_spoof_locale);

        mTvCurrentCountry = findViewById(R.id.tv_current_country);
        mTvCurrentCarrierInfo = findViewById(R.id.tv_current_carrier_info);
        View btnSelectPreset = findViewById(R.id.btn_select_preset);

        mSwitchCustomOverride = findViewById(R.id.switch_custom_override);
        mLayoutCustomInputs = findViewById(R.id.layout_custom_inputs);
        mEtCustomIso = findViewById(R.id.et_custom_iso);
        mEtCustomOperator = findViewById(R.id.et_custom_operator);
        mEtCustomName = findViewById(R.id.et_custom_name);
        Button btnSaveCustom = findViewById(R.id.btn_save_custom);

        mSwitchForceRegion = findViewById(R.id.switch_force_region);
        mSwitchStrictForceRegion = findViewById(R.id.switch_strict_force_region);
        mSwitchLockedRegion = findViewById(R.id.switch_locked_region);
        mSwitchBlockCountries = findViewById(R.id.switch_block_countries);
        mBtnManageBlockedCountries = findViewById(R.id.btn_manage_blocked_countries);
        mTvBlockedCountriesSummary = findViewById(R.id.tv_blocked_countries_summary);
        mSwitchLanguageFilter = findViewById(R.id.switch_language_filter);
        mBtnManageLanguages = findViewById(R.id.btn_manage_languages);
        mTvLanguagesSummary = findViewById(R.id.tv_languages_summary);
        mSwitchDownloadStory = findViewById(R.id.switch_download_story);
        mDividerDownloadStory = findViewById(R.id.divider_download_story);
        mSwitchHideAds = findViewById(R.id.switch_hide_ads);
        mSwitchNoWatermark = findViewById(R.id.switch_no_watermark);
        mSwitchBypassDownload = findViewById(R.id.switch_bypass_download);
        mSwitchHDUpload = findViewById(R.id.switch_hd_upload);
        mRowHDUpload = findViewById(R.id.row_hd_upload);
        mSwitchUpload4K = findViewById(R.id.switch_upload_4k);
        mRowUpload4K = findViewById(R.id.row_upload_4k);
        mDividerUpload4K = findViewById(R.id.divider_upload_4k);
        mSwitchForceHighQuality = findViewById(R.id.switch_force_high_quality);
        mSwitchTelemetryHUD = findViewById(R.id.switch_telemetry_hud);
        mSwitchTelemetryPopup = findViewById(R.id.switch_telemetry_popup);
        mRowHideNearbyTab = findViewById(R.id.row_hide_nearby_tab);
        mDividerHideNearbyTab = findViewById(R.id.divider_hide_nearby_tab);
        mSwitchHideNearbyTab = findViewById(R.id.switch_hide_nearby_tab);
        mSwitchSpoof = findViewById(R.id.switch_enable_spoof);
        mSwitchLocale = findViewById(R.id.switch_spoof_locale);
        mSwitchHideIcon = findViewById(R.id.switch_hide_icon);

        mBtnForceStop = findViewById(R.id.btn_force_stop_tiktok);
        mBtnLaunch = findViewById(R.id.btn_launch_tiktok);

        mTabGlobal = findViewById(R.id.tab_profile_global);
        mTabAsia = findViewById(R.id.tab_profile_asia);
        mTabChina = findViewById(R.id.tab_profile_china);
        mTvTabGlobal = findViewById(R.id.tv_tab_global);
        mTvTabAsia = findViewById(R.id.tv_tab_asia);
        mTvTabChina = findViewById(R.id.tv_tab_china);

        if (mTabGlobal != null) {
            mTabGlobal.setOnClickListener(v -> setProfile(PROFILE_GLOBAL));
        }
        if (mTabAsia != null) {
            mTabAsia.setOnClickListener(v -> setProfile(PROFILE_ASIA));
        }
        if (mTabChina != null) {
            mTabChina.setOnClickListener(v -> setProfile(PROFILE_CHINA));
        }

        View layoutSegmentProfiles = findViewById(R.id.layout_segment_profiles);
        boolean globalInstalled = isAppInstalled(PACKAGE_GLOBAL);
        boolean asiaInstalled = isAppInstalled(PACKAGE_ASIA);
        boolean chinaInstalled = isAppInstalled(PACKAGE_CHINA);
        int installedCount = (globalInstalled ? 1 : 0) + (asiaInstalled ? 1 : 0) + (chinaInstalled ? 1 : 0);

        if (layoutSegmentProfiles != null) {
            if (installedCount > 1) {
                layoutSegmentProfiles.setVisibility(View.VISIBLE);
                if (mTabGlobal != null) mTabGlobal.setVisibility(globalInstalled ? View.VISIBLE : View.GONE);
                if (mTabAsia != null) mTabAsia.setVisibility(asiaInstalled ? View.VISIBLE : View.GONE);
                if (mTabChina != null) mTabChina.setVisibility(chinaInstalled ? View.VISIBLE : View.GONE);
            } else {
                // When 1 or fewer apps are installed, do not show the profile switcher menu
                layoutSegmentProfiles.setVisibility(View.GONE);
            }
        }

        if (mBtnHamburger != null) {
            mBtnHamburger.setOnClickListener(v -> showProfilePickerBottomSheet());
        }
        if (mLayoutHeaderProfile != null) {
            mLayoutHeaderProfile.setOnClickListener(v -> showProfilePickerBottomSheet());
        }

        String savedProfile = mPrefs.getString(KEY_ACTIVE_PROFILE, PROFILE_GLOBAL);
        if (!isAppInstalled(getTargetPackageForProfile(savedProfile))) {
            if (isAppInstalled(PACKAGE_GLOBAL)) {
                savedProfile = PROFILE_GLOBAL;
            } else if (isAppInstalled(PACKAGE_ASIA)) {
                savedProfile = PROFILE_ASIA;
            } else if (isAppInstalled(PACKAGE_CHINA)) {
                savedProfile = PROFILE_CHINA;
            }
        }
        setProfile(savedProfile);

        boolean active = isModuleActive();
        if (active) {
            dotStatus.setBackgroundResource(R.drawable.bg_dot_active);
            tvStatus.setText(R.string.status_active);
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        } else {
            dotStatus.setBackgroundResource(R.drawable.bg_dot_inactive);
            tvStatus.setText(R.string.status_inactive);
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        }

        boolean enabled = mPrefs.getBoolean(KEY_ENABLED, true);
        boolean spoofLocale = mPrefs.getBoolean(KEY_SPOOF_LOCALE, false);
        boolean isCustom = mPrefs.getBoolean(KEY_IS_CUSTOM, false);
        boolean isIconHidden = isLauncherIconHidden();
        boolean forceRegion = mPrefs.getBoolean(KEY_FORCE_REGION, true);
        boolean strictForceRegion = mPrefs.getBoolean(KEY_STRICT_FORCE_REGION, false);
        boolean lockedRegionFilter = mPrefs.getBoolean(KEY_LOCKED_REGION_FILTER, false);
        boolean blockCountries = mPrefs.getBoolean(KEY_BLOCK_COUNTRIES, false);
        boolean languageFilter = mPrefs.getBoolean(KEY_LANGUAGE_FILTER, false);
        boolean downloadStory = mPrefs.getBoolean(KEY_DOWNLOAD_STORY, true);
        boolean hideAds = mPrefs.getBoolean(KEY_HIDE_ADS, true);
        boolean noWatermark = mPrefs.getBoolean(KEY_NO_WATERMARK, true);
        boolean bypassDownload = mPrefs.getBoolean(KEY_BYPASS_DOWNLOAD_RESTRICTION, true);
        boolean hdUpload = mPrefs.getBoolean(KEY_HD_UPLOAD, true);
        boolean upload4K = mPrefs.getBoolean(KEY_UPLOAD_4K, false);
        boolean forceHighQuality = mPrefs.getBoolean(KEY_FORCE_HIGH_QUALITY, true);
        boolean telemetryHUD = mPrefs.getBoolean(KEY_TELEMETRY_HUD, true);
        boolean telemetryPopup = mPrefs.getBoolean(KEY_TELEMETRY_POPUP, true);
        boolean hideNearbyTab = mPrefs.getBoolean(KEY_HIDE_NEARBY_TAB, false);

        mSwitchSpoof.setChecked(enabled);
        mSwitchLocale.setChecked(spoofLocale);
        mSwitchHideIcon.setChecked(isIconHidden);
        mSwitchForceRegion.setChecked(forceRegion);
        mSwitchStrictForceRegion.setChecked(strictForceRegion);
        if (mSwitchLockedRegion != null) mSwitchLockedRegion.setChecked(lockedRegionFilter);
        mSwitchBlockCountries.setChecked(blockCountries);
        if (mSwitchLanguageFilter != null) mSwitchLanguageFilter.setChecked(languageFilter);
        mSwitchDownloadStory.setChecked(downloadStory);
        mSwitchHideAds.setChecked(hideAds);
        mSwitchNoWatermark.setChecked(noWatermark);
        mSwitchBypassDownload.setChecked(bypassDownload);
        if (mSwitchHDUpload != null) mSwitchHDUpload.setChecked(hdUpload);
        if (mSwitchUpload4K != null) mSwitchUpload4K.setChecked(upload4K);
        if (mSwitchForceHighQuality != null) mSwitchForceHighQuality.setChecked(forceHighQuality);
        if (mSwitchTelemetryHUD != null) mSwitchTelemetryHUD.setChecked(telemetryHUD);
        if (mSwitchTelemetryPopup != null) mSwitchTelemetryPopup.setChecked(telemetryPopup);
        if (mSwitchHideNearbyTab != null) mSwitchHideNearbyTab.setChecked(hideNearbyTab);
        mSwitchCustomOverride.setChecked(isCustom);
        mLayoutCustomInputs.setVisibility(isCustom ? View.VISIBLE : View.GONE);

        updateRegionDisplay();
        updateBlockedCountriesSummary();
        updateLanguagesSummary();

        btnSelectPreset.setOnClickListener(v -> showSearchablePresetDialog());

        mSwitchCustomOverride.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mLayoutCustomInputs.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            mPrefs.edit().putBoolean(KEY_IS_CUSTOM, isChecked).apply();
            saveToDeviceProtectedStorage(KEY_IS_CUSTOM, isChecked);
            updateRegionDisplay();
        });

        btnSaveCustom.setOnClickListener(v -> {
            String iso = mEtCustomIso.getText() != null ? mEtCustomIso.getText().toString().trim().toLowerCase() : "";
            String op = mEtCustomOperator.getText() != null ? mEtCustomOperator.getText().toString().trim() : "";
            String name = mEtCustomName.getText() != null ? mEtCustomName.getText().toString().trim() : "";

            if (iso.length() != 2) {
                Toast.makeText(this, "Enter a valid 2-letter country code (e.g. us, sg, my)", Toast.LENGTH_SHORT).show();
                return;
            }

            mPrefs.edit()
                    .putBoolean(KEY_IS_CUSTOM, true)
                    .putString(KEY_CUSTOM_ISO, iso)
                    .putString(KEY_CUSTOM_OPERATOR, op)
                    .putString(KEY_CUSTOM_NAME, name)
                    .apply();

            saveToDeviceProtectedStorage(KEY_IS_CUSTOM, true);
            saveToDeviceProtectedStorage(KEY_CUSTOM_ISO, iso);
            saveToDeviceProtectedStorage(KEY_CUSTOM_OPERATOR, op);
            saveToDeviceProtectedStorage(KEY_CUSTOM_NAME, name);
            makePrefsWorldReadable();

            updateRegionDisplay();
            Toast.makeText(this, R.string.pref_saved_notice, Toast.LENGTH_SHORT).show();
        });

        mSwitchForceRegion.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && mSwitchStrictForceRegion != null && mSwitchStrictForceRegion.isChecked()) {
                mSwitchStrictForceRegion.setChecked(false);
            }
            mPrefs.edit().putBoolean(KEY_FORCE_REGION, isChecked).apply();
            saveToDeviceProtectedStorage(KEY_FORCE_REGION, isChecked);
            makePrefsWorldReadable();
            Toast.makeText(this, R.string.pref_saved_notice, Toast.LENGTH_SHORT).show();
        });

        mSwitchStrictForceRegion.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && mSwitchForceRegion != null && mSwitchForceRegion.isChecked()) {
                mSwitchForceRegion.setChecked(false);
            }
            mPrefs.edit().putBoolean(KEY_STRICT_FORCE_REGION, isChecked).apply();
            saveToDeviceProtectedStorage(KEY_STRICT_FORCE_REGION, isChecked);
            makePrefsWorldReadable();
            Toast.makeText(this, R.string.pref_saved_notice, Toast.LENGTH_SHORT).show();
        });

        if (mSwitchLockedRegion != null) {
            mSwitchLockedRegion.setOnCheckedChangeListener((buttonView, isChecked) -> {
                mPrefs.edit().putBoolean(KEY_LOCKED_REGION_FILTER, isChecked).apply();
                saveToDeviceProtectedStorage(KEY_LOCKED_REGION_FILTER, isChecked);
                makePrefsWorldReadable();
                Toast.makeText(this, R.string.pref_saved_notice, Toast.LENGTH_SHORT).show();
            });
        }

        mSwitchBlockCountries.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mPrefs.edit().putBoolean(KEY_BLOCK_COUNTRIES, isChecked).apply();
            saveToDeviceProtectedStorage(KEY_BLOCK_COUNTRIES, isChecked);
            makePrefsWorldReadable();
            Toast.makeText(this, R.string.pref_saved_notice, Toast.LENGTH_SHORT).show();
        });

        if (mBtnManageBlockedCountries != null) {
            mBtnManageBlockedCountries.setOnClickListener(v -> showBlockedCountriesDialog());
        }

        if (mSwitchLanguageFilter != null) {
            mSwitchLanguageFilter.setOnCheckedChangeListener((buttonView, isChecked) -> {
                mPrefs.edit().putBoolean(KEY_LANGUAGE_FILTER, isChecked).apply();
                saveToDeviceProtectedStorage(KEY_LANGUAGE_FILTER, isChecked);
                makePrefsWorldReadable();
                Toast.makeText(this, R.string.pref_saved_notice, Toast.LENGTH_SHORT).show();
            });
        }

        if (mBtnManageLanguages != null) {
            mBtnManageLanguages.setOnClickListener(v -> showLanguageSelectionDialog());
        }

        mSwitchDownloadStory.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mPrefs.edit().putBoolean(KEY_DOWNLOAD_STORY, isChecked).apply();
            saveToDeviceProtectedStorage(KEY_DOWNLOAD_STORY, isChecked);
            makePrefsWorldReadable();
            Toast.makeText(this, R.string.pref_saved_notice, Toast.LENGTH_SHORT).show();
        });

        mSwitchHideAds.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mPrefs.edit().putBoolean(KEY_HIDE_ADS, isChecked).apply();
            saveToDeviceProtectedStorage(KEY_HIDE_ADS, isChecked);
            makePrefsWorldReadable();
            Toast.makeText(this, R.string.pref_saved_notice, Toast.LENGTH_SHORT).show();
        });

        mSwitchNoWatermark.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mPrefs.edit().putBoolean(KEY_NO_WATERMARK, isChecked).apply();
            saveToDeviceProtectedStorage(KEY_NO_WATERMARK, isChecked);
            makePrefsWorldReadable();
            Toast.makeText(this, R.string.pref_saved_notice, Toast.LENGTH_SHORT).show();
        });

        mSwitchBypassDownload.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mPrefs.edit().putBoolean(KEY_BYPASS_DOWNLOAD_RESTRICTION, isChecked).apply();
            saveToDeviceProtectedStorage(KEY_BYPASS_DOWNLOAD_RESTRICTION, isChecked);
            makePrefsWorldReadable();
            Toast.makeText(this, R.string.pref_saved_notice, Toast.LENGTH_SHORT).show();
        });

        if (mSwitchHDUpload != null) {
            mSwitchHDUpload.setOnCheckedChangeListener((buttonView, isChecked) -> {
                mPrefs.edit().putBoolean(KEY_HD_UPLOAD, isChecked).commit();
                saveToDeviceProtectedStorage(KEY_HD_UPLOAD, isChecked);
                if (!isChecked && mSwitchUpload4K != null && mSwitchUpload4K.isChecked()) {
                    mSwitchUpload4K.setChecked(false);
                }
                makePrefsWorldReadable();
                Toast.makeText(this, R.string.pref_saved_notice, Toast.LENGTH_SHORT).show();
            });
        }

        if (mSwitchUpload4K != null) {
            mSwitchUpload4K.setOnCheckedChangeListener((buttonView, isChecked) -> {
                mPrefs.edit().putBoolean(KEY_UPLOAD_4K, isChecked).commit();
                saveToDeviceProtectedStorage(KEY_UPLOAD_4K, isChecked);
                if (isChecked && mSwitchHDUpload != null && !mSwitchHDUpload.isChecked()) {
                    mSwitchHDUpload.setChecked(true);
                }
                makePrefsWorldReadable();
                Toast.makeText(this, R.string.pref_saved_notice, Toast.LENGTH_SHORT).show();
            });
        }

        if (mSwitchForceHighQuality != null) {
            mSwitchForceHighQuality.setOnCheckedChangeListener((buttonView, isChecked) -> {
                mPrefs.edit().putBoolean(KEY_FORCE_HIGH_QUALITY, isChecked).apply();
                saveToDeviceProtectedStorage(KEY_FORCE_HIGH_QUALITY, isChecked);
                makePrefsWorldReadable();
                Toast.makeText(this, R.string.pref_saved_notice, Toast.LENGTH_SHORT).show();
            });
        }

        if (mSwitchTelemetryHUD != null) {
            mSwitchTelemetryHUD.setOnCheckedChangeListener((buttonView, isChecked) -> {
                mPrefs.edit().putBoolean(KEY_TELEMETRY_HUD, isChecked).apply();
                saveToDeviceProtectedStorage(KEY_TELEMETRY_HUD, isChecked);
                makePrefsWorldReadable();
                Toast.makeText(this, R.string.pref_saved_notice, Toast.LENGTH_SHORT).show();
            });
        }

        if (mSwitchTelemetryPopup != null) {
            mSwitchTelemetryPopup.setOnCheckedChangeListener((buttonView, isChecked) -> {
                mPrefs.edit().putBoolean(KEY_TELEMETRY_POPUP, isChecked).apply();
                saveToDeviceProtectedStorage(KEY_TELEMETRY_POPUP, isChecked);
                makePrefsWorldReadable();
                Toast.makeText(this, R.string.pref_saved_notice, Toast.LENGTH_SHORT).show();
            });
        }

        if (mRowHideNearbyTab != null && mSwitchHideNearbyTab != null) {
            mRowHideNearbyTab.setOnClickListener(v -> mSwitchHideNearbyTab.toggle());
        }

        if (mSwitchHideNearbyTab != null) {
            mSwitchHideNearbyTab.setOnCheckedChangeListener((buttonView, isChecked) -> {
                mPrefs.edit().putBoolean(KEY_HIDE_NEARBY_TAB, isChecked).commit();
                saveToDeviceProtectedStorage(KEY_HIDE_NEARBY_TAB, isChecked);
                makePrefsWorldReadable();
                Toast.makeText(this, R.string.pref_saved_notice, Toast.LENGTH_SHORT).show();
            });
        }

        mSwitchSpoof.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mPrefs.edit().putBoolean(KEY_ENABLED, isChecked).apply();
            saveToDeviceProtectedStorage(KEY_ENABLED, isChecked);
            makePrefsWorldReadable();
            Toast.makeText(this, R.string.pref_saved_notice, Toast.LENGTH_SHORT).show();
        });

        mSwitchLocale.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mPrefs.edit().putBoolean(KEY_SPOOF_LOCALE, isChecked).apply();
            saveToDeviceProtectedStorage(KEY_SPOOF_LOCALE, isChecked);
            makePrefsWorldReadable();
            Toast.makeText(this, R.string.pref_saved_notice, Toast.LENGTH_SHORT).show();
        });

        mSwitchHideIcon.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mPrefs.edit().putBoolean(KEY_HIDE_ICON, isChecked).apply();
            setLauncherIconHidden(isChecked);
            if (isChecked) {
                Toast.makeText(this, R.string.icon_hidden_notice, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, R.string.icon_restored_notice, Toast.LENGTH_SHORT).show();
            }
        });

        mBtnForceStop.setOnClickListener(v -> forceStopCurrentProfileTarget());
        mBtnLaunch.setOnClickListener(v -> launchCurrentProfileTarget());

        makePrefsWorldReadable();
    }

    private void updateRegionDisplay() {
        boolean isCustom = mPrefs.getBoolean(KEY_IS_CUSTOM, false);
        if (isCustom) {
            String iso = mPrefs.getString(KEY_CUSTOM_ISO, "us").toUpperCase();
            String op = mPrefs.getString(KEY_CUSTOM_OPERATOR, "310260");
            String name = mPrefs.getString(KEY_CUSTOM_NAME, "Custom Carrier");

            mTvCurrentCountry.setText("🌐  Custom (" + iso + ")");
            mTvCurrentCarrierInfo.setText(name + " · MCC+MNC " + op + " · " + iso);

            mEtCustomIso.setText(iso.toLowerCase());
            mEtCustomOperator.setText(op);
            mEtCustomName.setText(name);
        } else {
            String presetId = mPrefs.getString(KEY_PRESET_ID, CountryPreset.getDefault().getId());
            CountryPreset preset = CountryPreset.getById(presetId);

            mTvCurrentCountry.setText(preset.getFlag() + "  " + preset.getCountryName());
            mTvCurrentCarrierInfo.setText(preset.getOperatorName() + " · MCC+MNC " + preset.getOperatorMccMnc() + " · " + preset.getCountryIso().toUpperCase());
        }
    }

    private void showSearchablePresetDialog() {
        CarrierPresetDialog.show(this, selected -> {
            mSwitchCustomOverride.setChecked(false);
            mLayoutCustomInputs.setVisibility(View.GONE);
            updateRegionDisplay();
        });
    }

    private void updateBlockedCountriesSummary() {
        if (mTvBlockedCountriesSummary == null) return;
        String list = mPrefs.getString(KEY_BLOCKED_COUNTRY_LIST, "");
        int count = 0;
        if (list != null && !list.trim().isEmpty()) {
            String[] parts = list.split(",");
            for (String p : parts) {
                if (!p.trim().isEmpty()) {
                    count++;
                }
            }
        }
        if (count == 0) {
            mTvBlockedCountriesSummary.setText(R.string.no_countries_blocked);
        } else {
            mTvBlockedCountriesSummary.setText(getString(R.string.blocked_count_format, count));
        }
    }

    private void showBlockedCountriesDialog() {
        BlockedCountriesDialog.show(this, this::updateBlockedCountriesSummary);
    }

    private void updateLanguagesSummary() {
        if (mTvLanguagesSummary == null) return;
        String list = mPrefs.getString(KEY_ALLOWED_LANGUAGES, "");
        int count = 0;
        if (list != null && !list.trim().isEmpty()) {
            String[] parts = list.split(",");
            for (String p : parts) {
                if (!p.trim().isEmpty()) {
                    count++;
                }
            }
        }
        if (count == 0) {
            mTvLanguagesSummary.setText(R.string.no_languages_selected);
        } else {
            mTvLanguagesSummary.setText(getString(R.string.language_filter_count_format, count));
        }
    }

    public String getCurrentRegionIso() {
        if (mPrefs.getBoolean(KEY_IS_CUSTOM, false)) {
            return mPrefs.getString(KEY_CUSTOM_ISO, "us").toLowerCase(Locale.ROOT);
        } else {
            String presetId = mPrefs.getString(KEY_PRESET_ID, CountryPreset.getDefault().getId());
            CountryPreset preset = CountryPreset.getById(presetId);
            return preset.getCountryIso().toLowerCase(Locale.ROOT);
        }
    }

    private Set<String> getLanguagesForCountry(String countryIso) {
        return LanguageFilterDialog.getLanguagesForCountry(countryIso);
    }

    private void showLanguageSelectionDialog() {
        LanguageFilterDialog.show(this, this::updateLanguagesSummary);
    }


    private void setLauncherIconHidden(boolean hide) {
        try {
            ComponentName aliasComponent = new ComponentName(this, getPackageName() + ".LauncherAlias");
            int newState = hide
                    ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    : PackageManager.COMPONENT_ENABLED_STATE_ENABLED;

            getPackageManager().setComponentEnabledSetting(
                    aliasComponent,
                    newState,
                    PackageManager.DONT_KILL_APP
            );
        } catch (Throwable ignored) {
        }
    }

    private boolean isLauncherIconHidden() {
        try {
            ComponentName aliasComponent = new ComponentName(this, getPackageName() + ".LauncherAlias");
            int state = getPackageManager().getComponentEnabledSetting(aliasComponent);
            return state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public SharedPreferences getPrefs() {
        return mPrefs;
    }

    public MaterialSwitch getSwitchBlockCountries() {
        return mSwitchBlockCountries;
    }

    public MaterialSwitch getSwitchLanguageFilter() {
        return mSwitchLanguageFilter;
    }

    void saveToDeviceProtectedStorage(String key, boolean value) {
        try {
            Context deContext = createDeviceProtectedStorageContext();
            SharedPreferences dePrefs = deContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            dePrefs.edit().putBoolean(key, value).commit();
        } catch (Throwable ignored) {
        }
    }

    void saveToDeviceProtectedStorage(String key, String value) {
        try {
            Context deContext = createDeviceProtectedStorageContext();
            SharedPreferences dePrefs = deContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            dePrefs.edit().putString(key, value).commit();
        } catch (Throwable ignored) {
        }
    }

    void makePrefsWorldReadable() {
        try {
            File prefsDir = new File(getApplicationInfo().dataDir, "shared_prefs");
            if (prefsDir.exists()) {
                prefsDir.setReadable(true, false);
                prefsDir.setExecutable(true, false);
            }
            File prefsFile = new File(prefsDir, PREF_NAME + ".xml");
            if (prefsFile.exists()) {
                prefsFile.setReadable(true, false);
            }
        } catch (Throwable ignored) {
        }
    }

    public boolean isModuleActive() {
        long lastPing = mPrefs.getLong(ConfigProvider.PREF_LAST_PING, 0);
        return (System.currentTimeMillis() - lastPing < 7 * 24 * 60 * 60 * 1000L);
    }

    public void setProfile(String profile) {
        mCurrentProfile = profile;
        mPrefs.edit().putString(KEY_ACTIVE_PROFILE, profile).apply();
        saveToDeviceProtectedStorage(KEY_ACTIVE_PROFILE, profile);
        makePrefsWorldReadable();

        boolean isGlobal = PROFILE_GLOBAL.equals(profile);
        boolean isAsia = PROFILE_ASIA.equals(profile);
        boolean isChina = PROFILE_CHINA.equals(profile);

        // Sync Cupertino segmented control UI
        if (mTabGlobal != null && mTabAsia != null && mTabChina != null) {
            mTabGlobal.setBackgroundResource(isGlobal ? R.drawable.bg_profile_tab_selected : R.drawable.bg_profile_tab_unselected);
            if (mTvTabGlobal != null) {
                mTvTabGlobal.setTextColor(ContextCompat.getColor(this, isGlobal ? R.color.profile_tab_text_active : R.color.profile_tab_text_inactive));
            }

            mTabAsia.setBackgroundResource(isAsia ? R.drawable.bg_profile_tab_selected : R.drawable.bg_profile_tab_unselected);
            if (mTvTabAsia != null) {
                mTvTabAsia.setTextColor(ContextCompat.getColor(this, isAsia ? R.color.profile_tab_text_active : R.color.profile_tab_text_inactive));
            }

            mTabChina.setBackgroundResource(isChina ? R.drawable.bg_profile_tab_selected : R.drawable.bg_profile_tab_unselected);
            if (mTvTabChina != null) {
                mTvTabChina.setTextColor(ContextCompat.getColor(this, isChina ? R.color.china_amber : R.color.profile_tab_text_inactive));
            }
        }

        if (mTvActiveProfileTitle != null && mTvActiveProfileBadge != null && mTvActiveProfilePackage != null) {
            if (isChina) {
                mTvActiveProfileTitle.setText("Douyin");
                mTvActiveProfileBadge.setText("CHINA");
                mTvActiveProfileBadge.setTextColor(ContextCompat.getColor(this, R.color.china_amber));
                mTvActiveProfilePackage.setText(PACKAGE_CHINA + " · Tap to switch");
            } else if (isAsia) {
                mTvActiveProfileTitle.setText("TikTok Asia");
                mTvActiveProfileBadge.setText("ASIA");
                mTvActiveProfileBadge.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                mTvActiveProfilePackage.setText(PACKAGE_ASIA + " · Tap to switch");
            } else {
                mTvActiveProfileTitle.setText("TikTok Global");
                mTvActiveProfileBadge.setText("GLOBAL");
                mTvActiveProfileBadge.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                mTvActiveProfilePackage.setText(PACKAGE_GLOBAL + " · Tap to switch");
            }
        }

        if (isChina) {

            if (mTvNoWatermarkTitle != null) mTvNoWatermarkTitle.setText(R.string.feature_no_watermark_douyin);
            if (mTvNoWatermarkDesc != null) mTvNoWatermarkDesc.setText(R.string.feature_no_watermark_douyin_desc);

            mLayoutChinaInfo.setVisibility(View.VISIBLE);
            mLayoutRegionSection.setVisibility(View.GONE);

            mRowForceRegion.setVisibility(View.GONE);
            mDividerForceRegion.setVisibility(View.GONE);
            mRowStrictForceRegion.setVisibility(View.GONE);
            mDividerStrictForceRegion.setVisibility(View.GONE);
            if (mRowLockedRegion != null) mRowLockedRegion.setVisibility(View.GONE);
            if (mDividerLockedRegion != null) mDividerLockedRegion.setVisibility(View.GONE);
            mRowBlockCountries.setVisibility(View.GONE);
            mBtnManageBlockedCountries.setVisibility(View.GONE);
            mDividerBlockCountries.setVisibility(View.GONE);
            if (mRowLanguageFilter != null) mRowLanguageFilter.setVisibility(View.GONE);
            if (mBtnManageLanguages != null) mBtnManageLanguages.setVisibility(View.GONE);
            if (mDividerLanguageFilter != null) mDividerLanguageFilter.setVisibility(View.GONE);

            mRowEnableSpoof.setVisibility(View.GONE);
            mDividerEnableSpoof.setVisibility(View.GONE);
            mRowSpoofLocale.setVisibility(View.GONE);

            if (mRowHDUpload != null) mRowHDUpload.setVisibility(View.GONE);
            if (mRowUpload4K != null) mRowUpload4K.setVisibility(View.GONE);
            if (mDividerUpload4K != null) mDividerUpload4K.setVisibility(View.GONE);
            if (mDividerDownloadStory != null) mDividerDownloadStory.setVisibility(View.GONE);
            if (mRowHideNearbyTab != null) mRowHideNearbyTab.setVisibility(View.GONE);
            if (mDividerHideNearbyTab != null) mDividerHideNearbyTab.setVisibility(View.GONE);

            mBtnForceStop.setText(R.string.btn_force_stop_tiktok);
            mBtnLaunch.setText(R.string.btn_open_tiktok);
        } else {

            if (mTvNoWatermarkTitle != null) mTvNoWatermarkTitle.setText(R.string.feature_no_watermark);
            if (mTvNoWatermarkDesc != null) mTvNoWatermarkDesc.setText(R.string.feature_no_watermark_desc);

            mLayoutChinaInfo.setVisibility(View.GONE);
            mLayoutRegionSection.setVisibility(View.VISIBLE);

            mRowForceRegion.setVisibility(View.VISIBLE);
            mDividerForceRegion.setVisibility(View.VISIBLE);
            mRowStrictForceRegion.setVisibility(View.VISIBLE);
            mDividerStrictForceRegion.setVisibility(View.VISIBLE);
            if (mRowLockedRegion != null) mRowLockedRegion.setVisibility(View.VISIBLE);
            if (mDividerLockedRegion != null) mDividerLockedRegion.setVisibility(View.VISIBLE);
            mRowBlockCountries.setVisibility(View.VISIBLE);
            mBtnManageBlockedCountries.setVisibility(View.VISIBLE);
            mDividerBlockCountries.setVisibility(View.VISIBLE);
            if (mRowLanguageFilter != null) mRowLanguageFilter.setVisibility(View.VISIBLE);
            if (mBtnManageLanguages != null) mBtnManageLanguages.setVisibility(View.VISIBLE);
            if (mDividerLanguageFilter != null) mDividerLanguageFilter.setVisibility(View.VISIBLE);

            mRowEnableSpoof.setVisibility(View.VISIBLE);
            mDividerEnableSpoof.setVisibility(View.VISIBLE);
            mRowSpoofLocale.setVisibility(View.VISIBLE);

            if (mRowHDUpload != null) mRowHDUpload.setVisibility(View.VISIBLE);
            if (mRowUpload4K != null) mRowUpload4K.setVisibility(View.VISIBLE);
            if (mDividerUpload4K != null) mDividerUpload4K.setVisibility(View.VISIBLE);
            if (mDividerDownloadStory != null) mDividerDownloadStory.setVisibility(View.VISIBLE);
            if (mRowHideNearbyTab != null) mRowHideNearbyTab.setVisibility(View.VISIBLE);
            if (mDividerHideNearbyTab != null) mDividerHideNearbyTab.setVisibility(View.VISIBLE);

            mBtnForceStop.setText(R.string.btn_force_stop_tiktok);
            mBtnLaunch.setText(R.string.btn_open_tiktok);
        }
    }

    private boolean isAppInstalled(String pkg) {
        if (pkg == null) return false;
        try {
            getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
            return false;
        }
    }

    private void showProfilePickerBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.dialog_profile_picker, null);
        dialog.setContentView(sheetView);

        View bottomSheetInternal = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheetInternal != null) {
            bottomSheetInternal.setBackgroundResource(android.R.color.transparent);
        }

        View btnClose = sheetView.findViewById(R.id.btn_close_profile_sheet);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        View cardGlobal = sheetView.findViewById(R.id.card_profile_global);
        View cardAsia = sheetView.findViewById(R.id.card_profile_asia);
        View cardChina = sheetView.findViewById(R.id.card_profile_china);

        ImageView checkGlobal = sheetView.findViewById(R.id.check_profile_global);
        ImageView checkAsia = sheetView.findViewById(R.id.check_profile_asia);
        ImageView checkChina = sheetView.findViewById(R.id.check_profile_china);

        TextView tvNotice = sheetView.findViewById(R.id.tv_no_installed_notice);

        boolean globalInstalled = isAppInstalled(PACKAGE_GLOBAL);
        boolean asiaInstalled = isAppInstalled(PACKAGE_ASIA);
        boolean chinaInstalled = isAppInstalled(PACKAGE_CHINA);

        boolean anyInstalled = globalInstalled || asiaInstalled || chinaInstalled;

        if (anyInstalled) {
            if (cardGlobal != null) cardGlobal.setVisibility(globalInstalled ? View.VISIBLE : View.GONE);
            if (cardAsia != null) cardAsia.setVisibility(asiaInstalled ? View.VISIBLE : View.GONE);
            if (cardChina != null) cardChina.setVisibility(chinaInstalled ? View.VISIBLE : View.GONE);
            if (tvNotice != null) tvNotice.setVisibility(View.GONE);
        } else {
            if (cardGlobal != null) cardGlobal.setVisibility(View.VISIBLE);
            if (cardAsia != null) cardAsia.setVisibility(View.VISIBLE);
            if (cardChina != null) cardChina.setVisibility(View.VISIBLE);
            if (tvNotice != null) tvNotice.setVisibility(View.VISIBLE);
        }

        if (checkGlobal != null) {
            checkGlobal.setVisibility(PROFILE_GLOBAL.equals(mCurrentProfile) ? View.VISIBLE : View.GONE);
        }
        if (checkAsia != null) {
            checkAsia.setVisibility(PROFILE_ASIA.equals(mCurrentProfile) ? View.VISIBLE : View.GONE);
        }
        if (checkChina != null) {
            checkChina.setVisibility(PROFILE_CHINA.equals(mCurrentProfile) ? View.VISIBLE : View.GONE);
        }

        if (cardGlobal != null) {
            cardGlobal.setOnClickListener(v -> {
                setProfile(PROFILE_GLOBAL);
                dialog.dismiss();
            });
        }
        if (cardAsia != null) {
            cardAsia.setOnClickListener(v -> {
                setProfile(PROFILE_ASIA);
                dialog.dismiss();
            });
        }
        if (cardChina != null) {
            cardChina.setOnClickListener(v -> {
                setProfile(PROFILE_CHINA);
                dialog.dismiss();
            });
        }

        View btnGithub = sheetView.findViewById(R.id.btn_open_github);
        if (btnGithub != null) {
            btnGithub.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/xihzs/TiktokEnhancer"));
                    startActivity(intent);
                } catch (Throwable ignored) {}
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    private String getTargetPackageForProfile(String profile) {
        if (PROFILE_CHINA.equals(profile)) {
            return PACKAGE_CHINA;
        } else if (PROFILE_ASIA.equals(profile)) {
            return PACKAGE_ASIA;
        } else {
            return PACKAGE_GLOBAL;
        }
    }

    private String getTargetAppNameForProfile(String profile) {
        if (PROFILE_CHINA.equals(profile)) {
            return getString(R.string.target_app_china);
        } else if (PROFILE_ASIA.equals(profile)) {
            return getString(R.string.target_app_asia);
        } else {
            return getString(R.string.target_app_global);
        }
    }

    private void forceStopCurrentProfileTarget() {
        String targetPkg = getTargetPackageForProfile(mCurrentProfile);
        String appName = getTargetAppNameForProfile(mCurrentProfile);

        boolean success = tryForceStopPackageWithRoot(targetPkg);
        if (success) {
            Toast.makeText(this, appName + " stopped via root", Toast.LENGTH_SHORT).show();
        } else {
            openAppDetailsSettingsForPackage(targetPkg);
            Toast.makeText(this, "Root not available. Tap 'Force Stop' in App Info", Toast.LENGTH_LONG).show();
        }
    }

    private boolean tryForceStopPackageWithRoot(String pkg) {
        Process process = null;
        DataOutputStream os = null;
        try {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("am force-stop " + pkg + "\n");
            os.writeBytes("exit\n");
            os.flush();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        } finally {
            try {
                if (os != null) os.close();
                if (process != null) process.destroy();
            } catch (Exception ignored) {}
        }
    }

    private void openAppDetailsSettingsForPackage(String pkg) {
        try {
            getPackageManager().getPackageInfo(pkg, 0);
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + pkg));
            startActivity(intent);
            return;
        } catch (PackageManager.NameNotFoundException ignored) {}

        for (String p : TIKTOK_PACKAGES) {
            try {
                getPackageManager().getPackageInfo(p, 0);
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + p));
                startActivity(intent);
                return;
            } catch (PackageManager.NameNotFoundException ignored) {}
        }

        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + pkg));
            startActivity(intent);
        } catch (Throwable ignored) {}
    }

    private void launchCurrentProfileTarget() {
        String targetPkg = getTargetPackageForProfile(mCurrentProfile);
        String appName = getTargetAppNameForProfile(mCurrentProfile);
        PackageManager pm = getPackageManager();

        try {
            Intent intent = pm.getLaunchIntentForPackage(targetPkg);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return;
            }
        } catch (Throwable ignored) {}

        try {
            Intent mainIntent = new Intent(Intent.ACTION_MAIN);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            mainIntent.setPackage(targetPkg);
            List<ResolveInfo> matches = pm.queryIntentActivities(mainIntent, 0);
            if (!matches.isEmpty()) {
                ResolveInfo info = matches.get(0);
                ComponentName component = new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.setComponent(component);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return;
            }
        } catch (Throwable ignored) {}

        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "monkey -p " + targetPkg + " -c android.intent.category.LAUNCHER 1"});
            if (p.waitFor() == 0) return;
        } catch (Throwable ignored) {}

        Toast.makeText(this, appName + " is not installed on device", Toast.LENGTH_SHORT).show();
    }
}

