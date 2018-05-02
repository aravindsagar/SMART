package com.cs565project.smart.fragments


import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.cs565project.smart.R
import com.cs565project.smart.SettingsActivity
import com.cs565project.smart.db.AppDatabase
import com.cs565project.smart.db.entities.AppDetails
import com.cs565project.smart.fragments.adapter.RestrictionsAdapter
import com.cs565project.smart.recommender.RestrictionRecommender
import com.cs565project.smart.util.AppInfo
import com.cs565project.smart.util.DbUtils
import com.cs565project.smart.util.PreferencesHelper
import java.util.*
import java.util.concurrent.Executors


/**
 * A simple [Fragment] subclass to display and modify app restrictions.
 */
class RestrictionsFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener, RestrictionsAdapter.OnItemSelectedListener, SetRestrictionFragment.OnDurationSelectedListener, View.OnKeyListener {

    // Views that we care about.
    private var myAppList: RecyclerView? = null
    private var mySwipeRefreshLayout: SwipeRefreshLayout? = null
    private var myNoBlockView: View? = null

    // Our state.
    private var restrictedApps: MutableList<AppDetails>? = null
    private var otherApps: MutableList<AppDetails>? = null
    private var recommendations: MutableList<Int>? = null
    private var appInfo: MutableMap<String, AppInfo>? = null

    // Helper fields.
    private val myHandler = Handler()
    private val myExecutor = Executors.newSingleThreadExecutor()

    // Runnable to load data in background.
    private val loadData = Runnable {
        if (activity == null) return@Runnable
        val c = activity

        val dao = AppDatabase.getAppDatabase(c!!).appDao()

        restrictedApps = ArrayList()
        otherApps = ArrayList()
        appInfo = HashMap()
        recommendations = ArrayList()

        val categoriesToRestrict = HashSet(dao.getCategories(true))
        val moodLogs = dao.allMoodLog

        for (appDetails in dao.appDetails) {
            appInfo!![appDetails.packageName] = AppInfo(appDetails.packageName, c)

            val recommendation = RestrictionRecommender.recommendRestriction(
                    appDetails,
                    dao.getAppUsage(appDetails.packageName),
                    moodLogs,
                    categoriesToRestrict
            )
            if (c.packageName != appDetails.packageName && (appDetails.thresholdTime >= 0 || recommendation > 0)) {
                restrictedApps!!.add(appDetails)
                recommendations!!.add(recommendation)
            } else {
                otherApps!!.add(appDetails)
            }
        }

        myHandler.post(postLoadData)
    }

    // Runnable to handle UI updates after loading data.
    private val postLoadData = Runnable {
        if (activity == null) return@Runnable

        myAppList!!.adapter = RestrictionsAdapter(restrictedApps!!, otherApps!!, appInfo!!,
                recommendations!!, this@RestrictionsFragment)
        myAppList!!.invalidate()

        mySwipeRefreshLayout!!.isRefreshing = false
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_restrictions, container, false)
        rootView.setOnKeyListener(this)
        myAppList = rootView.findViewById(R.id.list_restriction_apps)
        myAppList!!.layoutManager = LinearLayoutManager(activity)
        mySwipeRefreshLayout = rootView.findViewById(R.id.refresh_restrictions)
        mySwipeRefreshLayout!!.setOnRefreshListener(this)
        myNoBlockView = rootView.findViewById(R.id.no_app_block_view)

        setupAppList()

        val settingsBtn = rootView.findViewById<Button>(R.id.btn_settings)
        settingsBtn.setOnClickListener { v -> startActivity(Intent(activity, SettingsActivity::class.java)) }
        return rootView
    }

    private fun setupAppList() {
        mySwipeRefreshLayout!!.isRefreshing = true
        myExecutor.execute(loadData)
    }

    override fun onRefresh() {
        setupAppList()
    }

    override fun onItemSelected(appDetails: AppDetails, recommendation: Long) {
        SetRestrictionFragment.newInstance(appDetails.appName, appDetails.packageName, recommendation)
                .setListener(this)
                .show(childFragmentManager, "DURATION_PICKER")
    }

    override fun onDurationConfirmed(packageName: String?, duration: Long) {
        if (packageName == null) return
        if (activity == null) return
        mySwipeRefreshLayout!!.isRefreshing = true
        myExecutor.execute(DbUtils.SaveRestrictionToDb(activity!!, packageName, duration.toInt(), loadData))
    }

    override fun onResume() {
        super.onResume()
        if (PreferencesHelper.getBoolPreference(activity!!, GeneralSettingsFragment.PREF_ALLOW_APP_BLOCK.key, true)) {
            myNoBlockView!!.visibility = View.GONE
            myAppList!!.visibility = View.VISIBLE
        } else {
            myAppList!!.visibility = View.GONE
            myNoBlockView!!.visibility = View.VISIBLE
        }
    }

    override fun onCancel() {

    }

    override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
        return false
    }
}// Required empty public constructor
