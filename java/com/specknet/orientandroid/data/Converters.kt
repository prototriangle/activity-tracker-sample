package com.specknet.orientandroid.data

import android.util.Log
import androidx.room.TypeConverter
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class Converters {
    companion object {

        @JvmStatic
        val df = SimpleDateFormat("dd-MM-yy", Locale.UK)

        @TypeConverter
        @JvmStatic
        fun fromDateToString(date: Date?): String {
            return if (date != null) {
                df.format(date)
            } else {
                ""
            }
        }

        @TypeConverter
        @JvmStatic
        fun fromStringToDate(value: String?): Date {
            if (value != null) {
                try {
                    return df.parse(value)
                } catch (e: ParseException) {
                    Log.e("Date Conversion", "tried to convert $value to a date")
                    e.printStackTrace()
                }
                return Date()
            } else {
                return Date()
            }
        }
    }
}

fun Date.stripTime(): Date {
    return Converters.fromStringToDate(Converters.fromDateToString(Date()))
}