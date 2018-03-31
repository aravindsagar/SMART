package com.cs565project.smart.db;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;

import java.sql.Time;

@Entity
public class UserApp {
    @PrimaryKey
    private int id;    // app id if exists

    @PrimaryKey
    private Time accessTime;    // App Access time if exists

    @ColumnInfo(name = "app_name")
    private String appName;

    @ColumnInfo(name = "usage_count")
    private int appUsageCount;

    // Temp
    @ColumnInfo(name = "factor")
    private int appFactor;

    @Ignore
    public UserApp(int aid) {
        id = aid;
        accessTime = null;
        appUsageCount = 0;
    }
}
