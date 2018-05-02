package com.cs565project.smart.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import java.util.*

/**
 * Class representing an installed app, and related functions. Do not initialize in UI thread; use
 * a background thread.
 */
class AppInfo(var packageName: String, val appName: String?, private val packageManager: PackageManager) {
    var appIcon: Drawable? = null
        get() {
            return try {
                    packageManager.getApplicationIcon(packageName)
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                    null
                }
        }
        private set

    constructor(packageName: String, context: Context) : this(packageName, getAppNameFromPackageName(packageName, context), context.packageManager) {}

    override fun equals(other: Any?): Boolean {
        if (other !is AppInfo) {
            return false
        }
        val app = other as AppInfo?
        return app!!.packageName == packageName
    }

    override fun toString(): String {
        return if (appName == null) packageName else appName
    }

    class AppComparator : Comparator<AppInfo> {
        override fun compare(lhs: AppInfo, rhs: AppInfo): Int {
            return lhs.appName!!.compareTo(rhs.appName!!, ignoreCase = true)
        }
    }

    companion object {
        const val NO_CATEGORY = "NONE"

        private fun getAppNameFromPackageName(packageName: String, context: Context): String? {
            return try {
                val info = context.packageManager.getApplicationInfo(packageName, 0)
                context.packageManager.getApplicationLabel(info).toString()
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }

        }

        fun getAllApps(packageManager: PackageManager): List<AppInfo> {
            val mainIntent = Intent(Intent.ACTION_MAIN, null)
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            val pkgAppsList = packageManager.queryIntentActivities(mainIntent, 0)

            val allApps = ArrayList<AppInfo>()
            for (info in pkgAppsList) {
                val appInfo = info.activityInfo.applicationInfo
                allApps.add(AppInfo(appInfo.packageName, appInfo.loadLabel(packageManager).toString(), packageManager))
            }
            Collections.sort(allApps, AppComparator())
            return allApps
        }
    }
}
