package com.cs565project.smart.util

import android.content.Context
import android.os.Environment
import android.os.Handler
import android.text.format.DateUtils
import android.util.Log
import android.widget.Toast
import com.cs565project.smart.R
import com.cs565project.smart.db.AppDatabase
import com.cs565project.smart.db.entities.MoodLog
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.*

/**
 * Some utility methods for emotion capture and storage.
 */
class EmotionUtil(private val myContext: Context) {
    private val myUIHandler = Handler()

    fun processPicture(data: ByteArray) {
        val file = File(myContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "picture.jpg")
        var os: OutputStream? = null
        try {
            os = FileOutputStream(file)
            os.write(data)
            os.close()
        } catch (e: IOException) {
            Log.w("EmotionUtil", "Cannot write to $file", e)
        } finally {
            if (os != null) {
                try {
                    os.close()
                } catch (e: IOException) {
                    // Ignore
                }

            }
        }

        // callback thread runs rest of picture data management?
        try {
            manageData(data)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }

    @Throws(JSONException::class)
    private fun manageData(data: ByteArray) {

        // Steps to connect to image analysis API's. Currently not connected/merged
        // facial feature characteristics scores are received in JSONArray
        val recvScores = JSONArray("[{'scores':[" + Math.random() + ", 0, 0, 0]}]")

        val featureScores = ArrayList<Double>()

        // Should only be 1 object (Face)
        val rootJSON = recvScores.get(0) as JSONObject
        val scores = rootJSON.get("scores") as JSONArray

        for (i in 0 until scores.length()) {
            featureScores.add(scores.getDouble(i))
        }

        insertMoodLog(featureScores)
    }

    fun insertMoodLog(moodData: MutableList<Double>) {
        val dao = AppDatabase.getAppDatabase(myContext.applicationContext).appDao()
        val now = Date()
        val existingLogs = getAllMoodLogs(now)

        if (!existingLogs.isEmpty()) {
            val latestLog = Collections.max(existingLogs
            ) { a, b -> java.lang.Long.compare(a.date.time, b.date.time) }
            val existingWeight = getAllMoodLogs(now).size.toLong()
            for (i in moodData.indices) {
                moodData[i] = (latestLog.getValByIndex(i) * existingWeight + moodData[i]) / (existingWeight + 1)
            }
        }
        val ml = MoodLog(now, moodData)
        dao.insertMood(ml)

        myUIHandler.post { Toast.makeText(myContext, "Mood saved", Toast.LENGTH_SHORT).show() }
    }

    fun getLatestMoodLog(date: Date): MoodLog? {
        val existingLogs = getAllMoodLogs(date)
        return if (existingLogs.isEmpty()) {
            null
        } else Collections.max(existingLogs) { a, b -> java.lang.Long.compare(a.date.time, b.date.time) }
    }

    private fun getAllMoodLogs(date: Date): List<MoodLog> {
        val dao = AppDatabase.getAppDatabase(myContext.applicationContext).appDao()
        val startOfDay = UsageStatsUtil.getStartOfDayMillis(date)
        return dao.getMoodLog(Date(startOfDay), Date(startOfDay + DateUtils.DAY_IN_MILLIS - 1))
    }

    fun getEmoji(value: Int): String {
        return when (value) {
            0 -> myContext.getString(R.string.emotion_level_1)
            1 -> myContext.getString(R.string.emotion_level_2)
            2 -> myContext.getString(R.string.emotion_level_3)
            3 -> myContext.getString(R.string.emotion_level_4)
            else -> myContext.getString(R.string.emotion_level_5)
        }
    }
}
