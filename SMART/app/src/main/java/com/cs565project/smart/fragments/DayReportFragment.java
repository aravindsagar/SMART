package com.cs565project.smart.fragments;


import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cs565project.smart.R;
import com.cs565project.smart.db.AppDao;
import com.cs565project.smart.db.AppDatabase;
import com.cs565project.smart.db.entities.AppDetails;
import com.cs565project.smart.db.entities.DailyAppUsage;
import com.cs565project.smart.util.AppInfo;
import com.cs565project.smart.util.UsageStatsUtil;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * A simple {@link Fragment} subclass.
 */
public class DayReportFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, OnChartValueSelectedListener {

    public static final String EXTRA_DATE = "extra_date";

    private static final float PIE_HOLE_RADIUS = 80f;

    // References to views.
    private PieChart myPieChart;
    private SwipeRefreshLayout myRefreshLayout;
    private RecyclerView myLegend;

    // Our state.
    private PieData myPieData;
    private long myTotalUsageTime;
    private Map<String, List<String>> myCategoryApps;
    private Date myDate;

    // For background execution.
    private Executor myExecutor = Executors.newSingleThreadExecutor();
    private Handler myHandler = new Handler();

    // Runnable to collect app usage information and update our state. Don't run in UI thread.
    private Runnable loadData = new Runnable() {
        @Override
        public void run() {
            List<PieEntry> entries = new ArrayList<>();
            AppDao dao = AppDatabase.getAppDatabase(getContext()).appDao();
            List<DailyAppUsage> appUsages = dao.getAppUsage(new Date(UsageStatsUtil.getStartOfDayMillis(myDate)));
            Map<String, Long> categoryUsages = new HashMap<>();

            // Initialize state with defaults.
            myCategoryApps = new HashMap<>();
            myTotalUsageTime = 0;

            for (DailyAppUsage appUsage : appUsages) {
                AppDetails appDetails = dao.getAppDetails(appUsage.getPackageName());
                String category = appDetails.getCategory();

                if (AppInfo.NO_CATEGORY.equals(category)) { continue; }

                myTotalUsageTime += appUsage.getDailyUseTime();
                if (categoryUsages.containsKey(category)) {
                    categoryUsages.put(category, categoryUsages.get(category) + appUsage.getDailyUseTime());
                    myCategoryApps.get(category).add(appDetails.getAppName());
                } else {
                    categoryUsages.put(category, appUsage.getDailyUseTime());
                    myCategoryApps.put(category, new ArrayList<>(Collections.singleton(appDetails.getAppName())));
                }
            }

            List<String> categories = new ArrayList<>(categoryUsages.keySet());
            Collections.sort(categories, (b, a) -> Long.compare(categoryUsages.get(a), categoryUsages.get(b)));
            for (String category : categories) {
                PieEntry entry = new PieEntry(categoryUsages.get(category), category);
                Log.d(String.valueOf(categoryUsages.get(category)), category);
                entries.add(entry);
            }

            ArrayList<Integer> colors = new ArrayList<>();


            for (int c : ColorTemplate.JOYFUL_COLORS)
                colors.add(c);

            for (int c : ColorTemplate.COLORFUL_COLORS)
                colors.add(c);

            for (int c : ColorTemplate.VORDIPLOM_COLORS)
                colors.add(c);

            for (int c : ColorTemplate.LIBERTY_COLORS)
                colors.add(c);

            for (int c : ColorTemplate.PASTEL_COLORS)
                colors.add(c);

            PieDataSet dataSet = new PieDataSet(entries, "App usage");
            dataSet.setColors(colors);
            dataSet.setDrawValues(false);
            myPieData = new PieData(dataSet);
            myHandler.post(postLoadData);
        }
    };

    // Runnable to be run in the UI thread after our state has been updated.
    private Runnable postLoadData = new Runnable() {
        @Override
        public void run() {
            if (getActivity() == null) {
                return;
            }
            // Update the pie chart.
            myPieChart.setData(myPieData);
            myPieChart.invalidate();
            String centerText = UsageStatsUtil.formatDuration(myTotalUsageTime, getActivity()) +
                    " " + getString(R.string.total);
            myPieChart.setCenterText(centerText);

            // Update the chart legend.
            PieLegendAdapter adapter = (PieLegendAdapter) myLegend.getAdapter();
            if (adapter == null) {
                adapter = new PieLegendAdapter(myPieData, myCategoryApps, myTotalUsageTime, getContext());
                myLegend.setAdapter(adapter);
            } else {
                adapter.setData(myPieData, myCategoryApps, myTotalUsageTime);
            }
            myLegend.getAdapter().notifyDataSetChanged();

            // Hide any loading spinners.
            myRefreshLayout.setRefreshing(false);
        }
    };

    public static DayReportFragment getInstance(long dateInMillis) {
        Bundle args = new Bundle();
        args.putLong(EXTRA_DATE, dateInMillis);
        DayReportFragment fragment = new DayReportFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public DayReportFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_day_report, container, false);

        // Get references to the required views.
        myRefreshLayout = rootView.findViewById(R.id.swipe_refresh);
        myPieChart = rootView.findViewById(R.id.pie_chart);
        myLegend = rootView.findViewById(R.id.pie_categories_list);

        // Init our views.
        myRefreshLayout.setOnRefreshListener(this);
        myPieChart.getDescription().setEnabled(false);
        myPieChart.getLegend().setEnabled(false);
        myPieChart.setOnChartValueSelectedListener(this);
        myPieChart.setUsePercentValues(true);
        myPieChart.setEntryLabelColor(Color.BLACK);
        myPieChart.setCenterTextTypeface(Typeface.DEFAULT_BOLD);
        myPieChart.setCenterTextSize(34);
        myPieChart.setHoleRadius(PIE_HOLE_RADIUS);
        myPieChart.setTransparentCircleRadius(PIE_HOLE_RADIUS+5);
        myPieChart.setDrawCenterText(true);
        myPieChart.setDrawEntryLabels(false);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        myLegend.setLayoutManager(layoutManager);
        DividerItemDecoration dividerItemDecoration =
                new DividerItemDecoration(Objects.requireNonNull(getContext()), layoutManager.getOrientation());
        myLegend.addItemDecoration(dividerItemDecoration);
        assert getArguments() != null;
        myDate = new Date(getArguments().getLong(EXTRA_DATE, System.currentTimeMillis()));
        setPieData();
        return rootView;
    }

    private void setPieData() {
        myRefreshLayout.setRefreshing(true);
        myExecutor.execute(loadData);
    }

    @Override
    public void onRefresh() {
        setPieData();
    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {

    }

    @Override
    public void onNothingSelected() {

    }

    private static class PieLegendAdapter extends RecyclerView.Adapter<PieLegendAdapter.ViewHolder> {

        private PieData myPieData;
        private Map<String, List<String>> myCategoryApps;
        private long total;
        private Context myContext;

        PieLegendAdapter(PieData pieData, Map<String, List<String>> categoryApps, long total, Context context) {
            this.myPieData = pieData;
            this.myCategoryApps = categoryApps;
            this.total = total;
            this.myContext = context;
        }

        public void setData(PieData pieData, Map<String, List<String>> categoryApps, long total) {
            this.myPieData = pieData;
            this.myCategoryApps = categoryApps;
            this.total = total;
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
            PieEntry entry = myPieData.getDataSet().getEntryForIndex(position);

            // Setup the background progress bar.
            holder.progressBar.setProgress((int) (entry.getValue() * 100 / total));
            int color = myPieData.getColors()[position];
            int lighterColor = Color.argb(50, Color.red(color), Color.green(color), Color.blue(color));
            holder.progressDrawable.setColorFilter(lighterColor, PorterDuff.Mode.MULTIPLY);

            // Set the legend color box.
            holder.legendColorBox.getDrawable().setColorFilter(color, PorterDuff.Mode.MULTIPLY);

            // Set title and subtitle.
            holder.title.setText(Html.fromHtml(entry.getLabel()));
            holder.subtitle.setText(buildSubtitle(myCategoryApps.get(entry.getLabel())));

            // Set duration.
            holder.duration.setText(UsageStatsUtil.formatDuration((long) entry.getValue(), myContext));
        }

        @Override
        public int getItemCount() {
            return myPieData.getEntryCount();
        }

        private String buildSubtitle(List<String> items) {
            StringBuilder sb = new StringBuilder();
            if (items.size() <= 0) {
                sb.append("");
            } else if (items.size() == 1) {
                sb.append(items.get(0));
            } else if (items.size() == 2) {
                sb.append(items.get(0)).append(" ").append("and").append(" ").append(items.get(1));
            } else {
                sb.append(items.get(0)).append(", ").append(items.get(1))
                        .append(String.format(Locale.getDefault(), myContext.getString(R.string.n_more_apps), items.size() - 2));
            }
            return sb.toString();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {

            ProgressBar progressBar;
            Drawable progressDrawable;
            ImageView legendColorBox;
            TextView title, subtitle, duration;

            ViewHolder(View itemView) {
                super(itemView);

                progressBar = itemView.findViewById(R.id.legend_progress_bar);
                progressDrawable = ((LayerDrawable) progressBar.getProgressDrawable()).findDrawableByLayerId(android.R.id.progress);
                legendColorBox = itemView.findViewById(R.id.legend_color_box);
                title = itemView.findViewById(R.id.legend_title);
                subtitle = itemView.findViewById(R.id.legend_subtitle);
                duration = itemView.findViewById(R.id.legend_duration);
            }
        }
    }
}
