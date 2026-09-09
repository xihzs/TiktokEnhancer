package com.ash.tiktokregion;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class BlockedCountryGridAdapter extends RecyclerView.Adapter<BlockedCountryGridAdapter.ViewHolder> {

    public interface OnCountryToggleListener {
        void onCountryToggled(CountryPreset.CountryItem item, boolean isBlocked, int totalBlockedCount);
    }

    private final Context mContext;
    private final List<CountryPreset.CountryItem> mDisplayList;
    private final Set<String> mBlockedIsoSet;
    private final OnCountryToggleListener mListener;

    public BlockedCountryGridAdapter(Context context,
                                     List<CountryPreset.CountryItem> countries,
                                     Set<String> initialBlockedSet,
                                     OnCountryToggleListener listener) {
        this.mContext = context;
        this.mDisplayList = new ArrayList<>(countries);
        this.mBlockedIsoSet = new HashSet<>();
        if (initialBlockedSet != null) {
            for (String iso : initialBlockedSet) {
                if (iso != null && !iso.trim().isEmpty()) {
                    this.mBlockedIsoSet.add(iso.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        this.mListener = listener;
    }

    public void updateList(List<CountryPreset.CountryItem> newList) {
        mDisplayList.clear();
        if (newList != null) {
            mDisplayList.addAll(newList);
        }
        notifyDataSetChanged();
    }

    public void addAndSelectCountry(CountryPreset.CountryItem item) {
        if (item == null) return;
        mBlockedIsoSet.add(item.getIso().toLowerCase(Locale.ROOT));
        if (!mDisplayList.contains(item)) {
            mDisplayList.add(0, item);
        }
        notifyDataSetChanged();
        if (mListener != null) {
            mListener.onCountryToggled(item, true, mBlockedIsoSet.size());
        }
    }

    public void clearAll() {
        mBlockedIsoSet.clear();
        notifyDataSetChanged();
        if (mListener != null) {
            mListener.onCountryToggled(null, false, 0);
        }
    }

    public Set<String> getBlockedIsoSet() {
        return Collections.unmodifiableSet(mBlockedIsoSet);
    }

    public int getBlockedCount() {
        return mBlockedIsoSet.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_blocked_country, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CountryPreset.CountryItem country = mDisplayList.get(position);
        String iso = country.getIso().toLowerCase(Locale.ROOT);
        boolean isBlocked = mBlockedIsoSet.contains(iso);

        holder.tvFlag.setText(country.getFlag());
        holder.tvIso.setText(iso.toUpperCase(Locale.ROOT));
        holder.tvCountry.setText(country.getName());

        if (isBlocked) {
            holder.layoutCard.setBackgroundResource(R.drawable.bg_country_card_blocked);
            holder.ivBlockStatus.setVisibility(View.VISIBLE);
            holder.tvStatusLabel.setText("Blocked");
            holder.tvStatusLabel.setTextColor(ContextCompat.getColor(mContext, R.color.status_blocked_dot));
        } else {
            holder.layoutCard.setBackgroundResource(R.drawable.bg_country_card);
            holder.ivBlockStatus.setVisibility(View.GONE);
            holder.tvStatusLabel.setText("Allowed");
            holder.tvStatusLabel.setTextColor(ContextCompat.getColor(mContext, R.color.text_tertiary));
        }

        holder.layoutCard.setOnClickListener(v -> {
            boolean nowBlocked;
            if (mBlockedIsoSet.contains(iso)) {
                mBlockedIsoSet.remove(iso);
                nowBlocked = false;
            } else {
                mBlockedIsoSet.add(iso);
                nowBlocked = true;
            }
            notifyItemChanged(holder.getBindingAdapterPosition());
            if (mListener != null) {
                mListener.onCountryToggled(country, nowBlocked, mBlockedIsoSet.size());
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDisplayList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final View layoutCard;
        final TextView tvFlag;
        final TextView tvIso;
        final TextView tvCountry;
        final TextView tvStatusLabel;
        final ImageView ivBlockStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutCard = itemView.findViewById(R.id.layout_country_card);
            tvFlag = itemView.findViewById(R.id.tv_grid_flag);
            tvIso = itemView.findViewById(R.id.tv_grid_iso);
            tvCountry = itemView.findViewById(R.id.tv_grid_country);
            tvStatusLabel = itemView.findViewById(R.id.tv_grid_status_label);
            ivBlockStatus = itemView.findViewById(R.id.iv_grid_block_status);
        }
    }
}

