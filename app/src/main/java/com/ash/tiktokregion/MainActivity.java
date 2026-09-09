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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import java.io.DataOutputStream;
import java.io.File;
import java.util.ArrayList;
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
    public static final String KEY_HIDE_ADS = "hide_ads";
    public static final String KEY_FORCE_REGION = "force_region";
    public static final String KEY_STRICT_FORCE_REGION = "strict_force_region";
    public static final String KEY_BLOCK_COUNTRIES = "block_countries";
    public static final String KEY_BLOCKED_COUNTRY_LIST = "blocked_country_list";
    public static final String KEY_DOWNLOAD_STORY = "download_story";

    public static final String KEY_ACTIVE_PROFILE = "active_profile";
    public static final String PROFILE_GLOBAL = "global";
    public static final String PROFILE_ASIA = "asia";
    public static final String PROFILE_CHINA = "china";

    public static final String PACKAGE_GLOBAL = "com.zhiliaoapp.musically";
    public static final String PACKAGE_ASIA = "com.ss.android.ugc.trill";
    public static final String PACKAGE_CHINA = "com.ss.android.ugc.aweme";

    private SharedPreferences mPrefs;
    private String mCurrentProfile = PROFILE_GLOBAL;
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
    private View mRowBlockCountries;
    private View mDividerBlockCountries;
    private View mRowEnableSpoof;
    private View mDividerEnableSpoof;
    private View mRowSpoofLocale;
    private View mDividerSpoofLocale;

    private MaterialSwitch mSwitchSpoof;
    private MaterialSwitch mSwitchLocale;
    private MaterialSwitch mSwitchHideIcon;
    private MaterialSwitch mSwitchForceRegion;
    private MaterialSwitch mSwitchStrictForceRegion;
    private MaterialSwitch mSwitchBlockCountries;
    private View mBtnManageBlockedCountries;
    private TextView mTvBlockedCountriesSummary;
    private MaterialSwitch mSwitchDownloadStory;
    private MaterialSwitch mSwitchHideAds;
    private MaterialSwitch mSwitchNoWatermark;
    private MaterialSwitch mSwitchBypassDownload;
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
        mRowBlockCountries = findViewById(R.id.row_block_countries);
        mDividerBlockCountries = findViewById(R.id.divider_block_countries);
        mRowEnableSpoof = findViewById(R.id.row_enable_spoof);
        mDividerEnableSpoof = findViewById(R.id.divider_enable_spoof);
        mRowSpoofLocale = findViewById(R.id.row_spoof_locale);
        mDividerSpoofLocale = findViewById(R.id.divider_spoof_locale);

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
        mSwitchBlockCountries = findViewById(R.id.switch_block_countries);
        mBtnManageBlockedCountries = findViewById(R.id.btn_manage_blocked_countries);
        mTvBlockedCountriesSummary = findViewById(R.id.tv_blocked_countries_summary);
        mSwitchDownloadStory = findViewById(R.id.switch_download_story);
        mSwitchHideAds = findViewById(R.id.switch_hide_ads);
        mSwitchNoWatermark = findViewById(R.id.switch_no_watermark);
        mSwitchBypassDownload = findViewById(R.id.switch_bypass_download);
        mSwitchSpoof = findViewById(R.id.switch_enable_spoof);
        mSwitchLocale = findViewById(R.id.switch_spoof_locale);
        mSwitchHideIcon = findViewById(R.id.switch_hide_icon);

        mBtnForceStop = findViewById(R.id.btn_force_stop_tiktok);
        mBtnLaunch = findViewById(R.id.btn_launch_tiktok);

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
        boolean blockCountries = mPrefs.getBoolean(KEY_BLOCK_COUNTRIES, false);
        boolean downloadStory = mPrefs.getBoolean(KEY_DOWNLOAD_STORY, true);
        boolean hideAds = mPrefs.getBoolean(KEY_HIDE_ADS, true);
        boolean noWatermark = mPrefs.getBoolean(KEY_NO_WATERMARK, true);
        boolean bypassDownload = mPrefs.getBoolean(KEY_BYPASS_DOWNLOAD_RESTRICTION, true);

        mSwitchSpoof.setChecked(enabled);
        mSwitchLocale.setChecked(spoofLocale);
        mSwitchHideIcon.setChecked(isIconHidden);
        mSwitchForceRegion.setChecked(forceRegion);
        mSwitchStrictForceRegion.setChecked(strictForceRegion);
        mSwitchBlockCountries.setChecked(blockCountries);
        mSwitchDownloadStory.setChecked(downloadStory);
        mSwitchHideAds.setChecked(hideAds);
        mSwitchNoWatermark.setChecked(noWatermark);
        mSwitchBypassDownload.setChecked(bypassDownload);
        mSwitchCustomOverride.setChecked(isCustom);
        mLayoutCustomInputs.setVisibility(isCustom ? View.VISIBLE : View.GONE);

        updateRegionDisplay();
        updateBlockedCountriesSummary();

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

        mSwitchBlockCountries.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mPrefs.edit().putBoolean(KEY_BLOCK_COUNTRIES, isChecked).apply();
            saveToDeviceProtectedStorage(KEY_BLOCK_COUNTRIES, isChecked);
            makePrefsWorldReadable();
            Toast.makeText(this, R.string.pref_saved_notice, Toast.LENGTH_SHORT).show();
        });

        if (mBtnManageBlockedCountries != null) {
            mBtnManageBlockedCountries.setOnClickListener(v -> showBlockedCountriesDialog());
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
            mTvCurrentCarrierInfo.setText(name + " · MCC+MNC " + op + " · ISO " + iso);

            mEtCustomIso.setText(iso.toLowerCase());
            mEtCustomOperator.setText(op);
            mEtCustomName.setText(name);
        } else {
            String presetId = mPrefs.getString(KEY_PRESET_ID, CountryPreset.getDefault().getId());
            CountryPreset preset = CountryPreset.getById(presetId);

            mTvCurrentCountry.setText(preset.getFlag() + "  " + preset.getCountryName());
            mTvCurrentCarrierInfo.setText(preset.getOperatorName() + " · MCC+MNC " + preset.getOperatorMccMnc() + " · ISO " + preset.getCountryIso().toUpperCase());
        }
    }

    private void showSearchablePresetDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_select_preset, null);
        EditText etSearch = dialogView.findViewById(R.id.et_search_country);
        RecyclerView rvPresets = dialogView.findViewById(R.id.rv_presets);
        TextView tvEmptyState = dialogView.findViewById(R.id.tv_empty_state);
        View btnClose = dialogView.findViewById(R.id.btn_close_dialog);

        rvPresets.setLayoutManager(new GridLayoutManager(this, 2));

        String currentPresetId = mPrefs.getString(KEY_PRESET_ID, CountryPreset.getDefault().getId());
        if (mPrefs.getBoolean(KEY_IS_CUSTOM, false)) {
            currentPresetId = null;
        }

        Dialog dialog = new Dialog(this, R.style.DialogDarkTheme);
        dialog.setContentView(dialogView);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setDimAmount(0.65f);

            DisplayMetrics metrics = getResources().getDisplayMetrics();
            int width = (int) (metrics.widthPixels * 0.90);
            int height = (int) (metrics.heightPixels * 0.78);
            dialog.getWindow().setLayout(width, height);
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        CountryGridAdapter adapter = new CountryGridAdapter(this, CountryPreset.PRESETS, currentPresetId, selected -> {
            mPrefs.edit()
                    .putString(KEY_PRESET_ID, selected.getId())
                    .putBoolean(KEY_IS_CUSTOM, false)
                    .apply();

            saveToDeviceProtectedStorage(KEY_PRESET_ID, selected.getId());
            saveToDeviceProtectedStorage(KEY_IS_CUSTOM, false);
            makePrefsWorldReadable();

            mSwitchCustomOverride.setChecked(false);
            mLayoutCustomInputs.setVisibility(View.GONE);

            updateRegionDisplay();
            Toast.makeText(this, "Region set to " + selected.getFlag() + " " + selected.getCountryName(), Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        rvPresets.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s != null ? s.toString() : "";
                List<CountryPreset> filtered = CountryPreset.filter(query);
                adapter.updateList(filtered);
                boolean isEmpty = filtered.isEmpty();
                tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                rvPresets.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        dialog.show();
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
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_block_countries, null);
        EditText etSearch = dialogView.findViewById(R.id.et_search_blocked);
        EditText etCustomIso = dialogView.findViewById(R.id.et_custom_blocked_iso);
        View btnAddCustomIso = dialogView.findViewById(R.id.btn_add_custom_iso);
        TextView tvBlockedCounter = dialogView.findViewById(R.id.tv_blocked_counter);
        TextView btnClearAll = dialogView.findViewById(R.id.btn_clear_all_blocked);
        RecyclerView rvBlockedPresets = dialogView.findViewById(R.id.rv_blocked_presets);
        TextView tvEmptyState = dialogView.findViewById(R.id.tv_empty_blocked_state);
        View btnClose = dialogView.findViewById(R.id.btn_close_dialog);
        View btnDone = dialogView.findViewById(R.id.btn_done_blocked);

        rvBlockedPresets.setLayoutManager(new GridLayoutManager(this, 2));

        String savedList = mPrefs.getString(KEY_BLOCKED_COUNTRY_LIST, "");
        Set<String> initialBlocked = new HashSet<>();
        if (savedList != null && !savedList.trim().isEmpty()) {
            for (String iso : savedList.split(",")) {
                String clean = iso.trim().toLowerCase(Locale.ROOT);
                if (!clean.isEmpty()) {
                    initialBlocked.add(clean);
                }
            }
        }

        List<CountryPreset.CountryItem> allCountries = CountryPreset.getUniqueCountries();
        Set<String> knownIsos = new HashSet<>();
        for (CountryPreset.CountryItem c : allCountries) {
            knownIsos.add(c.getIso().toLowerCase(Locale.ROOT));
        }
        for (String iso : initialBlocked) {
            if (!knownIsos.contains(iso)) {
                CountryPreset.CountryItem customItem = CountryPreset.createCountryItem(iso);
                if (customItem != null) {
                    allCountries.add(0, customItem);
                    knownIsos.add(iso);
                }
            }
        }

        Dialog dialog = new Dialog(this, R.style.DialogDarkTheme);
        dialog.setContentView(dialogView);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setDimAmount(0.65f);

            DisplayMetrics metrics = getResources().getDisplayMetrics();
            int width = (int) (metrics.widthPixels * 0.90);
            int height = (int) (metrics.heightPixels * 0.85);
            dialog.getWindow().setLayout(width, height);
        }

        BlockedCountryGridAdapter[] adapterHolder = new BlockedCountryGridAdapter[1];

        Runnable updateCounterUI = () -> {
            int count = adapterHolder[0] != null ? adapterHolder[0].getBlockedCount() : initialBlocked.size();
            tvBlockedCounter.setText(getString(R.string.blocked_count_format, count));
            btnClearAll.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        };

        BlockedCountryGridAdapter adapter = new BlockedCountryGridAdapter(this, allCountries, initialBlocked,
                (item, isBlocked, totalBlockedCount) -> {
                    tvBlockedCounter.setText(getString(R.string.blocked_count_format, totalBlockedCount));
                    btnClearAll.setVisibility(totalBlockedCount > 0 ? View.VISIBLE : View.GONE);
                });
        adapterHolder[0] = adapter;
        rvBlockedPresets.setAdapter(adapter);
        updateCounterUI.run();

        btnClearAll.setOnClickListener(v -> adapter.clearAll());

        btnAddCustomIso.setOnClickListener(v -> {
            String iso = etCustomIso.getText() != null ? etCustomIso.getText().toString().trim().toLowerCase(Locale.ROOT) : "";
            if (iso.length() != 2) {
                Toast.makeText(this, "Enter a 2-letter ISO code", Toast.LENGTH_SHORT).show();
                return;
            }
            CountryPreset.CountryItem customItem = CountryPreset.createCountryItem(iso);
            if (customItem == null) {
                Toast.makeText(this, "Invalid country code", Toast.LENGTH_SHORT).show();
                return;
            }
            adapter.addAndSelectCountry(customItem);
            etCustomIso.setText("");
            Toast.makeText(this, "Added " + customItem.getFlag() + " " + iso.toUpperCase(Locale.ROOT), Toast.LENGTH_SHORT).show();
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s != null ? s.toString() : "";
                List<CountryPreset.CountryItem> filtered = CountryPreset.filterCountries(allCountries, query);
                adapter.updateList(filtered);
                boolean isEmpty = filtered.isEmpty();
                tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                rvBlockedPresets.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        Runnable saveAndDismiss = () -> {
            Set<String> blockedSet = adapter.getBlockedIsoSet();
            StringBuilder sb = new StringBuilder();
            for (String iso : blockedSet) {
                if (sb.length() > 0) sb.append(",");
                sb.append(iso);
            }
            String joined = sb.toString();

            mPrefs.edit().putString(KEY_BLOCKED_COUNTRY_LIST, joined).apply();
            saveToDeviceProtectedStorage(KEY_BLOCKED_COUNTRY_LIST, joined);
            makePrefsWorldReadable();

            updateBlockedCountriesSummary();
            Toast.makeText(this, R.string.pref_saved_notice, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        };

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> saveAndDismiss.run());
        }
        if (btnDone != null) {
            btnDone.setOnClickListener(v -> saveAndDismiss.run());
        }
        dialog.setOnCancelListener(d -> saveAndDismiss.run());

        dialog.show();
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

    private void saveToDeviceProtectedStorage(String key, boolean value) {
        try {
            Context deContext = createDeviceProtectedStorageContext();
            SharedPreferences dePrefs = deContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            dePrefs.edit().putBoolean(key, value).apply();
        } catch (Throwable ignored) {
        }
    }

    private void saveToDeviceProtectedStorage(String key, String value) {
        try {
            Context deContext = createDeviceProtectedStorageContext();
            SharedPreferences dePrefs = deContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            dePrefs.edit().putString(key, value).apply();
        } catch (Throwable ignored) {
        }
    }

    private void makePrefsWorldReadable() {
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
            mRowBlockCountries.setVisibility(View.GONE);
            mBtnManageBlockedCountries.setVisibility(View.GONE);
            mDividerBlockCountries.setVisibility(View.GONE);

            mRowEnableSpoof.setVisibility(View.GONE);
            mDividerEnableSpoof.setVisibility(View.GONE);
            mRowSpoofLocale.setVisibility(View.GONE);
            mDividerSpoofLocale.setVisibility(View.GONE);

            mBtnForceStop.setText("Restart Douyin");
            mBtnLaunch.setText("Launch Douyin");
        } else {

            if (mTvNoWatermarkTitle != null) mTvNoWatermarkTitle.setText(R.string.feature_no_watermark);
            if (mTvNoWatermarkDesc != null) mTvNoWatermarkDesc.setText(R.string.feature_no_watermark_desc);

            mLayoutChinaInfo.setVisibility(View.GONE);
            mLayoutRegionSection.setVisibility(View.VISIBLE);

            mRowForceRegion.setVisibility(View.VISIBLE);
            mDividerForceRegion.setVisibility(View.VISIBLE);
            mRowStrictForceRegion.setVisibility(View.VISIBLE);
            mDividerStrictForceRegion.setVisibility(View.VISIBLE);
            mRowBlockCountries.setVisibility(View.VISIBLE);
            mBtnManageBlockedCountries.setVisibility(View.VISIBLE);
            mDividerBlockCountries.setVisibility(View.VISIBLE);

            mRowEnableSpoof.setVisibility(View.VISIBLE);
            mDividerEnableSpoof.setVisibility(View.VISIBLE);
            mRowSpoofLocale.setVisibility(View.VISIBLE);
            mDividerSpoofLocale.setVisibility(View.VISIBLE);

            if (isAsia) {
                mBtnForceStop.setText("Restart TikTok Asia");
                mBtnLaunch.setText("Launch");
            } else {
                mBtnForceStop.setText(R.string.btn_force_stop_tiktok);
                mBtnLaunch.setText(R.string.btn_open_tiktok);
            }
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

