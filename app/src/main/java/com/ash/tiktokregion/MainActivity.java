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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
    public static final String KEY_BLOCK_COUNTRY_SONGS = "block_country_songs";
    public static final String KEY_BLOCKED_COUNTRY_LIST = "blocked_country_list";
    public static final String KEY_DOWNLOAD_STORY = "download_story";
    public static final String KEY_FORCE_HIGHEST_QUALITY = "force_highest_quality";
    public static final String KEY_FILTER_NON_HD = "filter_non_hd";
    public static final String KEY_ANTI_DELETE_MSG = "anti_delete_msg";
    public static final String KEY_FORCE_MASTER_UPLOAD = "force_master_upload";
    public static final String KEY_ANONYMOUS_VIEW = "anonymous_view";
    public static final String KEY_DISABLE_POPUPS = "disable_popups";

    private SharedPreferences mPrefs;
    private MaterialSwitch mSwitchSpoof;
    private MaterialSwitch mSwitchLocale;
    private MaterialSwitch mSwitchHideIcon;
    private MaterialSwitch mSwitchForceRegion;
    private MaterialSwitch mSwitchStrictForceRegion;
    private MaterialSwitch mSwitchBlockCountries;
    private MaterialSwitch mSwitchBlockCountrySongs;
    private View mBtnManageBlockedCountries;
    private TextView mTvBlockedCountriesSummary;
    private MaterialSwitch mSwitchDownloadStory;
    private MaterialSwitch mSwitchHideAds;
    private MaterialSwitch mSwitchNoWatermark;
    private MaterialSwitch mSwitchBypassDownload;
    private MaterialSwitch mSwitchForceHighestQuality;
    private MaterialSwitch mSwitchFilterNonHd;
    private MaterialSwitch mSwitchAntiDeleteMsg;
    private MaterialSwitch mSwitchMasterUpload;
    private MaterialSwitch mSwitchAnonymousView;
    private MaterialSwitch mSwitchDisablePopups;
    private MaterialSwitch mSwitchCustomOverride;
    private LinearLayout mLayoutCustomInputs;
    private TextInputEditText mEtCustomIso;
    private TextInputEditText mEtCustomOperator;
    private TextInputEditText mEtCustomName;
    private TextView mTvCurrentCountry;
    private TextView mTvCurrentCarrierInfo;
    private TextView mTvTiktokPackage;
    private TextView mTvTiktokVersion;
    private TextView mTvTiktokSupportStatus;

    private static final String[] TIKTOK_PACKAGES = {
            "com.zhiliaoapp.musically",
            "com.ss.android.ugc.trill",
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
        mSwitchBlockCountrySongs = findViewById(R.id.switch_block_country_songs);
        mBtnManageBlockedCountries = findViewById(R.id.btn_manage_blocked_countries);
        mTvBlockedCountriesSummary = findViewById(R.id.tv_blocked_countries_summary);
        mSwitchDownloadStory = findViewById(R.id.switch_download_story);
        mSwitchHideAds = findViewById(R.id.switch_hide_ads);
        mSwitchDisablePopups = findViewById(R.id.switch_disable_popups);
        mSwitchNoWatermark = findViewById(R.id.switch_no_watermark);
        mSwitchBypassDownload = findViewById(R.id.switch_bypass_download);
        mSwitchForceHighestQuality = findViewById(R.id.switch_force_highest_quality);
        mSwitchFilterNonHd = findViewById(R.id.switch_filter_non_hd);
        mSwitchAntiDeleteMsg = findViewById(R.id.switch_anti_delete_msg);
        mSwitchMasterUpload = findViewById(R.id.switch_force_master_upload);
        mSwitchAnonymousView = findViewById(R.id.switch_anonymous_view);
        mSwitchSpoof = findViewById(R.id.switch_enable_spoof);
        mSwitchLocale = findViewById(R.id.switch_spoof_locale);
        mSwitchHideIcon = findViewById(R.id.switch_hide_icon);

        Button btnForceStopTikTok = findViewById(R.id.btn_force_stop_tiktok);
        Button btnLaunchTikTok = findViewById(R.id.btn_launch_tiktok);

        // 1. Status Indicator
        boolean active = isModuleActive();
        if (active) {
            dotStatus.setBackgroundResource(R.drawable.bg_dot_active);
            tvStatus.setText(R.string.status_active);
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.ios_green));
        } else {
            dotStatus.setBackgroundResource(R.drawable.bg_dot_inactive);
            tvStatus.setText(R.string.status_inactive);
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        }

        // 1.1 Native Engine & Architecture Telemetry
        TextView tvEngineVersion = findViewById(R.id.tv_engine_version);
        TextView tvLibUsed = findViewById(R.id.tv_lib_used);
        TextView tvTargetLib = findViewById(R.id.tv_target_lib);
        mTvTiktokPackage = findViewById(R.id.tv_tiktok_package);
        mTvTiktokVersion = findViewById(R.id.tv_tiktok_version);
        mTvTiktokSupportStatus = findViewById(R.id.tv_tiktok_support_status);
        TextView tvEngineArch = findViewById(R.id.tv_engine_arch);

        if (tvEngineVersion != null) {
            tvEngineVersion.setText("DexKit v2.3.0 Engine (Native C++)");
            tvEngineVersion.setTextColor(ContextCompat.getColor(this, R.color.ios_green));
        }
        if (tvLibUsed != null) {
            tvLibUsed.setText("libdexkit.so (LuckyPray)");
        }
        if (tvTargetLib != null) {
            tvTargetLib.setText("libsscronet.so (TTNet)");
        }
        updateTikTokInfo();
        if (tvEngineArch != null) {
            tvEngineArch.setText("DexKit 2.3.0 • C++ Native Dex Map • LSPosed");
        }

        // 2. Load Region & Options State
        boolean enabled = mPrefs.getBoolean(KEY_ENABLED, true);
        boolean spoofLocale = mPrefs.getBoolean(KEY_SPOOF_LOCALE, false);
        boolean isCustom = mPrefs.getBoolean(KEY_IS_CUSTOM, false);
        boolean isIconHidden = isLauncherIconHidden();
        boolean forceRegion = mPrefs.getBoolean(KEY_FORCE_REGION, true);
        boolean strictForceRegion = mPrefs.getBoolean(KEY_STRICT_FORCE_REGION, false);
        boolean blockCountries = mPrefs.getBoolean(KEY_BLOCK_COUNTRIES, false);
        boolean blockCountrySongs = mPrefs.getBoolean(KEY_BLOCK_COUNTRY_SONGS, true);
        boolean downloadStory = mPrefs.getBoolean(KEY_DOWNLOAD_STORY, true);
        boolean hideAds = mPrefs.getBoolean(KEY_HIDE_ADS, true);
        boolean noWatermark = mPrefs.getBoolean(KEY_NO_WATERMARK, true);
        boolean bypassDownload = mPrefs.getBoolean(KEY_BYPASS_DOWNLOAD_RESTRICTION, true);
        boolean forceHighestQuality = mPrefs.getBoolean(KEY_FORCE_HIGHEST_QUALITY, true);
        boolean filterNonHd = mPrefs.getBoolean(KEY_FILTER_NON_HD, true);
        boolean antiDeleteMsg = mPrefs.getBoolean(KEY_ANTI_DELETE_MSG, true);
        boolean forceMasterUpload = mPrefs.getBoolean(KEY_FORCE_MASTER_UPLOAD, true);
        boolean anonymousView = mPrefs.getBoolean(KEY_ANONYMOUS_VIEW, true);
        boolean disablePopups = mPrefs.getBoolean(KEY_DISABLE_POPUPS, true);

        mSwitchSpoof.setChecked(enabled);
        mSwitchLocale.setChecked(spoofLocale);
        mSwitchHideIcon.setChecked(isIconHidden);
        mSwitchForceRegion.setChecked(forceRegion);
        mSwitchStrictForceRegion.setChecked(strictForceRegion);
        mSwitchBlockCountries.setChecked(blockCountries);
        if (mSwitchBlockCountrySongs != null) mSwitchBlockCountrySongs.setChecked(blockCountrySongs);
        mSwitchDownloadStory.setChecked(downloadStory);
        mSwitchHideAds.setChecked(hideAds);
        mSwitchNoWatermark.setChecked(noWatermark);
        mSwitchBypassDownload.setChecked(bypassDownload);
        if (mSwitchForceHighestQuality != null) mSwitchForceHighestQuality.setChecked(forceHighestQuality);
        if (mSwitchFilterNonHd != null) mSwitchFilterNonHd.setChecked(filterNonHd);
        if (mSwitchAntiDeleteMsg != null) mSwitchAntiDeleteMsg.setChecked(antiDeleteMsg);
        if (mSwitchMasterUpload != null) mSwitchMasterUpload.setChecked(forceMasterUpload);
        if (mSwitchAnonymousView != null) mSwitchAnonymousView.setChecked(anonymousView);
        if (mSwitchDisablePopups != null) mSwitchDisablePopups.setChecked(disablePopups);
        mSwitchCustomOverride.setChecked(isCustom);
        mLayoutCustomInputs.setVisibility(isCustom ? View.VISIBLE : View.GONE);

        updateRegionDisplay();
        updateBlockedCountriesSummary();

        // 3. Preset Selector Dialog (Searchable 60+ countries)
        btnSelectPreset.setOnClickListener(v -> showSearchablePresetDialog());

        // 4. Custom Override Toggle
        mSwitchCustomOverride.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mLayoutCustomInputs.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            mPrefs.edit().putBoolean(KEY_IS_CUSTOM, isChecked).apply();
            saveToDeviceProtectedStorage(KEY_IS_CUSTOM, isChecked);
            updateRegionDisplay();
            notifySettingsChanged();
        });

        // 5. Custom Save Button
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
            notifySettingsChanged();
        });

        // 6. Switches Listeners
        mSwitchForceRegion.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && mSwitchStrictForceRegion != null && mSwitchStrictForceRegion.isChecked()) {
                mSwitchStrictForceRegion.setChecked(false);
            }
            mPrefs.edit().putBoolean(KEY_FORCE_REGION, isChecked).apply();
            saveToDeviceProtectedStorage(KEY_FORCE_REGION, isChecked);
            makePrefsWorldReadable();
            notifySettingsChanged();
        });

        mSwitchStrictForceRegion.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && mSwitchForceRegion != null && mSwitchForceRegion.isChecked()) {
                mSwitchForceRegion.setChecked(false);
            }
            mPrefs.edit().putBoolean(KEY_STRICT_FORCE_REGION, isChecked).apply();
            saveToDeviceProtectedStorage(KEY_STRICT_FORCE_REGION, isChecked);
            makePrefsWorldReadable();
            notifySettingsChanged();
        });

        mSwitchBlockCountries.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mPrefs.edit().putBoolean(KEY_BLOCK_COUNTRIES, isChecked).apply();
            saveToDeviceProtectedStorage(KEY_BLOCK_COUNTRIES, isChecked);
            makePrefsWorldReadable();
            notifySettingsChanged();
        });

        if (mSwitchBlockCountrySongs != null) {
            mSwitchBlockCountrySongs.setOnCheckedChangeListener((buttonView, isChecked) -> {
                mPrefs.edit().putBoolean(KEY_BLOCK_COUNTRY_SONGS, isChecked).apply();
                saveToDeviceProtectedStorage(KEY_BLOCK_COUNTRY_SONGS, isChecked);
                makePrefsWorldReadable();
                notifySettingsChanged();
            });
        }

        if (mBtnManageBlockedCountries != null) {
            mBtnManageBlockedCountries.setOnClickListener(v -> showBlockedCountriesDialog());
        }

        mSwitchDownloadStory.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mPrefs.edit().putBoolean(KEY_DOWNLOAD_STORY, isChecked).apply();
            saveToDeviceProtectedStorage(KEY_DOWNLOAD_STORY, isChecked);
            makePrefsWorldReadable();
            notifySettingsChanged();
        });

        mSwitchHideAds.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mPrefs.edit().putBoolean(KEY_HIDE_ADS, isChecked).apply();
            saveToDeviceProtectedStorage(KEY_HIDE_ADS, isChecked);
            makePrefsWorldReadable();
            notifySettingsChanged();
        });

        if (mSwitchDisablePopups != null) {
            mSwitchDisablePopups.setOnCheckedChangeListener((buttonView, isChecked) -> {
                mPrefs.edit().putBoolean(KEY_DISABLE_POPUPS, isChecked).apply();
                saveToDeviceProtectedStorage(KEY_DISABLE_POPUPS, isChecked);
                makePrefsWorldReadable();
                notifySettingsChanged();
            });
        }

        mSwitchNoWatermark.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mPrefs.edit().putBoolean(KEY_NO_WATERMARK, isChecked).apply();
            saveToDeviceProtectedStorage(KEY_NO_WATERMARK, isChecked);
            makePrefsWorldReadable();
            notifySettingsChanged();
        });

        mSwitchBypassDownload.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mPrefs.edit().putBoolean(KEY_BYPASS_DOWNLOAD_RESTRICTION, isChecked).apply();
            saveToDeviceProtectedStorage(KEY_BYPASS_DOWNLOAD_RESTRICTION, isChecked);
            makePrefsWorldReadable();
            notifySettingsChanged();
        });

        if (mSwitchForceHighestQuality != null) {
            mSwitchForceHighestQuality.setOnCheckedChangeListener((buttonView, isChecked) -> {
                mPrefs.edit().putBoolean(KEY_FORCE_HIGHEST_QUALITY, isChecked).apply();
                saveToDeviceProtectedStorage(KEY_FORCE_HIGHEST_QUALITY, isChecked);
                makePrefsWorldReadable();
                notifySettingsChanged();
            });
        }

        if (mSwitchFilterNonHd != null) {
            mSwitchFilterNonHd.setOnCheckedChangeListener((buttonView, isChecked) -> {
                mPrefs.edit().putBoolean(KEY_FILTER_NON_HD, isChecked).apply();
                saveToDeviceProtectedStorage(KEY_FILTER_NON_HD, isChecked);
                makePrefsWorldReadable();
                notifySettingsChanged();
            });
        }

        if (mSwitchAntiDeleteMsg != null) {
            mSwitchAntiDeleteMsg.setOnCheckedChangeListener((buttonView, isChecked) -> {
                mPrefs.edit().putBoolean(KEY_ANTI_DELETE_MSG, isChecked).apply();
                saveToDeviceProtectedStorage(KEY_ANTI_DELETE_MSG, isChecked);
                makePrefsWorldReadable();
                notifySettingsChanged();
            });
        }

        if (mSwitchMasterUpload != null) {
            mSwitchMasterUpload.setOnCheckedChangeListener((buttonView, isChecked) -> {
                mPrefs.edit().putBoolean(KEY_FORCE_MASTER_UPLOAD, isChecked).apply();
                saveToDeviceProtectedStorage(KEY_FORCE_MASTER_UPLOAD, isChecked);
                makePrefsWorldReadable();
                notifySettingsChanged();
            });
        }

        if (mSwitchAnonymousView != null) {
            mSwitchAnonymousView.setOnCheckedChangeListener((buttonView, isChecked) -> {
                mPrefs.edit().putBoolean(KEY_ANONYMOUS_VIEW, isChecked).apply();
                saveToDeviceProtectedStorage(KEY_ANONYMOUS_VIEW, isChecked);
                makePrefsWorldReadable();
                notifySettingsChanged();
            });
        }

        mSwitchSpoof.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mPrefs.edit().putBoolean(KEY_ENABLED, isChecked).apply();
            saveToDeviceProtectedStorage(KEY_ENABLED, isChecked);
            makePrefsWorldReadable();
            notifySettingsChanged();
        });

        mSwitchLocale.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mPrefs.edit().putBoolean(KEY_SPOOF_LOCALE, isChecked).apply();
            saveToDeviceProtectedStorage(KEY_SPOOF_LOCALE, isChecked);
            makePrefsWorldReadable();
            notifySettingsChanged();
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

        // 7. Action Buttons
        btnForceStopTikTok.setOnClickListener(v -> {
            sendForceStopBroadcast();
            tryForceStopTikTokWithRoot();
            Toast.makeText(this, "TikTok closed", Toast.LENGTH_SHORT).show();
        });

        btnLaunchTikTok.setOnClickListener(v -> launchTikTok());

        makePrefsWorldReadable();
    }

    private void updateRegionDisplay() {
        boolean isCustom = mPrefs.getBoolean(KEY_IS_CUSTOM, false);
        if (isCustom) {
            String iso = mPrefs.getString(KEY_CUSTOM_ISO, "us").toUpperCase();
            String op = mPrefs.getString(KEY_CUSTOM_OPERATOR, "310260");
            String name = mPrefs.getString(KEY_CUSTOM_NAME, "Custom Carrier");

            String flag = CountryPreset.isoToFlagEmoji(iso);
            mTvCurrentCountry.setText((flag.isEmpty() ? "" : flag + " ") + "Custom (" + iso.toUpperCase() + ")");
            mTvCurrentCarrierInfo.setText(name + " · MCC+MNC " + op + " · ISO " + iso);

            mEtCustomIso.setText(iso.toLowerCase());
            mEtCustomOperator.setText(op);
            mEtCustomName.setText(name);
        } else {
            String presetId = mPrefs.getString(KEY_PRESET_ID, CountryPreset.getDefault().getId());
            CountryPreset preset = CountryPreset.getById(presetId);

            String flag = preset != null ? preset.getFlag() : "";
            mTvCurrentCountry.setText((flag.isEmpty() ? "" : flag + " ") + (preset != null ? preset.getCountryName() : ""));
            mTvCurrentCarrierInfo.setText((preset != null ? preset.getOperatorName() : "") + " · MCC+MNC " + (preset != null ? preset.getOperatorMccMnc() : "") + " · ISO " + (preset != null ? preset.getCountryIso().toUpperCase() : ""));
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
            notifySettingsChanged();
            Toast.makeText(this, "Region set to " + selected.getCountryName(), Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Added " + iso.toUpperCase(Locale.ROOT), Toast.LENGTH_SHORT).show();
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

            if (!blockedSet.isEmpty() && mSwitchBlockCountries != null && !mSwitchBlockCountries.isChecked()) {
                mSwitchBlockCountries.setChecked(true);
            }

            updateBlockedCountriesSummary();
            notifySettingsChanged();
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

    /**
     * Hooked dynamically by MainHook in LSPosed/Xposed framework to return true when active.
     * Also checks if TikTok has pinged ConfigProvider recently.
     */
    public boolean isModuleActive() {
        long lastPing = mPrefs.getLong(ConfigProvider.PREF_LAST_PING, 0);
        return (System.currentTimeMillis() - lastPing < 7 * 24 * 60 * 60 * 1000L);
    }

    private boolean tryForceStopTikTokWithRoot() {
        Process process = null;
        DataOutputStream os = null;
        try {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            for (String pkg : TIKTOK_PACKAGES) {
                os.writeBytes("am force-stop " + pkg + "\n");
            }
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
            } catch (Exception ignored) {
            }
        }
    }

    private void openAppDetailsSettings() {
        for (String pkg : TIKTOK_PACKAGES) {
            try {
                getPackageManager().getPackageInfo(pkg, 0);
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + pkg));
                startActivity(intent);
                return;
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:com.zhiliaoapp.musically"));
            startActivity(intent);
        } catch (Throwable ignored) {
        }
    }

    private void launchTikTok() {
        PackageManager pm = getPackageManager();

        // 1. Try getLaunchIntentForPackage
        for (String pkg : TIKTOK_PACKAGES) {
            try {
                Intent intent = pm.getLaunchIntentForPackage(pkg);
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    return;
                }
            } catch (Throwable ignored) {}
        }

        // 2. Try generic ACTION_MAIN launcher intent per package
        for (String pkg : TIKTOK_PACKAGES) {
            try {
                Intent mainIntent = new Intent(Intent.ACTION_MAIN);
                mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                mainIntent.setPackage(pkg);
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
        }

        // 3. Fallback: try root command to launch
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "monkey -p com.zhiliaoapp.musically -c android.intent.category.LAUNCHER 1"});
            if (p.waitFor() == 0) return;
        } catch (Throwable ignored) {}

        Toast.makeText(this, "TikTok app not found on device", Toast.LENGTH_SHORT).show();
    }

    public static final String ACTION_SETTINGS_CHANGED = "com.ash.tiktokregion.ACTION_SETTINGS_CHANGED";
    public static final String ACTION_FORCE_STOP = "com.ash.tiktokregion.ACTION_FORCE_STOP";
    public static final String EXTRA_AUTO_KILL = "auto_kill";

    private void sendForceStopBroadcast() {
        for (String pkg : TIKTOK_PACKAGES) {
            try {
                Intent intent = new Intent(ACTION_FORCE_STOP);
                intent.setPackage(pkg);
                intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND | Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                sendBroadcast(intent);
            } catch (Throwable ignored) {}
        }
        try {
            Intent genericIntent = new Intent(ACTION_FORCE_STOP);
            genericIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            sendBroadcast(genericIntent);
        } catch (Throwable ignored) {}
    }

    private void notifySettingsChanged() {
        // 1. Notify ContentResolver observers (triggers ContentObserver self-kill in TikTok)
        try {
            getContentResolver().notifyChange(ConfigProvider.CONTENT_URI, null);
        } catch (Throwable ignored) {}

        // 2. Broadcast signal directly to TikTok packages with auto-kill enabled (zero root required)
        for (String pkg : TIKTOK_PACKAGES) {
            try {
                Intent intent = new Intent(ACTION_SETTINGS_CHANGED);
                intent.setPackage(pkg);
                intent.putExtra(EXTRA_AUTO_KILL, true);
                intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND | Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                sendBroadcast(intent);
            } catch (Throwable ignored) {}
        }
        try {
            Intent genericIntent = new Intent(ACTION_SETTINGS_CHANGED);
            genericIntent.putExtra(EXTRA_AUTO_KILL, true);
            genericIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            sendBroadcast(genericIntent);
        } catch (Throwable ignored) {}
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateTikTokInfo();
    }

    private void updateTikTokInfo() {
        PackageManager pm = getPackageManager();
        String detectedPkg = null;
        String versionName = null;
        long versionCode = 0;

        for (String pkg : TIKTOK_PACKAGES) {
            try {
                android.content.pm.PackageInfo pi = pm.getPackageInfo(pkg, 0);
                detectedPkg = pkg;
                versionName = pi.versionName;
                if (android.os.Build.VERSION.SDK_INT >= 28) {
                    versionCode = pi.getLongVersionCode();
                } else {
                    versionCode = pi.versionCode;
                }
                break;
            } catch (PackageManager.NameNotFoundException ignored) {}
        }

        if (detectedPkg != null && versionName != null) {
            if (mTvTiktokPackage != null) {
                String friendly = detectedPkg;
                if (detectedPkg.equals("com.zhiliaoapp.musically")) {
                    friendly = "TikTok Global";
                } else if (detectedPkg.equals("com.ss.android.ugc.trill")) {
                    friendly = "TikTok Asia";
                } else if (detectedPkg.equals("com.zhiliaoapp.musically.go")) {
                    friendly = "TikTok Lite";
                }
                mTvTiktokPackage.setText(friendly + " (" + detectedPkg + ")");
                mTvTiktokPackage.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            }
            if (mTvTiktokVersion != null) {
                mTvTiktokVersion.setText("v" + versionName + " (" + versionCode + ")");
                mTvTiktokVersion.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            }
            if (mTvTiktokSupportStatus != null) {
                if (versionName.equals("47.6.3") || versionName.startsWith("47.6.3")) {
                    mTvTiktokSupportStatus.setText("Supported (v47.6.3 Optimized)");
                    mTvTiktokSupportStatus.setTextColor(ContextCompat.getColor(this, R.color.ios_green));
                } else if (versionName.startsWith("47.") || versionName.startsWith("46.")) {
                    mTvTiktokSupportStatus.setText("Supported (v" + versionName + ")");
                    mTvTiktokSupportStatus.setTextColor(ContextCompat.getColor(this, R.color.ios_green));
                } else {
                    mTvTiktokSupportStatus.setText("Untested (v" + versionName + ")");
                    mTvTiktokSupportStatus.setTextColor(Color.parseColor("#FF9500"));
                }
            }
        } else {
            if (mTvTiktokPackage != null) {
                mTvTiktokPackage.setText("None Detected");
                mTvTiktokPackage.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
            }
            if (mTvTiktokVersion != null) {
                mTvTiktokVersion.setText("Not Installed");
                mTvTiktokVersion.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
            }
            if (mTvTiktokSupportStatus != null) {
                mTvTiktokSupportStatus.setText("TikTok Not Installed");
                mTvTiktokSupportStatus.setTextColor(ContextCompat.getColor(this, R.color.ios_red));
            }
        }
    }
}
