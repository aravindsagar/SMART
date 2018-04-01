package com.cs565project.smart.db.entities;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

@SuppressWarnings("unused")
@Entity
public class AppDetails {
    @PrimaryKey
    @NonNull
    private String packageName;

    private String appName;
    private String category;

    public AppDetails(@NonNull String packageName, String appName, String category) {
        this.packageName = packageName;
        this.appName = appName;
        this.category = category;
    }

    @NonNull
    public String getPackageName() {
        return packageName;
    }

    public String getAppName() {
        return appName;
    }

    public String getCategory() {
        return category;
    }
}
