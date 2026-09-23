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

public class LanguageGridAdapter extends RecyclerView.Adapter<LanguageGridAdapter.ViewHolder> {

    public interface OnLanguageToggleListener {
        void onLanguageToggled(LanguageModel item, boolean isSelected, int totalSelectedCount);
    }

    private final Context mContext;
    private final List<LanguageModel> mDisplayList;
    private final Set<String> mSelectedCodes;
    private final OnLanguageToggleListener mListener;

    public LanguageGridAdapter(Context context,
                               List<LanguageModel> languages,
                               Set<String> initialSelectedCodes,
                               OnLanguageToggleListener listener) {
        this.mContext = context;
        this.mDisplayList = new ArrayList<>(languages);
        this.mSelectedCodes = new HashSet<>();
        if (initialSelectedCodes != null) {
            for (String code : initialSelectedCodes) {
                if (code != null && !code.trim().isEmpty()) {
                    this.mSelectedCodes.add(code.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        this.mListener = listener;
    }

    public void updateList(List<LanguageModel> newList) {
        mDisplayList.clear();
        if (newList != null) {
            mDisplayList.addAll(newList);
        }
        notifyDataSetChanged();
    }

    public void addAndSelectLanguage(LanguageModel item) {
        if (item == null) return;
        mSelectedCodes.add(item.getCode().toLowerCase(Locale.ROOT));
        if (!mDisplayList.contains(item)) {
            mDisplayList.add(0, item);
        }
        notifyDataSetChanged();
        if (mListener != null) {
            mListener.onLanguageToggled(item, true, mSelectedCodes.size());
        }
    }

    public void setSelectedCodes(Set<String> newCodes) {
        mSelectedCodes.clear();
        if (newCodes != null) {
            for (String c : newCodes) {
                if (c != null && !c.trim().isEmpty()) {
                    mSelectedCodes.add(c.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        notifyDataSetChanged();
        if (mListener != null) {
            mListener.onLanguageToggled(null, false, mSelectedCodes.size());
        }
    }

    public void clearAll() {
        mSelectedCodes.clear();
        notifyDataSetChanged();
        if (mListener != null) {
            mListener.onLanguageToggled(null, false, 0);
        }
    }

    public Set<String> getSelectedCodes() {
        return Collections.unmodifiableSet(mSelectedCodes);
    }

    public int getSelectedCount() {
        return mSelectedCodes.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_language_grid, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LanguageModel lang = mDisplayList.get(position);
        String code = lang.getCode().toLowerCase(Locale.ROOT);
        boolean isSelected = mSelectedCodes.contains(code);

        holder.tvLangCode.setText(code.toUpperCase(Locale.ROOT));
        holder.tvLangName.setText(lang.getName());
        holder.tvLangNative.setText(lang.getNativeName());

        if (isSelected) {
            holder.layoutCard.setBackgroundResource(R.drawable.bg_country_card_selected);
            holder.ivCheckStatus.setVisibility(View.VISIBLE);
            holder.tvLangNative.setTextColor(ContextCompat.getColor(mContext, R.color.ios_blue));
        } else {
            holder.layoutCard.setBackgroundResource(R.drawable.bg_country_card);
            holder.ivCheckStatus.setVisibility(View.GONE);
            holder.tvLangNative.setTextColor(ContextCompat.getColor(mContext, R.color.text_tertiary));
        }

        holder.layoutCard.setOnClickListener(v -> {
            boolean nowSelected;
            if (mSelectedCodes.contains(code)) {
                mSelectedCodes.remove(code);
                nowSelected = false;
            } else {
                mSelectedCodes.add(code);
                nowSelected = true;
            }
            notifyItemChanged(holder.getBindingAdapterPosition());
            if (mListener != null) {
                mListener.onLanguageToggled(lang, nowSelected, mSelectedCodes.size());
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDisplayList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final View layoutCard;
        final TextView tvLangCode;
        final TextView tvLangName;
        final TextView tvLangNative;
        final ImageView ivCheckStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutCard = itemView.findViewById(R.id.layout_language_card);
            tvLangCode = itemView.findViewById(R.id.tv_grid_lang_code);
            tvLangName = itemView.findViewById(R.id.tv_grid_lang_name);
            tvLangNative = itemView.findViewById(R.id.tv_grid_lang_native);
            ivCheckStatus = itemView.findViewById(R.id.iv_grid_check_status);
        }
    }
}
