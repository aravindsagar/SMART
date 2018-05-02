package com.cs565project.smart.db

import android.arch.persistence.room.TypeConverter
import java.util.*

/**
 * Converters required for Room database.
 */

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return if (value == null) null else Date(value)
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}
