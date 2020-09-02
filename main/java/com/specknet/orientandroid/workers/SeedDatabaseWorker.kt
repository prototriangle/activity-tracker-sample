package com.specknet.orientandroid.workers

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.specknet.orientandroid.data.ActiveDay
import com.specknet.orientandroid.data.AppDatabase
import com.specknet.orientandroid.utilities.TESTING_DATA_FILENAME

class SeedDatabaseWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    private val TAG by lazy { SeedDatabaseWorker::class.java.simpleName }

    override fun doWork(): Result {
        val activeDayType = object : TypeToken<List<ActiveDay>>() {}.type
        var jsonReader: JsonReader? = null

        return try {
            val inputStream = applicationContext.assets.open(TESTING_DATA_FILENAME)
            jsonReader = JsonReader(inputStream.reader())
            val activeDayList: List<ActiveDay> = Gson().fromJson(jsonReader, activeDayType)
            Log.d(TAG, "Seed list size: ${activeDayList.size}")
            val database = AppDatabase.getInstance(applicationContext)
//            database.activeDayDao().insertAll(*(activeDayList.toTypedArray()))
            for (day in activeDayList.toTypedArray()) {
                database.activeDayDao().insert(day)
            }
            Result.success()
        } catch (ex: Exception) {
            Log.e(TAG, "Error seeding database", ex)
            Result.failure()
        } finally {
            jsonReader?.close()
        }
    }
}
