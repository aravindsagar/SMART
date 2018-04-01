package com.cs565project.smart.db;

import android.arch.persistence.room.TypeConverter;

import java.util.Date;

/**
 * Converters required for Room database.
 */

public class Converters {
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
}
