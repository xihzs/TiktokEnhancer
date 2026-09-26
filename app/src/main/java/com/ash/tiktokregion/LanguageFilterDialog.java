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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class LanguageFilterDialog {

    public interface OnLanguagesSavedListener {
        void onSaved();
    }

    public static Set<String> getLanguagesForCountry(String countryIso) {
        Set<String> set = new HashSet<>();
        if (countryIso == null) return set;
        String iso = countryIso.trim().toLowerCase(Locale.ROOT);
        switch (iso) {
            case "id":
                set.add("id");
                break;
            case "my":
                set.add("ms");
                set.add("en");
                break;
            case "sg":
                set.add("en");
                set.add("zh");
                break;
            case "jp":
                set.add("ja");
                break;
            case "kr":
                set.add("ko");
                break;
            case "vn":
                set.add("vi");
                break;
            case "th":
                set.add("th");
                break;
            case "ph":
                set.add("tl");
                set.add("en");
                break;
            case "in":
                set.add("en");
                set.add("hi");
                break;
            case "es":
            case "mx":
            case "ar":
            case "co":
            case "cl":
            case "pe":
                set.add("es");
                break;
            case "br":
            case "pt":
                set.add("pt");
                break;
            case "fr":
                set.add("fr");
                break;
            case "de":
            case "at":
            case "ch":
                set.add("de");
                break;
            case "it":
                set.add("it");
                break;
            case "ru":
            case "by":
                set.add("ru");
                break;
            case "kz":
                set.add("kk");
                set.add("ru");
                break;
            case "sa":
            case "ae":
            case "eg":
            case "qa":
            case "kw":
                set.add("ar");
                break;
            case "tr":
                set.add("tr");
                break;
            case "nl":
                set.add("nl");
                set.add("en");
                break;
            case "pl":
                set.add("pl");
                break;
            case "ua":
                set.add("uk");
                break;
            case "cn":
                set.add("zh");
                break;
            case "tw":
            case "hk":
                set.add("zh-Hant");
                break;
            case "us":
            case "gb":
            case "ca":
            case "au":
            case "nz":
            case "ie":
            default:
                if (LanguageModel.findByCode(iso) != null) {
                    set.add(iso);
                } else {
                    set.add("en");
                }
                break;
        }
        return set;
    }

    public static void show(MainActivity activity, OnLanguagesSavedListener listener) {
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_select_languages, null);
        EditText etSearch = dialogView.findViewById(R.id.et_search_lang);
        EditText etCustomLang = dialogView.findViewById(R.id.et_custom_lang_code);
        View btnAddCustomLang = dialogView.findViewById(R.id.btn_add_custom_lang);
        TextView tvLangCounter = dialogView.findViewById(R.id.tv_language_counter);
        TextView btnSyncRegion = dialogView.findViewById(R.id.btn_sync_region_lang);
        TextView btnClearAll = dialogView.findViewById(R.id.btn_clear_all_languages);
        RecyclerView rvLanguages = dialogView.findViewById(R.id.rv_languages);
        TextView tvEmptyState = dialogView.findViewById(R.id.tv_empty_languages_state);
        View btnClose = dialogView.findViewById(R.id.btn_close_dialog);
        View btnDone = dialogView.findViewById(R.id.btn_done_languages);

        rvLanguages.setLayoutManager(new GridLayoutManager(activity, 2));

        String savedList = activity.getPrefs().getString(MainActivity.KEY_ALLOWED_LANGUAGES, "");
        Set<String> initialSelected = new HashSet<>();
        if (savedList != null && !savedList.trim().isEmpty()) {
            for (String code : savedList.split(",")) {
                String clean = code.trim().toLowerCase(Locale.ROOT);
                if (!clean.isEmpty()) {
                    initialSelected.add(clean);
                }
            }
        }

        List<LanguageModel> allLanguages = new ArrayList<>(LanguageModel.getAll());
        Set<String> knownCodes = new HashSet<>();
        for (LanguageModel l : allLanguages) {
            knownCodes.add(l.getCode().toLowerCase(Locale.ROOT));
        }
        for (String code : initialSelected) {
            if (!knownCodes.contains(code)) {
                LanguageModel custom = new LanguageModel(code, code.toUpperCase(Locale.ROOT), code.toUpperCase(Locale.ROOT));
                allLanguages.add(0, custom);
                knownCodes.add(code);
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

        LanguageGridAdapter[] adapterHolder = new LanguageGridAdapter[1];

        LanguageGridAdapter adapter = new LanguageGridAdapter(activity, allLanguages, initialSelected,
                (item, isSelected, totalSelectedCount) -> {
                    tvLangCounter.setText(activity.getString(R.string.language_filter_count_format, totalSelectedCount));
                    btnClearAll.setVisibility(totalSelectedCount > 0 ? View.VISIBLE : View.GONE);
                });
        adapterHolder[0] = adapter;
        rvLanguages.setAdapter(adapter);

        int count = initialSelected.size();
        tvLangCounter.setText(activity.getString(R.string.language_filter_count_format, count));
        btnClearAll.setVisibility(count > 0 ? View.VISIBLE : View.GONE);

        btnClearAll.setOnClickListener(v -> adapter.clearAll());

        btnSyncRegion.setOnClickListener(v -> {
            String regionIso = activity.getCurrentRegionIso();
            Set<String> matchedLangs = getLanguagesForCountry(regionIso);
            for (String langCode : matchedLangs) {
                if (!knownCodes.contains(langCode)) {
                    LanguageModel custom = new LanguageModel(langCode, langCode.toUpperCase(Locale.ROOT), langCode.toUpperCase(Locale.ROOT));
                    allLanguages.add(0, custom);
                    knownCodes.add(langCode);
                }
            }
            adapter.setSelectedCodes(matchedLangs);
            Toast.makeText(activity, "Matched languages for " + regionIso.toUpperCase(Locale.ROOT), Toast.LENGTH_SHORT).show();
        });

        btnAddCustomLang.setOnClickListener(v -> {
            String code = etCustomLang.getText() != null ? etCustomLang.getText().toString().trim().toLowerCase(Locale.ROOT) : "";
            if (code.length() < 2) {
                Toast.makeText(activity, "Enter valid language code (e.g. en, id, ja)", Toast.LENGTH_SHORT).show();
                return;
            }
            LanguageModel existing = LanguageModel.findByCode(code);
            if (existing == null) {
                existing = new LanguageModel(code, code.toUpperCase(Locale.ROOT), code.toUpperCase(Locale.ROOT));
            }
            adapter.addAndSelectLanguage(existing);
            etCustomLang.setText("");
            Toast.makeText(activity, "Added " + existing.getDisplayName(), Toast.LENGTH_SHORT).show();
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s != null ? s.toString().trim().toLowerCase(Locale.ROOT) : "";
                List<LanguageModel> filtered = new ArrayList<>();
                if (query.isEmpty()) {
                    filtered.addAll(allLanguages);
                } else {
                    for (LanguageModel lang : allLanguages) {
                        if (lang.getCode().toLowerCase(Locale.ROOT).contains(query)
                                || lang.getName().toLowerCase(Locale.ROOT).contains(query)
                                || lang.getNativeName().toLowerCase(Locale.ROOT).contains(query)) {
                            filtered.add(lang);
                        }
                    }
                }
                adapter.updateList(filtered);
                boolean isEmpty = filtered.isEmpty();
                tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                rvLanguages.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        Runnable saveAndDismiss = () -> {
            Set<String> selected = adapter.getSelectedCodes();
            StringBuilder sb = new StringBuilder();
            for (String code : selected) {
                if (sb.length() > 0) sb.append(",");
                sb.append(code);
            }
            String joined = sb.toString();

            if (!selected.isEmpty()) {
                if (activity.getSwitchLanguageFilter() != null && !activity.getSwitchLanguageFilter().isChecked()) {
                    activity.getSwitchLanguageFilter().setChecked(true);
                }
                activity.getPrefs().edit().putBoolean(MainActivity.KEY_LANGUAGE_FILTER, true).apply();
                activity.saveToDeviceProtectedStorage(MainActivity.KEY_LANGUAGE_FILTER, true);
            }

            activity.getPrefs().edit().putString(MainActivity.KEY_ALLOWED_LANGUAGES, joined).apply();
            activity.saveToDeviceProtectedStorage(MainActivity.KEY_ALLOWED_LANGUAGES, joined);
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
