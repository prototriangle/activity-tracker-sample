package com.specknet.orientandroid.viewmodels

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import com.specknet.orientandroid.data.ActiveDay
import com.specknet.orientandroid.data.ActiveDayRepository
import com.specknet.orientandroid.data.stripTime
import kotlinx.coroutines.*
import java.util.Date

class MainStubViewModel internal constructor(
        private val activeDayRepository: ActiveDayRepository
) : ViewModel() {

    private val today = Date().stripTime()
    public val activeDay = MediatorLiveData<ActiveDay>()
    private val liveActiveDay = activeDayRepository.getLiveActiveDay(today)

    init {
        activeDay.addSource(liveActiveDay, activeDay::setValue)
    }

}
