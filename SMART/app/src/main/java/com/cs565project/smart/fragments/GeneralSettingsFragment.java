package com.cs565project.smart.fragments;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;

import com.cs565project.smart.R;
import com.cs565project.smart.util.PreferencesHelper.SeekbarPreference;
import com.cs565project.smart.util.PreferencesHelper.SwitchPreference;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class GeneralSettingsFragment extends Fragment implements CompoundButton.OnCheckedChangeListener, SeekBar.OnSeekBarChangeListener {

    /**
     * Our preferences.
     */
    public static final SwitchPreference PREF_ALLOW_APP_BLOCK =
            new SwitchPreference("allow_app_block", true, R.id.allow_block_switch);
    public static final SwitchPreference PREF_ALLOW_BLOCK_BYPASS =
            new SwitchPreference("allow_block_bypass", true, R.id.hard_block_switch);
    public static final SwitchPreference PREF_ALLOW_PICTURES =
            new SwitchPreference("allow_pictures", true, R.id.allow_picture_switch);
    public static final SeekbarPreference PREF_PICTURE_FREQ = new SeekbarPreference(
            "picture_freq", 1, R.id.picture_frequency_seekbar, R.id.picture_freq_text_view) {
        @Override
        public String getSecondaryViewString(int val, Context c) {
            String base = c.getString(R.string.picture_frequency) + " ";
            switch (val) {
                case 0:
                    return c.getString(R.string.manual);
                case 1:
                    return base + c.getString(R.string.once_a_day);
                case 2:
                    return base + c.getString(R.string.twice_a_day);
                default:
                    return base + val + " " + c.getString(R.string.n_times_a_day);
            }
        }
    };

    private static final List<SwitchPreference> SWITCH_PREFERENCES = Arrays.asList(
            PREF_ALLOW_BLOCK_BYPASS, PREF_ALLOW_PICTURES, PREF_ALLOW_APP_BLOCK
    );
    private static final List<SeekbarPreference> SEEKBAR_PREFERENCES = Collections.singletonList(
            PREF_PICTURE_FREQ
    );

    private View myRootView;

    public GeneralSettingsFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        myRootView = inflater.inflate(R.layout.fragment_general_settings, container, false);
        if (getActivity() == null) {
            return myRootView;
        }

//        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
//        String users_email = sharedPref.getString("user_email", "Not Registered");
//
//        TextView T = myRootView.findViewById(R.id.settingsEmail);
//        T.setText(users_email);

        for (SwitchPreference p: SWITCH_PREFERENCES) {
            p.setViewValueFromSavedPreference(myRootView, getContext());
            Switch s = p.findView(myRootView);
            s.setOnCheckedChangeListener(this);
            s.setTag(p);
        }
        for (SeekbarPreference p: SEEKBAR_PREFERENCES) {
            p.setViewValueFromSavedPreference(myRootView, getContext());
            p.setSecondaryViewValue(myRootView, getContext());
            SeekBar s = p.findView(myRootView);
            s.setOnSeekBarChangeListener(this);
            s.setTag(p);
        }
        return myRootView;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        ((SwitchPreference) buttonView.getTag()).saveViewValue(myRootView, getContext());
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            ((SeekbarPreference) seekBar.getTag()).saveViewValue(myRootView, getContext());
        }
        ((SeekbarPreference) seekBar.getTag()).setSecondaryViewValue(myRootView, getContext());
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}
}
