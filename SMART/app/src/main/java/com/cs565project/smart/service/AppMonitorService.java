package com.cs565project.smart.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Pair;
import android.view.WindowManager;

import com.cs565project.smart.MainActivity;
import com.cs565project.smart.R;
import com.cs565project.smart.db.AppDao;
import com.cs565project.smart.db.AppDatabase;
import com.cs565project.smart.db.entities.AppDetails;
import com.cs565project.smart.db.entities.DailyAppUsage;
import com.cs565project.smart.fragments.GeneralSettingsFragment;
import com.cs565project.smart.recommender.ActivityRecommender;
import com.cs565project.smart.recommender.NewsItem;
import com.cs565project.smart.util.AppInfo;
import com.cs565project.smart.util.DbUtils;
import com.cs565project.smart.util.PreferencesHelper;
import com.cs565project.smart.util.UsageStatsUtil;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * The main service of the app. It updates the database with usage data, monitors the current app
 * (and blocks it if necessary), and updates news items, recommended activities etc.
 */
public class AppMonitorService extends Service {

    private static final String CHANNEL_ID = "persistent";

    public static final String ACTION_START_SERVICE = "start_service";
    public static final String ACTION_STOP_SERVICE = "stop_service";
    public static final String ACTION_TOGGLE_SERVICE = "toggle_service";
    public static final String ACTION_BYPASS_BLOCK = "bypass_block";
    public static final String KEY_SERVICE_RUNNING = "service_running";

    private static final int CYCLE_DELAY = 200;
    private static final int DATA_UPDATE_DELAY = 10000;
    private static final int NEWS_UPDATE_DELAY = (int) DateUtils.HOUR_IN_MILLIS;

    private UsageStatsUtil myUsageStatsUtil;
    private BlockOverlay myOverlay;
    private boolean isRunning = false;
    private String notificationTitle = "SMART is starting up";
    private String notificationText = "You day summary will appear here";
    private boolean updateNotification = false;
    private String myCurrentApp;
    private boolean myBlockBypassed = false;

    private Executor myExecutor = Executors.newFixedThreadPool(3);
    private Handler myHandler = new Handler();
    private Runnable myBgJob = new Runnable() {
        @Override
        public void run() {
            String currentApp = myUsageStatsUtil.getForegroundApp();

            // Adding/removing overlay should happen in the main thread.
            if (currentApp != null && shouldBlockApp(currentApp)) {
                if (!myBlockBypassed || !currentApp.equals(myCurrentApp)) {
                    AppDao dao = AppDatabase.getAppDatabase(AppMonitorService.this).appDao();
                    AppDetails details = dao.getAppDetails(currentApp);

                    myOverlay.setApp(details, new AppInfo(currentApp, getApplicationContext()).getAppIcon());
                    if (!myOverlay.isVisible()) {
                        myHandler.post(myShowOverlay);
                    }
                }

            } else if (currentApp != null && !"android".equals(currentApp) && myOverlay.isVisible()) {
                myHandler.post(myHideOverlay);
            }

            myCurrentApp = currentApp;

            if (isRunning) {
                myHandler.postDelayed(myBgJobStarter, CYCLE_DELAY);
            }
        }
    };
    private Runnable myBgJobStarter = new BgStarter(myBgJob);

    private Runnable myUpdateDb = new Runnable() {
        @Override
        public void run() {
            Log.d("SMART", "Updating app usage data");
            List<Pair<AppDetails, UsageStatsUtil.ForegroundStats>> restrictedAppsStatus =
                    DbUtils.updateAndGetRestrictedAppsStatus(AppMonitorService.this);
            long timeInRestrictedApps = 0;
            int exceededApps = 0;
            for(Pair<AppDetails, UsageStatsUtil.ForegroundStats> appStatus : restrictedAppsStatus) {
                timeInRestrictedApps += appStatus.second.getTotalTimeInForeground();
                if (appStatus.second.getTotalTimeInForeground() >= appStatus.first.getThresholdTime()) {
                    exceededApps += 1;
                }
            }

            // Update notification title and text.
            notificationText = String.format(Locale.getDefault(), getString(R.string.time_in_restricted_apps),
                    UsageStatsUtil.formatDuration(timeInRestrictedApps, AppMonitorService.this));
            notificationTitle = (exceededApps <= 0) ?
                    "Good job!" :
                    String.format(Locale.getDefault(), getString(R.string.limit_exceeded), exceededApps);
            updateNotification = true;

            if (isRunning) {
                myHandler.postDelayed(myDbJobStarter, DATA_UPDATE_DELAY);
            }
        }
    };
    private Runnable myDbJobStarter = new BgStarter(myUpdateDb);

    private Runnable myUpdateNews = new Runnable() {
        @Override
        public void run() {
            Log.d("SMART", "Updating news");
            List<NewsItem> recommendedNews = NewsItem.getRecommendedNews(AppMonitorService.this);
            if (recommendedNews != null && !recommendedNews.isEmpty()) {
                myOverlay.setNewsItems(recommendedNews);
            }
            myOverlay.setActivities(ActivityRecommender.getRecommendedActivities(
                    AppDatabase.getAppDatabase(AppMonitorService.this).appDao().getRecommendationActivities()
            ));
            if (isRunning) {
                myHandler.postDelayed(myNewsJobStarter, NEWS_UPDATE_DELAY);
            }
        }
    };
    private Runnable myNewsJobStarter = new BgStarter(myUpdateNews);

    private Runnable myShowOverlay = new Runnable() {
        @Override
        public void run() {
            myOverlay.scrollToStart();
            myOverlay.execute();
        }
    };
    private Runnable myHideOverlay = new Runnable() {
        @Override
        public void run() {
            myOverlay.remove();
        }
    };

    public AppMonitorService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        myUsageStatsUtil = new UsageStatsUtil(this);
        myOverlay = new BlockOverlay(this, (WindowManager) getSystemService(WINDOW_SERVICE));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null) {
            String action = intent.getAction();
            if (ACTION_START_SERVICE.equals(action)) {
                start();
            } else if (ACTION_STOP_SERVICE.equals(action)) {
                stop();
            } else if (ACTION_TOGGLE_SERVICE.equals(action)) {
                if (isRunning) stop(); else start();
            } else if (ACTION_BYPASS_BLOCK.equals(action)) {
                myBlockBypassed = true;
                myOverlay.remove();
            }
        }
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    private void stop() {
        isRunning = false;
        PreferencesHelper.setPreference(this, KEY_SERVICE_RUNNING, false);
        myHandler.removeCallbacks(myBgJobStarter);
        myHandler.removeCallbacks(myDbJobStarter);
        myHandler.removeCallbacks(myNewsJobStarter);
    }

    private void start() {
        if (isRunning) {
            return;
        }

        isRunning = true;
        PreferencesHelper.setPreference(this, KEY_SERVICE_RUNNING, true);
        myHandler.post(myBgJobStarter);
        myHandler.post(myDbJobStarter);
        myHandler.post(myNewsJobStarter);
    }

    private void postNotification(String title, String text){
        createNotificationChannel();
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);
        Intent intent = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = notificationBuilder.setContentTitle(title).
                setSmallIcon(R.mipmap.ic_launcher).setContentText(text).setPriority(NotificationCompat.PRIORITY_MIN).
                setContentIntent(pi).build();
        startForeground(1, n);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_MIN);
            channel.setDescription(description);
            // Register the channel with the system
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(channel);
        }
    }

    private boolean shouldBlockApp(String packageName) {
        if (!PreferencesHelper.getBoolPreference(this,
                GeneralSettingsFragment.PREF_ALLOW_APP_BLOCK.getKey(), true)) {
            return false;
        }
        AppDao dao = AppDatabase.getAppDatabase(AppMonitorService.this).appDao();
        AppDetails details = dao.getAppDetails(packageName);
        DailyAppUsage appUsage = dao.getAppUsage(packageName, new Date(UsageStatsUtil.getStartOfDayMillis(new Date())));

        return details != null && appUsage != null &&
                details.getThresholdTime() > 0 && details.getThresholdTime() < appUsage.getDailyUseTime();

    }

    private class BgStarter implements Runnable {

        private Runnable bgJob;

        BgStarter(Runnable bgJob) {
            this.bgJob = bgJob;
        }

        @Override
        public void run() {
            myExecutor.execute(bgJob);
            if (updateNotification) {
                postNotification(notificationTitle, notificationText);
                updateNotification = false;
            }
        }
    }
}
