package com.ash.tiktokregion;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class BlockedCountriesDialog {

    public interface OnBlockedSavedListener {
        void onSaved();
    }

    public static void show(MainActivity activity, OnBlockedSavedListener listener) {
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_block_countries, null);
        EditText etSearch = dialogView.findViewById(R.id.et_search_blocked);
        EditText etCustomIso = dialogView.findViewById(R.id.et_custom_blocked_iso);
        View btnAddCustomIso = dialogView.findViewById(R.id.btn_add_custom_iso);
        TextView tvBlockedCounter = dialogView.findViewById(R.id.tv_blocked_counter);
        TextView btnClearAll = dialogView.findViewById(R.id.btn_clear_all_blocked);
        RecyclerView rvBlockedPresets = dialogView.findViewById(R.id.rv_blocked_presets);
        TextView tvEmptyState = dialogView.findViewById(R.id.tv_empty_blocked_state);
        View btnClose = dialogView.findViewById(R.id.btn_close_dialog);
        View btnDone = dialogView.findViewById(R.id.btn_done_blocked);

        rvBlockedPresets.setLayoutManager(new GridLayoutManager(activity, 2));

        String savedList = activity.getPrefs().getString(MainActivity.KEY_BLOCKED_COUNTRY_LIST, "");
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

        Dialog dialog = new Dialog(activity, R.style.DialogDarkTheme);
        dialog.setContentView(dialogView);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setDimAmount(0.65f);

            DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
            int width = (int) (metrics.widthPixels * 0.90);
            int height = (int) (metrics.heightPixels * 0.85);
            dialog.getWindow().setLayout(width, height);
        }

        BlockedCountryGridAdapter[] adapterHolder = new BlockedCountryGridAdapter[1];

        BlockedCountryGridAdapter adapter = new BlockedCountryGridAdapter(activity, allCountries, initialBlocked,
                (item, isBlocked, totalBlockedCount) -> {
                    tvBlockedCounter.setText(activity.getString(R.string.blocked_count_format, totalBlockedCount));
                    btnClearAll.setVisibility(totalBlockedCount > 0 ? View.VISIBLE : View.GONE);
                });
        adapterHolder[0] = adapter;
        rvBlockedPresets.setAdapter(adapter);

        int count = initialBlocked.size();
        tvBlockedCounter.setText(activity.getString(R.string.blocked_count_format, count));
        btnClearAll.setVisibility(count > 0 ? View.VISIBLE : View.GONE);

        btnClearAll.setOnClickListener(v -> adapter.clearAll());

        btnAddCustomIso.setOnClickListener(v -> {
            String iso = etCustomIso.getText() != null ? etCustomIso.getText().toString().trim().toLowerCase(Locale.ROOT) : "";
            if (iso.length() != 2) {
                Toast.makeText(activity, "Enter a 2-letter country code", Toast.LENGTH_SHORT).show();
                return;
            }
            CountryPreset.CountryItem customItem = CountryPreset.createCountryItem(iso);
            if (customItem == null) {
                Toast.makeText(activity, "Invalid country code", Toast.LENGTH_SHORT).show();
                return;
            }
            adapter.addAndSelectCountry(customItem);
            etCustomIso.setText("");
            Toast.makeText(activity, "Added " + customItem.getFlag() + " " + iso.toUpperCase(Locale.ROOT), Toast.LENGTH_SHORT).show();
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

            if (!blockedSet.isEmpty()) {
                if (activity.getSwitchBlockCountries() != null && !activity.getSwitchBlockCountries().isChecked()) {
                    activity.getSwitchBlockCountries().setChecked(true);
                }
                activity.getPrefs().edit().putBoolean(MainActivity.KEY_BLOCK_COUNTRIES, true).apply();
                activity.saveToDeviceProtectedStorage(MainActivity.KEY_BLOCK_COUNTRIES, true);
            }

            activity.getPrefs().edit().putString(MainActivity.KEY_BLOCKED_COUNTRY_LIST, joined).apply();
            activity.saveToDeviceProtectedStorage(MainActivity.KEY_BLOCKED_COUNTRY_LIST, joined);
            activity.makePrefsWorldReadable();

            if (listener != null) {
                listener.onSaved();
            }
            Toast.makeText(activity, R.string.pref_saved_notice, Toast.LENGTH_SHORT).show();
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
}
