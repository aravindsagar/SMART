package com.cs565project.smart;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Pair;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.cs565project.smart.fragments.ReportsFragment;
import com.cs565project.smart.fragments.RestrictionsFragment;
import com.cs565project.smart.service.AppMonitorService;

import java.util.Arrays;
import java.util.List;

/**
 * The main entry point into the app. We have 2 tabs, managed by a TabLayout. Corresponding views
 * are displayed by a ViewPager, to whom appropriate fragments are supplied by
 * {@link MainTabsAdapter}.
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setUpNavigation();

        // Setup the tabs.
        TabLayout tabs = findViewById(R.id.tabs);
        ViewPager pager = findViewById(R.id.main_viewpager);
        MainTabsAdapter adapter = new MainTabsAdapter(getSupportFragmentManager());
        pager.setAdapter(adapter);
        tabs.setupWithViewPager(pager);

        startService(new Intent(this, AppMonitorService.class).setAction(AppMonitorService.ACTION_START_SERVICE));
    }

    /**
     * Setup our toolbar and navigation drawer.
     */
    private void setUpNavigation() {

        // First we set our toolbar and add toggle button for nav drawer.
        Toolbar toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout myDrawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, myDrawer, toolbar, R.string.app_name, R.string.app_name);
        myDrawer.addDrawerListener(toggle);
        toggle.syncState();

        // Setup the menu items inside the nav drawer. Going with individual elements instead of a
        // menu since this will allow easier dynamic changes.
        TextView logout = findViewById(R.id.logout),
                settings = findViewById(R.id.settings),
                toggleService = findViewById(R.id.toggle_service);
        logout.setOnClickListener(this);
        settings.setOnClickListener(this);
        toggleService.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.logout:
                Toast.makeText(this, "Logging out", Toast.LENGTH_SHORT).show();
                break;
            case R.id.settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.toggle_service:
                startService(new Intent(this, AppMonitorService.class)
                        .setAction(AppMonitorService.ACTION_TOGGLE_SERVICE));
                break;
        }
    }

    /**
     * Adapter to populate tabs on the main activity.
     * We have 2 tabs: one to show user's activity reports, and one where the user can set blocking
     * parameters. See FRAGMENT_CLASSES to see the fragments that are returned.
     * Created by aravind on 3/22/18.
     */
    public static class MainTabsAdapter extends FragmentPagerAdapter {

        private static List<Pair<Class<? extends Fragment>, String>> FRAGMENT_CLASSES = Arrays.asList(
                new Pair<Class<? extends Fragment>, String>(ReportsFragment.class, "YOUR ACTIVITY"),
                new Pair<Class<? extends Fragment>, String>(RestrictionsFragment.class, "RESTRICTIONS")
        );

        MainTabsAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            try {
                return FRAGMENT_CLASSES.get(position).first.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("Error instantiating fragment", e);
            }
        }

        @Override
        public int getCount() {
            return FRAGMENT_CLASSES.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return FRAGMENT_CLASSES.get(position).second;
        }
    }
}
