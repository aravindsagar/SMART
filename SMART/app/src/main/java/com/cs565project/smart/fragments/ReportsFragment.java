package com.cs565project.smart.fragments;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.cs565project.smart.R;
import com.cs565project.smart.db.AppDao;
import com.cs565project.smart.db.AppDatabase;
import com.cs565project.smart.util.UsageStatsUtil;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;


/**
 * A simple {@link Fragment} subclass. This fragment displays user's activity reports.
 */
public class ReportsFragment extends Fragment implements View.OnClickListener,
        DatePickerFragment.OnDateSelectedListener, AdapterView.OnItemSelectedListener {

    private static final String FRAGMENT_DAY_TAG = "date_picker";
    private static final String KEY_CATEGORY = "category";

    /**
     * A class for representing various report types.
     */
    private static class ReportType {
        String title;
        boolean selectRange;
        Class reportFragmentClass;
        Date defaultStart;
        Date defaultEnd;

        ReportType(String title, boolean selectRange, Class reportFragmentClass, Date defaultStart, Date defaultEnd) {
            this.title = title;
            this.selectRange = selectRange;
            this.reportFragmentClass = reportFragmentClass;
            this.defaultStart = defaultStart;
            this.defaultEnd = defaultEnd;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    /**
     * All the report types statically initialized.
     */
    private static final List<ReportType> REPORT_TYPES;
    static {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -7);
        Date startDate = calendar.getTime();
        Date endDate = new Date();
        REPORT_TYPES = Arrays.asList(
                new ReportType("Daily", false, DayReportFragment.class, endDate, null),
                new ReportType("Over time", true, null, startDate, endDate)
        );
    }

    @SuppressLint("SimpleDateFormat")
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd");

    // Keys for saving data in state bundle.
    private static final String KEY_SPINNER_POSITION = "spinner_position";
    private static final String KEY_START_DATE = "start_date";
    private static final String KEY_END_DATE = "end_date";
    private static final String KEY_SINGLE_DATE = "single_date";

    private TextView myDatesText;
    private int myCurrentSpinnerItem = -1;
    private Date myStartDate;
    private Date mySingleSelectionDate;
    private Date myRangeSelectionStartDate;
    private Date myRangeSelectionEndDate;
    private String myDayReportCategory;

    public ReportsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if (getContext() == null) {
            throw new IllegalStateException("null context inside fragement; cannot continue.");
        }

        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_reports, container, false);
        ImageView pickDateButton = root.findViewById(R.id.date_picker_button);
        pickDateButton.setEnabled(false);
        pickDateButton.setOnClickListener(this);

        Spinner reportSpinner = root.findViewById(R.id.report_view_spinner);
        ArrayAdapter<ReportType> reportTypeArrayAdapter =
                new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, REPORT_TYPES);
        reportTypeArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        reportSpinner.setAdapter(reportTypeArrayAdapter);
        reportSpinner.setOnItemSelectedListener(this);

        myDatesText = root.findViewById(R.id.date_selected_text);

        /*TextView selfieButton = root.findViewById(R.id.button_log_mood);
        selfieButton.setOnClickListener(this);*/

        // Restore from saved instance state if necessary.
        int spinnerPos = 0;
        if (savedInstanceState != null) {
            long singleDate = savedInstanceState.getLong(KEY_SINGLE_DATE, 0);
            long startDate = savedInstanceState.getLong(KEY_START_DATE, 0);
            long endDate = savedInstanceState.getLong(KEY_END_DATE, 0);
            spinnerPos = savedInstanceState.getInt(KEY_SPINNER_POSITION, -1);
            String category = savedInstanceState.getString(KEY_CATEGORY);

            if (startDate > 0) {
                myRangeSelectionStartDate = new Date(startDate);
            }
            if (endDate > 0) {
                myRangeSelectionEndDate = new Date(endDate);
            }
            if (singleDate > 0) {
                mySingleSelectionDate = new Date(singleDate);
            }
            if (category != null) {
                myDayReportCategory = category;
            }
        }
        if (spinnerPos > -1) {
            reportSpinner.setSelection(spinnerPos);
            handleSpinnerItemChange(spinnerPos);
        } else {
            reportSpinner.setSelection(0);
            handleSpinnerItemChange(0);
        }

        // Setup the date picker fragment.
        new Thread() {
            @Override
            public void run() {
                AppDao dao = AppDatabase.getAppDatabase(getActivity()).appDao();
                myStartDate = dao.getUsageDataStartDate();
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> pickDateButton.setEnabled(true));
            }
        }.start();

        return root;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.date_picker_button:
                DatePickerFragment datePickerFragment =
                        DatePickerFragment.getInstance(
                                REPORT_TYPES.get(myCurrentSpinnerItem).selectRange,
                                myStartDate.getTime(),
                                UsageStatsUtil.getTomorrowMillis()
                        ).setListener(ReportsFragment.this);
                datePickerFragment.show(getChildFragmentManager(), FRAGMENT_DAY_TAG);
                break;
            /*case R.id.button_log_mood:
                // TODO start activity to log mood.
                break;*/
        }
    }

    @Override
    public void onDateSelected(List<Date> selectedDates) {
        if (selectedDates.isEmpty()) {
            return;
        }

        String dateStr = DATE_FORMAT.format(selectedDates.get(0));
        if (selectedDates.size() > 1) {
            dateStr += " - " + DATE_FORMAT.format(selectedDates.get(selectedDates.size()-1));

            myRangeSelectionStartDate = selectedDates.get(0);
            myRangeSelectionEndDate = selectedDates.get(selectedDates.size()-1);
        } else {
            mySingleSelectionDate = selectedDates.get(0);
        }
        myDatesText.setText(dateStr);

        // If myDayReportCategory is not empty, it means that we restored from saved state, and has
        // to get our child fragment into the same state. So we pass in the category. But we don't
        // want to enforce this category when user selects a different date, so unset the category.
        String category = myDayReportCategory;
        myDayReportCategory = "";
        getChildFragmentManager().beginTransaction().replace(
                R.id.reports_child_frame,
                DayReportFragment.getInstance(selectedDates.get(0).getTime(), category)
        ).commit();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (position != myCurrentSpinnerItem) {
            handleSpinnerItemChange(position);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    private void handleSpinnerItemChange(int newPosition) {
        myCurrentSpinnerItem = newPosition;
        ReportType curType = REPORT_TYPES.get(newPosition);

        List<Date> selectedDates;
        if (curType.defaultEnd == null) {
            // We are in single date selection mode.
            Date singleDate = (mySingleSelectionDate == null) ? curType.defaultStart : mySingleSelectionDate;
            selectedDates = Collections.singletonList(singleDate);
        } else {
            // We are in range date selection mode.
            Date startDate = (myRangeSelectionStartDate == null) ? curType.defaultStart : myRangeSelectionStartDate,
                    endDate = (myRangeSelectionEndDate == null) ? curType.defaultEnd : myRangeSelectionEndDate;
            selectedDates = Arrays.asList(startDate, endDate);
        }
        onDateSelected(selectedDates);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mySingleSelectionDate != null) {
            outState.putLong(KEY_SINGLE_DATE, mySingleSelectionDate.getTime());
        }
        if (myRangeSelectionStartDate != null) {
            outState.putLong(KEY_START_DATE, myRangeSelectionStartDate.getTime());
        }
        if (myRangeSelectionEndDate != null) {
            outState.putLong(KEY_END_DATE, myRangeSelectionEndDate.getTime());
        }
        outState.putInt(KEY_SPINNER_POSITION, myCurrentSpinnerItem);

        Fragment fragment = getChildFragmentManager().findFragmentById(R.id.reports_child_frame);
        if (fragment instanceof DayReportFragment) {
            outState.putString(KEY_CATEGORY, ((DayReportFragment) fragment).getCurrentCategory());
        }
    }
}
