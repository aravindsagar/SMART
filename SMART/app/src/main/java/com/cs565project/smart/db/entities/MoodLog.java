package com.cs565project.smart.db.entities;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import java.util.Date;
import java.util.List;

@Entity
public class MoodLog {
    @PrimaryKey
    public Date dateTime;

/*  // Prob shouldn't store image
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    private byte[] image;       // for image storing
*/
    // Basic feature char scores

    @ColumnInfo(name = "happiness")
    public double happy_value;

    @ColumnInfo(name = "sadness")
    public double sad_value;

    @ColumnInfo(name = "neutral")
    public double neutral_value;

    @ColumnInfo(name = "anger")
    public double anger_value;

    public MoodLog() {
        /* Empty */
    }

    public MoodLog(Date dateTime, List<Double> moodList) {
        this.dateTime   = dateTime;
        happy_value     = moodList.get(0);
        sad_value       = moodList.get(1);
        neutral_value   = moodList.get(2);
        anger_value     = moodList.get(3);
    }

    /*
    public Date getDate() { return dateTime; }
    public void setDate(Date dateTime) { this.dateTime = dateTime; }

    public double getHappy() { return happy_value; }
    public void setHappy(double happy) { happy_value = happy; }

    public double getSad() { return sad_value; }
    public void setSad(double sad) { sad_value = sad; }

    public double getNeutral() { return neutral_value; }
    public void setNeutral(double neutral) { neutral_value = neutral; }

    public double getAnger() { return anger_value; }
    public void setAnger(double anger) { anger_value = anger; }
    */
}
