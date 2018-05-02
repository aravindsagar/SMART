package com.cs565project.smart.fragments

import android.content.DialogInterface
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDialog
import android.support.v7.app.AppCompatDialogFragment
import com.cs565project.smart.R
import com.savvi.rangedatepicker.CalendarPickerView
import com.savvi.rangedatepicker.CalendarPickerView.SelectionMode.RANGE
import com.savvi.rangedatepicker.CalendarPickerView.SelectionMode.SINGLE
import java.util.*

/**
 * A dialog box for picking a date or a date range. Activities using this Dialog should implement
 * OnDateSelectedListener.
 */

class DatePickerFragment : AppCompatDialogFragment(), DialogInterface.OnClickListener {

    private var myListener: OnDateSelectedListener? = null
    private var myPicker: CalendarPickerView? = null

    /**
     * Callback interface to pass the selected date values back to the parent activity.
     */
    interface OnDateSelectedListener {
        fun onDateSelected(selectedDates: List<Date>)
    }

    fun setListener(listener: OnDateSelectedListener): DatePickerFragment {
        myListener = listener
        return this
    }

    /*@Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // Make sure that the container of this Fragment has implemented the callback interface.
        try {
            myListener = (OnDateSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement NoticeDialogListener");
        }
    }*/

    override fun onCreateDialog(savedInstanceState: Bundle?): AppCompatDialog {
        val args = arguments ?: throw IllegalStateException("Required arguments missing")
        val activity = activity ?: throw IllegalStateException("Parent activity cannot be null")

        val builder = AlertDialog.Builder(activity)

        val startDate = Date(args.getLong(KEY_START_DATE))
        val endDate = Date(args.getLong(KEY_END_DATE))
        myPicker = activity.layoutInflater.inflate(R.layout.date_range_picker, null) as CalendarPickerView
        myPicker!!.init(startDate, endDate)
                .inMode(if (args.getBoolean(KEY_SELECT_RANGE)) RANGE else SINGLE)
        myPicker!!.clearSelectedDates()
        myPicker!!.scrollToDate(endDate)

        builder.setView(myPicker)
                .setPositiveButton("OK", this)
                .setCancelable(true)
        return builder.create()
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        if (myListener != null) {
            myListener!!.onDateSelected(myPicker!!.selectedDates)
        }
    }

    companion object {

        private val KEY_SELECT_RANGE = "SELECT_RANGE"
        private val KEY_START_DATE = "START_DATE"
        private val KEY_END_DATE = "END_DATE"

        fun getInstance(selectRange: Boolean, startDate: Long, endDate: Long): DatePickerFragment {
            val fragment = DatePickerFragment()
            val bundle = Bundle()
            bundle.putBoolean(KEY_SELECT_RANGE, selectRange)
            bundle.putLong(KEY_START_DATE, startDate)
            bundle.putLong(KEY_END_DATE, endDate)
            fragment.arguments = bundle
            return fragment
        }
    }
}
