package com.cs565project.smart.util;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.Toast;

import com.cs565project.smart.R;
import com.cs565project.smart.db.AppDao;
import com.cs565project.smart.db.AppDatabase;
import com.cs565project.smart.db.entities.MoodLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Some utility methods for emotion capture and storage.
 */
public class EmotionUtil {
    private Handler myUIHandler = new Handler();
    private Context myContext;

    public EmotionUtil(Context context) {
        this.myContext = context;
    }

    public void processPicture(byte[] data) {
        File file = new File(myContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "picture.jpg");
        OutputStream os = null;
        try {
            os = new FileOutputStream(file);
            os.write(data);
            os.close();
        } catch (IOException e) {
            Log.w("EmotionUtil", "Cannot write to " + file, e);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }

        // callback thread runs rest of picture data management?
        try {
            manageData(data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void manageData(byte[] data) throws JSONException {
        byte[] myPic = data;

        // Steps to connect to image analysis API's. Currently not connected/merged
        // facial feature characteristics scores are received in JSONArray
        JSONArray recvScores = new JSONArray("[{'scores':[" + Math.random() + ", 0, 0, 0]}]");

        ArrayList<Double> featureScores = new ArrayList<Double>();

        // Should only be 1 object (Face)
        JSONObject rootJSON = (JSONObject) recvScores.get(0);
        JSONArray scores = (JSONArray) rootJSON.get("scores");

        for (int i = 0; i < scores.length(); i++) {
            featureScores.add(scores.getDouble(i));
        }

        insertMoodLog(featureScores);
    }

    public void insertMoodLog(List<Double> moodData) {
        AppDao dao = AppDatabase.getAppDatabase(myContext.getApplicationContext()).appDao();
        Date now = new Date();
        long startOfDay = UsageStatsUtil.getStartOfDayMillis(now);
        MoodLog existingLog = getLatestMoodLog(now);

        if (existingLog != null) {
            long logTimeDelta = now.getTime() - existingLog.dateTime.getTime();
            long existingWeight = existingLog.dateTime.getTime() - startOfDay;
            for (int i = 0; i < moodData.size(); i++) {
                moodData.set(i, (existingLog.getValByIndex(i) * existingWeight + moodData.get(i) * logTimeDelta) /
                        (existingWeight + logTimeDelta));
            }
        }
        MoodLog ml = new MoodLog(now, moodData);
        dao.insertMood(ml);

        myUIHandler.post(() -> Toast.makeText(myContext, "Mood saved", Toast.LENGTH_SHORT).show());
    }

    public MoodLog getLatestMoodLog(Date date) {
        AppDao dao = AppDatabase.getAppDatabase(myContext.getApplicationContext()).appDao();
        long startOfDay = UsageStatsUtil.getStartOfDayMillis(date);
        List<MoodLog> existingLogs = dao.getMoodLog(new Date(startOfDay), new Date(startOfDay + DateUtils.DAY_IN_MILLIS - 1));
        if (existingLogs.isEmpty()) { return null; }
        return Collections.max(existingLogs, (a, b) -> Long.compare(a.dateTime.getTime(), b.dateTime.getTime()));
    }

    public String getEmoji(int value) {
        switch (value) {
            case 0: return myContext.getString(R.string.emotion_level_1);
            case 1: return myContext.getString(R.string.emotion_level_2);
            case 2: return myContext.getString(R.string.emotion_level_3);
            case 3: return myContext.getString(R.string.emotion_level_4);
            default: return myContext.getString(R.string.emotion_level_5);

        }
    }
}
