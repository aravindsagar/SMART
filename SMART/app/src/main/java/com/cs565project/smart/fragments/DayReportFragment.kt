package com.cs565project.smart.fragments


import android.annotation.SuppressLint
import android.app.Activity
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import com.cs565project.smart.R
import com.cs565project.smart.db.AppDatabase
import com.cs565project.smart.fragments.adapter.ChartLegendAdapter
import com.cs565project.smart.recommender.RestrictionRecommender
import com.cs565project.smart.repository.model.PieAndLegendData
import com.cs565project.smart.util.DbUtils
import com.cs565project.smart.util.EmotionUtil
import com.cs565project.smart.util.UsageStatsUtil
import com.cs565project.smart.viewmodel.PieChartViewModel
import com.cs565project.smart.viewmodel.ViewModelFactory
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.utils.ColorTemplate.rgb
import java.util.*
import java.util.concurrent.Executors

/**
 * Fragment to show per-day reports to users.
 */
class DayReportFragment : Fragment(), ChartLegendAdapter.OnItemClickListener, View.OnKeyListener, SetRestrictionFragment.OnDurationSelectedListener {

    // References to views.
    private var myRootView: View? = null
    private var myPieChart: PieChart? = null
    private var myPieChartSecondary: PieChart? = null
    private var myRefreshLayout: SwipeRefreshLayout? = null
    private var myLegend: RecyclerView? = null

    // Our ViewModel which provides data for our views.
    private var mViewModel: PieChartViewModel? = null

    // Our data.
    private var myPieAndLegendData: LiveData<PieAndLegendData>? = null

    // Current args for which data is displayed.
    private var myDate: Date? = null
    var currentCategory: String? = null
        private set

    // Some helper fields.
    private var myPieX: Int = 0
    private var myMinimizedPieX: Int = 0
    private var myAnimatePie: Boolean = false

    // For background execution.
    private val myExecutor = Executors.newSingleThreadExecutor()
    private val myHandler = Handler()
    private val myInterpolator = AccelerateDecelerateInterpolator()
    private var myEmotionUtil: EmotionUtil? = null

    private val isInSecondaryView: Boolean
        get() = currentCategory != null && !currentCategory!!.isEmpty()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        myEmotionUtil = EmotionUtil(activity!!)

        // Inflate the layout for this fragment
        myRootView = inflater.inflate(R.layout.fragment_day_report, container, false)

        // Get references to the required views.
        myRefreshLayout = myRootView!!.findViewById(R.id.swipe_refresh)
        myPieChart = myRootView!!.findViewById(R.id.pie_chart)
        myPieChartSecondary = myRootView!!.findViewById(R.id.pie_chart_secondary)
        myLegend = myRootView!!.findViewById(R.id.pie_categories_list)

        // Listen for layout completion, so that we can start animations.
        myPieChart!!.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                init()
                myPieChart!!.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })

        mViewModel = ViewModelProviders
                .of(this@DayReportFragment, ViewModelFactory(activity!!.application))
                .get(PieChartViewModel::class.java)

        return myRootView
    }

    private fun init() {
        // Get position and size of our pie-chart and derive animation translation and scaling.
        myMinimizedPieX = (-PIE_SCALE_FACTOR * myPieChart!!.width).toInt()
        myPieX = myPieChart!!.x.toInt()

        // Init our views.
        myRefreshLayout!!.setOnRefreshListener({ this.onRefresh() })
        setupPieAndLegendView()
        // Populate some state. This calls setPieData internally.
        val category: String? = arguments?.getString(EXTRA_CATEGORY)
        myAnimatePie = arguments == null

        if (category == null || category.isEmpty()) {
            currentCategory = "invalid" // Ugly hack, ugh.
            switchToTotalView()
        } else {
            switchToPerCategoryView(category)
        }

        myRootView!!.isFocusableInTouchMode = true
        myRootView!!.requestFocus()
        myRootView!!.setOnKeyListener(this)
    }

    private fun onRefresh() {
        myAnimatePie = true
        setPieData()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupPieAndLegendView() {
        val context = activity ?: return
        for (pieChart in Arrays.asList<PieChart>(myPieChart, myPieChartSecondary)) {
            pieChart.description.isEnabled = false
            pieChart.legend.isEnabled = false
            pieChart.setUsePercentValues(true)
            pieChart.setEntryLabelColor(Color.BLACK)
            pieChart.holeRadius = PIE_HOLE_RADIUS
            pieChart.transparentCircleRadius = PIE_HOLE_RADIUS + 5
            pieChart.setDrawCenterText(true)
            pieChart.setCenterTextSize(22f)
            pieChart.setDrawEntryLabels(false)
        }
        val existingPieListener = myPieChart!!.onTouchListener
        myPieChart!!.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                switchToTotalView()
            }
            existingPieListener.onTouch(v, event)
        }

        val layoutManager = LinearLayoutManager(context)
        myLegend!!.layoutManager = layoutManager
        val dividerItemDecoration = DividerItemDecoration(context, layoutManager.orientation)
        myLegend!!.addItemDecoration(dividerItemDecoration)
        myLegend!!.itemAnimator = DefaultItemAnimator()
        assert(arguments != null)
        myDate = Date(UsageStatsUtil.getStartOfDayMillis(
                Date(arguments!!.getLong(EXTRA_DATE, System.currentTimeMillis()))
        ))
    }

    private fun setPieData() {
        myRefreshLayout!!.isRefreshing = true
        val c: Activity = activity ?: return

        // Remove observers in the existing LiveData, since we're about to get a new LiveData.
        myPieAndLegendData?.removeObservers(this)
        myPieAndLegendData = mViewModel!!.getData(myDate!!, currentCategory)
        myPieAndLegendData?.observe(this, android.arch.lifecycle.Observer { updateViews(it) })
    }

    // Runnable to be run in the UI thread after our state has been updated.
    private fun updateViews(data: PieAndLegendData?) {
        val context = activity ?: return
        if (data == null) return

        // Update the main pie chart data.
        if (myAnimatePie) {
            myPieChart!!.animateY(600, Easing.EasingOption.EaseInOutQuad)
            myAnimatePie = false
        }
        myPieChart!!.data = data.pieData

        if (isInSecondaryView) {
            myPieChartSecondary!!.animateY(600, Easing.EasingOption.EaseInOutQuad)
            myPieChartSecondary!!.data = data.pieDataSecondary
        }

        // The center text shows duration, the current category being viewed, and the recorded mood.
        val centerTextDuration = SpannableString(UsageStatsUtil.formatDuration(data.totalUsageTime, context))
        centerTextDuration.setSpan(RelativeSizeSpan(1.4f), 0, centerTextDuration.length, 0)
        centerTextDuration.setSpan(StyleSpan(Typeface.BOLD), 0, centerTextDuration.length, 0)
        @Suppress("DEPRECATION")
        val centerTextCategory = if (isInSecondaryView)
            String.format(getString(R.string.duration_in_category), Html.fromHtml(currentCategory).toString())
        else
            getString(R.string.total)
        val centerTextMood = SpannableString(
                getString(R.string.mood) + " " + data.mood)
        val centerText = TextUtils.concat(centerTextDuration, "\n", centerTextCategory, "\n\n", centerTextMood)

        if (isInSecondaryView) {
            myPieChartSecondary!!.centerText = centerText
        } else {
            myPieChart!!.centerText = centerText
        }
        myPieChart!!.invalidate()
        myPieChartSecondary!!.invalidate()

        // Update the chart legend.
        var adapter: ChartLegendAdapter? = if (myLegend!!.adapter != null) myLegend!!.adapter as ChartLegendAdapter else null
        if (adapter == null) {
            adapter = ChartLegendAdapter(data.legendInfo, data.totalUsageTime, context, this@DayReportFragment)
            myLegend!!.adapter = adapter
        } else {
            adapter.setData(data.legendInfo, data.totalUsageTime)
        }
        myLegend!!.adapter.notifyDataSetChanged()

        // Hide any loading spinners.
        myRefreshLayout!!.isRefreshing = false
    }

    override fun onItemClick(position: Int) {
        switchToPerCategoryView(myPieAndLegendData!!.value!!.legendInfo[position].title)
    }

    override fun onItemLongClick(position: Int): Boolean {
        if (isInSecondaryView) {
            val legendInfo = myPieAndLegendData!!.value!!.legendInfo[position]
            myExecutor.execute {
                val c: FragmentActivity = activity ?: return@execute
                val dao = AppDatabase.getAppDatabase(c).appDao()
                val thresholdTime = RestrictionRecommender.recommendRestriction(
                        dao.getAppDetails(legendInfo.subTitle),
                        dao.getAppUsage(legendInfo.subTitle),
                        dao.allMoodLog,
                        HashSet(dao.getCategories(true))
                )
                myHandler.post {
                    SetRestrictionFragment
                            .newInstance(legendInfo.title, legendInfo.subTitle, thresholdTime.toLong())
                            .setListener(this@DayReportFragment)
                            .show(childFragmentManager, "SET_RESTRICTION")
                }
            }

            return true
        }
        return false
    }

    private fun switchToPerCategoryView(category: String) {
        // If we are already in details view, nothing to do.
        if (isInSecondaryView) {
            return
        }

        currentCategory = category
        myPieChart!!.animate().x(myMinimizedPieX.toFloat()).scaleX(PIE_SCALE_FACTOR).scaleY(PIE_SCALE_FACTOR)
                .setInterpolator(myInterpolator).start()
        myPieChartSecondary!!.animate().alpha(1f).scaleX(1f).scaleY(1f).setInterpolator(myInterpolator).start()
        setPieData()
    }

    private fun switchToTotalView() {
        goBack()
    }

    /**
     * Handle back button press. Return whether the back press was consumed by the fragment.
     */
    private fun goBack(): Boolean {
        if (!isInSecondaryView) {
            return false
        }

        // We are in details view. Go back to total view and consume the button press so that
        // our parent does not go back.
        currentCategory = null

        // Animations!
        myPieChart!!.animate().x(myPieX.toFloat()).scaleX(1f).scaleY(1f).setInterpolator(myInterpolator).start()
        myPieChartSecondary!!.animate().alpha(0f).scaleX(0f).scaleY(0f).setInterpolator(myInterpolator).start()
        setPieData()
        return true
    }

    override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
        return event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK && goBack()
    }

    override fun onDurationConfirmed(packageName: String?, duration: Long) {
        if (packageName == null) return
        val context: Context = activity ?: return
        val postSaveRunnable = Runnable {
            myHandler.post {
                if (activity != null) {
                    Toast.makeText(activity, "Restriction saved", Toast.LENGTH_SHORT).show()
                }
            }
        }
        myExecutor.execute(DbUtils.SaveRestrictionToDb(context, packageName, duration.toInt(),
                postSaveRunnable))
    }

    override fun onCancel() {

    }

    companion object {

        private const val EXTRA_DATE = "extra_date"
        private const val EXTRA_CATEGORY = "category"

        private const val PIE_HOLE_RADIUS = 80f
        private const val PIE_SCALE_FACTOR = 0.3f

        val PIE_COLORS = intArrayOf(rgb("#bf360c"), rgb("#006064"), rgb("#5d4037"), rgb("#827717"), rgb("#f57f17"), rgb("#37474f"), rgb("#4a148c"), rgb("#ad1457"), rgb("#006064"), rgb("#0d47a1"), rgb("#fdd835"), rgb("#ff1744"), rgb("#000000"))
        private val MAX_ENTRIES = PIE_COLORS.size

        fun getInstance(dateInMillis: Long, category: String): DayReportFragment {
            val args = Bundle()
            args.putLong(EXTRA_DATE, dateInMillis)
            args.putString(EXTRA_CATEGORY, category)
            val fragment = DayReportFragment()
            fragment.arguments = args
            return fragment
        }
    }
}// Required empty public constructor
