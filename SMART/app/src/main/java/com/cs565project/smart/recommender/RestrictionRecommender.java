package com.cs565project.smart.recommender;

import com.cs565project.smart.db.entities.AppDetails;
import com.cs565project.smart.db.entities.DailyAppUsage;

import java.util.List;

public class RestrictionRecommender {

    public static int recommendRestriction(AppDetails appDetails, List<DailyAppUsage> dailyAppUsages) {
        return Math.max(-1, appDetails.getThresholdTime());
    }
}
