package com.specknet.orientandroid

import android.util.Log
import com.specknet.orientandroid.utilities.*
import org.apache.commons.math3.stat.descriptive.moment.Skewness
import org.apache.commons.math3.stat.descriptive.rank.Percentile
import org.apache.commons.math3.util.FastMath.abs

class StepCounter {

    val idleDetector = IdleDetector()

    private val Array<FloatArray>.accelX: FloatArray
        get() = this[0]
    private val Array<FloatArray>.accelY: FloatArray
        get() = this[1]
    private val Array<FloatArray>.accelZ: FloatArray
        get() = this[2]
    private val Array<FloatArray>.gyroX: FloatArray
        get() = this[3]
    private val Array<FloatArray>.gyroY: FloatArray
        get() = this[4]
    private val Array<FloatArray>.gyroZ: FloatArray
        get() = this[4]

    companion object {
        const val upperThreshold = 50
        const val lowerThreshold = 20
    }

    enum class State {
        AcceptingUp,
        AcceptingDown
    }

    private var state = State.AcceptingUp

    private fun skewness(data: FloatArray): Double = Skewness().evaluate(data.toDoubleArray())

    private fun getClosestAccelToOne(dataWindow: Array<FloatArray>, mean: FloatArray): FloatArray {
        val distanceToOne = (mean - floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f))
        val abs = distanceToOne.map { x -> abs(x) }
        return dataWindow[abs.argmin()]
    }

    fun countSteps(dataWindow: MultiDimRingBufferWithRollingMean): Int {
        if (idleDetector.isIdle(dataWindow))
            return 0
        val data = dataWindow.getData(true)
        printOut(data)

        val selectedAccel = accelTo1D(data, dataWindow)
        val selectedGyro = gyroTo1D(data)

        val accelSkewness = abs(skewness(selectedAccel))
        val gyroSkewness = abs(skewness(selectedGyro))

        var countBothUpAndDown = false

        val bias = 0.2

        val selectedData = if (accelSkewness <= gyroSkewness + bias) {
            Log.i("StepCount", "Using accel ($accelSkewness <= $gyroSkewness)")
            selectedAccel
        } else {
            Log.i("StepCount", "Using gyro ($accelSkewness > $gyroSkewness)")
            countBothUpAndDown = true
            selectedGyro
        }

        var count = 0
        val percentiles = getPercentiles(selectedData)
        selectedData.roll(2) { floats ->
            if (state == State.AcceptingUp && floats[1] >= percentiles.second) {
                if (floats[0] < percentiles.second) {
                    ++count
                    state = State.AcceptingDown
                }
            } else if (state == State.AcceptingDown && floats[1] <= percentiles.first) {
                if (floats[0] > percentiles.first) {
                    if (countBothUpAndDown)
                        ++count
                    state = State.AcceptingUp
                }
            }
        }
        return count
    }

    private fun gyroTo1D(data: Array<FloatArray>) = data.gyroY + data.gyroZ - data.accelX

    private fun accelTo1D(data: Array<FloatArray>, dataWindow: MultiDimRingBufferWithRollingMean) =
            getClosestAccelToOne(data, dataWindow.mean())

    private fun printOut(dataWindow: Array<FloatArray>) {
        dataWindow.transpose.forEach { row ->
            var str = ""
            row.forEach { str += ", $it" }
            Log.i("StepCount", str)
        }
    }

    private fun getPercentiles(arr: FloatArray, lower: Double = 40.0, upper: Double = 60.0): Pair<Double, Double> {
        val darr = arr.toDoubleArray()
        return Percentile().evaluate(darr, lower) to Percentile().evaluate(darr, upper)
    }

    private fun FloatArray.toDoubleArray(): DoubleArray = DoubleArray(this.size) { i -> this[i].toDouble() }

}

