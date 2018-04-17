package com.cs565project.smart.util;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

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
import java.util.Date;
import java.util.List;

public class CameraUtil {
    private Handler myUIHandler = new Handler();

    public void processPicture(Context context, byte[] data) {
        File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "picture.jpg");
        OutputStream os = null;
        try {
            os = new FileOutputStream(file);
            os.write(data);
            os.close();
        } catch (IOException e) {
            Log.w("CameraUtil", "Cannot write to " + file, e);
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
            manageData(context, data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void manageData(Context context, byte[] data) throws JSONException {
        byte[] myPic = data;       // TODO: need to grab the picture taken

        // Steps to connect to image analysis API's. Currently not connected/merged
        // facial feature characteristics scores are received in JSONArray
        JSONArray recvScores = new JSONArray("[{'scores':[" + Math.random() + ", 0, 0, 0, 0]}]");

        ArrayList<Double> featureScores = new ArrayList<Double>();

        // Should only be 1 object (Face)
        JSONObject rootJSON = (JSONObject) recvScores.get(0);
        JSONArray scores = (JSONArray) rootJSON.get("scores");

        for (int i = 0; i < scores.length(); i++) {
            featureScores.add(scores.getDouble(i));
        }

        insertMoodLog(context, featureScores);
    }

    public void insertMoodLog(Context context, List<Double> moodData) {
        MoodLog ml = new MoodLog(new Date(), moodData);
        AppDao dao = AppDatabase.getAppDatabase(context.getApplicationContext()).appDao();
        dao.insertMood(ml);

        myUIHandler.post(() -> Toast.makeText(context, "Mood saved", Toast.LENGTH_SHORT).show());
    }
}
