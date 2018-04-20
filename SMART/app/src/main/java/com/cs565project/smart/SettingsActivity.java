package com.cs565project.smart;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Pair;

import com.cs565project.smart.fragments.GeneralSettingsFragment;
import com.cs565project.smart.fragments.RecommendationSettingsFragment;

import java.util.Arrays;
import java.util.List;

/**
 * Settings activity. We have 2 tabs: general settings and activity recommendation settings.
 */
public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        ViewPager viewPager = findViewById(R.id.container);
        viewPager.setAdapter(sectionsPagerAdapter);

        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        private List<Pair<Class<? extends Fragment>, String>> FRAGMENT_CLASSES = Arrays.asList(
                new Pair<Class<? extends Fragment>, String>(GeneralSettingsFragment.class, "GENERAL"),
                new Pair<Class<? extends Fragment>, String>(RecommendationSettingsFragment.class, "RECOMMENDATIONS")
        );

        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            try {
                return FRAGMENT_CLASSES.get(position).first.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("Error instantiating fragment", e);
            }
        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return FRAGMENT_CLASSES.size();
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
                return FRAGMENT_CLASSES.get(position).second;
        }
    }
}
