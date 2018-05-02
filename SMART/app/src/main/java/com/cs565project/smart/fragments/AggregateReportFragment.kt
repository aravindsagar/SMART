package com.cs565project.smart.fragments


import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.cs565project.smart.R
import com.cs565project.smart.db.AppDatabase
import com.cs565project.smart.db.entities.AppDetails
import com.cs565project.smart.fragments.adapter.ChartLegendAdapter
import com.cs565project.smart.recommender.RestrictionRecommender
import com.cs565project.smart.util.*
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate.rgb
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.HashSet
import java.util.concurrent.Executors
import kotlin.Comparator

/**
 * Fragment which displays aggregate reports.
 */
class AggregateReportFragment : Fragment(), ChartLegendAdapter.OnItemClickListener, View.OnKeyListener, SetRestrictionFragment.OnDurationSelectedListener {

    // References to views.
    private var myRootView: View? = null
    private var myChart: CombinedChart? = null
    private var myRefreshLayout: SwipeRefreshLayout? = null
    private var myLegend: RecyclerView? = null

    // Our state.
    private var myChartData: CombinedData? = null
    private var myLegendInfos: MutableList<ChartLegendAdapter.LegendInfo>? = null
    private var myTotalUsageTime: Long = 0
    private var myStartDate: Date? = null
    private var myEndDate: Date? = null
    var currentCategory: String? = null
        private set
    var currentApp: String? = null
        private set

    // For background execution.
    private val myExecutor = Executors.newSingleThreadExecutor()
    private val myHandler = Handler()
    private var myEmotionUtil: EmotionUtil? = null

    // Runnable to collect app usage information and update our state. Don't run in UI thread.
    private val loadData = object : Runnable {

        private val emotionData: LineData?
            get() {
                if (activity == null) return null
                val lineEntries = ArrayList<Entry>()
                for (i in 0..getDayIdx(myStartDate!!.time, myEndDate!!.time)) {
                    val dayMood = myEmotionUtil!!.getLatestMoodLog(
                            Date(myStartDate!!.time + i * DateUtils.DAY_IN_MILLIS)
                    ) ?: continue
                    lineEntries.add(Entry(
                            getDayIdx(myStartDate!!.time, UsageStatsUtil.getStartOfDayMillis(dayMood.date)).toFloat(),
                            dayMood.happyValue.toFloat() * 4))
                    Log.d("Mood", dayMood.happyValue.toString() + " in " + AXIS_DATE_FORMAT.format(Date(myStartDate!!.time + i * DateUtils.DAY_IN_MILLIS)))
                }
                if (lineEntries.isEmpty()) return null

                val dataSet = LineDataSet(lineEntries, "Mood")
                dataSet.axisDependency = YAxis.AxisDependency.RIGHT
                dataSet.lineWidth = 1.5f
                dataSet.color = Color.BLACK
                dataSet.setCircleColor(Color.BLACK)
                dataSet.valueTextColor = Color.argb(150, 0, 0, 0)
                val data = LineData(dataSet)
                data.setValueFormatter { value, _, _, _ -> myEmotionUtil!!.getEmoji(Math.round(value)) }
                data.setDrawValues(true)
                data.setValueTextSize(12f)
                return data
            }

        override fun run() {
            val c = activity ?: return
// Read usage info from DB.
            val dao = AppDatabase.getAppDatabase(c).appDao()
            val appUsages = dao.getAppUsage(myStartDate!!, myEndDate!!)

            // This map will hold the key value pairs to be inserted in the chart.
            val maxDayIdx = getDayIdx(myStartDate!!.time, myEndDate!!.time)
            val usageData = ArrayList<MutableMap<String, Long>>(maxDayIdx + 1)
            for (i in 0..maxDayIdx) usageData.add(mutableMapOf())
            val subtitleInfo: MutableMap<String, MutableList<String>> = mutableMapOf()
            val xSubVals: MutableSet<String> = mutableSetOf()
            val appDetailMap: MutableMap<String, AppDetails> = mutableMapOf()
            val totalUsageMap: MutableMap<String, Long> = mutableMapOf()

            // Initialize state with defaults.
            myLegendInfos = ArrayList()
            myTotalUsageTime = 0

            // Populate the usageMap.
            for (appUsage in appUsages) {
                if (c.packageName == appUsage.packageName) {
                    continue
                }

                val appDetails = dao.getAppDetails(appUsage.packageName)
                val category = appDetails.category

                val usageMap = usageData[getDayIdx(myStartDate!!.time, appUsage.date.time)]

                val key: String
                val value: Long
                if (isInAppView) {
                    if (currentApp != appDetails.packageName) {
                        continue
                    }
                    key = appDetails.packageName
                    value = appUsage.dailyUseTime
                    usageMap[key] = value
                } else if (isInCategoryView) {
                    if (currentCategory != appDetails.category) {
                        continue
                    }
                    key = appDetails.packageName
                    value = appUsage.dailyUseTime
                    usageMap[key] = value
                } else {
                    key = category
                    value = appUsage.dailyUseTime
                    if (usageMap.containsKey(key)) {
                        usageMap[key] = usageMap[key]!! + value
                        subtitleInfo[key]!!.add(appDetails.appName)
                    } else {
                        usageMap[key] = value
                        subtitleInfo[key] = ArrayList(setOf(appDetails.appName))
                    }
                }
                xSubVals.add(key)
                myTotalUsageTime += appUsage.dailyUseTime
                appDetailMap[appDetails.packageName] = appDetails
                if (totalUsageMap.containsKey(key)) {
                    totalUsageMap[key] = totalUsageMap[key]!! + value
                } else {
                    totalUsageMap[key] = value
                }
            }

            // Calculate chart data from the usage data.
            val entries = processUsageMap(usageData, subtitleInfo, xSubVals, appDetailMap, totalUsageMap)
            val dataSet = BarDataSet(entries, "Usage")

            // Chart colors!
            val colors = IntArray((maxDayIdx + 1) * xSubVals.size)
            var i = 0
            while (i < colors.size) {
                System.arraycopy(CHART_COLORS, 0, colors, i, Math.min(xSubVals.size, CHART_COLORS.size))
                for (j in 0 until xSubVals.size - CHART_COLORS.size) {
                    colors[i + CHART_COLORS.size + j] = CHART_COLORS[CHART_COLORS.size - 1]
                }
                i += xSubVals.size
            }
            dataSet.setColors(*colors)
            dataSet.axisDependency = YAxis.AxisDependency.LEFT
            val barData = BarData(dataSet)
            barData.setDrawValues(false)
            myChartData = CombinedData()
            myChartData!!.setData(barData)
            myChartData!!.setData(emotionData)
            myHandler.post(postLoadData)
        }

        private fun processUsageMap(usageData: List<Map<String, Long>>,
                                    subtitleInfo: Map<String, List<String>>, xSubValSet: Set<String>, appDetailMap: Map<String, AppDetails>, totalUsageMap: Map<String, Long>): List<BarEntry> {

            val xSubVals = ArrayList(xSubValSet)
            xSubVals.sortWith(Comparator { a, b -> totalUsageMap[b]!!.compareTo(totalUsageMap[a]!!) })

            // Output list.
            val entries = ArrayList<BarEntry>(usageData.size)
            val c = activity ?: return entries

            for (i in usageData.indices) {
                val usageMap = usageData[i]
                val vals = xSubVals.map { usageMap.getOrElse(it, { 0L }).toFloat() }
                entries.add(BarEntry(i.toFloat(), vals.toFloatArray()))
            }

            // Legend entries.
            myLegendInfos = ArrayList()
            var i = 0
            for (xSubVal in xSubVals) {
                val title: String
                val subtitle: String
                var icon: Drawable? = null
                if (isInAppView || isInCategoryView) {
                    val app = appDetailMap[xSubVal]
                    title = app?.appName ?: ""
                    subtitle = xSubVal
                    icon = AppInfo(xSubVal, c).appIcon
                } else {
                    title = xSubVal
                    subtitle = GraphUtil.buildSubtitle(c, subtitleInfo[xSubVal]!!)
                }
                myLegendInfos!!.add(ChartLegendAdapter.LegendInfo(title, subtitle, icon,
                        totalUsageMap[xSubVal]!!, CHART_COLORS[Math.min(i, CHART_COLORS.size - 1)]))
                i++
            }

            return entries
        }
    }

    // Runnable to be run in the UI thread after our state has been updated.
    private val postLoadData = Runnable {
        val c = activity ?: return@Runnable

        // Update the chart data.
        myChart!!.animateY(600, Easing.EasingOption.EaseInOutQuad)
        myChart!!.xAxis.axisMaximum = getDayIdx(myStartDate!!.time, myEndDate!!.time) + 0.5f
        myChart!!.data = myChartData

        myChart!!.invalidate()

        // Update the chart legend.
        if (myLegend!!.adapter == null) {
            myLegend!!.adapter = ChartLegendAdapter(myLegendInfos, myTotalUsageTime, c, this@AggregateReportFragment)
        } else {
            (myLegend!!.adapter as ChartLegendAdapter).setData(myLegendInfos!!, myTotalUsageTime)
        }
        myLegend!!.adapter.notifyDataSetChanged()

        // Hide any loading spinners.
        myRefreshLayout!!.isRefreshing = false
    }

    private val isInCategoryView: Boolean
        get() = currentCategory != null && !currentCategory!!.isEmpty() && !isInAppView

    private val isInAppView: Boolean
        get() = currentApp != null && !currentApp!!.isEmpty()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val context: Context = activity ?: throw IllegalStateException("Activity cannot be null")
        myEmotionUtil = EmotionUtil(context)

        // Inflate the layout for this fragment
        myRootView = inflater.inflate(R.layout.fragment_aggreagate_report, container, false)

        // Get references to the required views.
        myRefreshLayout = myRootView!!.findViewById(R.id.swipe_refresh)
        myChart = myRootView!!.findViewById(R.id.bar_chart)
        myLegend = myRootView!!.findViewById(R.id.legend)

        init()

        return myRootView
    }

    private fun init() {

        // Init our views.
        myRefreshLayout!!.setOnRefreshListener(SwipeRefreshLayout.OnRefreshListener { this.onRefresh() })
        setupChartAndLegendView()
        // Populate some state. This calls setChartData internally.
        val category: String? = arguments?.getString(EXTRA_CATEGORY)
        val app: String? = arguments?.getString(EXTRA_APP)

        if (app != null && !app.isEmpty()) {
            currentCategory = category
            switchtoPerAppView(app)
        } else if (category != null && !category.isEmpty()) {
            switchToPerCategoryView(category)
        } else {
            currentCategory = "invalid"
            currentApp = "invalid"
            switchToTotalView()
        }

        myRootView!!.isFocusableInTouchMode = true
        myRootView!!.requestFocus()
        myRootView!!.setOnKeyListener(this)
    }

    private fun onRefresh() {
        setChartData()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupChartAndLegendView() {
        val c = activity ?: return

        myChart!!.description.isEnabled = false
        myChart!!.legend.isEnabled = false
        val xAxis = myChart!!.xAxis
        xAxis.setValueFormatter { v, a -> AXIS_DATE_FORMAT.format(Date(myStartDate!!.time + v.toLong() * DateUtils.DAY_IN_MILLIS)) }
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.axisMinimum = -0.5f
        xAxis.granularity = 1.0f
        val yAxisLeft = myChart!!.axisLeft
        val yAxisRight = myChart!!.axisRight
        yAxisLeft.axisMinimum = 0f
        yAxisLeft.setValueFormatter { v, a -> UsageStatsUtil.formatDuration(v.toLong(), c) }
        yAxisLeft.setDrawGridLines(false)
        yAxisRight.axisMinimum = 0f
        yAxisRight.setValueFormatter { v, a -> myEmotionUtil!!.getEmoji(v.toInt()) }
        yAxisRight.setDrawGridLines(false)
        yAxisRight.granularity = 1f
        yAxisRight.axisMinimum = 0f
        yAxisRight.axisMaximum = 4.5f

        val layoutManager = LinearLayoutManager(c)
        myLegend!!.layoutManager = layoutManager
        val dividerItemDecoration = DividerItemDecoration(c, layoutManager.orientation)
        myLegend!!.addItemDecoration(dividerItemDecoration)
        myLegend!!.itemAnimator = DefaultItemAnimator()

        myStartDate = Date(UsageStatsUtil.getStartOfDayMillis(
                Date(arguments!!.getLong(EXTRA_START_DATE, System.currentTimeMillis()))))
        myEndDate = Date(UsageStatsUtil.getStartOfDayMillis(
                Date(arguments!!.getLong(EXTRA_END_DATE, System.currentTimeMillis()))))
    }

    private fun setChartData() {
        myRefreshLayout!!.isRefreshing = true
        myExecutor.execute(loadData)
    }

    override fun onItemClick(position: Int) {
        if (isInCategoryView) {
            switchtoPerAppView(myLegendInfos!![position].subTitle)
        } else {
            switchToPerCategoryView(myLegendInfos!![position].title)
        }
    }

    override fun onItemLongClick(position: Int): Boolean {
        val c = activity ?: return false
        if (isInCategoryView) {
            val legendInfo = myLegendInfos!![position]
            myExecutor.execute {
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
                            .setListener(this@AggregateReportFragment)
                            .show(childFragmentManager, "SET_RESTRICTION")
                }
            }

            return true
        }
        return false
    }

    private fun switchtoPerAppView(packageName: String) {
        if (isInAppView) {
            return
        }

        currentApp = packageName
        setChartData()
    }

    private fun switchToPerCategoryView(category: String) {
        // If we are already in details view, nothing to do.
        if (isInCategoryView) {
            return
        }

        currentCategory = category
        setChartData()
    }

    private fun switchToTotalView() {
        currentApp = ""
        currentCategory = ""
        setChartData()
    }

    /**
     * Handle back button press. Return whether the back press was consumed by the fragment.
     */
    fun goBack(): Boolean {

        if (isInAppView) {
            currentApp = ""
        } else if (isInCategoryView) {
            currentCategory = ""
        } else {
            return false
        }

        setChartData()
        return true
    }

    private fun getDayIdx(startDate: Long, date: Long): Int {
        return ((date - startDate) / DateUtils.DAY_IN_MILLIS).toInt()
    }

    override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
        return event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK && goBack()
    }

    override fun onDurationConfirmed(packageName: String?, duration: Long) {
        val context: Context = activity ?: return
        if (packageName == null) return

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

        private val EXTRA_START_DATE = "start_date"
        private val EXTRA_END_DATE = "end_date"
        private val EXTRA_CATEGORY = "category"
        private val EXTRA_APP = "app"

        private val CHART_COLORS = intArrayOf(rgb("#bf360c"), rgb("#006064"), rgb("#5d4037"), rgb("#827717"), rgb("#f57f17"), rgb("#37474f"), rgb("#4a148c"), rgb("#ad1457"), rgb("#006064"), rgb("#0d47a1"), rgb("#fdd835"), rgb("#ff1744"), rgb("#000000"))

        @SuppressLint("SimpleDateFormat")
        private val AXIS_DATE_FORMAT = SimpleDateFormat("MMM dd")

        fun getInstance(startDate: Long, endDate: Long, category: String, app: String): AggregateReportFragment {
            val args = Bundle()
            args.putLong(EXTRA_START_DATE, startDate)
            args.putLong(EXTRA_END_DATE, endDate)
            args.putString(EXTRA_CATEGORY, category)
            args.putString(EXTRA_APP, app)
            val fragment = AggregateReportFragment()
            fragment.arguments = args
            return fragment
        }
    }
}// Required empty public constructor
