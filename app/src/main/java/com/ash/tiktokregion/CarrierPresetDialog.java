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

import java.util.List;

public class CarrierPresetDialog {

    public interface OnPresetSelectedListener {
        void onPresetSelected(CountryPreset preset);
    }

    public static void show(MainActivity activity, OnPresetSelectedListener listener) {
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_select_preset, null);
        EditText etSearch = dialogView.findViewById(R.id.et_search_country);
        RecyclerView rvPresets = dialogView.findViewById(R.id.rv_presets);
        TextView tvEmptyState = dialogView.findViewById(R.id.tv_empty_state);
        View btnClose = dialogView.findViewById(R.id.btn_close_dialog);

        rvPresets.setLayoutManager(new GridLayoutManager(activity, 2));

        String currentPresetId = activity.getPrefs().getString(MainActivity.KEY_PRESET_ID, CountryPreset.getDefault().getId());
        if (activity.getPrefs().getBoolean(MainActivity.KEY_IS_CUSTOM, false)) {
            currentPresetId = null;
        }

        Dialog dialog = new Dialog(activity, R.style.DialogDarkTheme);
        dialog.setContentView(dialogView);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setDimAmount(0.65f);

            DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
            int width = (int) (metrics.widthPixels * 0.90);
            int height = (int) (metrics.heightPixels * 0.78);
            dialog.getWindow().setLayout(width, height);
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        CountryGridAdapter adapter = new CountryGridAdapter(activity, CountryPreset.PRESETS, currentPresetId, selected -> {
            activity.getPrefs().edit()
                    .putString(MainActivity.KEY_PRESET_ID, selected.getId())
                    .putBoolean(MainActivity.KEY_IS_CUSTOM, false)
                    .apply();

            activity.saveToDeviceProtectedStorage(MainActivity.KEY_PRESET_ID, selected.getId());
            activity.saveToDeviceProtectedStorage(MainActivity.KEY_IS_CUSTOM, false);
            activity.makePrefsWorldReadable();

            if (listener != null) {
                listener.onPresetSelected(selected);
            }
            Toast.makeText(activity, "Region set to " + selected.getFlag() + " " + selected.getCountryName(), Toast.LENGTH_SHORT).show();
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
}
