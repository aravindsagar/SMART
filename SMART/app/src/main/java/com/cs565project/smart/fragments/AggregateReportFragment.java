package com.cs565project.smart.fragments;


import android.animation.TimeInterpolator;
import android.annotation.SuppressLint;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;

import com.cs565project.smart.R;
import com.cs565project.smart.db.AppDao;
import com.cs565project.smart.db.AppDatabase;
import com.cs565project.smart.db.entities.AppDetails;
import com.cs565project.smart.db.entities.DailyAppUsage;
import com.cs565project.smart.fragments.adapter.ChartLegendAdapter;
import com.cs565project.smart.recommender.RestrictionRecommender;
import com.cs565project.smart.util.AppInfo;
import com.cs565project.smart.util.DbUtils;
import com.cs565project.smart.util.UsageStatsUtil;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CombinedData;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.github.mikephil.charting.utils.ColorTemplate.rgb;

/**
 * A simple {@link Fragment} subclass.
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
    private static final int MAX_ENTRIES = CHART_COLORS.length;

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
    private TimeInterpolator myInterpolator = new AccelerateDecelerateInterpolator();

    // For background execution.
    private Executor myExecutor = Executors.newSingleThreadExecutor();
    private Handler myHandler = new Handler();

    // Runnable to collect app usage information and update our state. Don't run in UI thread.
    private Runnable loadData = new Runnable() {
        @Override
        public void run() {

            // Read usage info from DB.
            AppDao dao = AppDatabase.getAppDatabase(getActivity()).appDao();
            List<DailyAppUsage> appUsages = dao.getAppUsage(
                    new Date(UsageStatsUtil.getStartOfDayMillis(myStartDate)),
                    new Date(UsageStatsUtil.getStartOfDayMillis(myEndDate)));

            // This map will hold the key value pairs to be inserted in the chart.
            int maxDayIdx = getDayIdx(myStartDate.getTime(), myEndDate.getTime());
            List<Map<String, Long>> usageData = new ArrayList<>(maxDayIdx + 1);
            for (int i = 0; i <= maxDayIdx; i++) usageData.add(new HashMap<>());
            Map<String, List<String>> subtitleInfo = new HashMap<>();
            Set<String> xSubVals = new HashSet<>();

            // Initialize state with defaults.
            myLegendInfos = new ArrayList<>();
            myTotalUsageTime = 0;

            // Populate the usageMap.
            for (DailyAppUsage appUsage : appUsages) {
                AppDetails appDetails = dao.getAppDetails(appUsage.getPackageName());
                String category = appDetails.getCategory();

                if (AppInfo.NO_CATEGORY.equals(category)) { continue; } // For testing; remove

                Map<String, Long> usageMap = usageData.get(getDayIdx(myStartDate.getTime(), appUsage.getDate().getTime()));

                if (isInAppView()) {
                    if (!myCurrentApp.equals(appDetails.getPackageName())) { continue; }
                    usageMap.put(appDetails.getPackageName(), appUsage.getDailyUseTime());
                    xSubVals.add(appDetails.getPackageName());
                } else if (isInCategoryView()) {
                    if (!myCurrentCategory.equals(appDetails.getCategory())) { continue; }
                    usageMap.put(appDetails.getPackageName(), appUsage.getDailyUseTime());
                    xSubVals.add(appDetails.getPackageName());
                } else {
                    if (usageMap.containsKey(category)) {
                        usageMap.put(category, usageMap.get(category) + appUsage.getDailyUseTime());
                        subtitleInfo.get(category).add(appDetails.getAppName());
                    } else {
                        usageMap.put(category, appUsage.getDailyUseTime());
                        subtitleInfo.put(category, new ArrayList<>(Collections.singleton(appDetails.getAppName())));
                    }
                    xSubVals.add(category);
                }
                myTotalUsageTime += appUsage.getDailyUseTime();
            }

            // Calculate chart data from the usage data.
            List<BarEntry> entries = processUsageMap(usageData, subtitleInfo, xSubVals);
            BarDataSet dataSet = new BarDataSet(entries, "Usage");
            dataSet.setColors(CHART_COLORS);
            dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
            myChartData = new CombinedData();
            myChartData.setData(new BarData(dataSet));
            myHandler.post(postLoadData);
        }

        private List<BarEntry> processUsageMap(List<Map<String, Long>> usageData,
                Map<String, List<String>> subtitleInfo, Set<String> xSubVals) {

            // Output list.
            List<BarEntry> entries = new ArrayList<>(usageData.size());

            for (int i = 0; i < usageData.size(); i++) {
                Map<String, Long> usageMap = usageData.get(i);
                float[] vals = new float[xSubVals.size()];
                int j = 0;
                for (String xSubVal : xSubVals) {
                    if (usageMap.containsKey(xSubVal)) {
                        vals[j++] = (float) usageMap.get(xSubVal) / DateUtils.MINUTE_IN_MILLIS;
                    } else {
                        vals[j++] = 0;
                    }
                }
                entries.add(new BarEntry(i, vals));
            }

            return entries;
        }

        private int getDayIdx(long startDate, long date) {
            return (int) ((date - startDate) / DateUtils.DAY_IN_MILLIS);
        }
    };

    // Runnable to be run in the UI thread after our state has been updated.
    private Runnable postLoadData = new Runnable() {
        @Override
        public void run() {
            if (getActivity() == null) {
                return;
            }

            // Update the chart data.
            myChart.animateY(600, Easing.EasingOption.EaseInOutQuad);
            myChart.setData(myChartData);

            myChart.invalidate();

            // Update the chart legend.
            ChartLegendAdapter adapter = (ChartLegendAdapter) myLegend.getAdapter();
            if (adapter == null) {
                adapter = new ChartLegendAdapter(myLegendInfos, myTotalUsageTime, getActivity(), AggregateReportFragment.this);
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
        if (getActivity() == null) return;

        myChart.getDescription().setEnabled(false);
        myChart.getLegend().setEnabled(false);
        myChart.getXAxis().setValueFormatter((v, a) ->
                AXIS_DATE_FORMAT.format(new Date((myStartDate.getTime() + (long) v * DateUtils.DAY_IN_MILLIS))));
        myChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        myChart.getAxisLeft().setAxisMinimum(0);
        myChart.getAxisRight().setAxisMinimum(0);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        myLegend.setLayoutManager(layoutManager);
        DividerItemDecoration dividerItemDecoration =
                new DividerItemDecoration(getActivity(), layoutManager.getOrientation());
        myLegend.addItemDecoration(dividerItemDecoration);
        myLegend.setItemAnimator(new DefaultItemAnimator());
        assert getArguments() != null;
        myStartDate = new Date(getArguments().getLong(EXTRA_START_DATE, System.currentTimeMillis()));
        myEndDate = new Date(getArguments().getLong(EXTRA_END_DATE, System.currentTimeMillis()));
    }

    private void setChartData() {
        myRefreshLayout.setRefreshing(true);
        myExecutor.execute(loadData);
    }

    @Override
    public void onItemClick(int position) {
        switchToPerCategoryView(myLegendInfos.get(position).getTitle());
    }

    @Override
    public boolean onItemLongClick(int position) {
        if (isInCategoryView()) {
            ChartLegendAdapter.LegendInfo legendInfo = myLegendInfos.get(position);
            myExecutor.execute(()->{
                AppDao dao = AppDatabase.getAppDatabase(getActivity()).appDao();
                int thresholdTime = RestrictionRecommender.recommendRestriction(
                        dao.getAppDetails(legendInfo.getSubTitle()),
                        dao.getAppUsage(legendInfo.getSubTitle())
                );
                myHandler.post(() -> {
                    SetRestrictionFragment
                            .newInstance(legendInfo.getTitle(), legendInfo.getSubTitle(), thresholdTime)
                            .setListener(AggregateReportFragment.this)
                            .show(getChildFragmentManager(), "SET_RESTRICTION");
                });
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

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        return event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK && goBack();
    }

    private String getEmojiByUnicode(int unicode){
        return new String(Character.toChars(unicode));
    }

    private String buildSubtitle(List<String> items) {
        if (getActivity() == null) return null;

        StringBuilder sb = new StringBuilder();
        if (items.size() <= 0) {
            sb.append("");
        } else if (items.size() == 1) {
            sb.append(items.get(0));
        } else if (items.size() == 2) {
            sb.append(items.get(0)).append(" ").append("and").append(" ").append(items.get(1));
        } else {
            sb.append(items.get(0)).append(", ").append(items.get(1))
                    .append(String.format(Locale.getDefault(), getActivity().getString(R.string.n_more_apps), items.size() - 2));
        }
        return sb.toString();
    }

    @Override
    public void onDurationConfirmed(String packageName, long duration) {
        Runnable postSaveRunnable = () -> myHandler.post(() ->
                Toast.makeText(getActivity(), "Restriction saved", Toast.LENGTH_SHORT).show());
        myExecutor.execute(new DbUtils.SaveRestrictionToDb(getActivity(), packageName, (int) duration,
                postSaveRunnable));
    }

    @Override
    public void onCancel() {

    }
}
