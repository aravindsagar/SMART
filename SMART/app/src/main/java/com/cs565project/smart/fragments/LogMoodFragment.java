package com.cs565project.smart.fragments;


import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v13.app.FragmentCompat;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cs565project.smart.R;
import com.cs565project.smart.db.AppDao;
import com.cs565project.smart.db.AppDatabase;
import com.cs565project.smart.db.entities.AppDetails;
import com.cs565project.smart.db.entities.MoodLog;
import com.cs565project.smart.service.AppMonitorService;
import com.google.android.cameraview.CameraView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class LogMoodFragment extends Fragment {

    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final String TAG = "CameraView";
    private AppDao dao = AppDatabase.getAppDatabase(getActivity().getApplicationContext()).appDao();

    private static final int[] FLASH_OPTIONS = {
            CameraView.FLASH_AUTO,
            CameraView.FLASH_OFF,
            CameraView.FLASH_ON
    };

    private Handler     myBackgroundHandler;
    private CameraView  myCameraView;

    public LogMoodFragment() {
        // Required empty public constructor
    }

    private View.OnClickListener myOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(v.getId() == R.id.take_pic) {
                if(myCameraView != null) {
                    myCameraView.takePicture();

                    // Rest taken care of after receiving callback
                }
            }
            else {
                Log.d(TAG, "myCamera has not been initialized");
            }
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_log_mood, container, false);
        myCameraView = root.findViewById(R.id.camera_view);
        myCameraView.setFacing(CameraView.FACING_BACK);     // FIXME if incorrect
        myCameraView.addCallback(onCallback);
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        FragmentCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        myCameraView.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        myCameraView.stop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (myBackgroundHandler != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                myBackgroundHandler.getLooper().quitSafely();
            } else {
                myBackgroundHandler.getLooper().quit();
            }
            myBackgroundHandler = null;
        }
    }

    private Handler getBackgroundHandler() {
        if (myBackgroundHandler == null) {
            HandlerThread thread = new HandlerThread("background");
            thread.start();
            myBackgroundHandler = new Handler(thread.getLooper());
        }
        return myBackgroundHandler;
    }

    private void manageData(byte[] data) throws JSONException {
        byte[] myPic = data;       // TODO: need to grab the picture taken

        // Steps to connect to image analysis API's. Currently not connected/merged
        // facial feature characteristics scores are received in JSONArray
        JSONArray recvScores = new JSONArray();

        ArrayList<Double> featureScores = new ArrayList<Double>();

        // Should only be 1 object (Face)
        JSONObject rootJSON = (JSONObject) recvScores.get(0);
        JSONArray scores = (JSONArray) rootJSON.get("scores");

        for (int i = 0; i < scores.length(); i++) {
            featureScores.add(scores.getDouble(i));
        }

        MoodLog ml = new MoodLog(new Date(), featureScores);
        dao.insertMood(ml);
    }

    // FIXME
    private CameraView.Callback onCallback = new CameraView.Callback() {

        @Override
        public void onPictureTaken(CameraView cameraView, final byte[] data) {

            Log.d(TAG, "onPictureTaken " + data.length);

            getBackgroundHandler().post(new Runnable() {
                @Override
                public void run() {
                    File file = new File(getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                            "picture.jpg");
                    OutputStream os = null;
                    try {
                        os = new FileOutputStream(file);
                        os.write(data);
                        os.close();
                    } catch (IOException e) {
                        Log.w(TAG, "Cannot write to " + file, e);
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
            });

            // Main thread idle now
        }
    };
}
