package com.cs565project.smart.fragments;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;

import com.cs565project.smart.MainActivity;
import com.cs565project.smart.R;


/**
 * A simple {@link Fragment} subclass.
 */
public class GeneralFragment extends Fragment {


    public GeneralFragment() {

        // Set default key values

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Context context = getActivity();
        SharedPreferences sharedPref = context.getSharedPreferences("settings_shared_preferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putBoolean("allow_blocked",false);
        editor.putBoolean("hard_blocked",false);
        editor.putBoolean("pics_allowed",false);
        editor.putInt("pics_per_day", 0);

        View rootView = inflater.inflate(R.layout.fragment_general, container, false);
        CompoundButton.OnCheckedChangeListener listener = (new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Context context = getActivity();
                SharedPreferences sharedPref = context.getSharedPreferences("settings_shared_preferences", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                if (isChecked) {
                    // do something when check is selected
                    switch (buttonView.getId()) {
                    case R.id.switch1:
                        editor.putBoolean("allow_blocked",true);
                        break;

                    case R.id.switch2:
                        editor.putBoolean("hard_blocked",true);
                        break;

                    case R.id.switch3:
                        editor.putBoolean("pics_allowed",true);
                        break;
                    }

                } else {
                    //do something when unchecked
                    switch (buttonView.getId()) {
                        case R.id.switch1:
                            editor.putBoolean("allow_blocked",false);
                            break;

                        case R.id.switch2:
                            editor.putBoolean("hard_blocked",false);
                            break;

                        case R.id.switch3:
                            editor.putBoolean("pics_allowed",false);
                            break;
                    }

                }
            }

        });
        Switch sw1 = rootView.findViewById(R.id.switch1);
        Switch sw2 = rootView.findViewById(R.id.switch2);
        Switch sw3 = rootView.findViewById(R.id.switch3);

        sw1.setOnCheckedChangeListener(listener);
        sw2.setOnCheckedChangeListener(listener);
        sw3.setOnCheckedChangeListener(listener);

        SeekBar s = (SeekBar) rootView.findViewById(R.id.seekBar);
        s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Context context = getActivity();
                SharedPreferences sharedPref = context.getSharedPreferences("settings_shared_preferences", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putInt("pics_per_day",progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

        });

        // Inflate the layout for this fragment
        return rootView;
    }


}
