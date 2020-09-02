package com.specknet.orientandroid.data

import androidx.lifecycle.LiveData
import androidx.room.*
import java.util.*


@Dao
abstract class ActiveDayDao {
    @Query("SELECT * FROM ActiveDay ORDER BY id DESC")
    abstract fun getAll(): LiveData<List<ActiveDay>>

    @Query("SELECT * FROM ActiveDay WHERE id IN (:recordIds)")
    abstract fun loadAllByIds(recordIds: IntArray): List<ActiveDay>

    @Query("SELECT * FROM ActiveDay WHERE date LIKE :date")
    abstract fun findLiveByDate(date: Date): LiveData<ActiveDay>

    @Query("SELECT * FROM ActiveDay WHERE date LIKE :date")
    abstract fun findByDate(date: Date): ActiveDay?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertAll(vararg days: ActiveDay)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(day: ActiveDay)

    @Transaction
    open fun update(day: ActiveDay) {
        val existing = findByDate(day.date)
        if (existing != null) {
            day.id = existing.id
            delete(day.date)
        }
        insert(day)
    }

    @Query("DELETE FROM ActiveDay WHERE date LIKE :date")
    abstract fun delete(date: Date)

    @Delete
    abstract fun delete(day: ActiveDay)

    @Query("DELETE FROM ActiveDay")
    abstract fun deleteAll()

    @Query("SELECT COUNT(*) FROM ActiveDay")
    abstract fun countEntries(): Int

    @Transaction
    open fun clearAndInsert(vararg days: ActiveDay) {
        deleteAll()
        insertAll(*days)
    }
}