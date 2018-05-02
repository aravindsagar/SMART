package com.cs565project.smart.fragments.adapter

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView

import com.cs565project.smart.R
import com.cs565project.smart.util.UsageStatsUtil

/**
 * Adapter to populate chart legend.
 */
class ChartLegendAdapter(private var myLegendInfos: List<LegendInfo>?, private var myTotal: Long, private val myContext: Context,
                         private val myListener: OnItemClickListener) : RecyclerView.Adapter<ChartLegendAdapter.ViewHolder>() {

    fun setData(myLegendInfos: List<LegendInfo>, myTotal: Long) {
        this.myLegendInfos = myLegendInfos
        this.myTotal = myTotal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val root = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_pie_legend, parent, false)
        return ViewHolder(root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = myLegendInfos!![position]

        // Setup the background progress bar.
        if (myTotal > 0) {
            holder.progressBar.progress = (entry.usageTime * 100 / myTotal).toInt()
        }
        val color = entry.color
        val lighterColor = Color.argb(50, Color.red(color), Color.green(color), Color.blue(color))
        holder.progressDrawable.setColorFilter(lighterColor, PorterDuff.Mode.MULTIPLY)

        // Set the legend color box.
        holder.legendColorBox.drawable.setColorFilter(color, PorterDuff.Mode.MULTIPLY)

        // Set title and subtitle.
        holder.title.text = Html.fromHtml(entry.title)
        holder.subtitle.text = Html.fromHtml(entry.subTitle)

        // Set app icon if available.
        if (entry.icon != null) {
            holder.appIcon.setImageDrawable(entry.icon)
            holder.appIcon.visibility = View.VISIBLE
        } else {
            holder.appIcon.visibility = View.GONE
        }

        // Set duration.
        holder.duration.text = UsageStatsUtil.formatDuration(entry.usageTime, myContext)

        holder.root.setOnClickListener { v -> myListener.onItemClick(position) }
        holder.root.setOnLongClickListener { v -> myListener.onItemLongClick(position) }
    }

    override fun getItemCount(): Int {
        return myLegendInfos!!.size
    }

    class ViewHolder(var root: View) : RecyclerView.ViewHolder(root) {
        var progressBar: ProgressBar
        var progressDrawable: Drawable
        var legendColorBox: ImageView
        var appIcon: ImageView
        var title: TextView
        var subtitle: TextView
        var duration: TextView

        init {
            progressBar = root.findViewById(R.id.legend_progress_bar)
            progressDrawable = (progressBar.progressDrawable as LayerDrawable).findDrawableByLayerId(android.R.id.progress)
            legendColorBox = root.findViewById(R.id.legend_color_box)
            title = root.findViewById(R.id.legend_title)
            subtitle = root.findViewById(R.id.legend_subtitle)
            duration = root.findViewById(R.id.legend_duration)
            appIcon = root.findViewById(R.id.legend_app_icon)
        }
    }

    class LegendInfo(title: String, subTitle: String, internal var icon: Drawable?, internal var usageTime: Long, internal var color: Int) {
        var title: String
            internal set
        var subTitle: String
            internal set

        init {
            this.title = title
            this.subTitle = subTitle
        }
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int)

        fun onItemLongClick(position: Int): Boolean
    }
}
