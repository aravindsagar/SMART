package com.cs565project.smart.fragments;


import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cs565project.smart.R;
import com.cs565project.smart.db.AppDao;
import com.cs565project.smart.db.AppDatabase;
import com.cs565project.smart.db.entities.AppDetails;
import com.cs565project.smart.fragments.adapter.RestrictionsAdapter;
import com.cs565project.smart.util.AppInfo;
import com.cs565project.smart.util.DbUtils;
import com.cs565project.smart.recommender.RestrictionRecommender;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


/**
 * A simple {@link Fragment} subclass.
 */
public class RestrictionsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener,
        RestrictionsAdapter.OnItemSelectedListener, SetRestrictionFragment.OnDurationSelectedListener {

    // Views that we care about.
    private RecyclerView myAppList;
    private SwipeRefreshLayout mySwipeRefreshLayout;

    // Our state.
    List<AppDetails> restrictedApps, recommendedApps, otherApps;
    Map<String, AppInfo> appInfo;

    // Helper fields.
    private Handler myHandler = new Handler();
    private Executor myExecutor = Executors.newSingleThreadExecutor();

    // Runnable to load data in background.
    private Runnable loadData = new Runnable() {
        @Override
        public void run() {
            if (getActivity() == null) return;

            AppDao dao = AppDatabase.getAppDatabase(getActivity()).appDao();

            restrictedApps = new ArrayList<>();
            recommendedApps = new ArrayList<>();
            otherApps = new ArrayList<>();
            appInfo = new HashMap<>();

            for (AppDetails appDetails : dao.getAppDetails()) {
                appInfo.put(appDetails.getPackageName(), new AppInfo(appDetails.getPackageName(), getActivity()));
                if (appDetails.getThresholdTime() >= 0) {
                    restrictedApps.add(appDetails);
                } else {
                    int recommendation = RestrictionRecommender.recommendRestriction(
                            appDetails, dao.getAppUsage(appDetails.getPackageName()));
                    if (recommendation >= 0) {
                        recommendedApps.add(new AppDetails(
                                appDetails.getPackageName(),
                                appDetails.getAppName(),
                                appDetails.getCategory(),
                                recommendation
                        ));
                    } else {
                        otherApps.add(appDetails);
                    }
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

            myAppList.setAdapter(new RestrictionsAdapter(restrictedApps, recommendedApps, otherApps,
                    appInfo, RestrictionsFragment.this));
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
        myAppList = rootView.findViewById(R.id.list_restriction_apps);
        myAppList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mySwipeRefreshLayout = rootView.findViewById(R.id.refresh_restrictions);
        mySwipeRefreshLayout.setOnRefreshListener(this);
        setupAppList();
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
        mySwipeRefreshLayout.setRefreshing(true);
        myExecutor.execute(new DbUtils.SaveRestrictionToDb(getActivity(), packageName, (int) duration, loadData));
    }

    @Override
    public void onCancel() {

    }
}
