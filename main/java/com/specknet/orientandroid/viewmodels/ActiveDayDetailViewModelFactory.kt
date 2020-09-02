package com.specknet.orientandroid.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.specknet.orientandroid.data.ActiveDayRepository
import java.util.*

class ActiveDayDetailViewModelFactory(
        private val activeDayDetailRepository: ActiveDayRepository,
        private val activeDayDetailId: Date
) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ActiveDayDetailViewModel(activeDayDetailRepository, activeDayDetailId) as T
    }
}
