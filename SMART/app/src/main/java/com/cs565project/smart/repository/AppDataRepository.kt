package com.cs565project.smart.repository

import android.app.Application
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.graphics.drawable.Drawable
import android.util.Log
import com.cs565project.smart.R
import com.cs565project.smart.db.AppDao
import com.cs565project.smart.db.AppDatabase
import com.cs565project.smart.db.entities.AppDetails
import com.cs565project.smart.db.entities.DailyAppUsage
import com.cs565project.smart.fragments.adapter.ChartLegendAdapter
import com.cs565project.smart.repository.model.PieAndLegendData
import com.cs565project.smart.util.AppInfo
import com.cs565project.smart.util.EmotionUtil
import com.cs565project.smart.util.GraphUtil
import com.cs565project.smart.util.UsageStatsUtil
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.collections.HashSet

class AppDataRepository(private val mApplication: Application) {

    private val mDao: AppDao = AppDatabase.getAppDatabase(mApplication).appDao()
    private val mExecutor:Executor = Executors.newSingleThreadExecutor()
    private val mEmotionUtil = EmotionUtil(mApplication)

    fun getPieData(date: Date, category: String?): LiveData<PieAndLegendData> {

        // Fetch app usage data (as LiveData)
        val appUsage = mDao.getAppUsageLiveData(date)
        // App details to be fetched depends on the usage data, so we wait before loading this.
        var lastAppDetailsLiveData: LiveData<List<AppDetails>>? = null

        Log.d("SMART", "Returning MediatorLiveData")
        // Return a LiveData object which updates as we receive app usage and app details.
        return MediatorLiveData<PieAndLegendData>().apply {
            // Fields to store last received data from db.
            var lastAppUsage: List<DailyAppUsage>? = null
            var lastAppDetails: List<AppDetails>? = null

            // Update our value, to be called when we receive updates from db.
            fun update() {
                val localAppUsage = lastAppUsage
                val localAppDetails = lastAppDetails
                if (localAppUsage != null && localAppDetails != null) {
                    Log.d("SMART", "About to build pie data")
                    mExecutor.execute {
                        Log.d("SMART", "Building pie data")
                        this.postValue(buildPieData(localAppUsage, localAppDetails, category, date))
                    }
                }
            }

            // Add app usage LiveData as a source. When we receive usage data from db, we'll add
            // required app details as another source and update the value exposed by this LiveData.
            addSource(appUsage) { appUsages ->
                if (appUsages == null) return@addSource
                Log.d("SMART", "Got app usage data " + appUsages.size)
                lastAppUsage = appUsages
                lastAppDetailsLiveData?.let { it1 -> removeSource(it1) }
                lastAppDetailsLiveData = mDao.getAppDetailsLiveData(appUsages.map { a -> a.packageName })
                addSource(lastAppDetailsLiveData!!) { appDetails ->
                    Log.d("SMART", "Got app details " + appDetails?.size)
                    lastAppDetails = appDetails
                    update()
                }
            }
        }
    }

    private fun buildPieData(appUsages: List<DailyAppUsage>,
                             appDetailsList: List<AppDetails>,
                             currentCategory: String?,
                             date: Date): PieAndLegendData {

        // Remove unwanted entries.
        val appUsagesFiltered = appUsages.filter { mApplication.packageName != it.packageName }

        // Create a map of app details to make lookup easier.
        val appDetailsMap = appDetailsList.associate { Pair(it.packageName, it) }

        // Calculate Usage details aggregated by category. Also populate subtitle info.
        val allCategories = HashSet(appDetailsList.map { it.category })
        Log.d("Categories", allCategories.joinToString(", "))
        Log.d("SMART", "Got categories " + allCategories.size)
        val usageMap = allCategories.associate { Pair(it, 0L) }.toMutableMap()
        val subtitleInfo = allCategories.associate { Pair<String, MutableList<String>>(it, mutableListOf()) }
        appUsagesFiltered.forEach {
            Log.d(it.packageName, UsageStatsUtil.formatDuration(it.dailyUseTime, mApplication))
            val appDetails = appDetailsMap[it.packageName]
            val category = appDetails?.category ?: return@forEach
            usageMap[category] = usageMap.getOrElse(category, {0L}) + it.dailyUseTime
            subtitleInfo[category]!!.add(appDetails.appName)
        }

        // If required, calculate secondary pie data. This will be per-app usage map for apps in
        // current category.
        val secondaryUsageMap =
                if (currentCategory != null)
                    appUsagesFiltered
                            .filter { currentCategory == appDetailsMap[it.packageName]?.category }
                            .associate { Pair(it.packageName, it.dailyUseTime) }
                else mapOf()

        val usageMapForTotal = if (currentCategory != null) secondaryUsageMap else usageMap
        val totalUsageTime = if (usageMapForTotal.isEmpty()) 0 else usageMapForTotal.values
                .reduce { acc, l -> acc + l }

        // List of legendInfos, to be populated while processing usage map.
        val legendInfos = mutableListOf<ChartLegendAdapter.LegendInfo>()

        // Process usage map to get pie entries.
        val entries = processUsageMap(usageMap, subtitleInfo, currentCategory != null,
                if (currentCategory != null) null else legendInfos)
        val dataSet = PieDataSet(entries, "App usage")
        dataSet.setColors(*PIE_COLORS)
        dataSet.setDrawValues(false)
        val pieData = PieData(dataSet)
        var secondaryPieData: PieData? = null

        if (currentCategory != null) {
            val secondaryEntries = processUsageMap(secondaryUsageMap, subtitleInfo, true, legendInfos)
            val secondaryDataSet = PieDataSet(secondaryEntries, "App usage")
            secondaryDataSet.setColors(*PIE_COLORS)
            secondaryDataSet.setDrawValues(false)
            secondaryPieData = PieData(secondaryDataSet)
        }

        // Also load the mood.
        val latestMoodLog = mEmotionUtil.getLatestMoodLog(date)
        val mood = if (latestMoodLog != null) {
            mEmotionUtil.getEmoji(Math.round(latestMoodLog.happyValue * 4).toInt())
        } else {
            mApplication.getString(R.string.unknown)
        }

        return PieAndLegendData(pieData, secondaryPieData, legendInfos, mood, totalUsageTime)
    }

    private fun processUsageMap(
            usageMap: Map<String, Long>, subtitleInfo: Map<String, List<String>>,
            isSecondaryData: Boolean, legendInfos: MutableList<ChartLegendAdapter.LegendInfo>?)
            : List<PieEntry> {

        // Output list.
        val entries = ArrayList<PieEntry>()

        // Add to output in the descending order of keys in the usageMap.
        val keys = ArrayList(usageMap.keys)
        keys.sortWith(Comparator { b, a -> usageMap[a]!!.compareTo(usageMap[b]!!) })
        for ((i, key) in keys.withIndex()) {
            val usage = usageMap[key]!!
            var icon: Drawable? = null
            val title: String
            val subTitle: String

            if (isSecondaryData) {
                // In PER_CATEGORY state, usageMap is keyed using package names, but we want to
                // show app name as the title in chart. package name will be the subtitle.
                val appInfo = AppInfo(key, mApplication)
                title = if (appInfo.appName != null) appInfo.appName else "Unknown"
                subTitle = key
                icon = appInfo.appIcon
            } else {
                // In TOTAL state, the categories are the titles, and apps in them are the subtitles.
                title = key
                subTitle = GraphUtil.buildSubtitle(mApplication, subtitleInfo[key]!!)
            }

            // We want to limit the number of entries in the chart.
            if (i >= MAX_ENTRIES) {
                val lastEntry = entries[MAX_ENTRIES - 1]
                val entry = PieEntry(usage + lastEntry.value, mApplication.getString(R.string.others))
                entries[MAX_ENTRIES - 1] = entry

            } else {
                val entry = PieEntry(usage.toFloat(), title)
                entries.add(entry)
            }

            legendInfos?.add(ChartLegendAdapter.LegendInfo(title, subTitle, icon,
                    usage, PIE_COLORS[Math.min(i, MAX_ENTRIES - 1)]))
        }

        return entries
    }

    companion object {
        val PIE_COLORS =
                listOf(
                        "#bf360c", "#006064", "#5d4037", "#827717", "#f57f17", "#37474f", "#4a148c",
                        "#ad1457", "#006064", "#0d47a1", "#fdd835", "#ff1744", "#000000"
                ).map { ColorTemplate.rgb(it) }.toIntArray()
        private val MAX_ENTRIES = PIE_COLORS.size
    }
}

