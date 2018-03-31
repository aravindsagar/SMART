package com.cs565project.smart.service;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.util.Log;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;

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

    public SortedMap<Long, UsageStats> getMostUsedAppsLastWeek() {
        long time = System.currentTimeMillis();
        return getMostUsedApps(time - 7*24*60*60*3600, time);
    }

    public SortedMap<Long, UsageStats> getMostUsedAppsToday() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return getMostUsedApps(cal.getTimeInMillis(), System.currentTimeMillis());
    }

    private SortedMap<Long, UsageStats> getMostUsedApps(long startTime, long endTime) {
        List<UsageStats> appList = mUsageStatsManager
                .queryUsageStats(UsageStatsManager.INTERVAL_DAILY,  startTime, endTime);
        SortedMap<Long, UsageStats> sortedMap = new TreeMap<>();
        if (appList != null) {
            for (UsageStats s : appList) {
                sortedMap.put(s.getTotalTimeInForeground(), s);
            }
        }
        return sortedMap;
    }

}
