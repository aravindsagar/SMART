package com.cs565project.smart.fragments;


import android.app.usage.UsageStats;
import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.cs565project.smart.R;
import com.cs565project.smart.db.AppDao;
import com.cs565project.smart.db.AppDatabase;
import com.cs565project.smart.db.entities.AppDetails;
import com.cs565project.smart.db.entities.Category;
import com.cs565project.smart.util.AppInfo;
import com.cs565project.smart.util.PreferencesHelper;
import com.cs565project.smart.util.UsageStatsUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.cs565project.smart.util.DbUtils.KEY_APPS_UPDATED_IN_DB;

/**
 * Fragment to get user's restriction preferences during onboarding.
 */
public class OnboardingRestrictionsFragment extends Fragment {

    private ProgressBar loading;
    private GridView appGrid;

    private Set<AppDetails> selectedApps;

    private Handler myHandler = new Handler();

    private Thread loadData = new Thread() {
        @Override
        public void run() {
            Context c = getContext();
            if (c == null) return;

            while(!PreferencesHelper.getBoolPreference(c, KEY_APPS_UPDATED_IN_DB, false)) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // Populate our grid with most used apps from last week.
            List<UsageStats> mostUsedApps = new UsageStatsUtil(c).getMostUsedAppsLastWeek();
            int appListSize = Math.min(30, mostUsedApps.size());
            List<String> packagesToFetch = new ArrayList<>(appListSize);
            for (int i = 0; i < appListSize; i++) {
                packagesToFetch.add(mostUsedApps.get(i).getPackageName());
            }

            List<AppDetails> appDetails = AppDatabase.getAppDatabase(c).appDao().getAppDetails(packagesToFetch);
            List<AppInfo> appInfos = new ArrayList<>(appDetails.size());
            boolean[] selected = new boolean[appDetails.size()];
            for (int i = 0, appDetailsSize = appDetails.size(); i < appDetailsSize; i++) {
                AppDetails details = appDetails.get(i);
                appInfos.add(new AppInfo(details.getPackageName(), c));
                selected[i] = false;
            }
            selectedApps = new HashSet<>();

            myHandler.post(() -> {
                loading.setVisibility(View.GONE);
                appGrid.setVisibility(View.VISIBLE);
                appGrid.setAdapter(new AppsAdapter(appDetails, appInfos));
                appGrid.setOnItemClickListener((parent, view, position, id) -> {
                    if (view instanceof ImageView) {
                        ImageView imageView = (ImageView) view;
                        if (selected[position]) {
                            selectedApps.remove(appDetails.get(position));
                            selected[position] = false;
                            imageView.setImageTintList(null);
                        } else {
                            selectedApps.add(appDetails.get(position));
                            selected[position] = true;
                            imageView.setImageTintList(parent.getContext().getResources().getColorStateList(R.color.image_selected));
                            imageView.setImageTintMode(PorterDuff.Mode.ADD);
                        }
                    }
                });
            });
        }
    };

    public OnboardingRestrictionsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_onboarding_restrictions, container, false);
        loading = root.findViewById(R.id.loading_progress_bar);
        appGrid = root.findViewById(R.id.apps_grid);
        loading.setVisibility(View.VISIBLE);
        appGrid.setVisibility(View.GONE);
        loadData.start();
        return root;
    }

    public void saveData() {
        new Thread() {
            @Override
            public void run() {
                Context c = getActivity();
                if (c == null) return;

                AppDao dao = AppDatabase.getAppDatabase(c).appDao();
                Set<String> categories = new HashSet<>();
                for (AppDetails appDetails: selectedApps) {
                    categories.add(appDetails.getCategory());
                }

                for (String category: categories) {
                    Log.d("Inserting", category);
                    dao.insertCategory(new Category(category, true));
                }
            }
        }.start();
    }

    private static class AppsAdapter extends BaseAdapter {

        List<AppDetails> appDetails;
        List<AppInfo> appInfos;

        public AppsAdapter(List<AppDetails> appDetails, List<AppInfo> appInfos) {
            this.appDetails = appDetails;
            this.appInfos = appInfos;
        }

        @Override
        public int getCount() {
            return appDetails.size();
        }

        @Override
        public Object getItem(int position) {
            return appDetails.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null || !(convertView instanceof ImageView)) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.grid_item_app, parent, false);
            }
            ((ImageView) convertView).setImageDrawable(appInfos.get(position).getAppIcon());
            return convertView;
        }
    }
}
