package com.cs565project.smart.util

import android.content.Context
import android.util.Log
import android.util.Pair
import com.cs565project.smart.db.AppDatabase
import com.cs565project.smart.db.entities.AppDetails
import com.cs565project.smart.db.entities.DailyAppUsage
import java.io.IOException
import java.util.*

/**
 * Some utility database methods.
 */
object DbUtils {
    const val KEY_APPS_UPDATED_IN_DB = "apps_updated_in_db"

    private fun populateAppDetailsInDb(context: Context) {
        val dao = AppDatabase.getAppDatabase(context).appDao()
        val dbApps = HashSet(dao.appPackageNames)
        /*for (String a : dbApps) {
            Log.d("Existing Db App", a);
        }*/
        val installedApps = AppInfo.getAllApps(context.packageManager)

        // Populate db with info of installed apps that are missing from db.
        val toAdd = ArrayList<AppDetails>()
        for (a in installedApps) {
            if (dbApps.contains(a.packageName)) continue

            val category = try {
                PlayStoreUtil.getPlayStoreCategory(a.packageName)
            } catch (e: IOException) {
                e.printStackTrace()
                AppInfo.NO_CATEGORY
            }

            val appDetails = AppDetails(a.packageName, a.appName!!, category, -1)
            toAdd.add(appDetails)
            dbApps.add(a.packageName)

            Log.d("New db app", a.packageName)
        }
        dao.insertAppDetails(toAdd)
        if (toAdd.size > 0) {
            Log.d("AppInfoUpdate", String.format("Updated category information of %d apps.", toAdd.size))
        }
        PreferencesHelper.setPreference(context, KEY_APPS_UPDATED_IN_DB, true)
    }

    fun updateAndGetRestrictedAppsStatus(context: Context): List<Pair<AppDetails, UsageStatsUtil.ForegroundStats>> {
        val dao = AppDatabase.getAppDatabase(context).appDao()

        // Construct a restricted apps map.
        val restrictedAppsList = dao.restrictedAppDetails
        val restrictedApps = HashMap<String, AppDetails>(restrictedAppsList.size)
        for (detail in restrictedAppsList) {
            restrictedApps[detail.packageName] = detail
        }

        // Compute the results.
        val results = ArrayList<Pair<AppDetails, UsageStatsUtil.ForegroundStats>>()
        val todayStats = UsageStatsUtil(context).mostUsedAppsToday
        populateAppDetailsInDb(context) // Make sure all apps are in our db.
        // Only collect stats for apps in our db.
        val dbApps = HashSet(dao.appPackageNames)
        val toInsert = ArrayList<DailyAppUsage>(todayStats.size)
        for (usageStats in todayStats) {
            //             Log.d("Usage stats received", usageStats.getPackageName() + ", " + usageStats.getTotalTimeInForeground()/1000);
            if (!dbApps.contains(usageStats.packageName)) continue
            toInsert.add(DailyAppUsage(
                    usageStats.packageName,
                    Date(UsageStatsUtil.getStartOfDayMillis(Date())),
                    usageStats.totalTimeInForeground,
                    0
            ))
            Log.d("Inserting", "${usageStats.packageName} - ${UsageStatsUtil.formatDuration(usageStats.totalTimeInForeground, context)}")
            if (restrictedApps.containsKey(usageStats.packageName)) {
                results.add(Pair<AppDetails, UsageStatsUtil.ForegroundStats>(restrictedApps[usageStats.packageName], usageStats))
            }
        }

        dao.insertAppUsage(toInsert)
        return results
    }

    class SaveRestrictionToDb(internal var context: Context, internal var packageName: String, internal var duration: Int, internal var postSave: Runnable) : Runnable {

        override fun run() {
            if (duration == 0) duration = -1

            val dao = AppDatabase.getAppDatabase(context).appDao()
            val appDetails = dao.getAppDetails(packageName)
            dao.updateAppDetails(AppDetails(
                    appDetails.packageName,
                    appDetails.appName,
                    appDetails.category,
                    duration))
            postSave.run()
        }
    }
}
