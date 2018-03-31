package com.cs565project.smart.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.WindowManager;

import com.cs565project.smart.util.AppInfo;
import com.cs565project.smart.util.UsageStatsUtil;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AppMonitorService extends Service {
    public static final String ACTION_START_SERVICE = "start_service";
    public static final String ACTION_STOP_SERVICE = "stop_service";
    public static final String ACTION_TOGGLE_SERVICE = "toggle_service";

    private UsageStatsUtil myUsageStatsUtil;
    private BlockOverlay myOverlay;
    private boolean isRunning = false;
    private Executor myExecutor = Executors.newSingleThreadExecutor();
    private Handler myHandler = new Handler();
    private Runnable myBgJobStarter = new Runnable() {
        @Override
        public void run() {
            myExecutor.execute(myBgJob);
        }
    };

    private Runnable myBgJob = new Runnable() {
        @Override
        public void run() {
            String currentApp = myUsageStatsUtil.getForegroundApp();
            Log.d("CURRENT_APP", currentApp + "");

            // Adding/removing overlay should happen in the main thread.
            if ("com.google.android.apps.messaging".equals(currentApp)) {
                myHandler.post(myShowOverlay);

            } else if (!"android".equals(currentApp) && myOverlay.isVisible()) {
                myHandler.post(myHideOverlay);
            }

            // Schedule the app check to run again after a second.
            if (isRunning) {
                myHandler.postDelayed(myBgJobStarter, 1000);
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
}
