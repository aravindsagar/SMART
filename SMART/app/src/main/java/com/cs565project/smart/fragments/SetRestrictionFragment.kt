package com.cs565project.smart.fragments

import android.content.DialogInterface
import android.os.Bundle
import android.support.v7.app.AppCompatDialog
import android.support.v7.app.AppCompatDialogFragment

import com.cs565project.smart.R

import mobi.upod.timedurationpicker.TimeDurationPicker
import mobi.upod.timedurationpicker.TimeDurationPickerDialog

/**
 * Dialog fragment to show a time duration picker.
 */
class SetRestrictionFragment : AppCompatDialogFragment(), TimeDurationPickerDialog.OnDurationSetListener {

    private var myAppName: String? = null
    private var myPackageName: String? = null
    protected var initialDuration: Long = 0
        private set

    private var mListener: OnDurationSelectedListener? = null

    fun setListener(listener: OnDurationSelectedListener): SetRestrictionFragment {
        mListener = listener
        return this
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            myAppName = arguments!!.getString(ARG_APP_NAME)
            initialDuration = arguments!!.getLong(ARG_DEFAULT_VAL)
            myPackageName = arguments!!.getString(ARG_PACKAGE_NAME)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): AppCompatDialog {
        val dialog = TimeDurationPickerDialog(activity!!, this, initialDuration, setTimeUnits())
        dialog.setTitle(String.format(getString(R.string.set_restriction_title), myAppName))
        return dialog
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    override fun onDurationSet(view: TimeDurationPicker, duration: Long) {
        if (mListener != null) {
            mListener!!.onDurationConfirmed(myPackageName, duration)
        }
    }

    override fun onCancel(dialog: DialogInterface?) {
        super.onCancel(dialog)
        if (mListener != null) {
            mListener!!.onCancel()
        }
    }

    protected fun setTimeUnits(): Int {
        return TimeDurationPicker.HH_MM
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    interface OnDurationSelectedListener {
        fun onDurationConfirmed(packageName: String?, duration: Long)

        fun onCancel()
    }

    companion object {

        // the fragment initialization parameter keys.
        private const val ARG_APP_NAME = "app_name"
        private const val ARG_PACKAGE_NAME = "package_name"
        private const val ARG_DEFAULT_VAL = "default_val"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         */
        fun newInstance(appName: String, packageName: String, defaultValue: Long): SetRestrictionFragment {
            val fragment = SetRestrictionFragment()
            val args = Bundle()
            args.putString(ARG_APP_NAME, appName)
            args.putLong(ARG_DEFAULT_VAL, defaultValue)
            args.putString(ARG_PACKAGE_NAME, packageName)
            fragment.arguments = args
            return fragment
        }
    }
}// Required empty public constructor
