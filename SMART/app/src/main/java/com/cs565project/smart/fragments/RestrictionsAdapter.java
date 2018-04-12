package com.cs565project.smart.fragments;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.cs565project.smart.R;
import com.cs565project.smart.db.entities.AppDetails;
import com.cs565project.smart.util.AppInfo;
import com.cs565project.smart.util.UsageStatsUtil;

import java.util.List;
import java.util.Map;

import static android.view.View.GONE;

class RestrictionsAdapter extends RecyclerView.Adapter<RestrictionsAdapter.ViewHolder> {

    private List<AppDetails> restrictedApps, recommendedApps, otherApps;
    private Map<String, AppInfo> appInfo;

    private int restrictedHeaderPos, recommendedHeaderPos, otherHeaderPos;

    private OnItemSelectedListener listener;

    RestrictionsAdapter(List<AppDetails> restrictedApps, List<AppDetails> recommendedApps,
                               List<AppDetails> otherApps, Map<String, AppInfo> appInfo,
                               OnItemSelectedListener listener) {
        this.restrictedApps = restrictedApps;
        this.recommendedApps = recommendedApps;
        this.otherApps = otherApps;
        this.appInfo = appInfo;
        this.listener = listener;

        restrictedHeaderPos = restrictedApps.size() > 0 ? 0 : -1;
        recommendedHeaderPos = restrictedHeaderPos + restrictedApps.size() +
                (recommendedApps.size() > 0 ? 1 : 0);
        otherHeaderPos = recommendedHeaderPos + recommendedApps.size() + 1;
        Log.d("restrictions", restrictedHeaderPos + ", " + recommendedHeaderPos + ", " + otherHeaderPos);
        Log.d("restrictions", restrictedApps.size() + ", " + recommendedApps.size() + ", " + otherApps.size());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_restriction, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Context c = holder.itemView.getContext();
        if (position <= restrictedApps.size() + restrictedHeaderPos) {
            if (restrictedHeaderPos == position) {
                populateHeaderItem(holder, R.string.restricted_apps);
            } else {
                populateAppItem(holder, restrictedApps.get(position - restrictedHeaderPos - 1),
                        c.getString(R.string.restricted));
            }
        } else if (position <= recommendedApps.size() + recommendedHeaderPos) {
            if (restrictedHeaderPos == position) {
                populateHeaderItem(holder, R.string.rec_restrictions);
            } else {
                populateAppItem(holder, recommendedApps.get(position - recommendedHeaderPos - 1),
                        c.getString(R.string.recommended));
            }
        } else {
            if (otherHeaderPos == position) {
                populateHeaderItem(holder, R.string.other_apps);
            } else {
                populateAppItem(holder, otherApps.get(position - otherHeaderPos - 1),
                        null);
            }
        }
    }

    @Override
    public int getItemCount() {
        return restrictedApps.size() + recommendedApps.size() + otherApps.size() +
                (restrictedApps.size() > 0 ? 1 : 0) + (recommendedApps.size() > 0 ? 1 : 0) +
                (otherApps.size() > 0 ? 1 : 0);
    }

    private void populateHeaderItem(ViewHolder holder, int titleId) {
        holder.icon.setVisibility(GONE);
        holder.subtitle.setVisibility(GONE);
        holder.restrictionTitle.setVisibility(GONE);
        holder.restrictionValue.setVisibility(GONE);

        holder.title.setText(titleId);
        holder.title.setTextAppearance(holder.itemView.getContext(), R.style.SettingsHeaderStyle);
    }

    private void populateAppItem(ViewHolder holder, AppDetails appDetails, String recommendationHeader) {
        AppInfo info = appInfo.get(appDetails.getPackageName());
        String restrictionDuration =
                UsageStatsUtil.formatDuration(appDetails.getThresholdTime(), holder.itemView.getContext());

        holder.icon.setVisibility(View.VISIBLE);
        holder.icon.setImageDrawable(info.getAppIcon());
        holder.title.setText(Html.fromHtml(appDetails.getAppName()));
        holder.title.setTextAppearance(holder.title.getContext(), R.style.SettingsSwitchStyle);
        holder.subtitle.setVisibility(View.VISIBLE);
        holder.subtitle.setText(Html.fromHtml(appDetails.getCategory()));

        if (recommendationHeader != null && !recommendationHeader.isEmpty()) {
            holder.restrictionTitle.setVisibility(View.VISIBLE);
            holder.restrictionValue.setVisibility(View.VISIBLE);
            holder.restrictionTitle.setText(recommendationHeader);
            holder.restrictionValue.setText(restrictionDuration);
        } else {
            holder.restrictionTitle.setVisibility(GONE);
            holder.restrictionValue.setVisibility(GONE);
        }

        holder.itemView.setOnClickListener((v -> listener.onItemSelected(appDetails)));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView icon;
        TextView title, subtitle, restrictionTitle, restrictionValue;

        ViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.restriction_app_icon);
            title = itemView.findViewById(R.id.restriction_title);
            subtitle = itemView.findViewById(R.id.restriction_subtitle);
            restrictionTitle = itemView.findViewById(R.id.restriction_recommendation_title);
            restrictionValue = itemView.findViewById(R.id.restriction_recommendation_value);
        }
    }

    public interface OnItemSelectedListener {
        void onItemSelected(AppDetails appDetails);
    }
}
