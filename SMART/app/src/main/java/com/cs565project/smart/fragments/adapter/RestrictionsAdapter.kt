package com.cs565project.smart.fragments.adapter

import android.support.v7.widget.RecyclerView
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.cs565project.smart.R
import com.cs565project.smart.db.entities.AppDetails
import com.cs565project.smart.util.AppInfo
import com.cs565project.smart.util.UsageStatsUtil

/**
 * Adapter to populate restrictions list.
 */
class RestrictionsAdapter(private val restrictedApps: List<AppDetails>, private val otherApps: List<AppDetails>,
                          private val appInfo: Map<String, AppInfo>, private val recommendations: List<Int>,
                          private val listener: OnItemSelectedListener) : RecyclerView.Adapter<RestrictionsAdapter.ViewHolder>() {

    private val restrictedHeaderPos: Int
    private val otherHeaderPos: Int

    init {

        restrictedHeaderPos = if (restrictedApps.size > 0) 0 else -1
        otherHeaderPos = restrictedHeaderPos + restrictedApps.size + 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_restriction, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val c = holder.itemView.context
        if (position <= restrictedApps.size + restrictedHeaderPos) {
            if (restrictedHeaderPos == position) {
                populateHeaderItem(holder, R.string.restricted_apps)
            } else {
                populateAppItem(holder, restrictedApps[position - restrictedHeaderPos - 1],
                        recommendations[position - restrictedHeaderPos - 1])
            }
        } else {
            if (otherHeaderPos == position) {
                populateHeaderItem(holder, R.string.other_apps)
            } else {
                populateAppItem(holder, otherApps[position - otherHeaderPos - 1], -1)
            }
        }
    }

    override fun getItemCount(): Int {
        return restrictedApps.size + otherApps.size + (if (restrictedApps.size > 0) 1 else 0) +
                if (otherApps.size > 0) 1 else 0
    }

    private fun populateHeaderItem(holder: ViewHolder, titleId: Int) {
        holder.icon.visibility = GONE
        holder.subtitle.visibility = GONE
        holder.restrictionText.visibility = GONE
        holder.recommendationText.visibility = GONE

        holder.title.setText(titleId)
        holder.title.setTextAppearance(holder.itemView.context, R.style.SettingsHeaderStyle)
    }

    private fun populateAppItem(holder: ViewHolder, appDetails: AppDetails, recommendation: Int) {
        val info = appInfo[appDetails.packageName]
        val context = holder.itemView.context

        holder.icon.visibility = View.VISIBLE

        holder.icon.setImageDrawable(info!!.appIcon)
        holder.title.text = Html.fromHtml(appDetails.appName)
        holder.title.setTextAppearance(holder.title.context, R.style.SettingsSwitchStyle)
        holder.subtitle.visibility = View.VISIBLE
        holder.subtitle.text = Html.fromHtml(appDetails.category)

        if (appDetails.thresholdTime > -1) {
            holder.restrictionText.visibility = View.VISIBLE
            holder.restrictionText.text = String.format(context.getString(R.string.restricted_val),
                    UsageStatsUtil.formatDuration(appDetails.thresholdTime.toLong(), holder.itemView.context))
        } else {
            holder.restrictionText.visibility = GONE
        }

        if (recommendation > -1) {
            holder.recommendationText.visibility = View.VISIBLE
            holder.recommendationText.text = String.format(context.getString(R.string.recommended_val),
                    UsageStatsUtil.formatDuration(recommendation.toLong(), context))
        } else {
            holder.recommendationText.visibility = GONE
        }

        holder.itemView.setOnClickListener { v -> listener.onItemSelected(appDetails, recommendation.toLong()) }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var icon: ImageView
        var title: TextView
        var subtitle: TextView
        var restrictionText: TextView
        var recommendationText: TextView

        init {
            icon = itemView.findViewById(R.id.restriction_app_icon)
            title = itemView.findViewById(R.id.restriction_title)
            subtitle = itemView.findViewById(R.id.restriction_subtitle)
            restrictionText = itemView.findViewById(R.id.restriction_text)
            recommendationText = itemView.findViewById(R.id.recommendation_text)
        }
    }

    interface OnItemSelectedListener {
        fun onItemSelected(appDetails: AppDetails, recommendation: Long)
    }
}
