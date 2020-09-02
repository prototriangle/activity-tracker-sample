package com.specknet.orientandroid.viewmodels

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import com.specknet.orientandroid.data.ActiveDay
import com.specknet.orientandroid.data.ActiveDayRepository
import java.util.*

class ActiveDayListViewModel internal constructor(
        private val activeDayRepository: ActiveDayRepository
) : ViewModel() {

    private val activeDayList = MediatorLiveData<List<ActiveDay>>()
    private val liveActiveDayList = activeDayRepository.getActiveDays()

    init {
        activeDayList.addSource(liveActiveDayList, activeDayList::setValue)
    }

    fun getActiveDays() = activeDayList

    fun clearDay(date: Date) {
        var day = activeDayRepository.getActiveDay(date)
        day = ActiveDay(day?.id ?: 0, date, 0, 0, 0, 0, 0)
        activeDayRepository.update(day)
    }

    fun removeDay(date: Date) {
        activeDayRepository.removeActiveDay(date)
    }

    fun removeAllDays() {
        activeDayRepository.removeAllActiveDays()
    }

}
