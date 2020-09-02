package com.specknet.orientandroid.utilities

import android.util.Log

class MultiDimRingBufferWithRollingMean(
        inputDim: Int,
        bufferSize: Int,
        private val rollingBufferSize: Int)
    : MultiDimRingBuffer(inputDim, bufferSize) {

    val window = MultiDimRingBuffer(inputDim, rollingBufferSize)

    fun getRolled(): MultiDimRingBuffer {
        val indexerCopy = indexer.copy()
        for (i in 0 until rollingBufferSize) {
            window.putData(this[indexerCopy.index])
            indexerCopy.inc()
        }
        val newSize = bufferSize - rollingBufferSize + 1
        val out = MultiDimRingBuffer(inputDim, newSize)
        for (i in 0 until newSize) {
            out.putData(window.mean())
            window.putData(this[indexerCopy.index])
            indexerCopy.inc()
        }
        return out
    }
}