package com.cs565project.smart.util

import android.annotation.TargetApi
import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateUtils
import android.text.format.DateUtils.WEEK_IN_MILLIS
import com.cs565project.smart.R
import java.util.ArrayList
import java.util.Calendar
import java.util.Date
import java.util.HashMap
import java.util.Locale
import kotlin.Comparator

/**
 * Utility class for fetching app usage stats.
 */
class UsageStatsUtil(c: Context) {

    private val mUsageStatsManager: UsageStatsManager

    val foregroundApp: String?
        get() {
            var foregroundApp: String? = null
            val time = System.currentTimeMillis()

            val usageEvents = mUsageStatsManager.queryEvents(time - 1000 * 3600, time)
            val event = UsageEvents.Event()
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    foregroundApp = event.packageName
                }
            }

            return foregroundApp
        }

    val mostUsedAppsLastWeek: List<ForegroundStats>
        get() {
            val time = System.currentTimeMillis()
            return getMostUsedApps(time - WEEK_IN_MILLIS, time)
        }

    val mostUsedAppsToday: List<ForegroundStats>
        get() {
            val today = Date()
            return getMostUsedApps(getStartOfDayMillis(today), today.time)
        }

    init {
        mUsageStatsManager = c.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }

    private fun getMostUsedApps(startTime: Long, endTime: Long): List<ForegroundStats> {
        val usageEvents = mUsageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        val usageMap = HashMap<String, Long>()
        val lastForegroundTransition = HashMap<String, Long>()
        var foregroundApp: String? = null

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                foregroundApp = event.packageName
                lastForegroundTransition[event.packageName] = event.timeStamp
            } else if (event.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                val usageTime = event.timeStamp - lastForegroundTransition.getOrElse(event.packageName, { startTime })
                usageMap[event.packageName] = usageMap.getOrElse(event.packageName, {0L}) + usageTime
            }
        }

        // Account for the app still in foreground.
        if (foregroundApp != null) {
            val usageTime = endTime - lastForegroundTransition.getOrElse(foregroundApp, { startTime })
            usageMap[event.packageName] = usageMap.getOrElse(foregroundApp, {0L}) + usageTime
        }

        val stats = ArrayList<ForegroundStats>(usageMap.size)
        val sortedPackages = ArrayList(usageMap.keys)
        sortedPackages.sortWith(Comparator { a, b -> usageMap[b]!!.compareTo(usageMap[a]!!) })
        for (packageName in sortedPackages) {
            stats.add(ForegroundStats(packageName, usageMap[packageName]!!))
        }

        return stats
        /*List<UsageStats> appList = new ArrayList<>(mUsageStatsManager.queryAndAggregateUsageStats(startTime, endTime).values());

        if (appList.size() > 0) {
            Collections.sort(appList, (b, a) -> Long.compare(a.getTotalTimeInForeground(), b.getTotalTimeInForeground()));
        }


        return appList;*/
    }

    class ForegroundStats(val packageName: String, val totalTimeInForeground: Long)

    companion object {

        fun getStartOfDayMillis(date: Date): Long {
            val cal = Calendar.getInstance()
            cal.time = date
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        val tomorrowMillis: Long
            get() {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                return cal.timeInMillis
            }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        fun hasUsageAccess(context: Context): Boolean {
            try {
                val packageManager = context.packageManager
                val applicationInfo = packageManager.getApplicationInfo(context.packageName, 0)
                val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                val mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                        applicationInfo.uid, applicationInfo.packageName)
                return mode == AppOpsManager.MODE_ALLOWED

            } catch (e: PackageManager.NameNotFoundException) {
                return false
            }

        }

        fun formatDuration(timeInMillis: Long, context: Context): String {
            val totalTimeMins = timeInMillis / DateUtils.MINUTE_IN_MILLIS
            return when {
                totalTimeMins < 1 -> context.getString(R.string.zero_min)
                totalTimeMins < 60 -> String.format(Locale.getDefault(), context.getString(R.string.duration_min), totalTimeMins)
                else -> String.format(Locale.getDefault(), context.getString(R.string.duration_hour), totalTimeMins / 60, totalTimeMins % 60)
            }
        }
    }
}
