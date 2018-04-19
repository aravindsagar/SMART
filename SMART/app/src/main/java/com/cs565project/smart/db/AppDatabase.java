package com.cs565project.smart.db;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;
import android.arch.persistence.room.migration.Migration;
import android.content.Context;
import android.support.annotation.NonNull;

import com.cs565project.smart.db.entities.AppDetails;
import com.cs565project.smart.db.entities.DailyAppUsage;
import com.cs565project.smart.db.entities.MoodLog;
import com.cs565project.smart.db.entities.RecommendationActivity;
import com.cs565project.smart.util.PreferencesHelper;

import static com.cs565project.smart.recommender.ActivityRecommender.ACADEMIC_ACTIVITIES;
import static com.cs565project.smart.recommender.ActivityRecommender.EXERCISE_ACTIVITIES;
import static com.cs565project.smart.recommender.ActivityRecommender.KEY_DB_POPULATED;
import static com.cs565project.smart.recommender.ActivityRecommender.NEWS_TOPICS;
import static com.cs565project.smart.recommender.ActivityRecommender.RELAX_ACTIVITIES;

@Database(entities = {DailyAppUsage.class, AppDetails.class, MoodLog.class, RecommendationActivity.class}, version = 2)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase{
    private static AppDatabase ourInstance;

    public static AppDatabase getAppDatabase(Context context) {
        if (ourInstance == null) {
            // Callback to insert default values when db is created.
            Callback myDbCallback = new Callback() {
                @Override
                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                    super.onCreate(db);
                    new Thread() {
                        @Override
                        public void run() {
                            super.run();
                            getAppDatabase(context).insertDefaultActivitiesIntoDb();
                            PreferencesHelper.setPreference(context, KEY_DB_POPULATED, true);
                        }
                    }.start();
                }
            };

            ourInstance = Room.databaseBuilder(context, AppDatabase.class, "appDB")
                    .addCallback(myDbCallback)
                    .addMigrations(MIGRATION_1_2)
                    .build();
        }
        return ourInstance;
    }

    public abstract AppDao appDao();

    public void insertDefaultActivitiesIntoDb() {
        AppDao dao = appDao();
        for (RecommendationActivity activity : EXERCISE_ACTIVITIES) {
            dao.insertRecommendationActivity(activity);
        }
        for (RecommendationActivity activity : RELAX_ACTIVITIES) {
            dao.insertRecommendationActivity(activity);
        }
        for (RecommendationActivity activity : ACADEMIC_ACTIVITIES) {
            dao.insertRecommendationActivity(activity);
        }
        for (RecommendationActivity activity : NEWS_TOPICS) {
            dao.insertRecommendationActivity(activity);
        }
    }

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("DROP TABLE IF EXISTS RecommendationActivity");
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS RecommendationActivity " +
                            "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "activityName TEXT, " +
                            "isSet INTEGER NOT NULL, " +
                            "timeOfDay INTEGER NOT NULL, " +
                            "activityType TEXT)"
            );
        }
    };
}
