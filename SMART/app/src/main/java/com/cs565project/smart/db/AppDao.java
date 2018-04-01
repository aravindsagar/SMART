package com.cs565project.smart.db;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import com.cs565project.smart.db.entities.DailyAppUsage;

import java.sql.Time;
import java.util.Date;
import java.util.List;

@Dao
public interface AppDao {
    @Query("SELECT * FROM DailyAppUsage WHERE packageName = (:packageName) AND date = (:date)")
    List<DailyAppUsage> getAppUsage(String packageName, Date date);

    @Insert
    void insertAppUsage(DailyAppUsage... app);

    @Delete
    void deleteAppUsage(DailyAppUsage... app);
}
