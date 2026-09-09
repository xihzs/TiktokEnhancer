package com.ash.tiktokregion;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CountryGridAdapter extends RecyclerView.Adapter<CountryGridAdapter.ViewHolder> {

    public interface OnPresetClickListener {
        void onPresetClick(CountryPreset preset);
    }

    private final Context mContext;
    private final List<CountryPreset> mPresets;
    private final String mSelectedPresetId;
    private final OnPresetClickListener mListener;

    public CountryGridAdapter(Context context, List<CountryPreset> presets, String selectedPresetId, OnPresetClickListener listener) {
        this.mContext = context;
        this.mPresets = new ArrayList<>(presets);
        this.mSelectedPresetId = selectedPresetId;
        this.mListener = listener;
    }

    public void updateList(List<CountryPreset> newList) {
        mPresets.clear();
        if (newList != null) {
            mPresets.addAll(newList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_country_grid, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CountryPreset preset = mPresets.get(position);

        holder.tvFlag.setText(preset.getFlag());
        holder.tvIso.setText(preset.getCountryIso().toUpperCase(Locale.ROOT));
        holder.tvCountry.setText(preset.getCountryName());
        holder.tvCarrier.setText(preset.getOperatorName());

        boolean isSelected = mSelectedPresetId != null && mSelectedPresetId.equals(preset.getId());
        if (isSelected) {
            holder.layoutCard.setBackgroundResource(R.drawable.bg_country_card_selected);
            holder.ivCheck.setVisibility(View.VISIBLE);
        } else {
            holder.layoutCard.setBackgroundResource(R.drawable.bg_country_card);
            holder.ivCheck.setVisibility(View.GONE);
        }

        holder.layoutCard.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onPresetClick(preset);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mPresets.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final View layoutCard;
        final TextView tvFlag;
        final TextView tvIso;
        final TextView tvCountry;
        final TextView tvCarrier;
        final ImageView ivCheck;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutCard = itemView.findViewById(R.id.layout_country_card);
            tvFlag = itemView.findViewById(R.id.tv_grid_flag);
            tvIso = itemView.findViewById(R.id.tv_grid_iso);
            tvCountry = itemView.findViewById(R.id.tv_grid_country);
            tvCarrier = itemView.findViewById(R.id.tv_grid_carrier);
            ivCheck = itemView.findViewById(R.id.iv_grid_check);
        }
    }
}

