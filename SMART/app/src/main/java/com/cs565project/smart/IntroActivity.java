package com.cs565project.smart;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.cs565project.smart.fragments.AccountFragment;
import com.cs565project.smart.fragments.OnboardingRestrictionsFragment;
import com.cs565project.smart.fragments.PermissionsFragment;
import com.cs565project.smart.fragments.RecommendationSettingsFragment;
import com.cs565project.smart.util.PreferencesHelper;
import com.cs565project.smart.util.UsageStatsUtil;
import com.github.paolorotolo.appintro.AppIntro;

import static com.cs565project.smart.MainActivity.KEY_FIRST_START;
import static com.cs565project.smart.util.DbUtils.KEY_APPS_UPDATED_IN_DB;

/**
 * The onboarding activity.
 */
public class IntroActivity extends AppIntro {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Note here that we DO NOT use setContentView();

        // Add your slide fragments here.
        // AppIntro will automatically generate the dots indicator and buttons.
        addSlide(new PermissionsFragment());
        addSlide(new AccountFragment());
        addSlide(new OnboardingRestrictionsFragment());
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
        if (PreferencesHelper.getBoolPreference(this, KEY_FIRST_START, true)) {
            //  Edit preference to make it false because we don't want this to run again
            PreferencesHelper.setPreference(this, KEY_FIRST_START, false);
            startActivity(new Intent(this, MainActivity.class));
        }
        finish();
    }

    @Override
    public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {
        super.onSlideChanged(oldFragment, newFragment);
        if (newFragment instanceof OnboardingRestrictionsFragment) {
            // Wait till our database is ready. Don't allow proceeding further till then.
            nextButton.setEnabled(false);
            new Thread() {
                @Override
                public void run() {
                    while(!PreferencesHelper.getBoolPreference(IntroActivity.this, KEY_APPS_UPDATED_IN_DB, false)) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    runOnUiThread(() -> {
                        nextButton.setEnabled(true);
                    });
                }
            }.start();
        }

        if (oldFragment instanceof OnboardingRestrictionsFragment) {
            ((OnboardingRestrictionsFragment) oldFragment).saveData();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean hasUsageAccess = UsageStatsUtil.hasUsageAccess(this);
        setSwipeLock(!hasUsageAccess);
        nextButton.setEnabled(hasUsageAccess);

    }
}
