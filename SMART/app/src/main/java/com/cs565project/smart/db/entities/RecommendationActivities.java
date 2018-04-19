package com.cs565project.smart.db.entities;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

@Entity
public class RecommendationActivities {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String activityName;
    public boolean isSet;
    public int timeOfDay;

    public RecommendationActivities(String activityName, boolean isSet, int timeOfDay) {
        this.activityName = activityName;
        this.isSet = isSet;
        this.timeOfDay = timeOfDay;
    }

}
