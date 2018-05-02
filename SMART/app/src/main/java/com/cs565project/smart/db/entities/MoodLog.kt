package com.cs565project.smart.db.entities

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import java.util.*

/**
 * Database entity to hold user's moods.
 */
@Entity
public class MoodLog(@field:PrimaryKey val date: Date, val happyValue: Double, val sadValue: Double,
                     val neutralValue: Double, val angerValue: Double) {

    fun getValByIndex(index: Int): Double {
        when (index) {
            0 -> return happyValue
            1 -> return sadValue
            2 -> return neutralValue
            3 -> return angerValue
            else -> throw IllegalArgumentException("Invalid index $index")
        }
    }

    constructor(date: Date, moodList: List<Double>) : this(date, moodList[0], moodList[1], moodList[2], moodList[3])
}
