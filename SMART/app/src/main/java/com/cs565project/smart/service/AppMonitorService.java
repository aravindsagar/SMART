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
import android.util.Log;
import android.view.WindowManager;

import com.cs565project.smart.MainActivity;
import com.cs565project.smart.R;
import com.cs565project.smart.util.UsageStatsUtil;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AppMonitorService extends Service {

    private static final String CHANNEL_ID = "persistent";

    public static final String ACTION_START_SERVICE = "start_service";
    public static final String ACTION_STOP_SERVICE = "stop_service";
    public static final String ACTION_TOGGLE_SERVICE = "toggle_service";

    private static final int CYCLE_DELAY = 200;
    private static final int DATA_UPDATE_DELAY = 60000;

    private UsageStatsUtil myUsageStatsUtil;
    private BlockOverlay myOverlay;
    private boolean isRunning = false;
    private String notificationTitle = "SMART is starting up";
    private String notificationText = "You day summary will appear here";
    private boolean updateNotification = false;
    private int cycleCount = DATA_UPDATE_DELAY/CYCLE_DELAY - 1;
    private Executor myExecutor = Executors.newSingleThreadExecutor();
    private Handler myHandler = new Handler();
    private Runnable myBgJobStarter = new Runnable() {
        @Override
        public void run() {
            myExecutor.execute(myBgJob);
            if (updateNotification) {
                postNotification(notificationTitle, notificationText);
                updateNotification = false;
            }
        }
    };

    private Runnable myBgJob = new Runnable() {
        @Override
        public void run() {
            cycleCount++;
            String currentApp = myUsageStatsUtil.getForegroundApp();
            // Log.d("CURRENT_APP", currentApp + "");

            // Adding/removing overlay should happen in the main thread.
            if ("com.google.android.apps.messaging".equals(currentApp)) {
                myHandler.post(myShowOverlay);

            } else if (!"android".equals(currentApp) && myOverlay.isVisible()) {
                myHandler.post(myHideOverlay);

            }

            if (cycleCount == DATA_UPDATE_DELAY/CYCLE_DELAY) {
                cycleCount = 0;
                Log.d("SMART", "Updating app usage data");
                // TODO collect app usage time and update db.
                // TODO update notification title and text.
                notificationText = "1:20 hours in restricted apps";
                notificationTitle = "Good job!";
                updateNotification = true;
            }
            // Schedule the app check to run again after a second.
            if (isRunning) {
                myHandler.postDelayed(myBgJobStarter, CYCLE_DELAY);
            }
        }
    };
    private Runnable myShowOverlay = new Runnable() {
        @Override
        public void run() {
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
            }
        }
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    private void stop() {
        isRunning = false;
        myHandler.removeCallbacks(myBgJobStarter, null);
    }

    private void start() {
        if (isRunning) {
            return;
        }

        isRunning = true;
        myHandler.post(myBgJobStarter);
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
}
