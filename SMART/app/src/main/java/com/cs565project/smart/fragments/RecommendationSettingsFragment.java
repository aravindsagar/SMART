package com.cs565project.smart.fragments;


import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.cs565project.smart.R;
import com.cs565project.smart.db.AppDatabase;
import com.cs565project.smart.db.entities.RecommendationActivity;
import com.cs565project.smart.util.PreferencesHelper;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.cs565project.smart.recommender.ActivityRecommender.ACTIVITY_TYPE_ACADEMIC;
import static com.cs565project.smart.recommender.ActivityRecommender.ACTIVITY_TYPE_EXERCISE;
import static com.cs565project.smart.recommender.ActivityRecommender.ACTIVITY_TYPE_NEWS;
import static com.cs565project.smart.recommender.ActivityRecommender.ACTIVITY_TYPE_RELAX;
import static com.cs565project.smart.recommender.ActivityRecommender.KEY_DB_POPULATED;


/**
 * A simple {@link Fragment} subclass.
 */


public class RecommendationSettingsFragment extends Fragment implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private static final LinearLayout.LayoutParams CB_LAYOUT_PARAMS = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    static {
        CB_LAYOUT_PARAMS.setMargins(50, 10, 50, 10);
    }

    private View root;
    private List<ImageView> recommendationHeaders;

    private List<RecommendationActivity> myExerciseActivities, myAcademicActivities, myRelaxActivites, myNewsTopics;

    private Handler myUIHandler = new Handler();
    private Executor myExecutor = Executors.newSingleThreadExecutor();

    private Runnable fetchActivityData = new Runnable() {
        @Override
        public void run() {
            Context c = getActivity();
            if (c == null) return;

            AppDatabase db = AppDatabase.getAppDatabase(c);
            if (!PreferencesHelper.getBoolPreference(c, KEY_DB_POPULATED, false)) {
                db.insertDefaultActivitiesIntoDb();
                PreferencesHelper.setPreference(c, KEY_DB_POPULATED, true);
            }

            myExerciseActivities = db.appDao().getRecommendationActivities(ACTIVITY_TYPE_EXERCISE);
            myAcademicActivities = db.appDao().getRecommendationActivities(ACTIVITY_TYPE_ACADEMIC);
            myRelaxActivites = db.appDao().getRecommendationActivities(ACTIVITY_TYPE_RELAX);
            myNewsTopics = db.appDao().getRecommendationActivities(ACTIVITY_TYPE_NEWS);

            myUIHandler.post(() -> {
                for (ImageView view : recommendationHeaders) {
                    Log.d("Rec", "Setting onclick" + myExerciseActivities.size());
                    view.setOnClickListener(RecommendationSettingsFragment.this);
                }
            });
        }
    };

    public RecommendationSettingsFragment() {
        // Required empty public constructor
    }

    private void populateActivityList(List<RecommendationActivity> activities) {
        LinearLayout myActivitiesList = root.findViewById(R.id.scroll_layout);
        myActivitiesList.removeAllViews();
        for (RecommendationActivity activity : activities) {
            CheckBox newCB = new CheckBox(getContext());
            newCB.setText(activity.activityName);
            newCB.setChecked(activity.isSet);
            newCB.setOnCheckedChangeListener(this);
            newCB.setTag(activity);
            myActivitiesList.addView(newCB, CB_LAYOUT_PARAMS);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        root = inflater.inflate(R.layout.fragment_recommendation_settings, container, false);
        ImageView exercise = root.findViewById(R.id.exercise_button),
                academics = root.findViewById(R.id.academics_button),
                relax = root.findViewById(R.id.relax_button),
                news = root.findViewById(R.id.news_button);

        recommendationHeaders = Arrays.asList(exercise, academics, relax, news);
        myExecutor.execute(fetchActivityData);
        return root;
    }

    @Override
    public void onClick(View v) {
        Context c = getActivity();
        if (c == null) return;

        if (v instanceof ImageView) {
            for (ImageView view : recommendationHeaders) {
                view.setImageTintList(null);
            }

            ((ImageView) v).setImageTintList(c.getResources().getColorStateList(R.color.image_selected));
        }

        switch (v.getId()) {
            case R.id.exercise_button:
                populateActivityList(myExerciseActivities);
                break;
            case R.id.academics_button:
                populateActivityList(myAcademicActivities);
                break;
            case R.id.relax_button:
                populateActivityList(myRelaxActivites);
                break;
            case R.id.news_button:
                populateActivityList(myNewsTopics);
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Object tag = buttonView.getTag();
        if (tag == null || !(tag instanceof RecommendationActivity)) return;
        RecommendationActivity activity = (RecommendationActivity) buttonView.getTag();
        activity.isSet = isChecked;
        myExecutor.execute(() -> {
            Context c = getContext();
            if (c == null) return;
            AppDatabase.getAppDatabase(c).appDao().updateRecommendationActivity(activity);
        });
    }
}
