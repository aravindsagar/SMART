package com.cs565project.smart;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.cs565project.smart.fragments.ReportsFragment;
import com.cs565project.smart.fragments.RestrictionsFragment;

/**
 * Created by aravind on 3/22/18.
 */

public class MainTabsAdapter extends FragmentPagerAdapter {

    public MainTabsAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new ReportsFragment();
            case 1:
                return new RestrictionsFragment();
            default:
                throw new IllegalArgumentException("Invalid tab position");
        }
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return "ACTIVITY REPORTS";
            case 1:
                return "RESTRICTIONS";
            default:
                throw new IllegalArgumentException("Invalid tab position");
        }
    }
}
