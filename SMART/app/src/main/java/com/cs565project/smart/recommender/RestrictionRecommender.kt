package com.cs565project.smart.recommender

import android.text.format.DateUtils
import com.cs565project.smart.db.entities.AppDetails
import com.cs565project.smart.db.entities.DailyAppUsage
import com.cs565project.smart.db.entities.MoodLog
import com.cs565project.smart.util.UsageStatsUtil
import java.util.*

/**
 * Recommends app restrictions based on various factors.
 */
object RestrictionRecommender {

    fun recommendRestriction(appDetails: AppDetails,
                             dailyAppUsages: List<DailyAppUsage>,
                             moodLogs: List<MoodLog>,
                             categoriesToRestrict: Set<String>): Int {
        val weekAgo = UsageStatsUtil.getStartOfDayMillis(Date()) - DateUtils.WEEK_IN_MILLIS

        var usage: Long = 0
        var usageCount = 0
        for (appUsage in dailyAppUsages) {
            if (appUsage.date.time >= weekAgo && appUsage.packageName == appDetails.packageName) {
                usage += appUsage.dailyUseTime
                usageCount += 1
            }
        }

        if (appDetails.thresholdTime > -1) {
            // Check the mood over past week and see whether to relax the restrictions.
            var mood = 0.0
            var count = 0
            for (moodLog in moodLogs) {
                if (moodLog.date.time >= weekAgo) {
                    mood += moodLog.happyValue
                    count += 1
                }
            }
            return if (count > 0 && mood / count < 0.4) {
                (1.1 * appDetails.thresholdTime).toInt()
            } else if (usageCount > 0 && usage / usageCount > 30 * DateUtils.MINUTE_IN_MILLIS) {
                (0.9 * appDetails.thresholdTime).toInt()
            } else {
                appDetails.thresholdTime
            }
        }

        // An app without restrictions.
        return if (usageCount > 0 && usage / usageCount > 30 * DateUtils.MINUTE_IN_MILLIS &&
                categoriesToRestrict.contains(appDetails.category)) {
            (0.9 * (usage / usageCount)).toInt()
        } else -1

    }
}
