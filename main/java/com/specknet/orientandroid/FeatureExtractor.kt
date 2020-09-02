package com.specknet.orientandroid

import org.apache.commons.math3.stat.descriptive.AbstractStorelessUnivariateStatistic
import org.apache.commons.math3.stat.descriptive.moment.Kurtosis
import org.apache.commons.math3.stat.descriptive.moment.Mean
import org.apache.commons.math3.stat.descriptive.moment.Skewness
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation
import org.apache.commons.math3.stat.descriptive.rank.Max
import org.apache.commons.math3.stat.descriptive.rank.Min
import org.apache.commons.math3.stat.descriptive.rank.Percentile
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation

class FeatureExtractor {
    private var sensor_data: Array<DoubleArray>? = null
    private var colCount: Int = 0
    private var rowCount: Int = 0

    val means: DoubleArray
        get() = acc(Mean())

    val std: DoubleArray
        get() = acc(StandardDeviation())

    val meanDiffFromMean: DoubleArray
        get() {
            val means = means
            val diff = Array(6) { DoubleArray(rowCount) }
            val meanDiff = DoubleArray(6)
            var sum = 0.0
            for (i in 0..5) {
                for (j in 0 until rowCount) {
                    diff[i][j] = sensor_data!![i][j] - means[i]
                }
            }

            for (i in 0..5) {
                sum = 0.0
                for (j in 0 until rowCount) {
                    sum += diff[i][j]
                }
                meanDiff[i] = sum / rowCount
            }
            return meanDiff
        }

    val meanMagnitudes: DoubleArray
        get() {
            val mags = DoubleArray(2)
            for (i in 0 until rowCount) {
                mags[0] += Math.sqrt(Math.pow(sensor_data!![0][i], 2.0) + Math.pow(sensor_data!![1][i], 2.0) + Math.pow(sensor_data!![2][i], 2.0))
                mags[1] += Math.sqrt(Math.pow(sensor_data!![3][i], 2.0) + Math.pow(sensor_data!![4][i], 2.0) + Math.pow(sensor_data!![5][i], 2.0))
            }
            mags[0] = mags[0] / rowCount
            mags[1] = mags[1] / rowCount
            return mags
        }

    val meanToMin: DoubleArray
        get() {
            val means = means
            val tomins = DoubleArray(6)
            var min: Double
            for (i in 0..5) {
                min = 100000.0
                for (j in 0 until rowCount) {
                    if (sensor_data!![i][j] < min) {
                        min = sensor_data!![i][j]
                    }
                }
                tomins[i] = min - means[i]
            }
            return tomins
        }

    val meanToMax: DoubleArray
        get() {
            val means = means
            val tomaxs = DoubleArray(6)
            var max: Double
            for (i in 0..5) {
                max = -100000.0
                for (j in 0 until rowCount) {
                    if (sensor_data!![i][j] > max) {
                        max = sensor_data!![i][j]
                    }
                }
                tomaxs[i] = max - means[i]
            }
            return tomaxs
        }


    val range: DoubleArray
        get() {
            val ranges = DoubleArray(6)
            val min = Min()
            val max = Max()
            for (i in 0 until colCount) {

                ranges[i] = max.evaluate(sensor_data!![i]) - min.evaluate(sensor_data!![i])
            }
            return ranges
        }

    val skew: DoubleArray
        get() {
            val skew_all = DoubleArray(6)
            for (i in 0..5) {
                val skew = Skewness()
                skew_all[i] = skew.evaluate(sensor_data!![i])
            }
            return skew_all
        }

    val kurtosis: DoubleArray
        get() {
            val kurt_all = DoubleArray(6)
            for (i in 0..5) {
                val kurt = Kurtosis()
                kurt_all[i] = kurt.evaluate(sensor_data!![i])
            }
            return kurt_all
        }

    val quantiles: DoubleArray
        get() {
            val perc_all = DoubleArray(6 * 5)
            var k = 0
            var i = 0
            while (i < 6 * 5) {
                val percentile = Percentile().withEstimationType(Percentile.EstimationType.R_7)
                perc_all[i] = percentile.evaluate(sensor_data!![k], 10.0)
                perc_all[i + 1] = percentile.evaluate(sensor_data!![k], 25.0)
                perc_all[i + 2] = percentile.evaluate(sensor_data!![k], 50.0)
                perc_all[i + 3] = percentile.evaluate(sensor_data!![k], 75.0)
                perc_all[i + 4] = percentile.evaluate(sensor_data!![k], 90.0)
                k += 1
                i += 5
            }
            return perc_all
        }

    fun corr(i: Int, j: Int): Double {
        val c = PearsonsCorrelation()
        return c.correlation(sensor_data?.get(i), sensor_data?.get(j))
    }

    fun extractFeatures(sensor_data: Array<FloatArray>): FloatArray {
        this.sensor_data = convertFloatsToDoubles(sensor_data)
        this.colCount = sensor_data.size
        this.rowCount = sensor_data[0].size
        //        Log.i("ActivityTracker", "Column count: " + Integer.toString(colCount));
        val means = means
        val std = std
        val diff = meanDiffFromMean
        val mags = meanMagnitudes
        val tomin = meanToMin
        val range = range
        val tomax = meanToMax
        val skew = skew
        val kurt = kurtosis
        val quant = quantiles
        val corrXYAccel = corr(0, 1)
        val features = doubleArrayOf(
                std[0],             //'std_accel_x',
                std[2],             //'std_accel_z',
                tomax[0],           //'max-mean_accel_x',
                tomin[2],           //'min-mean_accel_z',
                tomin[0],           //'min-mean_accel_x',
                range[0],           //'range_accel_x',
                range[2],           //'range_accel_z',
                quant[4],           //'0.9quant_accel_x',
                quant[0],           //'0.1quant_accel_x',
                means[3],           //'mean_gyro_x',
                means[0],           //'mean_accel_x',
                means[4],           //'mean_gyro_y',
                quant[1],           //'0.25quant_accel_x',
                corrXYAccel,        //'corr_xy_accel',
                std[1],             //'std_accel_y',
                quant[2],           //'median_accel_x',
                quant[4 * 5 + 2]    //'median_gyro_y'
        )
        // 0 ax, 1 ay, 2 az, 3 gx, 4 gy, 5 gz
        //selected = [               '0.9quant_gyro_x','tomX_gyro_y','0.75qt_g_x', '0.9quant_gyro_y','0.75quant_gyro_y','median_g_x', 'mn_g_x', '0.25quant_gy', 'range_gy','0.1quant_gy','std_gy' 'mx-mn_az''mn-mn_gy'std_az',mn-mn_az',0.25q_ay', 'range_az', 'mi-mn_ax',rg_ax', '0.9q_ax', 'mx-mn-ax''std_ax''0.75q_ax''mx-mn_ay''med_ax', 'mi-mn_ay',range_ay','median_ay', 'mn_ay', 'std_ay', 'mn_ax', '0.9q_ay', '0.25q_ax', '0.75qu_acy', '0.1quant_accel_x']
        // val features = doubleArrayOf(quart[3 * 5 + 4], tomax[4], quart[3 * 5 + 3], quart[4 * 5 + 4], quart[4 * 5 + 3], quart[3 * 5 + 2], means[3], quart[4 * 5 + 1], range[4], quart[4 * 5], std[4], tomax[2], tomin[4], std[2], tomin[2], quart[5 + 1], range[2], tomin[0], range[0], quart[4], tomax[0], std[0], quart[3], tomax[1], quart[2], tomin[1], range[1], quart[5 + 2], means[1], std[1], means[0], quart[5 + 4], quart[1], quart[5 + 3], quart[0])
        //        Log.i("ActivityTracker", "Extracted feature count: " + Integer.toString(features.length));
        return convertDoublesToFloats(features)
    }

    fun acc(f: AbstractStorelessUnivariateStatistic): DoubleArray {
        val out = DoubleArray(colCount)
        for (i in 0 until colCount) {
            out[i] = f.evaluate(sensor_data!![i])
        }
        return out
    }

    companion object {
        fun convertDoublesToFloats(input: DoubleArray): FloatArray {
            val output = FloatArray(input.size)
            for (i in input.indices) {
                output[i] = input[i].toFloat()
            }
            return output
        }

        fun convertFloatsToDoubles(input: FloatArray): DoubleArray {
            val output = DoubleArray(input.size)
            for (i in input.indices) {
                output[i] = input[i].toDouble()
            }
            return output
        }

        fun convertFloatsToDoubles(input: Array<FloatArray>): Array<DoubleArray> {
            val output = Array(input.size) { DoubleArray(input[0].size) }
            for (i in input.indices) {
                for (j in 0 until input[0].size)
                    output[i][j] = input[i][j].toDouble()
            }
            return output
        }
    }

}
