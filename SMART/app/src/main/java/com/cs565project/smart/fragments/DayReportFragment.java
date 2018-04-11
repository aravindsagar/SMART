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
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
public class DayReportFragment extends Fragment implements OnChartValueSelectedListener, AdapterView.OnItemClickListener, View.OnKeyListener {

    private static final String EXTRA_DATE = "extra_date";
    private static final String EXTRA_CATEGORY = "category";

    private static final float PIE_HOLE_RADIUS = 80f;

    enum ViewState {TOTAL, PER_CATEGORY}

    // References to views.
    private PieChart myPieChart;
    private SwipeRefreshLayout myRefreshLayout;
    private RecyclerView myLegend;

    // Our state.
    private PieData myPieData;
    private long myTotalUsageTime;
    private Map<String, List<String>> myAdditionalLegendInfo;
    private Map<String, Drawable> myAppIcons;
    private Date myDate;
    private ViewState myViewState;
    private String myCurrentCategory;

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
            Map<String, Long> usageMap = new HashMap<>();

            // Initialize state with defaults.
            myAdditionalLegendInfo = new HashMap<>();
            myAppIcons = new HashMap<>();
            myTotalUsageTime = 0;

            for (DailyAppUsage appUsage : appUsages) {
                AppDetails appDetails = dao.getAppDetails(appUsage.getPackageName());
                String category = appDetails.getCategory();

                if (AppInfo.NO_CATEGORY.equals(category)) { continue; }

                // We have to collect different data depending on our state.
                if (myViewState == ViewState.TOTAL) {
                    myTotalUsageTime += appUsage.getDailyUseTime();
                    if (usageMap.containsKey(category)) {
                        usageMap.put(category, usageMap.get(category) + appUsage.getDailyUseTime());
                        myAdditionalLegendInfo.get(category).add(appDetails.getAppName());
                    } else {
                        usageMap.put(category, appUsage.getDailyUseTime());
                        myAdditionalLegendInfo.put(category, new ArrayList<>(Collections.singleton(appDetails.getAppName())));
                    }
                } else if (myViewState == ViewState.PER_CATEGORY) {
                    if (!myCurrentCategory.equals(category)) { continue; }

                    myTotalUsageTime += appUsage.getDailyUseTime();
                    usageMap.put(appDetails.getAppName(), appUsage.getDailyUseTime());
                    myAdditionalLegendInfo.put(appDetails.getAppName(), Collections.singletonList(appDetails.getPackageName()));
                    myAppIcons.put(appDetails.getAppName(), new AppInfo(appDetails.getPackageName(), getContext()).getAppIcon());
                }
            }

            List<String> categories = new ArrayList<>(usageMap.keySet());
            Collections.sort(categories, (b, a) -> Long.compare(usageMap.get(a), usageMap.get(b)));
            for (String category : categories) {
                PieEntry entry = new PieEntry(usageMap.get(category), category);
                Log.d(String.valueOf(usageMap.get(category)), category);
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
            myPieChart.animateY(600, Easing.EasingOption.EaseInOutQuad);
            myPieChart.setData(myPieData);
            // The center text shows duration, the current category being viewed, and the recorded mood.
            SpannableString centerTextDuration =
                    new SpannableString(UsageStatsUtil.formatDuration(myTotalUsageTime, getActivity()));
            centerTextDuration.setSpan(new RelativeSizeSpan(1.4f), 0, centerTextDuration.length(), 0);
            centerTextDuration.setSpan(new StyleSpan(Typeface.BOLD), 0, centerTextDuration.length(), 0);
            String centerTextCategory = (myViewState == ViewState.TOTAL) ?
                    getString(R.string.total) :
                    String.format(getString(R.string.duration_in_category), Html.fromHtml(myCurrentCategory).toString());
            SpannableString centerTextMood = new SpannableString(
                    getString(R.string.mood) + " " + getEmojiByUnicode(0x1F60A)); // TODO replace with emoji matching mood.
//            centerTextMood.setSpan(new StyleSpan(ITALIC), 0, centerTextMood.length(), 0);
            CharSequence centerText = TextUtils.concat(centerTextDuration, "\n", centerTextCategory, "\n\n", centerTextMood);

            myPieChart.setCenterText(centerText);
            myPieChart.invalidate();

            // Update the chart legend.
            PieLegendAdapter adapter = (PieLegendAdapter) myLegend.getAdapter();
            if (adapter == null) {
                adapter = new PieLegendAdapter(myPieData, myAdditionalLegendInfo, myTotalUsageTime,
                        myAppIcons, getContext(), DayReportFragment.this);
                myLegend.setAdapter(adapter);
            } else {
                adapter.setData(myPieData, myAdditionalLegendInfo, myTotalUsageTime, myAppIcons);
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
        View rootView = inflater.inflate(R.layout.fragment_day_report, container, false);

        // Get references to the required views.
        myRefreshLayout = rootView.findViewById(R.id.swipe_refresh);
        myPieChart = rootView.findViewById(R.id.pie_chart);
        myLegend = rootView.findViewById(R.id.pie_categories_list);

        // Populate some state.
        if (getArguments() != null) {
            myCurrentCategory = getArguments().getString(EXTRA_CATEGORY);
        }
        if (myCurrentCategory == null || myCurrentCategory.isEmpty()) {
            myViewState = ViewState.TOTAL;
            myCurrentCategory = "";
        } else {
            myViewState = ViewState.PER_CATEGORY;
        }

        // Init our views.
        myRefreshLayout.setOnRefreshListener(this::setPieData);
        setupPieAndLegendView();
        setPieData();

        rootView.setFocusableInTouchMode(true);
        rootView.requestFocus();
        rootView.setOnKeyListener(this);
        return rootView;
    }

    private void setupPieAndLegendView() {
        myPieChart.getDescription().setEnabled(false);
        myPieChart.getLegend().setEnabled(false);
        myPieChart.setOnChartValueSelectedListener(this);
        myPieChart.setUsePercentValues(true);
        myPieChart.setEntryLabelColor(Color.BLACK);
        myPieChart.setHoleRadius(PIE_HOLE_RADIUS);
        myPieChart.setTransparentCircleRadius(PIE_HOLE_RADIUS+5);
        myPieChart.setDrawCenterText(true);
        myPieChart.setCenterTextSize(22);
        myPieChart.setDrawEntryLabels(false);
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
        switchToPerCategoryView(((PieEntry) e).getLabel());
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        switchToPerCategoryView(myPieData.getDataSet().getEntryForIndex(position).getLabel());
    }

    private void switchToPerCategoryView(String category) {
        // If we are already in details view, nothing to do.
        if (myViewState == ViewState.PER_CATEGORY) {
            return;
        }

        myViewState = ViewState.PER_CATEGORY;
        myCurrentCategory = category;
        setPieData();
    }

    @Override
    public void onNothingSelected() {

    }

    /**
     * Handle back button press. Return whether the back press was consumed by the fragment.
     */
    public boolean goBack() {
        if (myViewState == ViewState.TOTAL) {
            return false;
        }

        // We are in details view. Go back to total view and consume the button press so that
        // our parent does not go back.
        myViewState = ViewState.TOTAL;
        myCurrentCategory = "";
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

    private static class PieLegendAdapter extends RecyclerView.Adapter<PieLegendAdapter.ViewHolder> {

        private PieData myPieData;
        private Map<String, List<String>> myAdditionalLegendInfo;
        private long myTotal;
        private Context myContext;
        private AdapterView.OnItemClickListener myListener;
        private Map<String, Drawable> myAppIcons;

        PieLegendAdapter(PieData pieData, Map<String, List<String>> categoryApps, long total,
                         Map<String, Drawable> appIcons, Context context,
                         AdapterView.OnItemClickListener listener) {
            this.myPieData = pieData;
            this.myAdditionalLegendInfo = categoryApps;
            this.myTotal = total;
            this.myAppIcons = appIcons;
            this.myContext = context;
            this.myListener = listener;
        }

        public void setData(PieData pieData, Map<String, List<String>> categoryApps, long total,
                            Map<String, Drawable> appIcons) {
            this.myPieData = pieData;
            this.myAdditionalLegendInfo = categoryApps;
            this.myTotal = total;
            this.myAppIcons = appIcons;
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
            holder.progressBar.setProgress((int) (entry.getValue() * 100 / myTotal));
            int color = myPieData.getColors()[position];
            int lighterColor = Color.argb(50, Color.red(color), Color.green(color), Color.blue(color));
            holder.progressDrawable.setColorFilter(lighterColor, PorterDuff.Mode.MULTIPLY);

            // Set the legend color box.
            holder.legendColorBox.getDrawable().setColorFilter(color, PorterDuff.Mode.MULTIPLY);

            // Set title and subtitle.
            holder.title.setText(Html.fromHtml(entry.getLabel()));
            String subtitle = buildSubtitle(myAdditionalLegendInfo.get(entry.getLabel()));
            holder.subtitle.setText(subtitle);

            // Set app icon if available.
            Drawable icon;

            if (myAppIcons != null && (icon = myAppIcons.get(entry.getLabel())) != null) {
                holder.appIcon.setImageDrawable(icon);
                holder.appIcon.setVisibility(View.VISIBLE);
            } else {
                holder.appIcon.setVisibility(View.GONE);
            }

            // Set duration.
            holder.duration.setText(UsageStatsUtil.formatDuration((long) entry.getValue(), myContext));

            holder.root.setOnClickListener(v -> myListener.onItemClick(null, v, position, position));
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
}
