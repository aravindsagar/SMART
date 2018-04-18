package com.cs565project.smart.fragments;


import android.animation.TimeInterpolator;
import android.annotation.SuppressLint;
import android.app.Fragment;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
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
import com.cs565project.smart.util.GraphUtil;
import com.cs565project.smart.util.UsageStatsUtil;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.github.mikephil.charting.utils.ColorTemplate.rgb;

/**
 * A simple {@link Fragment} subclass.
 */
public class DayReportFragment extends Fragment implements ChartLegendAdapter.OnItemClickListener, View.OnKeyListener, SetRestrictionFragment.OnDurationSelectedListener {

    private static final String EXTRA_DATE = "extra_date";
    private static final String EXTRA_CATEGORY = "category";

    private static final float PIE_HOLE_RADIUS = 80f;
    private static final float PIE_SCALE_FACTOR = 0.3f;

    public static final int[] PIE_COLORS = {
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
    private List<ChartLegendAdapter.LegendInfo> myLegendInfos;
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
            AppDao dao = AppDatabase.getAppDatabase(getActivity()).appDao();
            List<DailyAppUsage> appUsages = dao.getAppUsage(new Date(UsageStatsUtil.getStartOfDayMillis(myDate)));

            // This map will hold the key value pairs to be inserted in the chart.
            Map<String, Long> usageMap = new HashMap<>();
            Map<String, Long> secondaryUsageMap = new HashMap<>();

            // Initialize state with defaults.
            Map<String, List<String>> subtitleInfo = new HashMap<>();
            myLegendInfos = new ArrayList<>();
            myTotalUsageTime = 0;

            // Populate the usageMap.
            for (DailyAppUsage appUsage : appUsages) {
                AppDetails appDetails = dao.getAppDetails(appUsage.getPackageName());
                String category = appDetails.getCategory();

//                if (AppInfo.NO_CATEGORY.equals(category)) { continue; } // For testing; remove

                if (usageMap.containsKey(category)) {
                    usageMap.put(category, usageMap.get(category) + appUsage.getDailyUseTime());
                    subtitleInfo.get(category).add(appDetails.getAppName());
                } else {
                    usageMap.put(category, appUsage.getDailyUseTime());
                    subtitleInfo.put(category, new ArrayList<>(Collections.singleton(appDetails.getAppName())));
                }

                // If apps within a category are visible, we need to adjust the data accordingly.
                if (isInSecondaryView()) {
                    if (myCurrentCategory.equals(category)) {
                        myTotalUsageTime += appUsage.getDailyUseTime();
                        secondaryUsageMap.put(appDetails.getPackageName(), appUsage.getDailyUseTime());
                    }
                } else {
                    myTotalUsageTime += appUsage.getDailyUseTime();
                }
            }

            if (getActivity() == null) return;
            List<PieEntry> entries = processUsageMap(usageMap, subtitleInfo, isInSecondaryView(), !isInSecondaryView());
            PieDataSet dataSet = new PieDataSet(entries, "App usage");
            dataSet.setColors(PIE_COLORS);
            dataSet.setDrawValues(false);
            myPieData = new PieData(dataSet);

            if (isInSecondaryView()) {
                List<PieEntry> secondaryEntries = processUsageMap(secondaryUsageMap,
                        subtitleInfo, true, true);
                PieDataSet secondaryDataSet = new PieDataSet(secondaryEntries, "App usage");
                secondaryDataSet.setColors(PIE_COLORS);
                secondaryDataSet.setDrawValues(false);
                mySecondaryPieData = new PieData(secondaryDataSet);
            }

            myHandler.post(postLoadData);
        }

        private List<PieEntry> processUsageMap(
                Map<String, Long> usageMap, Map<String, List<String>> subtitleInfo,
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
                    AppInfo appInfo = new AppInfo(key, getActivity());
                    title = appInfo.getAppName();
                    subTitle = key;
                    icon = appInfo.getAppIcon();
                } else {
                    // In TOTAL state, the categories are the titles, and apps in them are the subtitles.
                    title = key;
                    subTitle = GraphUtil.buildSubtitle(getActivity(), subtitleInfo.get(key));
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
                    myLegendInfos.add(new ChartLegendAdapter.LegendInfo(title, subTitle, icon, usage, PIE_COLORS[Math.min(i, MAX_ENTRIES - 1)]));
                }
                i++;
            }

            return entries;
        }
    };

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
            ChartLegendAdapter adapter = (ChartLegendAdapter) myLegend.getAdapter();
            if (adapter == null) {
                adapter = new ChartLegendAdapter(myLegendInfos, myTotalUsageTime, getActivity(), DayReportFragment.this);
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

    private boolean isInSecondaryView() {
        return myCurrentCategory != null && !myCurrentCategory.isEmpty();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupPieAndLegendView() {
        if (getActivity() == null) return;
        for (PieChart pieChart : Arrays.asList(myPieChart, myPieChartSecondary)) {
            pieChart.getDescription().setEnabled(false);
            pieChart.getLegend().setEnabled(false);
            pieChart.setUsePercentValues(true);
            pieChart.setEntryLabelColor(Color.BLACK);
            pieChart.setHoleRadius(PIE_HOLE_RADIUS);
            pieChart.setTransparentCircleRadius(PIE_HOLE_RADIUS + 5);
            pieChart.setDrawCenterText(true);
            pieChart.setCenterTextSize(22);
            pieChart.setDrawEntryLabels(false);
        }
        View.OnTouchListener existingPieListener = myPieChart.getOnTouchListener();
        myPieChart.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                switchToTotalView();
            }
            return existingPieListener.onTouch(v, event);
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        myLegend.setLayoutManager(layoutManager);
        DividerItemDecoration dividerItemDecoration =
                new DividerItemDecoration(getActivity(), layoutManager.getOrientation());
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
    public void onItemClick(int position) {
        switchToPerCategoryView(myLegendInfos.get(position).getTitle());
    }

    @Override
    public boolean onItemLongClick(int position) {
        if (isInSecondaryView()) {
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
                            .setListener(DayReportFragment.this)
                            .show(getChildFragmentManager(), "SET_RESTRICTION");
                });
            });

            return true;
        }
        return false;
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
