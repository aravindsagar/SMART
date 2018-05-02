package com.cs565project.smart.db

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import android.content.Context
import com.cs565project.smart.db.entities.*
import com.cs565project.smart.recommender.ActivityRecommender.ACADEMIC_ACTIVITIES
import com.cs565project.smart.recommender.ActivityRecommender.EXERCISE_ACTIVITIES
import com.cs565project.smart.recommender.ActivityRecommender.KEY_DB_POPULATED
import com.cs565project.smart.recommender.ActivityRecommender.NEWS_TOPICS
import com.cs565project.smart.recommender.ActivityRecommender.RELAX_ACTIVITIES
import com.cs565project.smart.util.PreferencesHelper

@Database(entities = arrayOf(DailyAppUsage::class, AppDetails::class, MoodLog::class,
                             RecommendationActivity::class, Category::class),
          version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appDao(): AppDao

    fun insertDefaultActivitiesIntoDb() {
        val dao = appDao()
        for (activity in EXERCISE_ACTIVITIES) {
            dao.insertRecommendationActivity(activity)
        }
        for (activity in RELAX_ACTIVITIES) {
            dao.insertRecommendationActivity(activity)
        }
        for (activity in ACADEMIC_ACTIVITIES) {
            dao.insertRecommendationActivity(activity)
        }
        for (activity in NEWS_TOPICS) {
            dao.insertRecommendationActivity(activity)
        }
    }

    companion object {
        private var ourInstance: AppDatabase? = null

        fun getAppDatabase(context: Context): AppDatabase {
            if (ourInstance == null) {
                // Callback to insert default values when db is created.
                val myDbCallback = object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        object : Thread() {
                            override fun run() {
                                super.run()
                                getAppDatabase(context).insertDefaultActivitiesIntoDb()
                                PreferencesHelper.setPreference(context, KEY_DB_POPULATED, true)
                            }
                        }.start()
                    }
                }

                ourInstance = Room.databaseBuilder(context, AppDatabase::class.java, "appDB")
                        .addCallback(myDbCallback)
                        .build()
            }
            return ourInstance as AppDatabase
        }
    }
}
