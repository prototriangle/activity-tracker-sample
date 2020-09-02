package com.specknet.orientandroid

import android.content.Context
import android.util.Log
import com.specknet.orientandroid.utilities.PCATransform

class WindowClassifier(ctx: Context) : FeatureClassifier<Array<FloatArray>>(ctx) {

    private var labelProbArray: Array<FloatArray> = Array(1) { FloatArray(getNumLabels()) }

    override val modelPath: String
        get() = "final.h5.tflite"

    override val labelPath: String
        get() = "labels.txt"

    override fun getProbability(labelIndex: Int): Float =
            labelProbArray[0][labelIndex]

    fun getProbabilities(): FloatArray =
            labelProbArray[0]

    override fun setProbability(labelIndex: Int, value: Number) {
        labelProbArray[0][labelIndex] = value.toFloat()
    }

    override fun getNormalizedProbability(labelIndex: Int): Float {
        // Incorrect but probs ok
        return getProbability(labelIndex)
    }

    override fun runInference() {
        if (dataBuffer == null) {
            Log.e(TAG, "Input data is null!")
            return
        }
        if (tflite == null) {
            Log.e(TAG, "tflite is null!")
            return
        }
        dataBuffer?.let { tflite?.run(it, labelProbArray) }
    }

    private val pca = PCATransform()

    override fun processInputAndLoadBuffer(input: Array<FloatArray>) {
        if (dataBuffer != null) {
            dataBuffer!!.rewind()

            val features = featureExtractor.extractFeatures(input)
            val components = pca.transform(features)

            loadBuffer(components)
        }
    }

    fun loadBuffer(input: FloatArray) {
        if (dataBuffer != null) {
            val capacity = dataBuffer!!.capacity()
            if (input.size * inputFloatLength != capacity)
                throw Exception("Buffer capacity ($capacity) != input size (${input.size * inputFloatLength}.")
            dataBuffer!!.rewind()
            for (i in 0 until capacity / inputFloatLength) {
                dataBuffer!!.putFloat(input[i])
            }
        }
    }

    companion object {
        const val TAG = "ActivityTrack"
        val featureExtractor = FeatureExtractor()
    }

}