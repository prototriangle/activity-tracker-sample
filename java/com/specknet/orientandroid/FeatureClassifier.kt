package com.specknet.orientandroid

import android.content.Context
import android.os.SystemClock
import android.util.Log
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import org.tensorflow.lite.Interpreter

abstract class FeatureClassifier<T> @Throws(IOException::class) constructor(ctx: Context) {
    companion object {
        const val TAG = "ActivityTrack"

        /** Preallocated buffers for storing image data in.  */
        val floatValues = emptyArray<Float>()

        /** Options for configuring the Interpreter.  */
        val tfliteOptions = Interpreter.Options()

        /** The loaded TensorFlow Lite model.  */
        var tfliteModel: MappedByteBuffer? = null

        /** An instance of the driver class to run model inference with Tensorflow Lite.  */
        var tflite: Interpreter? = null

        /** Labels corresponding to the output of the vision model.  */
        lateinit var labelList: List<String>

        /** A ByteBuffer to hold image data, to be feed into Tensorflow Lite as inputs.  */
        var dataBuffer: ByteBuffer? = null

//        val sortedLabels = PriorityQueue<MutableMap.MutableEntry<String, Float>>(
//                RESULTS_TO_SHOW
//        ) { o1, o2 -> o1.value.compareTo(o2.value) }

        const val inputFloatLength = 4

        const val inputFloatCount = 17
    }


    init {
        tfliteModel = loadModelFile(ctx)
        tfliteOptions.setUseNNAPI(false)
        tfliteOptions.setNumThreads(4)
        tflite = Interpreter(tfliteModel!!, tfliteOptions)
        labelList = listOf("Walk", "Run", "Ascend", "Descend")
        dataBuffer = ByteBuffer.allocateDirect(inputFloatCount * inputFloatLength)
        dataBuffer!!.order(ByteOrder.nativeOrder())
        Log.d(TAG, "Created a Tensorflow Lite Classifier.")
    }

    public fun classifyInput(input: T) {
        if (tflite == null) {
            Log.e(TAG, "Classifier has not been initialized; Skipped.")
        }
        processInputAndLoadBuffer(input)
        val startTime = SystemClock.uptimeMillis()
        runInference()
        val endTime = SystemClock.uptimeMillis()
//        Log.d(TAG, "Timecost to run model inference: " + java.lang.Long.toString(endTime - startTime))
    }

    private fun recreateInterpreter() {
        if (tflite != null) {
            tflite!!.close()
            tflite = tfliteModel?.let { Interpreter(it, tfliteOptions) }
        }
    }

    fun setUseNNAPI(nnapi: Boolean) {
        tfliteOptions.setUseNNAPI(nnapi)
        recreateInterpreter()
    }

    fun setNumThreads(numThreads: Int) {
        tfliteOptions.setNumThreads(numThreads)
        recreateInterpreter()
    }

    /** Closes tflite to release resources.  */
    fun close() {
        tflite?.close()
        tflite = null
        tfliteModel = null
    }

    /** Memory-map the model file in Assets.  */
    @Throws(IOException::class)
    private fun loadModelFile(ctx: Context): MappedByteBuffer {
        val fileDescriptor = ctx.assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /** Writes Image data into a `ByteBuffer`.  */
    protected abstract fun processInputAndLoadBuffer(input: T)

    abstract val modelPath: String
    abstract val labelPath: String

    protected abstract fun getProbability(labelIndex: Int): Float


    protected abstract fun setProbability(labelIndex: Int, value: Number)


    protected abstract fun getNormalizedProbability(labelIndex: Int): Float

    abstract fun runInference()

    protected fun getNumLabels(): Int {
        return labelList.size
    }

}