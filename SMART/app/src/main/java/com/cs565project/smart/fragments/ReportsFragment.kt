package com.cs565project.smart.fragments


import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.cs565project.smart.R
import com.cs565project.smart.db.AppDatabase
import com.cs565project.smart.util.UsageStatsUtil
import java.text.SimpleDateFormat
import java.util.*


/**
 * A simple [Fragment] subclass. This fragment displays user's activity reports.
 */
class ReportsFragment : Fragment(), View.OnClickListener, DatePickerFragment.OnDateSelectedListener, AdapterView.OnItemSelectedListener {

    private var myDatesText: TextView? = null
    private var myCurrentSpinnerItem = -1
    private var myStartDate: Date? = null
    private var mySingleSelectionDate: Date? = null
    private var myRangeSelectionStartDate: Date? = null
    private var myRangeSelectionEndDate: Date? = null
    private var myCategory = ""
    private var myApp = ""

    /**
     * A class for representing various report types.
     */
    private class ReportType internal constructor(internal var title: String, internal var selectRange: Boolean, internal var reportFragmentClass: Class<*>, internal var defaultStart: Date, internal var defaultEnd: Date?) {

        override fun toString(): String {
            return title
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        if (activity == null) {
            throw IllegalStateException("null context inside fragement; cannot continue.")
        }

        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_reports, container, false)
        val pickDateButton = root.findViewById<ImageView>(R.id.date_picker_button)
        pickDateButton.isEnabled = false
        pickDateButton.setOnClickListener(this)

        val reportSpinner = root.findViewById<Spinner>(R.id.report_view_spinner)
        val reportTypeArrayAdapter = ArrayAdapter(activity!!, android.R.layout.simple_spinner_item, REPORT_TYPES)
        reportTypeArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        reportSpinner.adapter = reportTypeArrayAdapter
        reportSpinner.onItemSelectedListener = this

        myDatesText = root.findViewById(R.id.date_selected_text)

        /*TextView selfieButton = root.findViewById(R.id.button_log_mood);
        selfieButton.setOnClickListener(this);*/

        // Restore from saved instance state if necessary.
        var spinnerPos = 0
        if (savedInstanceState != null) {
            val singleDate = savedInstanceState.getLong(KEY_SINGLE_DATE, 0)
            val startDate = savedInstanceState.getLong(KEY_START_DATE, 0)
            val endDate = savedInstanceState.getLong(KEY_END_DATE, 0)
            spinnerPos = savedInstanceState.getInt(KEY_SPINNER_POSITION, -1)
            val category = savedInstanceState.getString(KEY_CATEGORY)
            val app = savedInstanceState.getString(KEY_APP)

            if (startDate > 0) {
                myRangeSelectionStartDate = Date(startDate)
            }
            if (endDate > 0) {
                myRangeSelectionEndDate = Date(endDate)
            }
            if (singleDate > 0) {
                mySingleSelectionDate = Date(singleDate)
            }
            if (category != null) {
                myCategory = category
            }
            if (app != null) {
                myApp = app
            }
        }
        if (spinnerPos > -1) {
            reportSpinner.setSelection(spinnerPos)
            handleSpinnerItemChange(spinnerPos)
        } else {
            reportSpinner.setSelection(0)
            handleSpinnerItemChange(0)
        }

        // Setup the date picker fragment.
        object : Thread() {
            override fun run() {
                val dao = AppDatabase.getAppDatabase(activity!!).appDao()
                myStartDate = dao.usageDataStartDate
                if (activity == null) return
                activity!!.runOnUiThread { pickDateButton.isEnabled = true }
            }
        }.start()

        return root
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.date_picker_button -> {
                if (myStartDate == null) return
                val datePickerFragment = DatePickerFragment.getInstance(
                        REPORT_TYPES[myCurrentSpinnerItem].selectRange,
                        myStartDate!!.time,
                        UsageStatsUtil.tomorrowMillis
                ).setListener(this@ReportsFragment)
                datePickerFragment.show(childFragmentManager, FRAGMENT_DAY_TAG)
            }
        }
    }

    override fun onDateSelected(selectedDates: List<Date>) {
        if (selectedDates.isEmpty()) {
            return
        }

        var dateStr = DATE_FORMAT.format(selectedDates[0])
        if (selectedDates.size > 1) {
            dateStr += " - " + DATE_FORMAT.format(selectedDates[selectedDates.size - 1])

            myRangeSelectionStartDate = selectedDates[0]
            myRangeSelectionEndDate = selectedDates[selectedDates.size - 1]
        } else {
            mySingleSelectionDate = selectedDates[0]
        }
        myDatesText!!.text = dateStr

        // If myCategory is not empty, it means that we restored from saved state, and has
        // to get our child fragment into the same state. So we pass in the category. But we don't
        // want to enforce this category when user selects a different date, so unset the category.
        val category = myCategory
        val app = myApp
        myCategory = ""
        myApp = ""

        if (myCurrentSpinnerItem == 0) {
            childFragmentManager.beginTransaction().replace(
                    R.id.reports_child_frame,
                    DayReportFragment.getInstance(mySingleSelectionDate!!.time, category)
            ).commit()
        } else if (myCurrentSpinnerItem == 1) {
            childFragmentManager.beginTransaction().replace(
                    R.id.reports_child_frame,
                    AggregateReportFragment.getInstance(myRangeSelectionStartDate!!.time,
                            myRangeSelectionEndDate!!.time, category, app)
            ).commit()
        }
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
        if (position != myCurrentSpinnerItem) {
            handleSpinnerItemChange(position)
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>) {}

    private fun handleSpinnerItemChange(newPosition: Int) {
        myCurrentSpinnerItem = newPosition
        val curType = REPORT_TYPES[newPosition]

        val selectedDates: List<Date> = if (curType.defaultEnd == null) {
            // We are in single date selection mode.
            val singleDate = if (mySingleSelectionDate == null) curType.defaultStart else mySingleSelectionDate
            listOf(singleDate!!)
        } else {
            // We are in range date selection mode.
            val startDate = if (myRangeSelectionStartDate == null) curType.defaultStart else myRangeSelectionStartDate
            val endDate = if (myRangeSelectionEndDate == null) curType.defaultEnd else myRangeSelectionEndDate
            Arrays.asList<Date>(startDate, endDate)
        }
        onDateSelected(selectedDates)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (mySingleSelectionDate != null) {
            outState.putLong(KEY_SINGLE_DATE, mySingleSelectionDate!!.time)
        }
        if (myRangeSelectionStartDate != null) {
            outState.putLong(KEY_START_DATE, myRangeSelectionStartDate!!.time)
        }
        if (myRangeSelectionEndDate != null) {
            outState.putLong(KEY_END_DATE, myRangeSelectionEndDate!!.time)
        }
        outState.putInt(KEY_SPINNER_POSITION, myCurrentSpinnerItem)

        val fragment = childFragmentManager.findFragmentById(R.id.reports_child_frame)
        if (fragment is DayReportFragment) {
            outState.putString(KEY_CATEGORY, fragment.currentCategory)
        } else if (fragment is AggregateReportFragment) {
            outState.putString(KEY_CATEGORY, fragment.currentCategory)
            outState.putString(KEY_APP, fragment.currentApp)
        }
    }

    companion object {

        private const val FRAGMENT_DAY_TAG = "date_picker"
        private const val KEY_CATEGORY = "category"
        private const val KEY_APP = "app"

        /**
         * All the report types statically initialized.
         */
        private val REPORT_TYPES: List<ReportType>

        init {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_MONTH, -7)
            val startDate = calendar.time
            val endDate = Date()
            REPORT_TYPES = Arrays.asList(
                    ReportType("Daily", false, DayReportFragment::class.java, endDate, null),
                    ReportType("Over time", true, AggregateReportFragment::class.java, startDate, endDate)
            )
        }

        @SuppressLint("SimpleDateFormat")
        private val DATE_FORMAT = SimpleDateFormat("MMM dd")

        // Keys for saving data in state bundle.
        private const val KEY_SPINNER_POSITION = "spinner_position"
        private const val KEY_START_DATE = "start_date"
        private const val KEY_END_DATE = "end_date"
        private const val KEY_SINGLE_DATE = "single_date"
    }
}// Required empty public constructor
