package com.cs565project.smart.fragments


import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import com.cs565project.smart.MainActivity
import com.cs565project.smart.R
import com.cs565project.smart.util.EmotionUtil
import com.cs565project.smart.util.PreferencesHelper
import com.google.android.cameraview.CameraView
import java.util.*

/**
 * A fragment to log mood either via camera or manual entry.
 */
class LogMoodFragment : Fragment(), View.OnKeyListener, RadioGroup.OnCheckedChangeListener {


    private var myBackgroundHandler: Handler? = null
    private var myCameraView: CameraView? = null
    private var myMoodRadios: RadioGroup? = null
    private var myTakePicBtn: FloatingActionButton? = null
    private var myCameraRadio: RadioButton? = null
    private var myInputTypeGroup: RadioGroup? = null

    private var myEmotionUtil: EmotionUtil? = null

    private val myOnClickListener = View.OnClickListener { v ->
        if (v.id == R.id.take_pic) {
            // Record mood according to user's selection.
            if (myCameraView!!.visibility == View.VISIBLE) {
                myCameraView!!.takePicture()
            }
        }
    }

    private val backgroundHandler: Handler
        get() {
            if (myBackgroundHandler == null) {
                val thread = HandlerThread("background")
                thread.start()
                myBackgroundHandler = Handler(thread.looper)
            }
            return myBackgroundHandler!!
        }

    private val onCallback = object : CameraView.Callback() {

        override fun onPictureTaken(cameraView: CameraView, data: ByteArray) {

            Log.d(TAG, "onPictureTaken " + data.size)

            backgroundHandler.post { myEmotionUtil!!.processPicture(data) }

            switchToActivityTab()
            // Main thread idle now
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_log_mood, container, false)
        root.setOnKeyListener(this)

        myEmotionUtil = EmotionUtil(activity!!)

        myCameraView = root.findViewById(R.id.camera_view)
        myCameraView!!.facing = CameraView.FACING_FRONT
        myCameraView!!.addCallback(onCallback)

        myTakePicBtn = root.findViewById(R.id.take_pic)
        myTakePicBtn!!.setOnClickListener(myOnClickListener)

        myInputTypeGroup = root.findViewById(R.id.radio_group)
        myMoodRadios = root.findViewById(R.id.mood_level_radios)
        myInputTypeGroup!!.setOnCheckedChangeListener(this)
        myInputTypeGroup!!.check(R.id.take_photo_radio)
        myCameraRadio = root.findViewById(R.id.take_photo_radio)
        myMoodRadios!!.setOnCheckedChangeListener(this)
        return root
    }

    override fun onResume() {
        super.onResume()
        //        FragmentCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
        //                    REQUEST_CAMERA_PERMISSION);
        if (PreferencesHelper.getBoolPreference(activity!!,
                        GeneralSettingsFragment.PREF_ALLOW_PICTURES.key, true)) {
            myCameraView!!.start()
            myCameraRadio!!.isEnabled = true
        } else {
            myCameraRadio!!.isEnabled = false
            myInputTypeGroup!!.check(R.id.enter_manual_radio)
        }
    }

    override fun onPause() {
        super.onPause()
        myCameraView!!.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (myBackgroundHandler != null) {
            myBackgroundHandler!!.looper.quitSafely()
            myBackgroundHandler = null
        }
    }

    override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
        return false
    }

    override fun onCheckedChanged(group: RadioGroup, checkedId: Int) {
        if (group.id == R.id.radio_group) {
            myCameraView!!.visibility = if (checkedId == R.id.take_photo_radio) View.VISIBLE else View.GONE
            myTakePicBtn!!.visibility = if (checkedId == R.id.take_photo_radio) View.VISIBLE else View.GONE
            myMoodRadios!!.visibility = if (checkedId == R.id.enter_manual_radio) View.VISIBLE else View.GONE
        } else if (group.id == R.id.mood_level_radios && checkedId != -1) {
            val radioBtnIdx = myMoodRadios!!.indexOfChild(myMoodRadios!!.findViewById(checkedId))
            val moodLevel = (4 - radioBtnIdx) / 4.0
            backgroundHandler.post { myEmotionUtil!!.insertMoodLog(Arrays.asList(moodLevel, 0.0, 0.0, 0.0)) }
            myMoodRadios!!.clearCheck()
            switchToActivityTab()
        }
    }

    private fun switchToActivityTab() {
        if (activity == null) return
        val activity = activity as MainActivity?
        activity!!.switchToTab(0)
    }

    companion object {

        //    private static final int REQUEST_CAMERA_PERMISSION = 1;
        private val TAG = "CameraView"
    }
}// Required empty public constructor
