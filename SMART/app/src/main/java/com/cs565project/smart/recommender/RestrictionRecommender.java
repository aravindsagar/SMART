package com.cs565project.smart.recommender;

import android.text.format.DateUtils;

import com.cs565project.smart.db.entities.AppDetails;
import com.cs565project.smart.db.entities.DailyAppUsage;
import com.cs565project.smart.db.entities.MoodLog;
import com.cs565project.smart.util.UsageStatsUtil;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Recommends app restrictions based on various factors.
 */
public class RestrictionRecommender {

    public static int recommendRestriction(AppDetails appDetails,
                                           List<DailyAppUsage> dailyAppUsages,
                                           List<MoodLog> moodLogs,
                                           Set<String> categoriesToRestrict) {
        long weekAgo = UsageStatsUtil.getStartOfDayMillis(new Date()) - DateUtils.WEEK_IN_MILLIS;

        long usage = 0;
        int usageCount = 0;
        for (DailyAppUsage appUsage : dailyAppUsages) {
            if (appUsage.getDate().getTime() >= weekAgo && appUsage.getPackageName().equals(appDetails.getPackageName())) {
                usage += appUsage.getDailyUseTime();
                usageCount += 1;
            }
        }

        if (appDetails.getThresholdTime() > -1) {
            // Check the mood over past week and see whether to relax the restrictions.
            double mood = 0;
            int count = 0;
            for (MoodLog moodLog : moodLogs) {
                if (moodLog.dateTime.getTime() >= weekAgo) {
                    mood += moodLog.happy_value;
                    count += 1;
                }
            }
            if (count > 0 && mood/count < 0.4) {
                return (int) (1.1 * appDetails.getThresholdTime());
            } else if (usageCount > 0 && usage / usageCount > 30 * DateUtils.MINUTE_IN_MILLIS) {
                return (int) (0.9 * appDetails.getThresholdTime());
            } else {
                return appDetails.getThresholdTime();
            }
        }

        // An app without restrictions.
        if (usageCount > 0 && usage / usageCount > 30 * DateUtils.MINUTE_IN_MILLIS &&
                categoriesToRestrict.contains(appDetails.getCategory())) {
            return (int) (0.9 * (usage/usageCount));
        }

        return -1;
    }
}
