package com.specknet.orientandroid

import android.util.Log
import com.specknet.orientandroid.utilities.MultiDimRingBuffer
import com.specknet.orientandroid.utilities.mean

class IdleDetector {

    companion object {
        @JvmStatic
        val accelMagnitudeThreshold: Float = 1.7f
        @JvmStatic
        val gyroMagnitudeThreshold: Float = 60f
        @JvmStatic
        val sqrAccelMagnitudeThreshold: Float = accelMagnitudeThreshold * accelMagnitudeThreshold
        @JvmStatic
        val sqrGyroMagnitudeThreshold: Float = gyroMagnitudeThreshold * gyroMagnitudeThreshold
    }

    fun isIdle(data: MultiDimRingBuffer): Boolean {
        val sqrAccelMags = FloatArray(data.bufferSize)
        val sqrGyroMags = FloatArray(data.bufferSize)
        data.forEachIndexed { i, floats ->
            val sqrAccel = floats.sliceArray(0 until 3).sqrMagnitude()
            val sqrGyro = floats.sliceArray(3 until 6).sqrMagnitude()
            sqrAccelMags[i] = sqrAccel
            sqrGyroMags[i] = sqrGyro
        }
        val accelWithinThresh by lazy { sqrAccelMags.mean() < sqrAccelMagnitudeThreshold }
        val gyroWithinThresh by lazy { sqrGyroMags.mean() < sqrGyroMagnitudeThreshold }
        val withinThresh = accelWithinThresh && gyroWithinThresh
        Log.v("IdleDetector", "sqrAccelMags.mean() ${sqrAccelMags.mean()}")
        Log.v("IdleDetector", "sqrGyroMags.mean() ${sqrGyroMags.mean()}")
        Log.v("IdleDetector", "idle: $withinThresh")
        return withinThresh
    }

}

private fun FloatArray.sqrMagnitude(): Float {
    var output = 0.0f
    this.forEach { v ->
        output += v * v
    }
    return output
}
