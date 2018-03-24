package com.cs565project.smart.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.widget.Toast;

import com.cs565project.smart.R;
import com.savvi.rangedatepicker.CalendarPickerView;

import java.util.Date;

import static com.savvi.rangedatepicker.CalendarPickerView.SelectionMode.RANGE;
import static com.savvi.rangedatepicker.CalendarPickerView.SelectionMode.SINGLE;

/**
 * A dialog box for picking a date or a date range.
 */

public class DatePickerFragment extends DialogFragment implements DialogInterface.OnClickListener {
    private static final String KEY_SELECT_RANGE = "SELECT_RANGE";
    private static final String KEY_START_DATE   = "START_DATE";
    private static final String KEY_END_DATE     = "END_DATE";

    public static DatePickerFragment getInstance(boolean selectRange, long startDate, long endDate) {
        DatePickerFragment fragment = new DatePickerFragment();
        Bundle bundle = new Bundle();
        bundle.putBoolean(KEY_SELECT_RANGE, selectRange);
        bundle.putLong(KEY_START_DATE, startDate);
        bundle.putLong(KEY_END_DATE, endDate);
        fragment.setArguments(bundle);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        if (args == null) {
            throw new IllegalStateException("Required arguments missing");
        }
        if (getActivity() == null) {
            throw new IllegalStateException("Parent activity cannot be null");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        CalendarPickerView datePickerView = (CalendarPickerView)
                getLayoutInflater().inflate(R.layout.date_range_picker, null);
        datePickerView.init(new Date(args.getLong(KEY_START_DATE)), new Date(args.getLong(KEY_END_DATE)))
                .inMode(args.getBoolean(KEY_SELECT_RANGE) ? RANGE : SINGLE);

        builder.setView(datePickerView)
                .setPositiveButton("OK", this)
                .setCancelable(true);
        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        Toast.makeText(getContext(), "Good", Toast.LENGTH_SHORT).show();
    }
}
