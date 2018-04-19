package com.cs565project.smart.fragments;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cs565project.smart.R;
import com.cs565project.smart.db.entities.RecommendationActivities;

import org.w3c.dom.Text;

import java.util.Arrays;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */


public class RecommendationSettingsFragment extends Fragment {
    View root;

    public static List<RecommendationActivities> exerciseActivities = Arrays.asList(
            new RecommendationActivities("Run", false, 7),
            new RecommendationActivities("Bike", false, 7),
            new RecommendationActivities("Hike", false, 3),
            new RecommendationActivities("Yoga", false, 3),
            new RecommendationActivities("Pilates", false, 3),
            new RecommendationActivities("Weight Lift", false, 3),
            new RecommendationActivities("Sport", false, 3)
            );

    public static List<RecommendationActivities> academicActivities = Arrays.asList(
            new RecommendationActivities("Study for courses", false, 7),
            new RecommendationActivities("Read a book", false, 7),
            new RecommendationActivities("Write in your journal", false, 12),
            new RecommendationActivities("Create a program", false, 3),
            new RecommendationActivities("Solve Puzzles", false, 3)
    );

    public static List<RecommendationActivities> relaxActivities = Arrays.asList(
            new RecommendationActivities("Drink Tea", false, 7),
            new RecommendationActivities("Talk with or hangout with Friend(s)", false, 15),
            new RecommendationActivities("Meditate", false, 15),
            new RecommendationActivities("Take a Nap", false, 3),
            new RecommendationActivities("Massage", false, 3),
            new RecommendationActivities("Go Outside", false, 3),
            new RecommendationActivities("Stretch", false, 15)
            );


    public RecommendationSettingsFragment() {
        // Required empty public constructor
    }

    private CheckBox createActivityElement(RecommendationActivities activity) {
        CheckBox newCB = new CheckBox(getContext());
        newCB.setText(activity.activityName);
        newCB.setOnCheckedChangeListener(checkboxListener);
        return newCB;
    }

    private View.OnClickListener exerciseListener = new View.OnClickListener() {
        public void onClick(View v) {
            LinearLayout linearLayout = (LinearLayout) root.findViewById(R.id.scroll_layout);
            linearLayout.removeAllViews();
            for(int i = 0; i < exerciseActivities.size(); i++) {
                linearLayout.addView(createActivityElement(exerciseActivities.get(i)));
            }
        }
    };

    private View.OnClickListener academicsListener = new View.OnClickListener() {
        public void onClick(View v) {
            LinearLayout linearLayout = (LinearLayout) root.findViewById(R.id.scroll_layout);
            linearLayout.removeAllViews();
            for(int i = 0; i < academicActivities.size(); i++) {
                linearLayout.addView(createActivityElement(academicActivities.get(i)));
            }
        }
    };

    private View.OnClickListener relaxListener = new View.OnClickListener() {
        public void onClick(View v) {
            LinearLayout linearLayout = (LinearLayout) root.findViewById(R.id.scroll_layout);
            linearLayout.removeAllViews();
            for(int i = 0; i < relaxActivities.size(); i++) {
                linearLayout.addView(createActivityElement(relaxActivities.get(i)));
            }
        }
    };

    private CompoundButton.OnCheckedChangeListener checkboxListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(isChecked) {
                Toast.makeText(getContext(), buttonView.getText(), Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(getContext(), buttonView.getText(), Toast.LENGTH_SHORT).show();
            }

        }


//        public void o(View v) {
//            boolean checked = ((CheckBox) root).isChecked();
//            if(checked) {
//                Toast.makeText(getContext(), "Checkbox Checked", Toast.LENGTH_SHORT).show();
//            }
//            else {
//                Toast.makeText(getContext(), "Checkbox Not Checked", Toast.LENGTH_SHORT).show();
//            }
//        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        root = inflater.inflate(R.layout.fragment_recommendation_settings, container, false);
        ImageButton exercise = root.findViewById(R.id.exercise_button);
        ImageButton academics = root.findViewById(R.id.academics_button);
        ImageButton relax = root.findViewById(R.id.relax_button);

        exercise.setOnClickListener(exerciseListener);
        academics.setOnClickListener(academicsListener);
        relax.setOnClickListener(relaxListener);
        return root;
    }

}
