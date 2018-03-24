package com.cs565project.smart;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Pair;

import com.cs565project.smart.fragments.ReportsFragment;
import com.cs565project.smart.fragments.RestrictionsFragment;

import java.util.Arrays;
import java.util.List;

/**
 * Adapter to populate tabs on the main activity.
 * We have 2 tabs: one to show user's activity reports, and one where the user can set blocking
 * parameters. See FRAGMENT_CLASSES to see the fragments that are returned.
 * Created by aravind on 3/22/18.
 */

public class MainTabsAdapter extends FragmentPagerAdapter {

    private static List<Pair<Class<? extends Fragment>, String>> FRAGMENT_CLASSES = Arrays.asList(
            new Pair<Class<? extends Fragment>, String>(ReportsFragment.class, "ACTIVITY REPORTS"),
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
        return 2;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return FRAGMENT_CLASSES.get(position).second;
    }
}
