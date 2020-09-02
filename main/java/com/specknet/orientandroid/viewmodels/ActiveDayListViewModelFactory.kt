package com.specknet.orientandroid.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

import com.specknet.orientandroid.data.ActiveDayRepository

class ActiveDayListViewModelFactory(
        private val repository: ActiveDayRepository
) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>) = ActiveDayListViewModel(repository) as T
}
