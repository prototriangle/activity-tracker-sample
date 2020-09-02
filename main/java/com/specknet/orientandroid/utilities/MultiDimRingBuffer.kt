package com.specknet.orientandroid.utilities

open class MultiDimRingBuffer(val inputDim: Int, val bufferSize: Int) : Iterable<FloatArray> {

    protected val multiBuffer = Array(inputDim) { FloatArray(bufferSize) }
    protected var indexer = RingIndexer(bufferSize)

    protected class RingIndexer(private val ringSize: Int, start: Int = 0) : Iterable<Int> {

        var index = start
            set(value) {
                field = value % ringSize
            }

        operator fun plus(i: Int): RingIndexer {
            index += i
            return this
        }

        fun copy(): RingIndexer = RingIndexer(ringSize, index)

        operator fun inc(): RingIndexer {
            index += 1
            return this
        }

        override fun iterator(): Iterator<Int> {
            return ((0 until index) + (index until ringSize)).iterator()
        }

        override fun toString(): String = "RingIndexer($ringSize) at index: $index"
    }

    init {
//        if (bufferSize == 54)
//            Log.i("StepCount", "Buffer(inputdim=$inputDim, bufferSize=$bufferSize)")
    }

    fun putData(data: FloatArray) {
//        if (bufferSize == 54) {
//            var str = "PUT"
//            data.forEach { str += ", $it" }
//            Log.i("StepCount", str)
//        }
        if (inputDim != data.size) {
            throw Exception("Buffer accepts data with dim $inputDim (${data.size} was provided")
        }
        for (i in 0 until inputDim) {
            multiBuffer[i][indexer.index] = data[i]
        }
        indexer++
//        if (bufferSize == 54) {
//            Log.i("StepCount", "PUT DONE\t| indexer=$indexer")
//        }
    }

    fun getData(sorted: Boolean = true): Array<FloatArray> {
//        if (bufferSize == 54) {
//            Log.i("StepCount", "GET DATA")
//        }
        when {
            sorted -> {
                val out = Array(inputDim) { FloatArray(bufferSize) }
                indexer.forEachIndexed { index, offsetIndex ->
                    for (i in 0 until inputDim)
                        out[i][index] = multiBuffer[i][offsetIndex]
                }
                return out
            }
            else -> return multiBuffer
        }
    }

    operator fun get(index: Int): FloatArray {
        return FloatArray(inputDim) {
            multiBuffer[it][index]
        }
    }

    override fun iterator(): Iterator<FloatArray> {
        val l = List(bufferSize) { i ->
            this[i]
        }
        return l.iterator()
    }

    operator fun set(index: Int, v: FloatArray) {
        multiBuffer.forEachIndexed { i, floats ->
            floats[index] = v[i]
        }
    }

    fun toArrayOfFloatArrays(): Array<FloatArray> = multiBuffer.copyOf()

    fun mean(): FloatArray {
        return sum() / bufferSize
    }

    fun FloatArray.add(b: FloatArray) {
        if (this.size != b.size) throw Exception("Arrays must be of same size; ${this.size}!=${b.size}")
        this.forEachIndexed { i, _ ->
            this[i] += b[i]
        }
    }

    fun sum(): FloatArray {
        var sum = FloatArray(inputDim)
        for (i in 0 until bufferSize) {
            sum.add(this[i])
        }
        return sum
    }
}