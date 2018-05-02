package com.cs565project.smart.service

import android.Manifest
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.cs565project.smart.MainActivity
import com.cs565project.smart.R
import com.cs565project.smart.db.entities.AppDetails
import com.cs565project.smart.db.entities.RecommendationActivity
import com.cs565project.smart.fragments.GeneralSettingsFragment
import com.cs565project.smart.recommender.NewsItem
import com.cs565project.smart.util.EmotionUtil
import com.cs565project.smart.util.PreferencesHelper
import com.cs565project.smart.util.UsageStatsUtil
import com.google.android.cameraview.CameraView
import org.json.JSONException
import java.util.concurrent.Executors

/**
 * Overlay class to manage the blocking screen shown to users when they open a restricted app after
 * threshold time.
 */
internal class BlockOverlay(context: Context, windowManager: WindowManager) : OverlayBase(context, windowManager, R.layout.block_overlay), View.OnTouchListener, View.OnClickListener {

    private var myAppDetails: AppDetails? = null
    private var myDrawable: Drawable? = null
    private var myNewsItems: List<NewsItem>? = null
    private var myActivities: List<RecommendationActivity>? = null
    private var myWallpaper: Drawable? = null

    private val myExecutor = Executors.newSingleThreadExecutor()
    private val myHandler = Handler()
    private var myEmotionUtil: EmotionUtil

    private var page = 0
    private var scrollStartY = 0f
    private var scrollStartTime = 0f

    private val pageView: View
        get() = viewRoot.findViewById(PAGE_IDS[page])

    init {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            val wallpaperManager = WallpaperManager.getInstance(context)
            myWallpaper = wallpaperManager.drawable
            myNewsItems = emptyList()
            myActivities = emptyList()
        }
        myEmotionUtil = EmotionUtil(context)
    }

    private val myCameraCallback = object : CameraView.Callback() {
        override fun onCameraOpened(cameraView: CameraView) {
            super.onCameraOpened(cameraView)
            myHandler.postDelayed({ cameraView.takePicture() }, 1000L)
        }

        @Throws(JSONException::class)
        override fun onPictureTaken(cameraView: CameraView, data: ByteArray) {
            super.onPictureTaken(cameraView, data)
            myExecutor.execute { myEmotionUtil.processPicture(data) }
            cameraView.stop()
        }
    }

    fun setApp(appDetails: AppDetails, icon: Drawable?) {
        myAppDetails = appDetails
        myDrawable = icon
    }

    fun setNewsItems(newsItems: List<NewsItem>) {
        myNewsItems = newsItems
    }

    fun setActivities(activities: List<RecommendationActivity>) {
        myActivities = activities
    }

    internal override fun setupLayout(rootView: View) {
        rootView.background = myWallpaper

        val inflater = LayoutInflater.from(context)
        val news = rootView.findViewById<LinearLayout>(R.id.news_feed)
        news.removeAllViews()
        for (newsItem in myNewsItems!!) {
            val articleLayout = inflater.inflate(R.layout.list_item_news, news, false)
            val titleView = articleLayout.findViewById<TextView>(R.id.article_title)
            val sourceView = articleLayout.findViewById<TextView>(R.id.article_source)
            val iconView = articleLayout.findViewById<ImageView>(R.id.article_icon)

            titleView.text = newsItem.title
            sourceView.text = newsItem.publisher
            iconView.setImageDrawable(newsItem.icon)

            articleLayout.setOnClickListener {
                context.startActivity(Intent(Intent.ACTION_VIEW, newsItem.uri))
                remove()
            }

            news.addView(articleLayout)
        }

        val activityRecommendation = StringBuilder()
        val myActivitiesSize = myActivities!!.size - 1
        for (i in 0 until myActivitiesSize) {
            val activity = myActivities!![i]
            activityRecommendation.append(activity.activityName).append("\n")
        }
        if (myActivitiesSize >= 0) {
            activityRecommendation.append(myActivities!![myActivitiesSize].activityName)
        }
        val recommendationView = rootView.findViewById<TextView>(R.id.activity_suggestions)
        recommendationView.text = activityRecommendation.toString()

        if (myAppDetails == null) return

        val appView = rootView.findViewById<TextView>(R.id.app_name)
        appView.text = myAppDetails!!.appName

        val iconView = rootView.findViewById<ImageView>(R.id.app_icon)
        iconView.setImageDrawable(myDrawable)

        val usageLimit = rootView.findViewById<TextView>(R.id.usage_limit)
        usageLimit.text = String.format(context.getString(R.string.usage_limit_reached),
                UsageStatsUtil.formatDuration(myAppDetails!!.thresholdTime.toLong(), context))

        val overrideMessage = rootView.findViewById<TextView>(R.id.override_block_message)
        overrideMessage.text = String.format(context.getString(R.string.override_message),
                myAppDetails!!.appName,
                UsageStatsUtil.formatDuration(myAppDetails!!.thresholdTime.toLong(), context))

        val continueButton = rootView.findViewById<Button>(R.id.continue_to_app)
        val goHome = rootView.findViewById<Button>(R.id.go_home)
        val detailsButton = rootView.findViewById<TextView>(R.id.more_details)
        continueButton.setOnClickListener(this)
        goHome.setOnClickListener(this)
        rootView.setOnTouchListener(this)
        detailsButton.setOnClickListener(this)

        if (PreferencesHelper.getBoolPreference(context,
                        GeneralSettingsFragment.PREF_ALLOW_BLOCK_BYPASS.key, true)) {
            continueButton.visibility = View.VISIBLE
        } else {
            continueButton.visibility = View.GONE
        }

        try {
            val cameraView = rootView.findViewById<CameraView>(R.id.overlay_camera_view)
            if (cameraView != null) {
                if (PreferencesHelper.getBoolPreference(context,
                                GeneralSettingsFragment.PREF_ALLOW_PICTURES.key, true)) {
                    cameraView.visibility = View.VISIBLE
                    cameraView.facing = CameraView.FACING_FRONT
                    cameraView.addCallback(myCameraCallback)
                    cameraView.start()
                } else {
                    cameraView.visibility = View.GONE
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    @Suppress("DEPRECATION")
    internal override fun buildLayoutParams(): WindowManager.LayoutParams {
        val overlayType = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT

        return WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT, overlayType,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT)
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (v.id == R.id.news_feed || v.id == R.id.overlay_btns || v.id == R.id.more_details) {
            v.onTouchEvent(event)
        }
        if (event.action == MotionEvent.ACTION_DOWN) {
            scrollStartY = event.y
            scrollStartTime = event.eventTime.toFloat()
        } else if (event.action == MotionEvent.ACTION_MOVE) {
            pageView.translationY = event.y - scrollStartY
        } else if (event.action == MotionEvent.ACTION_UP) {
            val velocity = (event.y - scrollStartY) / (event.eventTime - scrollStartTime)
            if (!fling(velocity)) {
                scrollStartY = 0f
                pageView.animate().translationY(0f).setDuration(200).start()
            }
        }
        return true
    }

    private fun fling(velocity: Float): Boolean {
        if (Math.abs(velocity) < FLING_THRESHOLD) {
            return false
        }

        val oldPage: View
        var animateDistance = displayHeight
        if (velocity < 0 && page < PAGE_IDS.size - 1) {
            // Swipe down.
            oldPage = pageView
            page++
            animateDistance = -animateDistance
        } else if (velocity > 0 && page > 0) {
            oldPage = pageView
            page--
        } else {
            return false
        }

        oldPage.animate().translationY(animateDistance.toFloat()).start()

        pageView.translationY = (-animateDistance).toFloat()
        pageView.visibility = View.VISIBLE
        pageView.animate().translationY(0f).setDuration(200).start()

        return true
    }

    fun scrollToStart() {
        while (page > 0) {
            fling((FLING_THRESHOLD + 1).toFloat())
        }
    }

    override fun onClick(v: View) {
        when (v.id){
            R.id.go_home -> {
                val startMain = Intent(Intent.ACTION_MAIN)
                startMain.addCategory(Intent.CATEGORY_HOME)
                startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(startMain)
            }
            R.id.continue_to_app -> {
                val serviceIntent = Intent(context, AppMonitorService::class.java).setAction(AppMonitorService.ACTION_BYPASS_BLOCK)
                context.startService(serviceIntent)
            }
            R.id.more_details -> context.startActivity(Intent(context, MainActivity::class.java))
        }
    }

    override fun remove() {
        try {
            val cameraView = viewRoot.findViewById<CameraView>(R.id.overlay_camera_view)
            if (cameraView != null && cameraView.isCameraOpened) {
                cameraView.stop()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        super.remove()
    }

    companion object {
        private const val FLING_THRESHOLD = 2
        private val PAGE_IDS = intArrayOf(R.id.overlay_page_1, R.id.overlay_page_2)
    }
}
