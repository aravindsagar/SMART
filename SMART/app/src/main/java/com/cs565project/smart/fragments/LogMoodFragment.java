package com.cs565project.smart.fragments;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cs565project.smart.R;
import com.google.android.cameraview.CameraView;

/**
 * A simple {@link Fragment} subclass.
 */
public class LogMoodFragment extends Fragment {

    private CameraView myCameraView;

    public LogMoodFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_log_mood, container, false);
        myCameraView = root.findViewById(R.id.camera_view);
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        myCameraView.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        myCameraView.stop();
    }
}
