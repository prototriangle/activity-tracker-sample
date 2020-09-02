package com.specknet.orientandroid.utilities

import android.content.Context
import com.specknet.orientandroid.viewmodels.ActiveDayDetailViewModelFactory
import com.specknet.orientandroid.viewmodels.ActiveDayListViewModelFactory
import com.specknet.orientandroid.data.ActiveDayRepository
import com.specknet.orientandroid.data.AppDatabase
import com.specknet.orientandroid.viewmodels.MainStubViewModelFactory
import java.util.*

object InjectorUtils {

    fun getActiveDayRepository(context: Context): ActiveDayRepository {
        return ActiveDayRepository.getInstance(AppDatabase.getInstance(context).activeDayDao())
    }

    fun provideActiveDayListViewModelFactory(context: Context): ActiveDayListViewModelFactory {
        val repository = getActiveDayRepository(context)
        return ActiveDayListViewModelFactory(repository)
    }

    fun provideActiveDayDetailViewModelFactory(context: Context, date: Date): ActiveDayDetailViewModelFactory {
        return ActiveDayDetailViewModelFactory(getActiveDayRepository(context), date)
    }

    fun provideMainStubViewModelFactory(context: Context): MainStubViewModelFactory {
        val repository = getActiveDayRepository(context)
        return MainStubViewModelFactory(repository)
    }
}
