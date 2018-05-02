package com.cs565project.smart.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.text.format.DateUtils
import android.util.Log
import android.view.WindowManager
import com.cs565project.smart.MainActivity
import com.cs565project.smart.R
import com.cs565project.smart.db.AppDatabase
import com.cs565project.smart.fragments.GeneralSettingsFragment
import com.cs565project.smart.recommender.ActivityRecommender
import com.cs565project.smart.recommender.NewsItem
import com.cs565project.smart.util.AppInfo
import com.cs565project.smart.util.DbUtils
import com.cs565project.smart.util.PreferencesHelper
import com.cs565project.smart.util.UsageStatsUtil
import java.util.*
import java.util.concurrent.Executors

/**
 * The main service of the app. It updates the database with usage data, monitors the current app
 * (and blocks it if necessary), and updates news items, recommended activities etc.
 */
class AppMonitorService : Service() {

    private var myUsageStatsUtil: UsageStatsUtil? = null
    private var myOverlay: BlockOverlay? = null
    private var isRunning = false
    private var notificationTitle = "SMART is starting up"
    private var notificationText = "You day summary will appear here"
    private var updateNotification = false
    private var myCurrentApp: String? = null
    private var myBlockBypassed = false

    private val myExecutor = Executors.newFixedThreadPool(3)
    private val myHandler = Handler()
    private val myBgJob = Runnable {
        val currentApp = myUsageStatsUtil!!.foregroundApp

        // Adding/removing overlay should happen in the main thread.
        if (currentApp != null && shouldBlockApp(currentApp)) {
            if (!myBlockBypassed || currentApp != myCurrentApp) {
                val dao = AppDatabase.getAppDatabase(this@AppMonitorService).appDao()
                val details = dao.getAppDetails(currentApp)

                myOverlay!!.setApp(details, AppInfo(currentApp, applicationContext).appIcon)
                if (!myOverlay!!.isVisible) {
                    myHandler.post(myShowOverlay)
                }
            }

        } else if (currentApp != null && "android" != currentApp && myOverlay!!.isVisible) {
            myHandler.post(myHideOverlay)
        }

        myCurrentApp = currentApp

        if (isRunning) {
            myHandler.postDelayed(myBgJobStarter, CYCLE_DELAY.toLong())
        }
    }
    private val myBgJobStarter: BgStarter = BgStarter(myBgJob)

    private val myUpdateDb = Runnable {
        Log.d("SMART", "Updating app usage data")
        val restrictedAppsStatus = DbUtils.updateAndGetRestrictedAppsStatus(this@AppMonitorService)
        var timeInRestrictedApps: Long = 0
        var exceededApps = 0
        for (appStatus in restrictedAppsStatus) {
            timeInRestrictedApps += appStatus.second.totalTimeInForeground
            if (appStatus.second.totalTimeInForeground >= appStatus.first.thresholdTime) {
                exceededApps += 1
            }
        }

        // Update notification title and text.
        notificationText = String.format(Locale.getDefault(), getString(R.string.time_in_restricted_apps),
                UsageStatsUtil.formatDuration(timeInRestrictedApps, this@AppMonitorService))
        notificationTitle = if (exceededApps <= 0)
            "Good job!"
        else
            String.format(Locale.getDefault(), getString(R.string.limit_exceeded), exceededApps)
        updateNotification = true

        if (isRunning) {
            myHandler.postDelayed(myDbJobStarter, DATA_UPDATE_DELAY.toLong())
        }
    }
    private val myDbJobStarter: BgStarter = BgStarter(myUpdateDb)

    private val myUpdateNews = Runnable {
        Log.d("SMART", "Updating news")
        val recommendedNews = NewsItem.getRecommendedNews(this@AppMonitorService)
        if (recommendedNews != null && !recommendedNews.isEmpty()) {
            myOverlay!!.setNewsItems(recommendedNews)
        }
        myOverlay!!.setActivities(ActivityRecommender.getRecommendedActivities(
                AppDatabase.getAppDatabase(this@AppMonitorService).appDao().recommendationActivities
        ))
        if (isRunning) {
            myHandler.postDelayed(myNewsJobStarter, NEWS_UPDATE_DELAY.toLong())
        }
    }
    private val myNewsJobStarter: BgStarter = BgStarter(myUpdateNews)

    private val myShowOverlay = Runnable {
        myOverlay!!.scrollToStart()
        myOverlay!!.execute()
    }
    private val myHideOverlay = Runnable { myOverlay!!.remove() }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        myUsageStatsUtil = UsageStatsUtil(this)
        myOverlay = BlockOverlay(this, getSystemService(Context.WINDOW_SERVICE) as WindowManager)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val action = intent.action
            if (ACTION_START_SERVICE == action) {
                start()
            } else if (ACTION_STOP_SERVICE == action) {
                stop()
            } else if (ACTION_TOGGLE_SERVICE == action) {
                if (isRunning) stop() else start()
            } else if (ACTION_BYPASS_BLOCK == action) {
                myBlockBypassed = true
                myOverlay!!.remove()
            }
        }
        super.onStartCommand(intent, flags, startId)
        return Service.START_STICKY
    }

    private fun stop() {
        isRunning = false
        PreferencesHelper.setPreference(this, KEY_SERVICE_RUNNING, false)
        myHandler.removeCallbacks(myBgJobStarter)
        myHandler.removeCallbacks(myDbJobStarter)
        myHandler.removeCallbacks(myNewsJobStarter)
    }

    private fun start() {
        if (isRunning) {
            return
        }

        isRunning = true
        PreferencesHelper.setPreference(this, KEY_SERVICE_RUNNING, true)
        myHandler.post(myBgJobStarter)
        myHandler.post(myDbJobStarter)
        myHandler.post(myNewsJobStarter)
    }

    private fun postNotification(title: String, text: String) {
        createNotificationChannel()
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pi = PendingIntent.getActivity(this, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val n = notificationBuilder.setContentTitle(title).setSmallIcon(R.mipmap.ic_launcher).setContentText(text).setPriority(NotificationCompat.PRIORITY_MIN).setContentIntent(pi).build()
        startForeground(1, n)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            val name = getString(R.string.channel_name)
            val description = getString(R.string.channel_description)
            val channel = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_MIN)
            channel.description = description
            // Register the channel with the system
            val notificationManager = (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun shouldBlockApp(packageName: String?): Boolean {
        if (!PreferencesHelper.getBoolPreference(this,
                        GeneralSettingsFragment.PREF_ALLOW_APP_BLOCK.key, true)) {
            return false
        }
        val dao = AppDatabase.getAppDatabase(this@AppMonitorService).appDao()
        val details = packageName?.let { dao.getAppDetails(it) }
        val appUsage = packageName?.let { dao.getAppUsage(it, Date(UsageStatsUtil.getStartOfDayMillis(Date()))) }

        return details != null && appUsage != null &&
                details.thresholdTime > 0 && details.thresholdTime < appUsage.dailyUseTime

    }

    private inner class BgStarter internal constructor(private val bgJob: Runnable) : Runnable {

        override fun run() {
            myExecutor.execute(bgJob)
            if (updateNotification) {
                postNotification(notificationTitle, notificationText)
                updateNotification = false
            }
        }
    }

    companion object {

        private const val CHANNEL_ID = "persistent"

        const val ACTION_START_SERVICE = "start_service"
        const val ACTION_STOP_SERVICE = "stop_service"
        const val ACTION_TOGGLE_SERVICE = "toggle_service"
        const val ACTION_BYPASS_BLOCK = "bypass_block"
        const val KEY_SERVICE_RUNNING = "service_running"

        private const val CYCLE_DELAY = 200
        private const val DATA_UPDATE_DELAY = 20000
        private const val NEWS_UPDATE_DELAY = DateUtils.HOUR_IN_MILLIS.toInt()
    }
}
