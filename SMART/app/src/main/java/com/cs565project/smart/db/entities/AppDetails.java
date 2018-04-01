package com.cs565project.smart.db.entities;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity
public class AppDetails {
    @PrimaryKey
    public String packageName;

    public String appName;
    public String category;

    public AppDetails(String packageName, String appName, String category) {
        this.packageName = packageName;
        this.appName = appName;
        this.category = category;
    }

}
