package com.cs565project.smart.db.entities;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.support.annotation.NonNull;

import java.util.Date;

/**
 * Database entity to hold app usages at a day-level granularity.
 */
@SuppressWarnings("unused")
@Entity(primaryKeys = {"packageName", "date"},
        foreignKeys = @ForeignKey(entity = AppDetails.class,
                parentColumns = "packageName",
                childColumns = "packageName"))
public class DailyAppUsage {
    @NonNull
    private String packageName;
    @NonNull
    private Date date;

    private long dailyUseTime;    // In milliseconds.
    private int dailyUseCount;

    public DailyAppUsage(@NonNull String packageName, @NonNull Date date, long dailyUseTime, int dailyUseCount) {
        this.packageName = packageName;
        this.date = date;
        this.dailyUseTime = dailyUseTime;
        this.dailyUseCount = dailyUseCount;
    }

    @NonNull
    public String getPackageName() {
        return packageName;
    }

    @NonNull
    public Date getDate() {
        return date;
    }

    public long getDailyUseTime() {
        return dailyUseTime;
    }

    public int getDailyUseCount() {
        return dailyUseCount;
    }
}
