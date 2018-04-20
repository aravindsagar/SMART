package com.cs565project.smart.fragments;


import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.cs565project.smart.R;
import com.cs565project.smart.SettingsActivity;
import com.cs565project.smart.db.AppDao;
import com.cs565project.smart.db.AppDatabase;
import com.cs565project.smart.db.entities.AppDetails;
import com.cs565project.smart.db.entities.MoodLog;
import com.cs565project.smart.fragments.adapter.RestrictionsAdapter;
import com.cs565project.smart.recommender.RestrictionRecommender;
import com.cs565project.smart.util.AppInfo;
import com.cs565project.smart.util.DbUtils;
import com.cs565project.smart.util.PreferencesHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


/**
 * A simple {@link Fragment} subclass to display and modify app restrictions.
 */
public class RestrictionsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener,
        RestrictionsAdapter.OnItemSelectedListener, SetRestrictionFragment.OnDurationSelectedListener, View.OnKeyListener {

    // Views that we care about.
    private RecyclerView myAppList;
    private SwipeRefreshLayout mySwipeRefreshLayout;
    private View myNoBlockView;

    // Our state.
    List<AppDetails> restrictedApps, otherApps;
    List<Integer> recommendations;
    Map<String, AppInfo> appInfo;

    // Helper fields.
    private Handler myHandler = new Handler();
    private Executor myExecutor = Executors.newSingleThreadExecutor();

    // Runnable to load data in background.
    private Runnable loadData = new Runnable() {
        @Override
        public void run() {
            if (getActivity() == null) return;
            Context c = getActivity();

            AppDao dao = AppDatabase.getAppDatabase(c).appDao();

            restrictedApps = new ArrayList<>();
            otherApps = new ArrayList<>();
            appInfo = new HashMap<>();
            recommendations = new ArrayList<>();

            Set<String> categoriesToRestrict = new HashSet<>(dao.getCategories(true));
            List<MoodLog> moodLogs = dao.getAllMoodLog();

            for (AppDetails appDetails : dao.getAppDetails()) {
                appInfo.put(appDetails.getPackageName(), new AppInfo(appDetails.getPackageName(), c));

                int recommendation = RestrictionRecommender.recommendRestriction(
                        appDetails,
                        dao.getAppUsage(appDetails.getPackageName()),
                        moodLogs,
                        categoriesToRestrict
                );
                if (!c.getPackageName().equals(appDetails.getPackageName()) &&
                        (appDetails.getThresholdTime() >= 0 || recommendation > 0)) {
                    restrictedApps.add(appDetails);
                    recommendations.add(recommendation);
                } else {
                    otherApps.add(appDetails);
                }
            }

            myHandler.post(postLoadData);
        }
    };

    // Runnable to handle UI updates after loading data.
    private Runnable postLoadData = new Runnable() {
        @Override
        public void run() {
            if (getActivity() == null) return;

            myAppList.setAdapter(new RestrictionsAdapter(restrictedApps, otherApps, appInfo,
                    recommendations, RestrictionsFragment.this));
            myAppList.invalidate();

            mySwipeRefreshLayout.setRefreshing(false);
        }
    };

    public RestrictionsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_restrictions, container, false);
        rootView.setOnKeyListener(this);
        myAppList = rootView.findViewById(R.id.list_restriction_apps);
        myAppList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mySwipeRefreshLayout = rootView.findViewById(R.id.refresh_restrictions);
        mySwipeRefreshLayout.setOnRefreshListener(this);
        myNoBlockView = rootView.findViewById(R.id.no_app_block_view);

        setupAppList();

        Button settingsBtn = rootView.findViewById(R.id.btn_settings);
        settingsBtn.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), SettingsActivity.class));
        });
        return rootView;
    }

    private void setupAppList() {
        mySwipeRefreshLayout.setRefreshing(true);
        myExecutor.execute(loadData);
    }

    @Override
    public void onRefresh() {
        setupAppList();
    }

    @Override
    public void onItemSelected(AppDetails appDetails) {
        SetRestrictionFragment.newInstance(appDetails.getAppName(), appDetails.getPackageName(), appDetails.getThresholdTime())
                .setListener(this)
                .show(getChildFragmentManager(), "DURATION_PICKER");
    }

    @Override
    public void onDurationConfirmed(String packageName, long duration) {
        if (getActivity() == null) return;
        mySwipeRefreshLayout.setRefreshing(true);
        myExecutor.execute(new DbUtils.SaveRestrictionToDb(getActivity(), packageName, (int) duration, loadData));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (PreferencesHelper.getBoolPreference(getActivity(), GeneralSettingsFragment.PREF_ALLOW_APP_BLOCK.getKey(), true)) {
            myNoBlockView.setVisibility(View.GONE);
            myAppList.setVisibility(View.VISIBLE);
        } else {
            myAppList.setVisibility(View.GONE);
            myNoBlockView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCancel() {

    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        return false;
    }
}
