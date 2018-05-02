package com.cs565project.smart.db.entities

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import java.util.*

/**
 * Database entity to hold app usages at a day-level granularity.
 */
@Entity(primaryKeys = arrayOf("packageName", "date"),
        foreignKeys = arrayOf(ForeignKey(entity = AppDetails::class,
                                         parentColumns = arrayOf("packageName"),
                                         childColumns = arrayOf("packageName"))))
class DailyAppUsage(val packageName: String, val date: Date, val dailyUseTime: Long, val dailyUseCount: Int)
