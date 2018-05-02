package com.cs565project.smart


import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.GravityCompat
import android.support.v4.view.ViewPager
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Pair
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.cs565project.smart.MainActivity.MainTabsAdapter
import com.cs565project.smart.fragments.LogMoodFragment
import com.cs565project.smart.fragments.ReportsFragment
import com.cs565project.smart.fragments.RestrictionsFragment
import com.cs565project.smart.service.AppMonitorService
import com.cs565project.smart.util.PreferencesHelper
import java.util.*

/**
 * The main entry point into the app. We have 2 tabs, managed by a TabLayout. Corresponding views
 * are displayed by a ViewPager, to whom appropriate fragments are supplied by
 * [MainTabsAdapter].
 */
class MainActivity : AppCompatActivity(), View.OnClickListener {

    private var myDrawer: DrawerLayout? = null
    private var myToggleButton: TextView? = null
    private var isServiceRunning: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Start our background service. Without this nothing will work!
        startService(Intent(this, AppMonitorService::class.java).setAction(AppMonitorService.ACTION_START_SERVICE))

        //  Declare a new thread to do a preference check and start intro activity if required.
        val t = Thread {

            //  Create a new boolean and preference and set it to true
            val isFirstStart = PreferencesHelper.getBoolPreference(this@MainActivity, KEY_FIRST_START, true)

            //  If the activity has never started before...
            if (isFirstStart) {

                //  Launch app intro
                val i = Intent(this@MainActivity, IntroActivity::class.java)

                runOnUiThread {
                    startActivity(i)
                    finish()
                }
            }
        }

        // Start the thread
        t.start()

        setUpNavigation()

        // Setup the tabs.
        val tabs = findViewById<TabLayout>(R.id.tabs)
        val pager = findViewById<ViewPager>(R.id.main_viewpager)

        val adapter = MainTabsAdapter(supportFragmentManager)
        pager.adapter = adapter
        tabs.setupWithViewPager(pager)
    }

    /**
     * Setup our toolbar and navigation drawer.
     */
    private fun setUpNavigation() {

        // First we set our toolbar and add toggle button for nav drawer.
        val toolbar = findViewById<Toolbar>(R.id.main_toolbar)
        setSupportActionBar(toolbar)
        myDrawer = findViewById(R.id.drawer_layout)
        val toggle = ActionBarDrawerToggle(
                this, myDrawer, toolbar, R.string.app_name, R.string.app_name)
        myDrawer!!.addDrawerListener(toggle)
        toggle.syncState()

        // Setup the menu items inside the nav drawer. Going with individual elements instead of a
        // menu since this will allow easier dynamic changes.
        isServiceRunning = PreferencesHelper.getBoolPreference(this, AppMonitorService.KEY_SERVICE_RUNNING, true)
        val logout = findViewById<TextView>(R.id.logout)
        val settings = findViewById<TextView>(R.id.settings)
        val viewIntro = findViewById<TextView>(R.id.view_intro)
        myToggleButton = findViewById(R.id.toggle_service)
        logout.setOnClickListener(this)
        settings.setOnClickListener(this)
        viewIntro.setOnClickListener(this)
        myToggleButton!!.setOnClickListener(this)
        setToggleDrawable()
    }

    private fun setToggleDrawable() {
        val toggleDrawable: Drawable?
        if (isServiceRunning) {
            toggleDrawable = getDrawable(R.drawable.ic_pause)
            myToggleButton!!.setText(R.string.pause_service)
        } else {
            toggleDrawable = getDrawable(R.drawable.ic_play_arrow)
            myToggleButton!!.setText(R.string.resume_service)
        }
        myToggleButton!!.setCompoundDrawablesWithIntrinsicBounds(toggleDrawable, null, null, null)
    }

    override fun onResume() {
        super.onResume()
        isServiceRunning = PreferencesHelper.getBoolPreference(this, AppMonitorService.KEY_SERVICE_RUNNING, true)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.logout -> {
                Toast.makeText(this, "Logging out", Toast.LENGTH_SHORT).show()
                myDrawer!!.closeDrawer(GravityCompat.START)
            }
            R.id.settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                myDrawer!!.closeDrawer(GravityCompat.START)
            }
            R.id.toggle_service -> {
                startService(Intent(this, AppMonitorService::class.java)
                        .setAction(AppMonitorService.ACTION_TOGGLE_SERVICE))
                myDrawer!!.closeDrawer(GravityCompat.START)
                isServiceRunning = !isServiceRunning
                setToggleDrawable()
            }
            R.id.view_intro -> {
                val introIntent = Intent(this, IntroActivity::class.java)
                startActivity(introIntent)
                myDrawer!!.closeDrawer(GravityCompat.START)
            }
        }
    }

    /**
     * Adapter to populate tabs on the main activity.
     * We have 2 tabs: one to show user's activity reports, and one where the user can set blocking
     * parameters. See FRAGMENT_CLASSES to see the fragments that are returned.
     * Created by aravind on 3/22/18.
     */
    class MainTabsAdapter internal constructor(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            try {
                return FRAGMENT_CLASSES[position].first.newInstance()
            } catch (e: InstantiationException) {
                throw RuntimeException("Error instantiating fragment", e)
            } catch (e: IllegalAccessException) {
                throw RuntimeException("Error instantiating fragment", e)
            }

        }

        override fun getCount(): Int {
            return FRAGMENT_CLASSES.size
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return FRAGMENT_CLASSES[position].second
        }

        companion object {

            private val FRAGMENT_CLASSES = Arrays.asList(
                    Pair<Class<out Fragment>, String>(ReportsFragment::class.java, "YOUR ACTIVITY"),
                    Pair<Class<out Fragment>, String>(RestrictionsFragment::class.java, "RESTRICTIONS"),
                    Pair<Class<out Fragment>, String>(LogMoodFragment::class.java, "LOG YOUR MOOD")
            )
        }
    }

    override fun onBackPressed() {
        if (myDrawer!!.isDrawerOpen(GravityCompat.START)) {
            myDrawer!!.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    fun switchToTab(index: Int) {
        val tabLayout = findViewById<TabLayout>(R.id.tabs)
        val tab = tabLayout.getTabAt(index)
        if (tab != null) {
            try {
                tab.select()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }

        }
    }

    companion object {
        const val KEY_FIRST_START = "firstStart"
    }
}
