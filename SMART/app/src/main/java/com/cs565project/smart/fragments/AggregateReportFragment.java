package com.cs565project.smart.fragments;


import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.cs565project.smart.R;
import com.cs565project.smart.db.AppDao;
import com.cs565project.smart.db.AppDatabase;
import com.cs565project.smart.db.entities.AppDetails;
import com.cs565project.smart.db.entities.DailyAppUsage;
import com.cs565project.smart.db.entities.MoodLog;
import com.cs565project.smart.fragments.adapter.ChartLegendAdapter;
import com.cs565project.smart.recommender.RestrictionRecommender;
import com.cs565project.smart.util.AppInfo;
import com.cs565project.smart.util.DbUtils;
import com.cs565project.smart.util.EmotionUtil;
import com.cs565project.smart.util.GraphUtil;
import com.cs565project.smart.util.UsageStatsUtil;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.github.mikephil.charting.utils.ColorTemplate.rgb;

/**
 * Fragment which displays aggregate reports.
 */
public class AggregateReportFragment extends Fragment implements
        ChartLegendAdapter.OnItemClickListener, View.OnKeyListener,
        SetRestrictionFragment.OnDurationSelectedListener {

    private static final String EXTRA_START_DATE = "start_date";
    private static final String EXTRA_END_DATE = "end_date";
    private static final String EXTRA_CATEGORY = "category";
    private static final String EXTRA_APP = "app";

    private static final int[] CHART_COLORS = {
            rgb("#bf360c"), rgb("#006064"), rgb("#5d4037"), rgb("#827717"),
            rgb("#f57f17"), rgb("#37474f"), rgb("#4a148c"), rgb("#ad1457"),
            rgb("#006064"), rgb("#0d47a1"), rgb("#fdd835"), rgb("#ff1744"),
            rgb("#000000")
    };

    @SuppressLint("SimpleDateFormat")
    private static final DateFormat AXIS_DATE_FORMAT = new SimpleDateFormat("MMM dd");

    // References to views.
    private View myRootView;
    private CombinedChart myChart;
    private SwipeRefreshLayout myRefreshLayout;
    private RecyclerView myLegend;

    // Our state.
    private CombinedData myChartData;
    private List<ChartLegendAdapter.LegendInfo> myLegendInfos;
    private long myTotalUsageTime;
    private Date myStartDate, myEndDate;
    private String myCurrentCategory;
    private String myCurrentApp;

    // For background execution.
    private Executor myExecutor = Executors.newSingleThreadExecutor();
    private Handler myHandler = new Handler();
    private EmotionUtil myEmotionUtil;

    // Runnable to collect app usage information and update our state. Don't run in UI thread.
    private Runnable loadData = new Runnable() {
        @Override
        public void run() {
            Context c = getActivity();
            if (c == null) return;
            // Read usage info from DB.
            AppDao dao = AppDatabase.getAppDatabase(c).appDao();
            List<DailyAppUsage> appUsages = dao.getAppUsage(myStartDate, myEndDate);

            // This map will hold the key value pairs to be inserted in the chart.
            int maxDayIdx = getDayIdx(myStartDate.getTime(), myEndDate.getTime());
            List<Map<String, Long>> usageData = new ArrayList<>(maxDayIdx + 1);
            for (int i = 0; i <= maxDayIdx; i++) usageData.add(new HashMap<>());
            Map<String, List<String>> subtitleInfo = new HashMap<>();
            Set<String> xSubVals = new HashSet<>();
            Map<String, AppDetails> appDetailMap = new HashMap<>();
            Map<String, Long> totalUsageMap = new HashMap<>(xSubVals.size());

            // Initialize state with defaults.
            myLegendInfos = new ArrayList<>();
            myTotalUsageTime = 0;

            // Populate the usageMap.
            for (DailyAppUsage appUsage : appUsages) {
                if (c.getPackageName().equals(appUsage.getPackageName())) { continue; }

                AppDetails appDetails = dao.getAppDetails(appUsage.getPackageName());
                String category = appDetails.getCategory();

                Map<String, Long> usageMap = usageData.get(getDayIdx(myStartDate.getTime(), appUsage.getDate().getTime()));

                String key;
                long val;
                if (isInAppView()) {
                    if (!myCurrentApp.equals(appDetails.getPackageName())) { continue; }
                    key = appDetails.getPackageName();
                    val = appUsage.getDailyUseTime();
                    usageMap.put(key, val);
                } else if (isInCategoryView()) {
                    if (!myCurrentCategory.equals(appDetails.getCategory())) { continue; }
                    key = appDetails.getPackageName();
                    val = appUsage.getDailyUseTime();
                    usageMap.put(key, val);
                } else {
                    key = category;
                    val = appUsage.getDailyUseTime();
                    if (usageMap.containsKey(key)) {
                        usageMap.put(key, usageMap.get(key) + val);
                        subtitleInfo.get(key).add(appDetails.getAppName());
                    } else {
                        usageMap.put(key, val);
                        subtitleInfo.put(key, new ArrayList<>(Collections.singleton(appDetails.getAppName())));
                    }
                }
                xSubVals.add(key);
                myTotalUsageTime += appUsage.getDailyUseTime();
                appDetailMap.put(appDetails.getPackageName(), appDetails);
                if (totalUsageMap.containsKey(key)) {
                    totalUsageMap.put(key, totalUsageMap.get(key) + val);
                } else {
                    totalUsageMap.put(key, val);
                }
            }

            // Calculate chart data from the usage data.
            List<BarEntry> entries = processUsageMap(usageData, subtitleInfo, xSubVals, appDetailMap, totalUsageMap);
            BarDataSet dataSet = new BarDataSet(entries, "Usage");

            // Chart colors!
            int[] colors = new int[(maxDayIdx+1) * (xSubVals.size())];
            for (int i = 0; i < colors.length; i+=xSubVals.size()) {
                System.arraycopy(CHART_COLORS, 0, colors, i, Math.min(xSubVals.size(), CHART_COLORS.length));
                for (int j = 0; j < xSubVals.size() - CHART_COLORS.length; j++) {
                    colors[i+CHART_COLORS.length+j] = CHART_COLORS[CHART_COLORS.length-1];
                }
            }
            dataSet.setColors(colors);
            dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
            BarData barData = new BarData(dataSet);
            barData.setDrawValues(false);
            myChartData = new CombinedData();
            myChartData.setData(barData);
            myChartData.setData(getEmotionData());
            myHandler.post(postLoadData);
        }

        private LineData getEmotionData() {
            if (getActivity() == null) return null;
            List<Entry> lineEntries = new ArrayList<>();
            for (int i = 0; i <= getDayIdx(myStartDate.getTime(), myEndDate.getTime()); i++) {
                MoodLog dayMood = myEmotionUtil.getLatestMoodLog(
                        new Date(myStartDate.getTime() + i * DateUtils.DAY_IN_MILLIS)
                );
                if (dayMood == null) continue;
                lineEntries.add(new Entry(
                        getDayIdx(myStartDate.getTime(), UsageStatsUtil.getStartOfDayMillis(dayMood.dateTime)),
                        (float) dayMood.happy_value * 4));
                Log.d("Mood", dayMood.happy_value + " in " + AXIS_DATE_FORMAT.format(new Date(myStartDate.getTime() + i * DateUtils.DAY_IN_MILLIS)));
            }
            if (lineEntries.isEmpty()) return null;

            LineDataSet dataSet = new LineDataSet(lineEntries, "Mood");
            dataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
            dataSet.setLineWidth(1.5f);
            dataSet.setColor(Color.BLACK);
            dataSet.setCircleColor(Color.BLACK);
            dataSet.setValueTextColor(Color.argb(150, 0, 0, 0));
            LineData data = new LineData(dataSet);
            data.setValueFormatter((value, entry, dataSetIndex, viewPortHandler) -> myEmotionUtil.getEmoji(Math.round(value)));
            data.setDrawValues(true);
            data.setValueTextSize(12f);
            return data;
        }

        private List<BarEntry> processUsageMap(List<Map<String, Long>> usageData,
                Map<String, List<String>> subtitleInfo, Set<String> xSubValSet, Map<String,
                AppDetails> appDetailMap, Map<String, Long> totalUsageMap) {

            List<String> xSubVals = new ArrayList<>(xSubValSet);
            Collections.sort(xSubVals, (a, b) -> Long.compare(totalUsageMap.get(b), totalUsageMap.get(a)));

            // Output list.
            List<BarEntry> entries = new ArrayList<>(usageData.size());
            Context c = getActivity();
            if (c == null) { return entries; }

            for (int i = 0; i < usageData.size(); i++) {
                Map<String, Long> usageMap = usageData.get(i);
                float[] vals = new float[xSubVals.size()];
                int j = 0;
                for (String xSubVal : xSubVals) {
                    if (usageMap.containsKey(xSubVal)) {
                        vals[j] = (float) usageMap.get(xSubVal);
                    } else {
                        vals[j] = 0;
                    }

                    j++;
                }
                entries.add(new BarEntry(i, vals));
            }

            // Legend entries.
            myLegendInfos = new ArrayList<>();
            int i = 0;
            for (String xSubVal : xSubVals) {
                String title, subtitle;
                Drawable icon = null;
                if (isInAppView() || isInCategoryView()) {
                    AppDetails app = appDetailMap.get(xSubVal);
                    title = app.getAppName();
                    subtitle = xSubVal;
                    icon = new AppInfo(xSubVal, c).getAppIcon();
                } else {
                    title = xSubVal;
                    subtitle = GraphUtil.buildSubtitle(c, subtitleInfo.get(xSubVal));
                }
                myLegendInfos.add(new ChartLegendAdapter.LegendInfo(title, subtitle, icon,
                        totalUsageMap.get(xSubVal), CHART_COLORS[Math.min(i, CHART_COLORS.length-1)]));
                i++;
            }

            return entries;
        }
    };

    // Runnable to be run in the UI thread after our state has been updated.
    private Runnable postLoadData = new Runnable() {
        @Override
        public void run() {
            Context c = getActivity();
            if (c == null) {
                return;
            }

            // Update the chart data.
            myChart.animateY(600, Easing.EasingOption.EaseInOutQuad);
            myChart.getXAxis().setAxisMaximum(getDayIdx(myStartDate.getTime(), myEndDate.getTime()) + 0.5f);
            myChart.setData(myChartData);

            myChart.invalidate();

            // Update the chart legend.
            ChartLegendAdapter adapter = (ChartLegendAdapter) myLegend.getAdapter();
            if (adapter == null) {
                adapter = new ChartLegendAdapter(myLegendInfos, myTotalUsageTime, c, AggregateReportFragment.this);
                myLegend.setAdapter(adapter);
            } else {
                adapter.setData(myLegendInfos, myTotalUsageTime);
            }
            myLegend.getAdapter().notifyDataSetChanged();

            // Hide any loading spinners.
            myRefreshLayout.setRefreshing(false);

        }
    };

    public static AggregateReportFragment getInstance(long startDate, long endDate, String category, String app) {
        Bundle args = new Bundle();
        args.putLong(EXTRA_START_DATE, startDate);
        args.putLong(EXTRA_END_DATE, endDate);
        args.putString(EXTRA_CATEGORY, category);
        args.putString(EXTRA_APP, app);
        AggregateReportFragment fragment = new AggregateReportFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public AggregateReportFragment() {
        // Required empty public constructor
    }

    public String getCurrentCategory() {
        return myCurrentCategory;
    }

    public String getCurrentApp() {
        return myCurrentApp;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        myEmotionUtil = new EmotionUtil(getActivity());

        // Inflate the layout for this fragment
        myRootView = inflater.inflate(R.layout.fragment_aggreagate_report, container, false);

        // Get references to the required views.
        myRefreshLayout = myRootView.findViewById(R.id.swipe_refresh);
        myChart = myRootView.findViewById(R.id.bar_chart);
        myLegend = myRootView.findViewById(R.id.legend);

        init();

        return myRootView;
    }

    private void init() {

        // Init our views.
        myRefreshLayout.setOnRefreshListener(this::onRefresh);
        setupChartAndLegendView();
        // Populate some state. This calls setChartData internally.
        String category = null, app = null;
        if (getArguments() != null) {
            category = getArguments().getString(EXTRA_CATEGORY);
            app = getArguments().getString(EXTRA_APP);
        }
        if (app != null && !app.isEmpty()) {
            myCurrentCategory = category;
            switchtoPerAppView(app);
        } else if (category != null && !category.isEmpty()) {
            switchToPerCategoryView(category);
        } else {
            myCurrentCategory = "invalid";
            myCurrentApp = "invalid";
            switchToTotalView();
        }

        myRootView.setFocusableInTouchMode(true);
        myRootView.requestFocus();
        myRootView.setOnKeyListener(this);
    }

    private void onRefresh() {
        setChartData();
    }

    private boolean isInCategoryView() {
        return myCurrentCategory != null && !myCurrentCategory.isEmpty() && !isInAppView();
    }

    private boolean isInAppView() {
        return myCurrentApp != null && !myCurrentApp.isEmpty();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupChartAndLegendView() {
        Context c = getActivity();
        if (c == null) return;

        myChart.getDescription().setEnabled(false);
        myChart.getLegend().setEnabled(false);
        XAxis xAxis = myChart.getXAxis();
        xAxis.setValueFormatter((v, a) ->
                AXIS_DATE_FORMAT.format(new Date((myStartDate.getTime() + (long) v * DateUtils.DAY_IN_MILLIS))));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setAxisMinimum(-0.5f);
        YAxis yAxisLeft = myChart.getAxisLeft(), yAxisRight = myChart.getAxisRight();
        yAxisLeft.setAxisMinimum(0);
        yAxisLeft.setValueFormatter((v, a) -> UsageStatsUtil.formatDuration((long) v, c));
        yAxisLeft.setDrawGridLines(false);
        yAxisRight.setAxisMinimum(0);
        yAxisRight.setValueFormatter((v, a) -> myEmotionUtil.getEmoji((int) v));
        yAxisRight.setDrawGridLines(false);
        yAxisRight.setGranularity(1f);
        yAxisRight.setAxisMinimum(0f);
        yAxisRight.setAxisMaximum(4.5f);

        LinearLayoutManager layoutManager = new LinearLayoutManager(c);
        myLegend.setLayoutManager(layoutManager);
        DividerItemDecoration dividerItemDecoration =
                new DividerItemDecoration(c, layoutManager.getOrientation());
        myLegend.addItemDecoration(dividerItemDecoration);
        myLegend.setItemAnimator(new DefaultItemAnimator());
        assert getArguments() != null;
        myStartDate = new Date(UsageStatsUtil.getStartOfDayMillis(
                new Date(getArguments().getLong(EXTRA_START_DATE, System.currentTimeMillis()))));
        myEndDate = new Date(UsageStatsUtil.getStartOfDayMillis(
                new Date(getArguments().getLong(EXTRA_END_DATE, System.currentTimeMillis()))));
    }

    private void setChartData() {
        myRefreshLayout.setRefreshing(true);
        myExecutor.execute(loadData);
    }

    @Override
    public void onItemClick(int position) {
        if (isInCategoryView()) {
            switchtoPerAppView(myLegendInfos.get(position).getSubTitle());
        } else {
            switchToPerCategoryView(myLegendInfos.get(position).getTitle());
        }
    }

    @Override
    public boolean onItemLongClick(int position) {
        Context c = getActivity();
        if (c == null) { return false; }
        if (isInCategoryView()) {
            ChartLegendAdapter.LegendInfo legendInfo = myLegendInfos.get(position);
            myExecutor.execute(()->{
                AppDao dao = AppDatabase.getAppDatabase(c).appDao();
                int thresholdTime = RestrictionRecommender.recommendRestriction(
                        dao.getAppDetails(legendInfo.getSubTitle()),
                        dao.getAppUsage(legendInfo.getSubTitle()),
                        dao.getAllMoodLog(),
                        new HashSet<>(dao.getCategories(true))
                );
                myHandler.post(() -> SetRestrictionFragment
                        .newInstance(legendInfo.getTitle(), legendInfo.getSubTitle(), thresholdTime)
                        .setListener(AggregateReportFragment.this)
                        .show(getChildFragmentManager(), "SET_RESTRICTION"));
            });

            return true;
        }
        return false;
    }

    private void switchtoPerAppView(String packageName) {
        if (isInAppView()) {
            return;
        }

        myCurrentApp = packageName;
        setChartData();
    }

    private void switchToPerCategoryView(String category) {
        // If we are already in details view, nothing to do.
        if (isInCategoryView()) {
            return;
        }

        myCurrentCategory = category;
        setChartData();
    }

    private void switchToTotalView() {
        myCurrentApp = "";
        myCurrentCategory = "";
        setChartData();
    }

    /**
     * Handle back button press. Return whether the back press was consumed by the fragment.
     */
    public boolean goBack() {

        if (isInAppView()) {
            myCurrentApp = "";
        } else if (isInCategoryView()) {
            myCurrentCategory = "";
        } else {
            return false;
        }

        setChartData();
        return true;
    }

    private int getDayIdx(long startDate, long date) {
        return (int) ((date - startDate) / DateUtils.DAY_IN_MILLIS);
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        return event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK && goBack();
    }

    @Override
    public void onDurationConfirmed(String packageName, long duration) {
        if (getActivity() == null) return;
        Runnable postSaveRunnable = () -> myHandler.post(() -> {
            if (getActivity() != null) {
                Toast.makeText(getActivity(), "Restriction saved", Toast.LENGTH_SHORT).show();
            }
        });
        myExecutor.execute(new DbUtils.SaveRestrictionToDb(getActivity(), packageName, (int) duration,
                postSaveRunnable));
    }

    @Override
    public void onCancel() {

    }
}
