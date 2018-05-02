package com.cs565project.smart.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import com.cs565project.smart.repository.AppDataRepository
import java.util.*

class PieChartViewModel(mApplication: Application) : AndroidViewModel(mApplication) {

    val mRepository = AppDataRepository(mApplication)

    fun getData(date: Date, category: String?) = mRepository.getPieData(date, category)
}

