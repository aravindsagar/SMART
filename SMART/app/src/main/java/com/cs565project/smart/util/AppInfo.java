package com.cs565project.smart.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import java.io.IOException;
import java.net.URL;
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


    public AppInfo(String packageName, String appName, Drawable appIcon){
        this.packageName = packageName;
        this.appName = appName;
        this.appIcon = appIcon;
    }

    public AppInfo(String packageName, Context context){
        this(packageName, null, null);
        setAppNameAndIconFromPackageName(packageName, context);
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

    public void setAppName(String appName) {
        this.appName = appName;
    }

    private void setAppNameAndIconFromPackageName(String packageName, Context context){
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(packageName, 0);
            appIcon = context.getPackageManager().getApplicationIcon(info);
            appName = context.getPackageManager().getApplicationLabel(info).toString();
        } catch (PackageManager.NameNotFoundException e){
            //Do nothing
        }
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
        return appIcon;
    }

    public void setAppIcon(Drawable appIcon) {
        this.appIcon = appIcon;
    }

    public static List<AppInfo> getAllApps(PackageManager packageManager){
        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        final List<ResolveInfo> pkgAppsList = packageManager.queryIntentActivities(mainIntent, 0);

        List<AppInfo> allApps = new ArrayList<>();
        for(ResolveInfo info: pkgAppsList){
            ApplicationInfo appInfo = info.activityInfo.applicationInfo;
            allApps.add(new AppInfo(appInfo.packageName, appInfo.loadLabel(packageManager).toString(),
                    appInfo.loadIcon(packageManager)));
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
