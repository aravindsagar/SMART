package com.cs565project.smart.db.entities;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity
public class RecommendationActivity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String activityName;
    public boolean isSet;
    public int timeOfDay;
    public String activityType;

    public RecommendationActivity(String activityName, boolean isSet, int timeOfDay, String activityType) {
        this.activityName = activityName;
        this.isSet = isSet;
        this.timeOfDay = timeOfDay;
        this.activityType = activityType;
    }

}
