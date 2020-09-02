package com.specknet.orientandroid.adapters

import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.specknet.orientandroid.data.Converters
import java.util.*

@BindingAdapter("android:text")
fun bindDateText(textView: TextView, date: Date?) {
    textView.text = Converters.fromDateToString(date)
}

@BindingAdapter("seconds")
fun bindSecondsText(textView: TextView, n: Number?) {
    textView.text = "${n}s"
}

@BindingAdapter("stepCount")
fun bindStepCount(textView: TextView, stepCount: Int?) {
    textView.text = "Total step count:\t\t\t$stepCount"
}

@BindingAdapter("duration", "activityName")
fun bindDuration(textView: TextView, duration: Int?, activityName: String) {
    textView.text = "Time spent $activityName:\t$duration"
}