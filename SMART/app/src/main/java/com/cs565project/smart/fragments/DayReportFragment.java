package com.cs565project.smart.fragments;


import android.animation.TimeInterpolator;
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
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AdapterView;
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
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.github.mikephil.charting.utils.ColorTemplate.rgb;

/**
 * A simple {@link Fragment} subclass.
 */
public class DayReportFragment extends Fragment implements OnChartValueSelectedListener, AdapterView.OnItemClickListener, View.OnKeyListener {

    private static final String EXTRA_DATE = "extra_date";
    private static final String EXTRA_CATEGORY = "category";

    private static final float PIE_HOLE_RADIUS = 80f;
    private static final float PIE_SCALE_FACTOR = 0.3f;

    private static final int[] PIE_COLORS = {
            rgb("#bf360c"), rgb("#006064"), rgb("#5d4037"), rgb("#827717"),
            rgb("#f57f17"), rgb("#37474f"), rgb("#4a148c"), rgb("#ad1457"),
            rgb("#006064"), rgb("#0d47a1"), rgb("#fdd835"), rgb("#ff1744"),
            rgb("#000000")
    };
    private static final int MAX_ENTRIES = PIE_COLORS.length;

    // References to views.
    private View myRootView;
    private PieChart myPieChart, myPieChartSecondary;
    private SwipeRefreshLayout myRefreshLayout;
    private RecyclerView myLegend;

    // Our state.
    private PieData myPieData, mySecondaryPieData;
    private List<LegendInfo> myLegendInfos;
    private long myTotalUsageTime;
    private Date myDate;
    private String myCurrentCategory;
    private TimeInterpolator myInterpolator = new AccelerateDecelerateInterpolator();
    private int myPieX, myMinimizedPieX;
    private boolean myAnimatePie;

    // For background execution.
    private Executor myExecutor = Executors.newSingleThreadExecutor();
    private Handler myHandler = new Handler();

    // Runnable to collect app usage information and update our state. Don't run in UI thread.
    private Runnable loadData = new Runnable() {
        @Override
        public void run() {

            // Read usage info from DB.
            AppDao dao = AppDatabase.getAppDatabase(getContext()).appDao();
            List<DailyAppUsage> appUsages = dao.getAppUsage(new Date(UsageStatsUtil.getStartOfDayMillis(myDate)));

            // This map will hold the key value pairs to be inserted in the chart.
            Map<String, Long> usageMap = new HashMap<>();
            Map<String, Long> secondaryUsageMap = new HashMap<>();

            // Initialize state with defaults.
            Map<String, List<String>> additionalLegendInfo = new HashMap<>();
            myLegendInfos = new ArrayList<>();
            myTotalUsageTime = 0;

            // Populate the usageMap.
            for (DailyAppUsage appUsage : appUsages) {
                AppDetails appDetails = dao.getAppDetails(appUsage.getPackageName());
                String category = appDetails.getCategory();

                if (AppInfo.NO_CATEGORY.equals(category)) { continue; }

                if (usageMap.containsKey(category)) {
                    usageMap.put(category, usageMap.get(category) + appUsage.getDailyUseTime());
                    additionalLegendInfo.get(category).add(appDetails.getAppName());
                } else {
                    usageMap.put(category, appUsage.getDailyUseTime());
                    additionalLegendInfo.put(category, new ArrayList<>(Collections.singleton(appDetails.getAppName())));
                }

                // If apps within a category are visible, we need to adjust the data accordingly.
                if (isInSecondaryView()) {
                    if ( myCurrentCategory.equals(category)) {
                        myTotalUsageTime += appUsage.getDailyUseTime();
                        secondaryUsageMap.put(appDetails.getPackageName(), appUsage.getDailyUseTime());
                    }
                } else {
                    myTotalUsageTime += appUsage.getDailyUseTime();
                }
            }

            List<PieEntry> entries = processUsageMap(usageMap, additionalLegendInfo, isInSecondaryView(), !isInSecondaryView());
            PieDataSet dataSet = new PieDataSet(entries, "App usage");
            dataSet.setColors(PIE_COLORS);
            dataSet.setDrawValues(false);
            myPieData = new PieData(dataSet);

            if (isInSecondaryView()) {
                List<PieEntry> secondaryEntries = processUsageMap(secondaryUsageMap,
                        additionalLegendInfo, true, true);
                PieDataSet secondaryDataSet = new PieDataSet(secondaryEntries, "App usage");
                secondaryDataSet.setColors(PIE_COLORS);
                secondaryDataSet.setDrawValues(false);
                mySecondaryPieData = new PieData(secondaryDataSet);
            }

            myHandler.post(postLoadData);
        }

        private List<PieEntry> processUsageMap(
                Map<String, Long> usageMap, Map<String, List<String>> additionalLegendInfo,
                boolean isSecondaryData, boolean addToLegend) {

            // Output list.
            List<PieEntry> entries = new ArrayList<>();

            // Add to output in the descending order of keys in the usageMap.
            List<String> keys = new ArrayList<>(usageMap.keySet());
            Collections.sort(keys, (b, a) -> Long.compare(usageMap.get(a), usageMap.get(b)));
            int i = 0;
            for (String key : keys) {
                long usage = usageMap.get(key);
                Drawable icon = null;
                String title, subTitle;

                if (isSecondaryData) {
                    // In PER_CATEGORY state, usageMap is keyed using package names, but we want to
                    // show app name as the title in chart. package name will be the subtitle.
                    AppInfo appInfo = new AppInfo(key, getContext());
                    title = appInfo.getAppName();
                    subTitle = key;
                    icon = appInfo.getAppIcon();
                } else {
                    // In TOTAL state, the categories are the titles, and apps in them are the subtitles.
                    title = key;
                    subTitle = buildSubtitle(additionalLegendInfo.get(key));
                }

                // We want to limit the number of entries in the chart.
                if (i >= MAX_ENTRIES) {
                    PieEntry lastEntry = entries.get(MAX_ENTRIES - 1);
                    PieEntry entry = new PieEntry(usage + lastEntry.getValue(), getString(R.string.others));
                    entries.set(MAX_ENTRIES-1, entry);

                } else {
                    PieEntry entry = new PieEntry(usage, title);
                    entries.add(entry);
                }

                if (addToLegend) {
                    myLegendInfos.add(new LegendInfo(title, subTitle, icon, usage, PIE_COLORS[Math.min(i, MAX_ENTRIES - 1)]));
                }
                i++;
            }

            return entries;
        }
    };

    private boolean isInSecondaryView() {
        return myCurrentCategory != null && !myCurrentCategory.isEmpty();
    }

    // Runnable to be run in the UI thread after our state has been updated.
    private Runnable postLoadData = new Runnable() {
        @Override
        public void run() {
            if (getActivity() == null) {
                return;
            }

            // Update the main pie chart data.
            if (myAnimatePie) {
                myPieChart.animateY(600, Easing.EasingOption.EaseInOutQuad);
                myAnimatePie = false;
            }
            myPieChart.setData(myPieData);

            if (isInSecondaryView()) {
                myPieChartSecondary.animateY(600, Easing.EasingOption.EaseInOutQuad);
                myPieChartSecondary.setData(mySecondaryPieData);
            }

            // The center text shows duration, the current category being viewed, and the recorded mood.
            SpannableString centerTextDuration =
                    new SpannableString(UsageStatsUtil.formatDuration(myTotalUsageTime, getActivity()));
            centerTextDuration.setSpan(new RelativeSizeSpan(1.4f), 0, centerTextDuration.length(), 0);
            centerTextDuration.setSpan(new StyleSpan(Typeface.BOLD), 0, centerTextDuration.length(), 0);
            String centerTextCategory = (isInSecondaryView()) ?
                    String.format(getString(R.string.duration_in_category), Html.fromHtml(myCurrentCategory).toString()) :
                    getString(R.string.total);
            SpannableString centerTextMood = new SpannableString(
                    getString(R.string.mood) + " " + getEmojiByUnicode(0x1F60A)); // TODO replace with emoji matching mood.
            CharSequence centerText = TextUtils.concat(centerTextDuration, "\n", centerTextCategory, "\n\n", centerTextMood);

            if (isInSecondaryView()) {
                myPieChartSecondary.setCenterText(centerText);
            } else {
                myPieChart.setCenterText(centerText);
            }
            myPieChart.invalidate();
            myPieChartSecondary.invalidate();

            // Update the chart legend.
            PieLegendAdapter adapter = (PieLegendAdapter) myLegend.getAdapter();
            if (adapter == null) {
                adapter = new PieLegendAdapter(myLegendInfos, myTotalUsageTime, getContext(), DayReportFragment.this);
                myLegend.setAdapter(adapter);
            } else {
                adapter.setData(myLegendInfos, myTotalUsageTime);
            }
            myLegend.getAdapter().notifyDataSetChanged();

            // Hide any loading spinners.
            myRefreshLayout.setRefreshing(false);

        }
    };

    public static DayReportFragment getInstance(long dateInMillis, String category) {
        Bundle args = new Bundle();
        args.putLong(EXTRA_DATE, dateInMillis);
        args.putString(EXTRA_CATEGORY, category);
        DayReportFragment fragment = new DayReportFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public DayReportFragment() {
        // Required empty public constructor
    }

    public String getCurrentCategory() {
        return myCurrentCategory;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        myRootView = inflater.inflate(R.layout.fragment_day_report, container, false);

        // Get references to the required views.
        myRefreshLayout = myRootView.findViewById(R.id.swipe_refresh);
        myPieChart = myRootView.findViewById(R.id.pie_chart);
        myPieChartSecondary = myRootView.findViewById(R.id.pie_chart_secondary);
        myPieChartSecondary.setData(new PieData(new PieDataSet(Collections.singletonList(new PieEntry(10, "Test")), "test")));
        myLegend = myRootView.findViewById(R.id.pie_categories_list);

        // Listen for layout completion, so that we can start animations.
        myPieChart.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                init();
                myPieChart.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        return myRootView;
    }

    private void init() {
        // Get position and size of our pie-chart and derive animation translation and scaling.
        myMinimizedPieX = (int) (-PIE_SCALE_FACTOR * myPieChart.getWidth());
        myPieX = (int) myPieChart.getX();

        // Init our views.
        myRefreshLayout.setOnRefreshListener(this::onRefresh);
        setupPieAndLegendView();
        // Populate some state. This calls setPieData internally.
        String category = null;
        if (getArguments() != null) {
            category = getArguments().getString(EXTRA_CATEGORY);
            myAnimatePie = false;
        } else {
            myAnimatePie = true;
        }
        if (category == null || category.isEmpty()) {
            myCurrentCategory = "invalid"; // Ugly hack, ugh.
            switchToTotalView();
        } else {
            switchToPerCategoryView(category);
        }

        myRootView.setFocusableInTouchMode(true);
        myRootView.requestFocus();
        myRootView.setOnKeyListener(this);
    }

    private void onRefresh() {
        myAnimatePie = true;
        setPieData();
    }

    private void setupPieAndLegendView() {
        for (PieChart pieChart : Arrays.asList(myPieChart, myPieChartSecondary)) {
            pieChart.getDescription().setEnabled(false);
            pieChart.getLegend().setEnabled(false);
            pieChart.setOnChartValueSelectedListener(this);
            pieChart.setUsePercentValues(true);
            pieChart.setEntryLabelColor(Color.BLACK);
            pieChart.setHoleRadius(PIE_HOLE_RADIUS);
            pieChart.setTransparentCircleRadius(PIE_HOLE_RADIUS + 5);
            pieChart.setDrawCenterText(true);
            pieChart.setCenterTextSize(22);
            pieChart.setDrawEntryLabels(false);
        }
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        myLegend.setLayoutManager(layoutManager);
        DividerItemDecoration dividerItemDecoration =
                new DividerItemDecoration(Objects.requireNonNull(getContext()), layoutManager.getOrientation());
        myLegend.addItemDecoration(dividerItemDecoration);
        myLegend.setItemAnimator(new DefaultItemAnimator());
        assert getArguments() != null;
        myDate = new Date(getArguments().getLong(EXTRA_DATE, System.currentTimeMillis()));
    }

    private void setPieData() {
        myRefreshLayout.setRefreshing(true);
        myExecutor.execute(loadData);
    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {
        String category = ((PieEntry) e).getLabel();
        if (!category.equals(getString(R.string.others))) {
            switchToPerCategoryView(category);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        switchToPerCategoryView(myLegendInfos.get(position).title);
    }

    private void switchToPerCategoryView(String category) {
        // If we are already in details view, nothing to do.
        if (isInSecondaryView()) {
            return;
        }

        myCurrentCategory = category;
        myPieChart.animate().x(myMinimizedPieX).scaleX(PIE_SCALE_FACTOR).scaleY(PIE_SCALE_FACTOR)
                .setInterpolator(myInterpolator).start();
        myPieChartSecondary.animate().alpha(1f).scaleX(1f).scaleY(1f).setInterpolator(myInterpolator).start();
        setPieData();
    }

    private void switchToTotalView() {
        goBack();
    }

    @Override
    public void onNothingSelected() {
        switchToTotalView();
    }

    /**
     * Handle back button press. Return whether the back press was consumed by the fragment.
     */
    public boolean goBack() {
        if (!isInSecondaryView()) {
            return false;
        }

        // We are in details view. Go back to total view and consume the button press so that
        // our parent does not go back.
        myCurrentCategory = "";

        // Animations!
        myPieChart.animate().x(myPieX).scaleX(1f).scaleY(1f).setInterpolator(myInterpolator).start();
        myPieChartSecondary.animate().alpha(0f).scaleX(0f).scaleY(0f).setInterpolator(myInterpolator).start();
        setPieData();
        return true;
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        return event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK && goBack();
    }

    private String getEmojiByUnicode(int unicode){
        return new String(Character.toChars(unicode));
    }

    private String buildSubtitle(List<String> items) {
        if (getContext() == null) return null;

        StringBuilder sb = new StringBuilder();
        if (items.size() <= 0) {
            sb.append("");
        } else if (items.size() == 1) {
            sb.append(items.get(0));
        } else if (items.size() == 2) {
            sb.append(items.get(0)).append(" ").append("and").append(" ").append(items.get(1));
        } else {
            sb.append(items.get(0)).append(", ").append(items.get(1))
                    .append(String.format(Locale.getDefault(), getContext().getString(R.string.n_more_apps), items.size() - 2));
        }
        return sb.toString();
    }

    private static class PieLegendAdapter extends RecyclerView.Adapter<PieLegendAdapter.ViewHolder> {

        private List<LegendInfo> myLegendInfos;
        private long myTotal;
        private Context myContext;
        private AdapterView.OnItemClickListener myListener;

        PieLegendAdapter(List<LegendInfo> myLegendInfos, long myTotal, Context myContext,
                                AdapterView.OnItemClickListener myListener) {
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

            holder.root.setOnClickListener(v -> myListener.onItemClick(null, v, position, position));
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
    }

    private static class LegendInfo {
        String title, subTitle;
        Drawable icon;
        long usageTime;
        int color;

        LegendInfo(String title, String subTitle, Drawable icon, long usageTime, int color) {
            this.title = title;
            this.subTitle = subTitle;
            this.icon = icon;
            this.usageTime = usageTime;
            this.color = color;
        }
    }
}
