package com.cs565project.smart.db.entities;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;

import com.cs565project.smart.db.entities.AppDetails;

import java.util.Date;

@Entity(primaryKeys = {"packageName", "date"},
        foreignKeys = @ForeignKey(entity = AppDetails.class,
                parentColumns = "packageName",
                childColumns = "packageName"))
public class DailyAppUsage {
    public String packageName;
    public Date date;

    public long dailyUseTime;    // In milliseconds.
    public int dailyUseCount;

    public DailyAppUsage(String packageName, Date date, long dailyUseTime, int dailyUseCount) {
        this.packageName = packageName;
        this.date = date;
        this.dailyUseTime = dailyUseTime;
        this.dailyUseCount = dailyUseCount;
    }
}
