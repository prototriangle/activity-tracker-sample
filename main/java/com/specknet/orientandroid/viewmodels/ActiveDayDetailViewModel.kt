package com.specknet.orientandroid.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.specknet.orientandroid.data.ActiveDay
import com.specknet.orientandroid.data.ActiveDayRepository
import com.specknet.orientandroid.data.Converters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import java.util.*

class ActiveDayDetailViewModel(
        activeDayRepository: ActiveDayRepository,
        private val date: Date
) : ViewModel() {

    init {
        Log.i("ActiveDayDetailViewModel", Converters.fromDateToString(date))
    }

    val activeDay: LiveData<ActiveDay> = activeDayRepository.getLiveActiveDay(date)
    val dateString by lazy { Converters.fromDateToString(date) }

    private val viewModelJob = Job()

    private val viewModelScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}
