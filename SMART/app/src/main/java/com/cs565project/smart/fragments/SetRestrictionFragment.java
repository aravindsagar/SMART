package com.cs565project.smart.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import mobi.upod.timedurationpicker.TimeDurationPicker;
import mobi.upod.timedurationpicker.TimeDurationPickerDialogFragment;

/**
 * Dialog fragment to show a time duration picker.
 */
public class SetRestrictionFragment extends TimeDurationPickerDialogFragment {

    // the fragment initialization parameter keys.
    private static final String ARG_APP_NAME = "app_name";
    private static final String ARG_PACKAGE_NAME = "package_name";
    private static final String ARG_DEFAULT_VAL = "default_val";

    private String myAppName, myPackageName;
    private long myDefaultValue;

    private OnDurationSelectedListener mListener;

    public SetRestrictionFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     */
    public static SetRestrictionFragment newInstance(String appName, String packageName, long defaultValue) {
        SetRestrictionFragment fragment = new SetRestrictionFragment();
        Bundle args = new Bundle();
        args.putString(ARG_APP_NAME, appName);
        args.putLong(ARG_DEFAULT_VAL, defaultValue);
        args.putString(ARG_PACKAGE_NAME, packageName);
        fragment.setArguments(args);
        return fragment;
    }

    public SetRestrictionFragment setListener(OnDurationSelectedListener listener) {
        mListener = listener;
        return this;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            myAppName = getArguments().getString(ARG_APP_NAME);
            myDefaultValue = getArguments().getLong(ARG_DEFAULT_VAL);
            myPackageName = getArguments().getString(ARG_PACKAGE_NAME);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(String.format("Set daily allowed duration for %s", myAppName));
        return dialog;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onDurationSet(TimeDurationPicker view, long duration) {
        if (mListener != null) {
            mListener.onDurationConfirmed(myPackageName, duration);
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        if (mListener != null) {
            mListener.onCancel();
        }
    }

    @Override
    protected long getInitialDuration() {
        return myDefaultValue;
    }

    @Override
    protected int setTimeUnits() {
        return TimeDurationPicker.HH_MM;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnDurationSelectedListener {
        void onDurationConfirmed(String packageName, long duration);

        void onCancel();
    }
}
