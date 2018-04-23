package com.cs565project.smart.recommender;

import com.cs565project.smart.db.entities.RecommendationActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Provides activity recommendations to be displayed on block overlay.
 */
public class ActivityRecommender {
    /**
     * Int denoting period of day. OR values together to denote multiple periods.
     */
    public static final int MORNING   = 1;
    public static final int AFTERNOON = 2;
    public static final int EVENING   = 4;
    public static final int NIGHT     = 8;

    public static final String ACTIVITY_TYPE_EXERCISE = "exercise";
    public static final String ACTIVITY_TYPE_ACADEMIC = "academic";
    public static final String ACTIVITY_TYPE_RELAX = "relax";
    public static final String ACTIVITY_TYPE_NEWS = "news";

    // Preference key which stores whether our initial population of database has taken place.
    public static final String KEY_DB_POPULATED = "db_activities_populated";

    public static final List<RecommendationActivity> EXERCISE_ACTIVITIES = Arrays.asList(
            new RecommendationActivity("\uD83C\uDFC3 Run", false, 7, ACTIVITY_TYPE_EXERCISE),
            new RecommendationActivity("\uD83D\uDEB4 Bike", false, 7, ACTIVITY_TYPE_EXERCISE),
            new RecommendationActivity("⛰️ Hike", false, 3, ACTIVITY_TYPE_EXERCISE),
            new RecommendationActivity("\uD83E\uDDD8 Yoga", false, 3, ACTIVITY_TYPE_EXERCISE),
            new RecommendationActivity("\uD83D\uDCAA️ Pilates", false, 3, ACTIVITY_TYPE_EXERCISE),
            new RecommendationActivity("\uD83C\uDFCB️ Weight Lift", false, 3, ACTIVITY_TYPE_EXERCISE),
            new RecommendationActivity("\uD83C\uDFC8 Sport", false, 3, ACTIVITY_TYPE_EXERCISE)
    );

    public static final List<RecommendationActivity> ACADEMIC_ACTIVITIES = Arrays.asList(
            new RecommendationActivity("\uD83D\uDCDA Study for courses", false, 7, ACTIVITY_TYPE_ACADEMIC),
            new RecommendationActivity("\uD83D\uDCD6 Read a book", false, 7, ACTIVITY_TYPE_ACADEMIC),
            new RecommendationActivity("✍️ Write in your journal", false, 12, ACTIVITY_TYPE_ACADEMIC),
            new RecommendationActivity("\uD83D\uDDA5️ Create a program", false, 3, ACTIVITY_TYPE_ACADEMIC),
            new RecommendationActivity("\uD83C\uDC04 Solve Puzzles", false, 3, ACTIVITY_TYPE_ACADEMIC)
    );

    public static final List<RecommendationActivity> RELAX_ACTIVITIES = Arrays.asList(
            new RecommendationActivity("☕ Drink Tea", false, 7, ACTIVITY_TYPE_RELAX),
            new RecommendationActivity("\uD83D\uDC4B Talk with or hangout with Friend(s)", false, 15, ACTIVITY_TYPE_RELAX),
            new RecommendationActivity("\uD83E\uDDD8 Meditate", false, 15, ACTIVITY_TYPE_RELAX),
            new RecommendationActivity("\uD83D\uDECC Take a Nap", false, 3, ACTIVITY_TYPE_RELAX),
            new RecommendationActivity("\uD83D\uDC86 Massage", false, 3, ACTIVITY_TYPE_RELAX),
            new RecommendationActivity("\uD83C\uDF05 Go Outside", false, 3, ACTIVITY_TYPE_RELAX),
            new RecommendationActivity("\uD83E\uDD3E Stretch", false, 15, ACTIVITY_TYPE_RELAX)
    );

    public static final List<RecommendationActivity> NEWS_TOPICS = Arrays.asList(
            new RecommendationActivity("Politics", true, 15, ACTIVITY_TYPE_NEWS),
            new RecommendationActivity("World", true, 15, ACTIVITY_TYPE_NEWS),
            new RecommendationActivity("Sports", true, 15, ACTIVITY_TYPE_NEWS),
            new RecommendationActivity("Science", true, 15, ACTIVITY_TYPE_NEWS),
            new RecommendationActivity("Finance", true, 15, ACTIVITY_TYPE_NEWS),
            new RecommendationActivity("Technology", true, 15, ACTIVITY_TYPE_NEWS)
    );

    public static List<RecommendationActivity> getRecommendedActivities(List<RecommendationActivity> allActivities) {
        List<RecommendationActivity> activities = new ArrayList<>();
        int dayPeriod = getPeriodOfDay(new Date());
        for (RecommendationActivity activity : allActivities) {
            if (activity.isSet && !ACTIVITY_TYPE_NEWS.equals(activity.activityType) && (dayPeriod & activity.timeOfDay) > 0) {
                activities.add(activity);
            }
        }
        return activities;
    }

    private static int getPeriodOfDay(Date date) {
        Calendar cal = GregorianCalendar.getInstance();
        cal.setTime(date);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        if (hour < 5) {
            return NIGHT;
        } else if (hour < 12) {
            return MORNING;
        } else if (hour < 17) {
            return AFTERNOON;
        } else if (hour < 22) {
            return EVENING;
        } else {
            return NIGHT;
        }
    }
}
