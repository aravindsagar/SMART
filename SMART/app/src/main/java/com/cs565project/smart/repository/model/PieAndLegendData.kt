package com.cs565project.smart.repository.model

import com.cs565project.smart.fragments.adapter.ChartLegendAdapter
import com.github.mikephil.charting.data.PieData

data class PieAndLegendData(val pieData: PieData, val pieDataSecondary: PieData?,
                            val legendInfo: List<ChartLegendAdapter.LegendInfo>, val mood: String,
                            val totalUsageTime: Long)