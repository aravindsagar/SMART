package com.cs565project.smart.service;

import android.Manifest;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cs565project.smart.MainActivity;
import com.cs565project.smart.R;
import com.cs565project.smart.db.entities.AppDetails;
import com.cs565project.smart.db.entities.RecommendationActivity;
import com.cs565project.smart.fragments.GeneralSettingsFragment;
import com.cs565project.smart.recommender.NewsItem;
import com.cs565project.smart.util.EmotionUtil;
import com.cs565project.smart.util.PreferencesHelper;
import com.cs565project.smart.util.UsageStatsUtil;
import com.google.android.cameraview.CameraView;

import org.json.JSONException;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

class BlockOverlay extends OverlayBase implements View.OnTouchListener, View.OnClickListener {
    private static final int FLING_THRESHOLD = 5;
    private static final int[] PAGE_IDS = {R.id.overlay_page_1, R.id.overlay_page_2};

    private AppDetails myAppDetails;
    private Drawable myDrawable;
    private List<NewsItem> myNewsItems;
    private List<RecommendationActivity> myActivities;
    private Drawable myWallpaper;

    private Executor myExecutor = Executors.newSingleThreadExecutor();
    private Handler myHandler = new Handler();
    private EmotionUtil myEmotionUtil;

    private int page = 0;
    private float scrollStartY = 0;
    private float scrollStartTime = 0;

    BlockOverlay(Context context, WindowManager windowManager) {
        super(context, windowManager, R.layout.block_overlay);
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(getContext());
            myWallpaper = wallpaperManager.getDrawable();
            myNewsItems = Collections.emptyList();
            myActivities = Collections.emptyList();
        }
        myEmotionUtil = new EmotionUtil(context);
    }

    public void setApp(AppDetails appDetails, Drawable icon) {
        myAppDetails = appDetails;
        myDrawable = icon;
    }

    public void setNewsItems(List<NewsItem> newsItems) {
        myNewsItems = newsItems;
    }

    public void setActivities(List<RecommendationActivity> activities) {
        myActivities = activities;
    }

    @Override
    void setupLayout(View rootView) {
        rootView.setBackground(myWallpaper);

        LayoutInflater inflater = LayoutInflater.from(getContext());
        LinearLayout news = rootView.findViewById(R.id.news_feed);
        news.removeAllViews();
        for (NewsItem newsItem : myNewsItems) {
            View articleLayout = inflater.inflate(R.layout.list_item_news, news, false);
            TextView titleView = articleLayout.findViewById(R.id.article_title),
                    sourceView = articleLayout.findViewById(R.id.article_source);
            ImageView iconView = articleLayout.findViewById(R.id.article_icon);

            titleView.setText(newsItem.getTitle());
            sourceView.setText(newsItem.getPublisher());
            iconView.setImageDrawable(newsItem.getIcon());

            articleLayout.setOnClickListener(v -> {
                    getContext().startActivity(new Intent(Intent.ACTION_VIEW, newsItem.getUri()));
                    remove();
            });

            news.addView(articleLayout);
        }

        StringBuilder activityRecommendation = new StringBuilder();
        int myActivitiesSize = myActivities.size() - 1;
        for (int i = 0; i < myActivitiesSize; i++) {
            RecommendationActivity activity = myActivities.get(i);
            activityRecommendation.append(activity.activityName).append(", ");
        }
        if (myActivitiesSize >= 0) {
            activityRecommendation.append(myActivities.get(myActivitiesSize).activityName);
        }
        TextView recommendationView = rootView.findViewById(R.id.activity_suggestions);
        recommendationView.setText(activityRecommendation.toString());

        if (myAppDetails == null) return;

        TextView appView = rootView.findViewById(R.id.app_name);
        appView.setText(myAppDetails.getAppName());

        ImageView iconView = rootView.findViewById(R.id.app_icon);
        iconView.setImageDrawable(myDrawable);

        TextView usageLimit = rootView.findViewById(R.id.usage_limit);
        usageLimit.setText(String.format(getContext().getString(R.string.usage_limit_reached),
                UsageStatsUtil.formatDuration(myAppDetails.getThresholdTime(), getContext())));

        TextView overrideMessage = rootView.findViewById(R.id.override_block_message);
        overrideMessage.setText(String.format(getContext().getString(R.string.override_message),
                myAppDetails.getAppName(),
                UsageStatsUtil.formatDuration(myAppDetails.getThresholdTime(), getContext())));

        Button continueButton = rootView.findViewById(R.id.continue_to_app),
                goHome = rootView.findViewById(R.id.go_home);
        TextView detailsButton = rootView.findViewById(R.id.more_details);
        continueButton.setOnClickListener(this);
        goHome.setOnClickListener(this);
        rootView.setOnTouchListener(this);
        detailsButton.setOnClickListener(this);

        if (PreferencesHelper.getBoolPreference(getContext(),
                GeneralSettingsFragment.PREF_ALLOW_BLOCK_BYPASS.getKey(), true)) {
            continueButton.setVisibility(View.VISIBLE);
        } else {
            continueButton.setVisibility(View.GONE);
        }

        try {
            CameraView cameraView = rootView.findViewById(R.id.overlay_camera_view);
            if (cameraView != null) {
                if (PreferencesHelper.getBoolPreference(getContext(),
                        GeneralSettingsFragment.PREF_ALLOW_PICTURES.getKey(), true)) {
                    cameraView.setVisibility(View.VISIBLE);
                    cameraView.setFacing(CameraView.FACING_FRONT);
                    cameraView.addCallback(myCameraCallback);
                    cameraView.start();
                } else {
                    cameraView.setVisibility(View.GONE);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    WindowManager.LayoutParams buildLayoutParams() {
        int overlayType = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;

        return new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT, overlayType,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v.getId() == R.id.news_feed || v.getId() == R.id.overlay_btns || v.getId() == R.id.more_details) {
            v.onTouchEvent(event);
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            scrollStartY = event.getY();
            scrollStartTime = event.getEventTime();
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            getPageView().setTranslationY(event.getY() - scrollStartY);
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            float velocity = (event.getY()-scrollStartY)/(event.getEventTime()-scrollStartTime);
            if (!fling(velocity)) {
                scrollStartY = 0;
                getPageView().animate().translationY(0).setDuration(100).start();
            }
        }
        return true;
    }

    private boolean fling(float velocity) {
        if (Math.abs(velocity) < FLING_THRESHOLD) {
            return false;
        }

        View oldPage;
        int animateDistance = getDisplayHeight();
        if (velocity < 0 && page < PAGE_IDS.length-1) {
            // Swipe down.
            oldPage = getPageView();
            page++;
            animateDistance = -animateDistance;
        } else if (velocity > 0 && page > 0){
            oldPage = getPageView();
            page--;
        } else {
            return false;
        }

        oldPage.animate().translationY(animateDistance).start();

        getPageView().setTranslationY(-animateDistance);
        getPageView().setVisibility(View.VISIBLE);
        getPageView().animate().translationY(0).start();

        return true;
    }

    private View getPageView() {
        return getViewRoot().findViewById(PAGE_IDS[page]);
    }

    public void scrollToStart() {
        while (page > 0) {
            fling(FLING_THRESHOLD + 1);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.go_home) {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(startMain);
        } else if (v.getId() == R.id.continue_to_app) {
            remove();
        } else if (v.getId() == R.id.more_details) {
            getContext().startActivity(new Intent(getContext(), MainActivity.class));
        }
    }

    @Override
    public void remove() {
        try {
            CameraView cameraView = getViewRoot().findViewById(R.id.overlay_camera_view);
            if (cameraView != null) {
                cameraView.stop();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.remove();
    }

    private CameraView.Callback myCameraCallback = new CameraView.Callback() {
        @Override
        public void onCameraOpened(CameraView cameraView) {
            super.onCameraOpened(cameraView);
            myHandler.postDelayed(cameraView::takePicture, 1000);
        }

        @Override
        public void onCameraClosed(CameraView cameraView) {
            super.onCameraClosed(cameraView);
        }

        @Override
        public void onPictureTaken(CameraView cameraView, byte[] data) throws JSONException {
            super.onPictureTaken(cameraView, data);
            myExecutor.execute(() -> myEmotionUtil.processPicture(data));
        }
    };
}
