package com.specknet.orientandroid.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.specknet.orientandroid.data.ActiveDayRepository

class MainStubViewModelFactory(
        private val repository: ActiveDayRepository
) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>) = MainStubViewModel(repository) as T
}
