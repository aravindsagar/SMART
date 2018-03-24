package com.cs565project.smart.fragments;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;

import com.cs565project.smart.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


/**
 * A simple {@link Fragment} subclass. This fragment displays user's activity reports.
 */
public class ReportsFragment extends Fragment implements View.OnClickListener {

    /**
     * A class for representing various report types.
     */
    private static class ReportType {
        String title;
        boolean selectRange;
        Class reportFragmentClass;

        ReportType(String title, boolean selectRange, Class reportFragmentClass) {
            this.title = title;
            this.selectRange = selectRange;
            this.reportFragmentClass = reportFragmentClass;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    /**
     * All the report types statically initialized.
     */
    private static final List<ReportType> REPORT_TYPES = Arrays.asList(
            new ReportType("Daily", false, null),
            new ReportType("Over time", true, null)
    );

    public ReportsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_reports, container, false);
        ImageView pickDateButton = root.findViewById(R.id.date_picker_button);
        pickDateButton.setOnClickListener(this);

        Spinner reportSpinner = root.findViewById(R.id.report_view_spinner);
        ArrayAdapter<ReportType> reportTypeArrayAdapter =
                new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, REPORT_TYPES);
        reportTypeArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        reportSpinner.setAdapter(reportTypeArrayAdapter);
        return root;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.date_picker_button:
                // Dummy dates, replace with actual ones.
                SimpleDateFormat fmt = new SimpleDateFormat("mm-DD-yyyy");
                try {
                    DatePickerFragment
                            .getInstance(false,
                                    fmt.parse("01-01-2018").getTime(),
                                    new Date().getTime())
                            .show(getChildFragmentManager(), "date_picker");
                } catch (ParseException e) {
                    e.printStackTrace();
                }
        }
    }
}
