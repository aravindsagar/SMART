package com.cs565project.smart.fragments;


import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import com.cs565project.smart.MainActivity;
import com.cs565project.smart.R;
import com.cs565project.smart.util.EmotionUtil;
import com.google.android.cameraview.CameraView;

import java.util.Arrays;

/**
 * A simple {@link Fragment} subclass.
 */
public class LogMoodFragment extends Fragment implements View.OnKeyListener, RadioGroup.OnCheckedChangeListener {

//    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final String TAG = "CameraView";


    private Handler              myBackgroundHandler;
    private CameraView           myCameraView;
    private RadioGroup           myMoodRadios;
    private FloatingActionButton myTakePicBtn;

    private EmotionUtil myEmotionUtil;

    public LogMoodFragment() {
        // Required empty public constructor
    }

    private View.OnClickListener myOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.take_pic) {
                // Record mood according to user's selection.
                if (myCameraView.getVisibility() == View.VISIBLE) {
                    myCameraView.takePicture();
                }
            }
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_log_mood, container, false);
        root.setOnKeyListener(this);

        myEmotionUtil = new EmotionUtil(getActivity());

        myCameraView = root.findViewById(R.id.camera_view);
        myCameraView.setFacing(CameraView.FACING_FRONT);
        myCameraView.addCallback(onCallback);

        myTakePicBtn = root.findViewById(R.id.take_pic);
        myTakePicBtn.setOnClickListener(myOnClickListener);

        RadioGroup inputTypeGroup = root.findViewById(R.id.radio_group);
        myMoodRadios = root.findViewById(R.id.mood_level_radios);
        inputTypeGroup.setOnCheckedChangeListener(this);
        inputTypeGroup.check(R.id.take_photo_radio);
        myMoodRadios.setOnCheckedChangeListener(this);
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
//        FragmentCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
//                    REQUEST_CAMERA_PERMISSION);
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
            myBackgroundHandler.getLooper().quitSafely();
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

    // FIXME
    private CameraView.Callback onCallback = new CameraView.Callback() {

        @Override
        public void onPictureTaken(CameraView cameraView, final byte[] data) {

            Log.d(TAG, "onPictureTaken " + data.length);

            getBackgroundHandler().post(() -> myEmotionUtil.processPicture(data));

            switchToActivityTab();
            // Main thread idle now
        }
    };

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (group.getId() == R.id.radio_group) {
            myCameraView.setVisibility(checkedId == R.id.take_photo_radio ? View.VISIBLE : View.GONE);
            myTakePicBtn.setVisibility(checkedId == R.id.take_photo_radio ? View.VISIBLE : View.GONE);
            myMoodRadios.setVisibility(checkedId == R.id.enter_manual_radio ? View.VISIBLE : View.GONE);
        } else if (group.getId() == R.id.mood_level_radios && checkedId != -1) {
            int radioBtnIdx = myMoodRadios.indexOfChild(myMoodRadios.findViewById(checkedId));
            double moodLevel = (4 - radioBtnIdx) / 4.0;
            getBackgroundHandler().post(() ->
                    myEmotionUtil.insertMoodLog(Arrays.asList(moodLevel, 0.0, 0.0, 0.0)));
            myMoodRadios.clearCheck();
            switchToActivityTab();
        }
    }

    private void switchToActivityTab() {
        if (getActivity() == null) return;
        MainActivity activity = (MainActivity) getActivity();
        activity.switchToTab(0);
    }
}
