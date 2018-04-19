package com.cs565project.smart.recommender;

import com.cs565project.smart.db.entities.RecommendationActivity;

import java.util.Arrays;
import java.util.List;

public class ActivityRecommender {
    public static final String ACTIVITY_TYPE_EXERCISE = "exercise";
    public static final String ACTIVITY_TYPE_ACADEMIC = "academic";
    public static final String ACTIVITY_TYPE_RELAX = "relax";
    public static final String ACTIVITY_TYPE_NEWS = "news";

    // Preference key which stores whether our initial population of database has taken place.
    public static final String KEY_DB_POPULATED = "db_activities_populated";

    public static final List<RecommendationActivity> EXERCISE_ACTIVITIES = Arrays.asList(
            new RecommendationActivity("Run", false, 7, ACTIVITY_TYPE_EXERCISE),
            new RecommendationActivity("Bike", false, 7, ACTIVITY_TYPE_EXERCISE),
            new RecommendationActivity("Hike", false, 3, ACTIVITY_TYPE_EXERCISE),
            new RecommendationActivity("Yoga", false, 3, ACTIVITY_TYPE_EXERCISE),
            new RecommendationActivity("Pilates", false, 3, ACTIVITY_TYPE_EXERCISE),
            new RecommendationActivity("Weight Lift", false, 3, ACTIVITY_TYPE_EXERCISE),
            new RecommendationActivity("Sport", false, 3, ACTIVITY_TYPE_EXERCISE)
    );

    public static final List<RecommendationActivity> ACADEMIC_ACTIVITIES = Arrays.asList(
            new RecommendationActivity("Study for courses", false, 7, ACTIVITY_TYPE_ACADEMIC),
            new RecommendationActivity("Read a book", false, 7, ACTIVITY_TYPE_ACADEMIC),
            new RecommendationActivity("Write in your journal", false, 12, ACTIVITY_TYPE_ACADEMIC),
            new RecommendationActivity("Create a program", false, 3, ACTIVITY_TYPE_ACADEMIC),
            new RecommendationActivity("Solve Puzzles", false, 3, ACTIVITY_TYPE_ACADEMIC)
    );

    public static final List<RecommendationActivity> RELAX_ACTIVITIES = Arrays.asList(
            new RecommendationActivity("Drink Tea", false, 7, ACTIVITY_TYPE_RELAX),
            new RecommendationActivity("Talk with or hangout with Friend(s)", false, 15, ACTIVITY_TYPE_RELAX),
            new RecommendationActivity("Meditate", false, 15, ACTIVITY_TYPE_RELAX),
            new RecommendationActivity("Take a Nap", false, 3, ACTIVITY_TYPE_RELAX),
            new RecommendationActivity("Massage", false, 3, ACTIVITY_TYPE_RELAX),
            new RecommendationActivity("Go Outside", false, 3, ACTIVITY_TYPE_RELAX),
            new RecommendationActivity("Stretch", false, 15, ACTIVITY_TYPE_RELAX)
    );

    public static final List<RecommendationActivity> NEWS_TOPICS = Arrays.asList(
            new RecommendationActivity("Politics", true, 15, ACTIVITY_TYPE_NEWS),
            new RecommendationActivity("World", true, 15, ACTIVITY_TYPE_NEWS),
            new RecommendationActivity("Sports", true, 15, ACTIVITY_TYPE_NEWS),
            new RecommendationActivity("Science", true, 15, ACTIVITY_TYPE_NEWS),
            new RecommendationActivity("Finance", true, 15, ACTIVITY_TYPE_NEWS),
            new RecommendationActivity("Technology", true, 15, ACTIVITY_TYPE_NEWS)
    );

}
