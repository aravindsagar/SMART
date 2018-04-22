package com.cs565project.smart.util;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.format.DateUtils;

import com.cs565project.smart.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static android.text.format.DateUtils.WEEK_IN_MILLIS;

/**
 * Utility class for fetching app usage stats.
 */
public class UsageStatsUtil {

    private final UsageStatsManager mUsageStatsManager;

    public UsageStatsUtil(Context c) {
        mUsageStatsManager = (UsageStatsManager) c.getSystemService(Context.USAGE_STATS_SERVICE);
    }

    public String getForegroundApp() {
        String foregroundApp = null;
        long time = System.currentTimeMillis();

        UsageEvents usageEvents = mUsageStatsManager.queryEvents(time - 1000 * 3600, time);
        UsageEvents.Event event = new UsageEvents.Event();
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event);
            if(event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                foregroundApp = event.getPackageName();
            }
        }

        return foregroundApp;
    }

    public List<ForegroundStats> getMostUsedAppsLastWeek() {
        long time = System.currentTimeMillis();
        return getMostUsedApps(time - WEEK_IN_MILLIS, time);
    }

    public List<ForegroundStats> getMostUsedAppsToday() {
        Date today = new Date();
        return getMostUsedApps(getStartOfDayMillis(today), today.getTime());
    }

    private List<ForegroundStats> getMostUsedApps(long startTime, long endTime) {
        UsageEvents usageEvents = mUsageStatsManager.queryEvents(startTime, endTime);
        UsageEvents.Event event = new UsageEvents.Event();
        Map<String, Long> usageMap = new HashMap<>();
        Map<String, Long> lastForegroundTransition = new HashMap<>();
        String foregroundApp = null;

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event);
            if(event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                foregroundApp = event.getPackageName();
                lastForegroundTransition.put(event.getPackageName(), event.getTimeStamp());
            } else if (event.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                long usageTime = event.getTimeStamp() -
                        (lastForegroundTransition.containsKey(event.getPackageName()) ?
                                lastForegroundTransition.get(event.getPackageName()) : startTime);
                if (usageMap.containsKey(event.getPackageName())) {
                    usageMap.put(event.getPackageName(), usageMap.get(event.getPackageName()) + usageTime);
                } else {
                    usageMap.put(event.getPackageName(), usageTime);
                }
            }
        }

        // Account for the app still in foreground.
        if (foregroundApp != null) {
            long usageTime = endTime -
                    (lastForegroundTransition.containsKey(foregroundApp) ?
                            lastForegroundTransition.get(foregroundApp) : startTime);
            if (usageMap.containsKey(foregroundApp)) {
                usageMap.put(foregroundApp, usageMap.get(foregroundApp) + usageTime);
            } else {
                usageMap.put(foregroundApp, usageTime);
            }
        }

        List<ForegroundStats> stats = new ArrayList<>(usageMap.size());
        List<String> sortedPackages = new ArrayList<>(usageMap.keySet());
        Collections.sort(sortedPackages, (a, b) -> Long.compare(usageMap.get(b), usageMap.get(a)));
        for (String packageName: sortedPackages) {
            stats.add(new ForegroundStats(packageName, usageMap.get(packageName)));
        }

        return stats;
        /*List<UsageStats> appList = new ArrayList<>(mUsageStatsManager.queryAndAggregateUsageStats(startTime, endTime).values());

        if (appList.size() > 0) {
            Collections.sort(appList, (b, a) -> Long.compare(a.getTotalTimeInForeground(), b.getTotalTimeInForeground()));
        }


        return appList;*/
    }

    public static long getStartOfDayMillis(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    public static long getTomorrowMillis() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 1);
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

    public static String formatDuration(long timeInMillis, Context context) {
        long totalTimeMins = timeInMillis / DateUtils.MINUTE_IN_MILLIS;
        if (totalTimeMins < 1) {
            return  context.getString(R.string.zero_min);
        } else if (totalTimeMins < 60) {
            return String.format(Locale.getDefault(), context.getString(R.string.duration_min), totalTimeMins);
        } else {
            return String.format(Locale.getDefault(), context.getString(R.string.duration_hour), totalTimeMins / 60, totalTimeMins % 60);
        }
    }

    public static class ForegroundStats {
        private String packageName;
        private long totalTimeInForeground;

        public ForegroundStats(String packageName, long totalTimeInForeground) {
            this.packageName = packageName;
            this.totalTimeInForeground = totalTimeInForeground;
        }

        public String getPackageName() {
            return packageName;
        }

        public long getTotalTimeInForeground() {
            return totalTimeInForeground;
        }
    }
}
