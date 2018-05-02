package com.cs565project.smart.fragments


import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.ImageView
import android.widget.ProgressBar
import com.cs565project.smart.R
import com.cs565project.smart.db.AppDatabase
import com.cs565project.smart.db.entities.AppDetails
import com.cs565project.smart.db.entities.Category
import com.cs565project.smart.util.AppInfo
import com.cs565project.smart.util.DbUtils.KEY_APPS_UPDATED_IN_DB
import com.cs565project.smart.util.PreferencesHelper
import com.cs565project.smart.util.UsageStatsUtil
import java.util.*
import java.util.concurrent.Executors

/**
 * Fragment to get user's restriction preferences during onboarding.
 */
class OnboardingRestrictionsFragment : Fragment() {

    private var loading: ProgressBar? = null
    private var appGrid: GridView? = null

    private var selectedApps: MutableSet<AppDetails>? = null

    private val myHandler = Handler()

    private val loadData = Runnable {
        val c = context ?: return@Runnable

        while (!PreferencesHelper.getBoolPreference(c, KEY_APPS_UPDATED_IN_DB, false)) {
            try {
                Thread.sleep(500)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

        }

        // Populate our grid with most used apps from last week.
        val mostUsedApps = UsageStatsUtil(c).mostUsedAppsLastWeek
        val appListSize = Math.min(30, mostUsedApps.size)
        val packagesToFetch = ArrayList<String>(appListSize)
        for (i in 0 until appListSize) {
            packagesToFetch.add(mostUsedApps[i].packageName)
        }

        val appDetails = AppDatabase.getAppDatabase(c).appDao().getAppDetails(packagesToFetch)
        val appInfos = ArrayList<AppInfo>(appDetails.size)
        val selected = BooleanArray(appDetails.size)
        var i = 0
        val appDetailsSize = appDetails.size
        while (i < appDetailsSize) {
            val details = appDetails[i]
            appInfos.add(AppInfo(details.packageName, c))
            selected[i] = false
            i++
        }
        selectedApps = HashSet()

        myHandler.post {
            loading!!.visibility = View.GONE
            appGrid!!.visibility = View.VISIBLE
            appGrid!!.adapter = AppsAdapter(appDetails, appInfos)
            appGrid!!.setOnItemClickListener { parent, view, position, id ->
                if (view is ImageView) {
                    if (selected[position]) {
                        selectedApps!!.remove(appDetails[position])
                        selected[position] = false
                        view.imageTintList = null
                    } else {
                        selectedApps!!.add(appDetails[position])
                        selected[position] = true
                        view.imageTintList = parent.context.resources.getColorStateList(R.color.image_selected)
                        view.imageTintMode = PorterDuff.Mode.ADD
                    }
                }
            }
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_onboarding_restrictions, container, false)
        loading = root.findViewById(R.id.loading_progress_bar)
        appGrid = root.findViewById(R.id.apps_grid)
        loading!!.visibility = View.VISIBLE
        appGrid!!.visibility = View.GONE
        Executors.newSingleThreadExecutor().execute(loadData)
        return root
    }

    fun saveData() {
        object : Thread() {
            override fun run() {
                val c = activity ?: return

                val dao = AppDatabase.getAppDatabase(c).appDao()
                val categories = HashSet<String>()
                for (appDetails in selectedApps!!) {
                    categories.add(appDetails.category)
                }

                for (category in categories) {
                    Log.d("Inserting", category)
                    dao.insertCategory(Category(category, true))
                }
            }
        }.start()
    }

    private class AppsAdapter(internal var appDetails: List<AppDetails>, internal var appInfos: List<AppInfo>) : BaseAdapter() {

        override fun getCount(): Int {
            return appDetails.size
        }

        override fun getItem(position: Int): Any {
            return appDetails[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var convertView = convertView
            if (convertView == null || convertView !is ImageView) {
                convertView = LayoutInflater.from(parent.context).inflate(R.layout.grid_item_app, parent, false)
            }
            (convertView as ImageView).setImageDrawable(appInfos[position].appIcon)
            return convertView
        }
    }
}// Required empty public constructor
