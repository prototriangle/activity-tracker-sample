package com.specknet.orientandroid.utilities


operator fun FloatArray.minus(b: FloatArray): FloatArray {
    if (this.size != b.size) throw Exception("Arrays must be of same size; ${this.size}!=${b.size}")
    return FloatArray(this.size) { i ->
        this[i] - b[i]
    }
}

operator fun FloatArray.plus(b: FloatArray): FloatArray {
    if (this.size != b.size) throw Exception("Arrays must be of same size; ${this.size}!=${b.size}")
    return FloatArray(this.size) { i ->
        this[i] + b[i]
    }
}

fun FloatArray.roll(windowSize: Int, action: (FloatArray) -> Unit) {
    val offset = windowSize - 1
    for (index in offset until this.size) {
        val slice = this.sliceArray((index - offset)..index)
        assert(slice.size == windowSize)
        action(slice)
    }
}

fun FloatArray.mean(): Float = this.sum() / this.size


operator fun FloatArray.times(other: FloatArray): FloatArray {
    return FloatArray(this.size) { i ->
        this[i] * other[i]
    }
}

fun FloatArray.dot(other: FloatArray): Float {
    return (this * other).sum()
}

fun FloatArray.argmax(): Int {
    var j = 0
    for (i in 0 until this.size) {
        if (this[i] > this[j])
            j = i
    }
    return j
}

val Array<FloatArray>.transpose: Array<FloatArray>
    get() {
        val dim = this.size2d
        return Array(dim.second) { i ->
            FloatArray(dim.first) { j ->
                this[j][i]
            }
        }
    }


//inline fun <T> Array<out T>.allIndexed(predicate: (Int, T) -> Boolean): Boolean {
//    for (i in 0 until this.size) if (!predicate(i, this[i])) return false
//    return true
//}

inline fun <T> Iterable<T>.allIndexed(predicate: (index: Int, T) -> Boolean): Boolean {
    var index = 0
    for (item in this) if (!predicate(index++, item)) return false
    return true
}

val Array<FloatArray>.size2d: Pair<Int, Int>
    get() {
        if (this.isEmpty())
            return 0 to 0
        return this.size to this[0].size
    }

operator fun FloatArray.div(d: Number): FloatArray {
    return FloatArray(this.size) {
        this[it] / d.toFloat()
    }
}


fun <T : Number> Array<Array<T>>.sumDiagonal(): Number {
    var sum = 0.0
    for (i in 0 until this.size) {
        if (i > this[i].size)
            return sum
        else {
            val v = this[i][i]
            sum = sum.plus(v.toDouble())
        }
    }
    return sum
}

internal fun <E : Comparable<E>> List<E>.argmin(): Int {
    var min = this[0]
    var amin = 0
    for (i in 0 until this.size) {
        val test = this[i]
        if (test < min) {
            amin = i
            min = test
        }
    }
    return amin
}