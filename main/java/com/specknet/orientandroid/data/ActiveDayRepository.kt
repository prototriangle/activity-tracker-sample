package com.specknet.orientandroid.data

import java.util.Date

class ActiveDayRepository private constructor(private val activeDayDao: ActiveDayDao) {
    fun getActiveDays() = activeDayDao.getAll()

    fun getActiveDays(vararg ids: Int) = activeDayDao.loadAllByIds(ids)

    fun getLiveActiveDay(date: Date) = activeDayDao.findLiveByDate(date)

    fun getActiveDay(date: Date) = activeDayDao.findByDate(date)

    fun removeActiveDay(date: Date) = activeDayDao.delete(date)

    fun removeAllActiveDays() = activeDayDao.deleteAll()

    fun insert(day: ActiveDay) = activeDayDao.insert(day)

    fun update(day: ActiveDay) = activeDayDao.update(day)

    companion object {

        @Volatile
        private var instance: ActiveDayRepository? = null

        fun getInstance(activeDayDao: ActiveDayDao) =
                instance ?: synchronized(this) {
                    instance ?: ActiveDayRepository(activeDayDao).also { instance = it }
                }
    }
}
