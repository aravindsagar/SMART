package com.cs565project.smart

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import com.cs565project.smart.MainActivity.Companion.KEY_FIRST_START
import com.cs565project.smart.fragments.AccountFragment
import com.cs565project.smart.fragments.OnboardingRestrictionsFragment
import com.cs565project.smart.fragments.PermissionsFragment
import com.cs565project.smart.fragments.RecommendationSettingsFragment
import com.cs565project.smart.util.DbUtils.KEY_APPS_UPDATED_IN_DB
import com.cs565project.smart.util.PreferencesHelper
import com.cs565project.smart.util.UsageStatsUtil
import com.github.paolorotolo.appintro.AppIntro

/**
 * The onboarding activity.
 */
class IntroActivity : AppIntro() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Note here that we DO NOT use setContentView();

        // Add your slide fragments here.
        // AppIntro will automatically generate the dots indicator and buttons.
        addSlide(PermissionsFragment())
        addSlide(AccountFragment())
        addSlide(OnboardingRestrictionsFragment())
        addSlide(RecommendationSettingsFragment())


        // OPTIONAL METHODS
        // Override bar/separator color.
        setBarColor(Color.parseColor("#3F51B5"))
        setSeparatorColor(Color.parseColor("#2196F3"))

        // Hide Skip/Done button.
        showSkipButton(false)
        isProgressButtonEnabled = true

        // Turn vibration on and set intensity.
        // NOTE: you will probably need to ask VIBRATE permission in Manifest.
        setVibrate(false)
        setVibrateIntensity(30)
    }

    override fun onSkipPressed(currentFragment: Fragment?) {
        super.onSkipPressed(currentFragment)
        // Do something when users tap on Skip button.
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        if (PreferencesHelper.getBoolPreference(this, KEY_FIRST_START, true)) {
            //  Edit preference to make it false because we don't want this to run again
            PreferencesHelper.setPreference(this, KEY_FIRST_START, false)
            startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }

    override fun onSlideChanged(oldFragment: Fragment?, newFragment: Fragment?) {
        super.onSlideChanged(oldFragment, newFragment)
        if (newFragment is OnboardingRestrictionsFragment) {
            // Wait till our database is ready. Don't allow proceeding further till then.
            nextButton.isEnabled = false
            object : Thread() {
                override fun run() {
                    while (!PreferencesHelper.getBoolPreference(this@IntroActivity, KEY_APPS_UPDATED_IN_DB, false)) {
                        try {
                            Thread.sleep(500)
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }

                    }
                    runOnUiThread { nextButton.isEnabled = true }
                }
            }.start()
        }

        if (oldFragment is OnboardingRestrictionsFragment) {
            oldFragment.saveData()
        }
    }

    override fun onResume() {
        super.onResume()
        val hasUsageAccess = UsageStatsUtil.hasUsageAccess(this)
        setSwipeLock(!hasUsageAccess)
        nextButton.isEnabled = hasUsageAccess

    }
}
