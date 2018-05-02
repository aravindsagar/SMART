package com.cs565project.smart.db

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.*
import com.cs565project.smart.db.entities.*
import java.util.*

/**
 * Data access object which provides queries, inserts, updates and deletes in the app database.
 */
@Dao
interface AppDao {

    @get:Query("SELECT MIN(date) FROM DailyAppUsage")
    val usageDataStartDate: Date

    @get:Query("SELECT * FROM AppDetails")
    val appDetails: List<AppDetails>

    @get:Query("SELECT packageName FROM AppDetails")
    val appPackageNames: List<String>

    @get:Query("SELECT * FROM AppDetails WHERE thresholdTime > -1")
    val restrictedAppDetails: List<AppDetails>

    @get:Query("SELECT * FROM MoodLog")
    val allMoodLog: List<MoodLog>

    @get:Query("SELECT * FROM RecommendationActivity")
    val recommendationActivities: List<RecommendationActivity>

    @get:Query("SELECT * FROM Category")
    val categories: List<Category>

    @Query("SELECT * FROM DailyAppUsage WHERE packageName = (:packageName) AND date = (:date) LIMIT 1")
    fun getAppUsage(packageName: String, date: Date): DailyAppUsage

    @Query("SELECT * FROM DailyAppUsage WHERE date = (:date)")
    fun getAppUsage(date: Date): List<DailyAppUsage>

    @Query("SELECT * FROM DailyAppUsage WHERE date = (:date)")
    fun getAppUsageLiveData(date: Date): LiveData<List<DailyAppUsage>>

    @Query("SELECT * FROM DailyAppUsage WHERE date >= (:startDate) AND date <= (:endDate)")
    fun getAppUsage(startDate: Date, endDate: Date): List<DailyAppUsage>

    @Query("SELECT * FROM DailyAppUsage WHERE packageName = (:packageName)")
    fun getAppUsage(packageName: String): List<DailyAppUsage>

    @Query("SELECT * FROM AppDetails WHERE packageName IN (:packageNames)")
    fun getAppDetails(packageNames: List<String>): List<AppDetails>

    @Query("SELECT * FROM AppDetails WHERE packageName = (:packageName) LIMIT 1")
    fun getAppDetails(packageName: String): AppDetails

    @Query("SELECT * FROM AppDetails WHERE packageName IN (:packageNames)")
    fun getAppDetailsLiveData(packageNames: List<String>): LiveData<List<AppDetails>>

    @Query("SELECT * FROM MoodLog WHERE date = (:date)")
    fun getMoodLog(date: Date): MoodLog

    @Query("SELECT * FROM MoodLog WHERE date >= (:startDate) AND date <= (:endDate)")
    fun getMoodLog(startDate: Date, endDate: Date): List<MoodLog>

    @Query("SELECT * FROM RecommendationActivity WHERE activityType = (:activityType)")
    fun getRecommendationActivities(activityType: String): List<RecommendationActivity>

    @Query("SELECT name FROM Category WHERE shouldRestrict = (:shouldRestrict)")
    fun getCategories(shouldRestrict: Boolean): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAppUsage(vararg appUsages: DailyAppUsage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAppUsage(appUsages: List<DailyAppUsage>)

    @Insert
    fun insertAppDetails(vararg apps: AppDetails)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAppDetails(apps: List<AppDetails>)

    @Insert
    fun insertMood(vararg mood: MoodLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRecommendationActivity(activity: RecommendationActivity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCategory(category: Category)

    @Update
    fun updateAppDetails(vararg apps: AppDetails)

    @Update
    fun updateAppUsage(vararg appUsages: DailyAppUsage)

    @Update
    fun updateRecommendationActivity(activity: RecommendationActivity)

    @Delete
    fun deleteAppUsage(vararg appUsages: DailyAppUsage)

    @Delete
    fun deleteAppDetails(vararg apps: AppDetails)

    @Delete
    fun deleteRecommendationActivities(activities: List<RecommendationActivity>)
}
