package com.cs565project.smart.db;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;
import android.content.Context;

import com.cs565project.smart.db.entities.AppDetails;
import com.cs565project.smart.db.entities.DailyAppUsage;
import com.cs565project.smart.db.entities.MoodLog;

@Database(entities = {DailyAppUsage.class, AppDetails.class, MoodLog.class}, version = 1)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase{
    private static AppDatabase ourInstance;

    public static AppDatabase getAppDatabase(Context context) {
        if (ourInstance == null) {
            ourInstance = Room.databaseBuilder(context, AppDatabase.class, "appDB").build();
        }
        return ourInstance;
    }

    public abstract AppDao appDao();

}
