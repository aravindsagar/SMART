package com.cs565project.smart.db;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import com.cs565project.smart.db.entities.AppDetails;
import com.cs565project.smart.db.entities.DailyAppUsage;

import java.util.Date;
import java.util.List;

@Dao
public interface AppDao {
    @Query("SELECT * FROM DailyAppUsage WHERE packageName = (:packageName) AND date = (:date) LIMIT 1")
    DailyAppUsage getAppUsage(String packageName, Date date);

    @Query("SELECT * FROM DailyAppUsage WHERE date = (:date)")
    List<DailyAppUsage> getAppUsage(Date date);

    @Query("SELECT MIN(date) FROM DailyAppUsage")
    Date getUsageDataStartDate();

    @Query("SELECT * FROM AppDetails")
    List<AppDetails> getAppDetails();

    @Query("SELECT packageName FROM AppDetails")
    List<String> getAppPackageNames();

    @Query("SELECT * FROM AppDetails WHERE packageName IN (:packageNames)")
    List<AppDetails> getAppDetails(List<String> packageNames);

    @Query("SELECT * FROM AppDetails WHERE packageName = (:packageName) LIMIT 1")
    AppDetails getAppDetails(String packageName);

    @Query("SELECT * FROM AppDetails WHERE thresholdTime > -1")
    List<AppDetails> getRestrictedAppDetails();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAppUsage(DailyAppUsage... appUsages);
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAppUsage(List<DailyAppUsage> appUsages);
    @Insert
    void insertAppDetails(AppDetails... apps);
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAppDetails(List<AppDetails> apps);

    @Update
    void updateAppDetails(AppDetails... apps);
    @Update
    void updateAppUsage(DailyAppUsage... appUsages);

    @Delete
    void deleteAppUsage(DailyAppUsage... appUsages);
    @Delete
    void deleteAppDetails(AppDetails... apps);
}
