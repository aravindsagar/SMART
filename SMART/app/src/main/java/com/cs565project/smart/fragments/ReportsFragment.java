package com.cs565project.smart.fragments;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.cs565project.smart.R;

import java.text.DateFormat;
import java.text.ParseException;
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
                new ReportType("Daily", false, null, endDate, null),
                new ReportType("Over time", true, null, startDate, endDate)
        );
    }

    @SuppressLint("SimpleDateFormat")
    private static DateFormat ourDateFormat = new SimpleDateFormat("MMM dd");

    private TextView myDatesText;
    private int myCurrentSpinnerItem = -1;

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
        pickDateButton.setOnClickListener(this);

        Spinner reportSpinner = root.findViewById(R.id.report_view_spinner);
        ArrayAdapter<ReportType> reportTypeArrayAdapter =
                new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, REPORT_TYPES);
        reportTypeArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        reportSpinner.setAdapter(reportTypeArrayAdapter);
        reportSpinner.setOnItemSelectedListener(this);

        myDatesText = root.findViewById(R.id.date_selected_text);
        // Select today by default.
        // TODO: save state and restore from bundle.
        handleSpinnerItemChange(0);
        return root;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.date_picker_button:
                // Dummy dates, replace with actual ones.
                @SuppressLint("SimpleDateFormat") SimpleDateFormat fmt = new SimpleDateFormat("mm-DD-yyyy");
                try {

                    DatePickerFragment
                            .getInstance(REPORT_TYPES.get(myCurrentSpinnerItem).selectRange,
                                    fmt.parse("01-01-2018").getTime(),
                                    new Date().getTime())
                            .setListener(this)
                            .show(getChildFragmentManager(), "date_picker");
                } catch (ParseException e) {
                    e.printStackTrace();
                }
        }
    }

    @Override
    public void onDateSelected(List<Date> selectedDates) {
        if (selectedDates.isEmpty()) {
            return;
        }

        String dateStr = ourDateFormat.format(selectedDates.get(0));
        if (selectedDates.size() > 1) {
            dateStr += " - " + ourDateFormat.format(selectedDates.get(selectedDates.size()-1));
        }
        myDatesText.setText(dateStr);

        // TODO : populate appropriate fragment.
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
        Date startDate = curType.defaultStart,
                endDate = curType.defaultEnd;
        List<Date> selectedDates = (endDate == null) ? Collections.singletonList(startDate) :
                Arrays.asList(startDate, endDate);
        onDateSelected(selectedDates);
    }
}
