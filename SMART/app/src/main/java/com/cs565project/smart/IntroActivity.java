package com.cs565project.smart;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.widget.Toast;

import com.cs565project.smart.fragments.AccountFragment;
import com.cs565project.smart.fragments.PermissionsFragment;
import com.cs565project.smart.fragments.RecommendationSettingsFragment;
import com.cs565project.smart.fragments.RestrictionsFragment;
import com.cs565project.smart.util.UsageStatsUtil;
import com.github.paolorotolo.appintro.AppIntro;

public class IntroActivity extends AppIntro {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Note here that we DO NOT use setContentView();

        // Add your slide fragments here.
        // AppIntro will automatically generate the dots indicator and buttons.
        addSlide(new PermissionsFragment());
        addSlide(new AccountFragment());
        addSlide(new RestrictionsFragment());
        addSlide(new RecommendationSettingsFragment());


        // OPTIONAL METHODS
        // Override bar/separator color.
        setBarColor(Color.parseColor("#3F51B5"));
        setSeparatorColor(Color.parseColor("#2196F3"));

        // Hide Skip/Done button.
        showSkipButton(false);
        setProgressButtonEnabled(true);

        // Turn vibration on and set intensity.
        // NOTE: you will probably need to ask VIBRATE permission in Manifest.
        setVibrate(false);
        setVibrateIntensity(30);
    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        // Do something when users tap on Skip button.
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        // Do something when users tap on Done button.
        finish();
    }

    @Override
    public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {
        super.onSlideChanged(oldFragment, newFragment);
//        if(UsageStatsUtil.hasUsageAccess(this)) {
//            Toast.makeText(this, "Usage Access Permission is already set", Toast.LENGTH_SHORT).show();
//        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean hasUsageAccess = UsageStatsUtil.hasUsageAccess(this);
        setSwipeLock(!hasUsageAccess);
        nextButton.setEnabled(hasUsageAccess);

    }
}