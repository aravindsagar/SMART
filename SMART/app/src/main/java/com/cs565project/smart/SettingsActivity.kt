package com.cs565project.smart

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Pair
import com.cs565project.smart.fragments.GeneralSettingsFragment
import com.cs565project.smart.fragments.RecommendationSettingsFragment
import java.util.*

/**
 * Settings activity. We have 2 tabs: general settings and activity recommendation settings.
 */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        val sectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

        // Set up the ViewPager with the sections adapter.
        val viewPager = findViewById<ViewPager>(R.id.container)
        viewPager.adapter = sectionsPagerAdapter

        val tabLayout = findViewById<TabLayout>(R.id.tabs)
        tabLayout.setupWithViewPager(viewPager)
    }

    /**
     * A [FragmentPagerAdapter] that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    inner class SectionsPagerAdapter internal constructor(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        private val FRAGMENT_CLASSES = Arrays.asList(
                Pair<Class<out Fragment>, String>(GeneralSettingsFragment::class.java, "GENERAL"),
                Pair<Class<out Fragment>, String>(RecommendationSettingsFragment::class.java, "RECOMMENDATIONS")
        )

        override fun getItem(position: Int): Fragment {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            try {
                return FRAGMENT_CLASSES[position].first.newInstance()
            } catch (e: InstantiationException) {
                throw RuntimeException("Error instantiating fragment", e)
            } catch (e: IllegalAccessException) {
                throw RuntimeException("Error instantiating fragment", e)
            }

        }

        override fun getCount(): Int {
            // Show 2 total pages.
            return FRAGMENT_CLASSES.size
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return FRAGMENT_CLASSES[position].second
        }
    }
}
