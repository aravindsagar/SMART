package com.cs565project.smart.fragments.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
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

/**
 * Adapter to populate restrictions list.
 */
public class RestrictionsAdapter extends RecyclerView.Adapter<RestrictionsAdapter.ViewHolder> {

    private List<AppDetails> restrictedApps, otherApps;
    private Map<String, AppInfo> appInfo;
    private List<Integer> recommendations;

    private int restrictedHeaderPos, otherHeaderPos;

    private OnItemSelectedListener listener;

    public RestrictionsAdapter(List<AppDetails> restrictedApps, List<AppDetails> otherApps,
                               Map<String, AppInfo> appInfo, List<Integer> recommendations,
                               OnItemSelectedListener listener) {
        this.restrictedApps = restrictedApps;
        this.recommendations = recommendations;
        this.otherApps = otherApps;
        this.appInfo = appInfo;
        this.listener = listener;

        restrictedHeaderPos = restrictedApps.size() > 0 ? 0 : -1;
        otherHeaderPos = restrictedHeaderPos + restrictedApps.size() + 1;
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
                        recommendations.get(position - restrictedHeaderPos - 1));
            }
        } else {
            if (otherHeaderPos == position) {
                populateHeaderItem(holder, R.string.other_apps);
            } else {
                populateAppItem(holder, otherApps.get(position - otherHeaderPos - 1), -1);
            }
        }
    }

    @Override
    public int getItemCount() {
        return restrictedApps.size() + otherApps.size() + (restrictedApps.size() > 0 ? 1 : 0) +
                (otherApps.size() > 0 ? 1 : 0);
    }

    private void populateHeaderItem(ViewHolder holder, int titleId) {
        holder.icon.setVisibility(GONE);
        holder.subtitle.setVisibility(GONE);
        holder.restrictionText.setVisibility(GONE);
        holder.recommendationText.setVisibility(GONE);

        holder.title.setText(titleId);
        holder.title.setTextAppearance(holder.itemView.getContext(), R.style.SettingsHeaderStyle);
    }

    private void populateAppItem(ViewHolder holder, AppDetails appDetails, int recommendation) {
        AppInfo info = appInfo.get(appDetails.getPackageName());
        Context  context = holder.itemView.getContext();

        holder.icon.setVisibility(View.VISIBLE);
        holder.icon.setImageDrawable(info.getAppIcon());
        holder.title.setText(Html.fromHtml(appDetails.getAppName()));
        holder.title.setTextAppearance(holder.title.getContext(), R.style.SettingsSwitchStyle);
        holder.subtitle.setVisibility(View.VISIBLE);
        holder.subtitle.setText(Html.fromHtml(appDetails.getCategory()));

        if (appDetails.getThresholdTime() > -1) {
            holder.restrictionText.setVisibility(View.VISIBLE);
            holder.restrictionText.setText(String.format(context.getString(R.string.restricted_val),
                    UsageStatsUtil.formatDuration(appDetails.getThresholdTime(), holder.itemView.getContext())));
        } else {
            holder.restrictionText.setVisibility(GONE);
        }

        if (recommendation > -1) {
            holder.recommendationText.setVisibility(View.VISIBLE);
            holder.recommendationText.setText(String.format(context.getString(R.string.recommended_val),
                    UsageStatsUtil.formatDuration(recommendation, context)));
        } else {
            holder.recommendationText.setVisibility(GONE);
        }

        holder.itemView.setOnClickListener((v -> listener.onItemSelected(appDetails, recommendation)));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView icon;
        TextView title, subtitle, restrictionText, recommendationText;

        ViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.restriction_app_icon);
            title = itemView.findViewById(R.id.restriction_title);
            subtitle = itemView.findViewById(R.id.restriction_subtitle);
            restrictionText = itemView.findViewById(R.id.restriction_text);
            recommendationText = itemView.findViewById(R.id.recommendation_text);
        }
    }

    public interface OnItemSelectedListener {
        void onItemSelected(AppDetails appDetails, long recommendation);
    }
}
