package com.cs565project.smart.db.entities

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

/**
 * Database entity to hold details of activities which can be recommended to the user.
 */
@Entity
class RecommendationActivity(var activityName: String, var isSet: Boolean, var timeOfDay: Int, var activityType: String) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0

}
