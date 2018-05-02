package com.cs565project.smart.fragments


import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.SeekBar
import com.cs565project.smart.R
import com.cs565project.smart.util.PreferencesHelper.SeekbarPreference
import com.cs565project.smart.util.PreferencesHelper.SwitchPreference
import java.util.*


/**
 * General settings in Settings activity.
 */
class GeneralSettingsFragment : Fragment(), CompoundButton.OnCheckedChangeListener, SeekBar.OnSeekBarChangeListener {

    private var myRootView: View? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        myRootView = inflater.inflate(R.layout.fragment_general_settings, container, false)
        val context: Context = activity ?: return myRootView

        for (p in SWITCH_PREFERENCES) {
            p.setViewValueFromSavedPreference(myRootView!!, context)
            val s = p.findView(myRootView!!)
            s.setOnCheckedChangeListener(this)
            s.tag = p
        }
        for (p in SEEKBAR_PREFERENCES) {
            p.setViewValueFromSavedPreference(myRootView!!, context)
            p.setSecondaryViewValue(myRootView!!, context)
            val s = p.findView(myRootView!!)
            s.setOnSeekBarChangeListener(this)
            s.tag = p
        }
        return myRootView
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        context?.let { (buttonView.tag as SwitchPreference).saveViewValue(myRootView!!, it) }
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        val context = activity ?: return
        if (fromUser) {
            (seekBar.tag as SeekbarPreference).saveViewValue(myRootView!!, context)
        }
        (seekBar.tag as SeekbarPreference).setSecondaryViewValue(myRootView!!, context)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {}

    override fun onStopTrackingTouch(seekBar: SeekBar) {}

    companion object {

        /**
         * Our preferences.
         */
        val PREF_ALLOW_APP_BLOCK = SwitchPreference("allow_app_block", true, R.id.allow_block_switch)
        val PREF_ALLOW_BLOCK_BYPASS = SwitchPreference("allow_block_bypass", true, R.id.hard_block_switch)
        val PREF_ALLOW_PICTURES = SwitchPreference("allow_pictures", true, R.id.allow_picture_switch)
        val PREF_PICTURE_FREQ: SeekbarPreference = object : SeekbarPreference(
                "picture_freq", 1, R.id.picture_frequency_seekbar, R.id.picture_freq_text_view) {
            public override fun getSecondaryViewString(`val`: Int, c: Context): String {
                val base = c.getString(R.string.picture_frequency) + " "
                when (`val`) {
                    0 -> return c.getString(R.string.manual)
                    1 -> return base + c.getString(R.string.once_a_day)
                    2 -> return base + c.getString(R.string.twice_a_day)
                    else -> return base + `val` + " " + c.getString(R.string.n_times_a_day)
                }
            }
        }

        private val SWITCH_PREFERENCES = Arrays.asList(
                PREF_ALLOW_BLOCK_BYPASS, PREF_ALLOW_PICTURES, PREF_ALLOW_APP_BLOCK
        )
        private val SEEKBAR_PREFERENCES = listOf(PREF_PICTURE_FREQ)
    }
}
