package com.cs565project.smart.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Class representing an installed app, and related functions. Do not initialize in UI thread; use
 * a background thread.
 */
public class AppInfo {
    public static final String NO_CATEGORY = "NONE";

    private String packageName, appName;
    private Drawable appIcon;
    private PackageManager packageManager;


    public AppInfo(String packageName, String appName, PackageManager packageManager){
        this.packageName = packageName;
        this.appName = appName;
        this.packageManager = packageManager;
    }

    public AppInfo(String packageName, Context context){
        this(packageName, getAppNameFromPackageName(packageName, context), context.getPackageManager());
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getAppName() {
        return appName;
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof AppInfo)){
            return false;
        }
        AppInfo app = (AppInfo) o;
        return app.packageName.equals(packageName);
    }

    @Override
    public String toString() {
        return appName;
    }

    public Drawable getAppIcon() {
        if (appIcon == null) {
            try {
                appIcon = packageManager.getApplicationIcon(packageName);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        return appIcon;
    }

    private static String getAppNameFromPackageName(String packageName, Context context){
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(packageName, 0);
            return context.getPackageManager().getApplicationLabel(info).toString();
        } catch (PackageManager.NameNotFoundException e){
            return null;
        }
    }

    public static List<AppInfo> getAllApps(PackageManager packageManager){
        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        final List<ResolveInfo> pkgAppsList = packageManager.queryIntentActivities(mainIntent, 0);

        List<AppInfo> allApps = new ArrayList<>();
        for(ResolveInfo info: pkgAppsList){
            ApplicationInfo appInfo = info.activityInfo.applicationInfo;
            allApps.add(new AppInfo(appInfo.packageName, appInfo.loadLabel(packageManager).toString(), packageManager));
        }
        Collections.sort(allApps, new AppComparator());
        return allApps;
    }

    public static class AppComparator implements Comparator<AppInfo>{
        @Override
        public int compare(AppInfo lhs, AppInfo rhs) {
            return lhs.getAppName().compareToIgnoreCase(rhs.getAppName());
        }
    }
}
