package com.cs565project.smart.util;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static android.text.format.DateUtils.WEEK_IN_MILLIS;

public class UsageStatsUtil {

    private final UsageStatsManager mUsageStatsManager;

    public UsageStatsUtil(Context c) {
        mUsageStatsManager = (UsageStatsManager) c.getSystemService(Context.USAGE_STATS_SERVICE);
    }

    public String getForegroundApp() {
        long time = System.currentTimeMillis();
        List<UsageStats> appList = mUsageStatsManager
                .queryUsageStats(UsageStatsManager.INTERVAL_DAILY,  time - 1000*100, time);
        if (appList != null && appList.size() > 0) {
            SortedMap<Long, UsageStats> mySortedMap = new TreeMap<>();
            for (UsageStats usageStats : appList) {
                mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
            }
            if (!mySortedMap.isEmpty()) {
                 return mySortedMap.get(mySortedMap.lastKey()).getPackageName();
            }
        }
        return null;
    }

    public List<UsageStats> getMostUsedAppsLastWeek() {
        long time = System.currentTimeMillis();
        return getMostUsedApps(time - WEEK_IN_MILLIS, time);
    }

    public List<UsageStats> getMostUsedAppsToday() {

        return getMostUsedApps(getStartOfTodayMillis(), System.currentTimeMillis());
    }

    private List<UsageStats> getMostUsedApps(long startTime, long endTime) {
        List<UsageStats> appList = mUsageStatsManager
                .queryUsageStats(UsageStatsManager.INTERVAL_BEST, startTime, endTime);

        if (appList != null) {
            Collections.sort(appList, (a,b) -> Long.compare(a.getTotalTimeInForeground(), b.getTotalTimeInForeground()));
        } else {
            appList = new ArrayList<>();
        }

        return appList;
    }

    public static long getStartOfTodayMillis() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static boolean hasUsageAccess(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);
            AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            assert appOpsManager != null;
            int mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                    applicationInfo.uid, applicationInfo.packageName);
            return (mode == AppOpsManager.MODE_ALLOWED);

        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
