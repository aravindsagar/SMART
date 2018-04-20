package com.cs565project.smart.fragments.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cs565project.smart.R;
import com.cs565project.smart.util.UsageStatsUtil;

import java.util.List;

/**
 * Adapter to populate chart legend.
 */
public class ChartLegendAdapter extends RecyclerView.Adapter<ChartLegendAdapter.ViewHolder> {

    private List<LegendInfo> myLegendInfos;
    private long myTotal;
    private Context myContext;
    private OnItemClickListener myListener;

    public ChartLegendAdapter(List<LegendInfo> myLegendInfos, long myTotal, Context myContext,
                              OnItemClickListener myListener) {
        this.myLegendInfos = myLegendInfos;
        this.myTotal = myTotal;
        this.myContext = myContext;
        this.myListener = myListener;
    }

    public void setData(List<LegendInfo> myLegendInfos, long myTotal) {
        this.myLegendInfos = myLegendInfos;
        this.myTotal = myTotal;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View root = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_pie_legend, parent, false);
        return new ViewHolder(root);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LegendInfo entry = myLegendInfos.get(position);

        // Setup the background progress bar.
        if (myTotal > 0) {
            holder.progressBar.setProgress((int) (entry.usageTime * 100 / myTotal));
        }
        int color = entry.color;
        int lighterColor = Color.argb(50, Color.red(color), Color.green(color), Color.blue(color));
        holder.progressDrawable.setColorFilter(lighterColor, PorterDuff.Mode.MULTIPLY);

        // Set the legend color box.
        holder.legendColorBox.getDrawable().setColorFilter(color, PorterDuff.Mode.MULTIPLY);

        // Set title and subtitle.
        holder.title.setText(Html.fromHtml(entry.title));
        holder.subtitle.setText(Html.fromHtml(entry.subTitle));

        // Set app icon if available.
        if (entry.icon != null) {
            holder.appIcon.setImageDrawable(entry.icon);
            holder.appIcon.setVisibility(View.VISIBLE);
        } else {
            holder.appIcon.setVisibility(View.GONE);
        }

        // Set duration.
        holder.duration.setText(UsageStatsUtil.formatDuration(entry.usageTime, myContext));

        holder.root.setOnClickListener(v -> myListener.onItemClick(position));
        holder.root.setOnLongClickListener(v -> myListener.onItemLongClick(position));
    }

    @Override
    public int getItemCount() {
        return myLegendInfos.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        View root;
        ProgressBar progressBar;
        Drawable progressDrawable;
        ImageView legendColorBox, appIcon;
        TextView title, subtitle, duration;

        ViewHolder(View itemView) {
            super(itemView);

            root = itemView;
            progressBar = itemView.findViewById(R.id.legend_progress_bar);
            progressDrawable = ((LayerDrawable) progressBar.getProgressDrawable()).findDrawableByLayerId(android.R.id.progress);
            legendColorBox = itemView.findViewById(R.id.legend_color_box);
            title = itemView.findViewById(R.id.legend_title);
            subtitle = itemView.findViewById(R.id.legend_subtitle);
            duration = itemView.findViewById(R.id.legend_duration);
            appIcon = itemView.findViewById(R.id.legend_app_icon);
        }
    }

    public static class LegendInfo {
        String title;
        String subTitle;
        Drawable icon;
        long usageTime;
        int color;

        public LegendInfo(String title, String subTitle, Drawable icon, long usageTime, int color) {
            this.title = title;
            this.subTitle = subTitle;
            this.icon = icon;
            this.usageTime = usageTime;
            this.color = color;
        }

        public String getTitle() {
            return title;
        }

        public String getSubTitle() {
            return subTitle;
        }
    }

    public interface OnItemClickListener {
        void onItemClick(int position);

        boolean onItemLongClick(int position);
    }
}
