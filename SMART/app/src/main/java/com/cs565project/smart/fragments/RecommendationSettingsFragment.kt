package com.cs565project.smart.fragments


import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.LinearLayout
import com.cs565project.smart.R
import com.cs565project.smart.db.AppDatabase
import com.cs565project.smart.db.entities.RecommendationActivity
import com.cs565project.smart.recommender.ActivityRecommender.ACTIVITY_TYPE_ACADEMIC
import com.cs565project.smart.recommender.ActivityRecommender.ACTIVITY_TYPE_EXERCISE
import com.cs565project.smart.recommender.ActivityRecommender.ACTIVITY_TYPE_NEWS
import com.cs565project.smart.recommender.ActivityRecommender.ACTIVITY_TYPE_RELAX
import com.cs565project.smart.recommender.ActivityRecommender.KEY_DB_POPULATED
import com.cs565project.smart.util.PreferencesHelper
import java.util.*
import java.util.concurrent.Executors


/**
 * Fragment to get activity recommendation settings from the user. Used in onboarding and settings.
 */


class RecommendationSettingsFragment : Fragment(), View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private var root: View? = null
    private var recommendationHeaders: List<ImageView>? = null

    private var myExerciseActivities: List<RecommendationActivity>? = null
    private var myAcademicActivities: List<RecommendationActivity>? = null
    private var myRelaxActivites: List<RecommendationActivity>? = null
    private var myNewsTopics: List<RecommendationActivity>? = null

    private val myUIHandler = Handler()
    private val myExecutor = Executors.newSingleThreadExecutor()

    private val fetchActivityData = Runnable {
        val c = activity ?: return@Runnable

        val db = AppDatabase.getAppDatabase(c)
        if (!PreferencesHelper.getBoolPreference(c, KEY_DB_POPULATED, false)) {
            db.insertDefaultActivitiesIntoDb()
            PreferencesHelper.setPreference(c, KEY_DB_POPULATED, true)
        }

        myExerciseActivities = db.appDao().getRecommendationActivities(ACTIVITY_TYPE_EXERCISE)
        myAcademicActivities = db.appDao().getRecommendationActivities(ACTIVITY_TYPE_ACADEMIC)
        myRelaxActivites = db.appDao().getRecommendationActivities(ACTIVITY_TYPE_RELAX)
        myNewsTopics = db.appDao().getRecommendationActivities(ACTIVITY_TYPE_NEWS)

        myUIHandler.post {
            for (view in recommendationHeaders!!) {
                Log.d("Rec", "Setting onclick" + myExerciseActivities!!.size)
                view.setOnClickListener(this@RecommendationSettingsFragment)
            }
            recommendationHeaders!![0].callOnClick()
        }
    }

    private fun populateActivityList(activities: List<RecommendationActivity>) {
        val myActivitiesList = root!!.findViewById<LinearLayout>(R.id.scroll_layout)
        myActivitiesList.removeAllViews()
        for (activity in activities) {
            val newCB = CheckBox(context)
            newCB.text = activity.activityName
            newCB.isChecked = activity.isSet
            newCB.setOnCheckedChangeListener(this)
            newCB.tag = activity
            myActivitiesList.addView(newCB, CB_LAYOUT_PARAMS)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        root = inflater.inflate(R.layout.fragment_recommendation_settings, container, false)
        val exercise = root!!.findViewById<ImageView>(R.id.exercise_button)
        val academics = root!!.findViewById<ImageView>(R.id.academics_button)
        val relax = root!!.findViewById<ImageView>(R.id.relax_button)
        val news = root!!.findViewById<ImageView>(R.id.news_button)

        recommendationHeaders = Arrays.asList(exercise, academics, relax, news)
        myExecutor.execute(fetchActivityData)
        return root
    }

    override fun onClick(v: View) {
        val c = activity ?: return

        if (v is ImageView) {
            for (view in recommendationHeaders!!) {
                view.imageTintList = null
            }

            v.imageTintList = c.resources.getColorStateList(R.color.image_selected)
        }

        when (v.id) {
            R.id.exercise_button -> populateActivityList(myExerciseActivities!!)
            R.id.academics_button -> populateActivityList(myAcademicActivities!!)
            R.id.relax_button -> populateActivityList(myRelaxActivites!!)
            R.id.news_button -> populateActivityList(myNewsTopics!!)
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        val tag = buttonView.tag
        if (tag == null || tag !is RecommendationActivity) return
        val activity = buttonView.tag as RecommendationActivity
        activity.isSet = isChecked
        myExecutor.execute {
            context?.let { AppDatabase.getAppDatabase(it).appDao().updateRecommendationActivity(activity) }
        }
    }

    companion object {
        private val CB_LAYOUT_PARAMS = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        init {
            CB_LAYOUT_PARAMS.setMargins(50, 10, 50, 10)
        }
    }
}// Required empty public constructor
