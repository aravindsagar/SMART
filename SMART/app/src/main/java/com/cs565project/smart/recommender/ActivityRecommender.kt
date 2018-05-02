package com.cs565project.smart.recommender

import com.cs565project.smart.db.entities.RecommendationActivity
import java.util.*

/**
 * Provides activity recommendations to be displayed on block overlay.
 */
object ActivityRecommender {
    /**
     * Int denoting period of day. OR values together to denote multiple periods.
     */
    const val MORNING = 1
    const val AFTERNOON = 2
    const val EVENING = 4
    const val NIGHT = 8

    const val ACTIVITY_TYPE_EXERCISE = "exercise"
    const val ACTIVITY_TYPE_ACADEMIC = "academic"
    const val ACTIVITY_TYPE_RELAX = "relax"
    const val ACTIVITY_TYPE_NEWS = "news"

    // Preference key which stores whether our initial population of database has taken place.
    const val KEY_DB_POPULATED = "db_activities_populated"

    val EXERCISE_ACTIVITIES = Arrays.asList(
            RecommendationActivity("\uD83C\uDFC3 Run", false, 7, ACTIVITY_TYPE_EXERCISE),
            RecommendationActivity("\uD83D\uDEB4 Bike", false, 7, ACTIVITY_TYPE_EXERCISE),
            RecommendationActivity("⛰️ Hike", false, 3, ACTIVITY_TYPE_EXERCISE),
            RecommendationActivity("\uD83E\uDDD8 Yoga", false, 3, ACTIVITY_TYPE_EXERCISE),
            RecommendationActivity("\uD83D\uDCAA️ Pilates", false, 3, ACTIVITY_TYPE_EXERCISE),
            RecommendationActivity("\uD83C\uDFCB️ Weight Lift", false, 3, ACTIVITY_TYPE_EXERCISE),
            RecommendationActivity("\uD83C\uDFC8 Sport", false, 3, ACTIVITY_TYPE_EXERCISE)
    )

    val ACADEMIC_ACTIVITIES = Arrays.asList(
            RecommendationActivity("\uD83D\uDCDA Study for courses", false, 7, ACTIVITY_TYPE_ACADEMIC),
            RecommendationActivity("\uD83D\uDCD6 Read a book", false, 7, ACTIVITY_TYPE_ACADEMIC),
            RecommendationActivity("✍️ Write in your journal", false, 12, ACTIVITY_TYPE_ACADEMIC),
            RecommendationActivity("\uD83D\uDDA5️ Create a program", false, 3, ACTIVITY_TYPE_ACADEMIC),
            RecommendationActivity("\uD83C\uDC04 Solve Puzzles", false, 3, ACTIVITY_TYPE_ACADEMIC)
    )

    val RELAX_ACTIVITIES = Arrays.asList(
            RecommendationActivity("☕ Drink Tea", false, 7, ACTIVITY_TYPE_RELAX),
            RecommendationActivity("\uD83D\uDC4B Talk with or hangout with Friend(s)", false, 15, ACTIVITY_TYPE_RELAX),
            RecommendationActivity("\uD83E\uDDD8 Meditate", false, 15, ACTIVITY_TYPE_RELAX),
            RecommendationActivity("\uD83D\uDECC Take a Nap", false, 3, ACTIVITY_TYPE_RELAX),
            RecommendationActivity("\uD83D\uDC86 Massage", false, 3, ACTIVITY_TYPE_RELAX),
            RecommendationActivity("\uD83C\uDF05 Go Outside", false, 3, ACTIVITY_TYPE_RELAX),
            RecommendationActivity("\uD83E\uDD3E Stretch", false, 15, ACTIVITY_TYPE_RELAX)
    )

    val NEWS_TOPICS = Arrays.asList(
            RecommendationActivity("Politics", true, 15, ACTIVITY_TYPE_NEWS),
            RecommendationActivity("World", true, 15, ACTIVITY_TYPE_NEWS),
            RecommendationActivity("Sports", true, 15, ACTIVITY_TYPE_NEWS),
            RecommendationActivity("Science", true, 15, ACTIVITY_TYPE_NEWS),
            RecommendationActivity("Finance", true, 15, ACTIVITY_TYPE_NEWS),
            RecommendationActivity("Technology", true, 15, ACTIVITY_TYPE_NEWS)
    )

    fun getRecommendedActivities(allActivities: List<RecommendationActivity>): List<RecommendationActivity> {
        val activities = ArrayList<RecommendationActivity>()
        val dayPeriod = getPeriodOfDay(Date())
        for (activity in allActivities) {
            if (activity.isSet && ACTIVITY_TYPE_NEWS != activity.activityType && dayPeriod and activity.timeOfDay > 0) {
                activities.add(activity)
            }
        }
        return activities
    }

    private fun getPeriodOfDay(date: Date): Int {
        val cal = GregorianCalendar.getInstance()
        cal.time = date
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        return if (hour < 5) {
            NIGHT
        } else if (hour < 12) {
            MORNING
        } else if (hour < 17) {
            AFTERNOON
        } else if (hour < 22) {
            EVENING
        } else {
            NIGHT
        }
    }
}
