package com.specknet.orientandroid.data

import androidx.room.*
import java.util.Date

@Entity
data class ActiveDay(
        @PrimaryKey(autoGenerate = true)
        var id: Int,

        @ColumnInfo(name = "date")
        var date: Date,

        @ColumnInfo(name = "step_count", typeAffinity = ColumnInfo.INTEGER)
        var stepCount: Int,

        @ColumnInfo(name = "walking_seconds", typeAffinity = ColumnInfo.INTEGER)
        var walkingSeconds: Int,

        @ColumnInfo(name = "running_seconds", typeAffinity = ColumnInfo.INTEGER)
        var runningSeconds: Int,

        @ColumnInfo(name = "ascending_seconds", typeAffinity = ColumnInfo.INTEGER)
        var descendingSeconds: Int,

        @ColumnInfo(name = "descending_seconds", typeAffinity = ColumnInfo.INTEGER)
        var ascendingSeconds: Int
) {
    constructor(date: Date) : this(0, date, 0, 0, 0, 0, 0)
}