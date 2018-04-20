package com.cs565project.smart.util;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.format.DateUtils;
import android.util.Log;

import com.cs565project.smart.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static android.text.format.DateUtils.HOUR_IN_MILLIS;
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
        long time = System.currentTimeMillis();
        Collection<UsageStats> appList = mUsageStatsManager.queryAndAggregateUsageStats(time - HOUR_IN_MILLIS, time)
                .values();
        if (appList.size() > 0) {
            return Collections.max(appList, (a, b) -> Long.compare(a.getLastTimeUsed(), b.getLastTimeUsed())).getPackageName();
        }

        Log.d("Usage stats", "Usage stats manager returned nothing");
        return null;
    }

    public List<UsageStats> getMostUsedAppsLastWeek() {
        long time = System.currentTimeMillis();
        return getMostUsedApps(time - WEEK_IN_MILLIS, time);
    }

    public List<UsageStats> getMostUsedAppsToday() {
        Date today = new Date();
        return getMostUsedApps(getStartOfDayMillis(today), today.getTime());
    }

    private List<UsageStats> getMostUsedApps(long startTime, long endTime) {
        List<UsageStats> appList = new ArrayList<>(mUsageStatsManager.queryAndAggregateUsageStats(startTime, endTime).values());

        if (appList.size() > 0) {
            Collections.sort(appList, (b, a) -> Long.compare(a.getTotalTimeInForeground(), b.getTotalTimeInForeground()));
        }

        return appList;
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
}

// On Resume