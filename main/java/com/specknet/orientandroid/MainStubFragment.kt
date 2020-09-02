package com.specknet.orientandroid

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.StateSet
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.Observer
import com.specknet.orientandroid.data.ActiveDay
import com.specknet.orientandroid.data.Converters
import com.specknet.orientandroid.data.stripTime
import com.specknet.orientandroid.utilities.InjectorUtils
import com.specknet.orientandroid.viewmodels.MainStubViewModel
import kotlinx.android.synthetic.main.main_stub_fragment.*
import lecho.lib.hellocharts.model.PieChartData
import lecho.lib.hellocharts.model.SliceValue
import java.util.*

class MainStubFragment : Fragment() {

    companion object {
        fun newInstance() = MainStubFragment()
        var serviceRunning = false
    }

    private lateinit var colors: Map<String, Int>

    private lateinit var viewModel: MainStubViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.main_stub_fragment, container, false)
    }

    private fun Int.toDayOfWeekString(): String {
        return when (this) {
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            Calendar.SUNDAY -> "Sunday"
            else -> "Invalid Day Index"
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.let {
            androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(it)
                    .registerReceiver(messageReceiver, IntentFilter("predicted_class"))
        }
        currentDay?.text = Calendar.getInstance().get(Calendar.DAY_OF_WEEK).toDayOfWeekString()
        currentDate?.text = Converters.fromDateToString(Date())
        if (serviceRunning) {
            showCentre()
        } else {
            classification?.text = "No classification"
            hideCentre()
        }
    }

    private val data = PieChartData()

    private val states = arrayOf(
            StateSet.WILD_CARD
    )

    private val messageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val pred = intent.getStringExtra("Prediction")
            val newSteps = intent.getIntExtra("Steps", 0)
            classification?.text = pred
            val color = colors.getValue(pred)
            spinner?.indeterminateTintList =
                    ColorStateList(states, intArrayOf(color))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val menuResource = arguments?.getInt("menuResource")
        Log.i("menuResource", menuResource.toString())
        if (menuResource != null) {
            (activity as? MainStub)?.setMenu(menuResource)
        }
        colors = mapOf(
                "Walking" to resources.getColor(R.color.colorWalking, null),
                "Running" to resources.getColor(R.color.colorRunning, null),
                "Ascending" to resources.getColor(R.color.colorAscending, null),
                "Descending" to resources.getColor(R.color.colorDescending, null),
                "Idle" to resources.getColor(R.color.colorIdle, null)
        ).withDefault { Color.GRAY }
        defaultSlices = mutableListOf(
                SliceValue(0.01f,
                        colors.getValue("Walking")).setLabel("Walking"),
                SliceValue(0.01f,
                        colors.getValue("Running")).setLabel("Running"),
                SliceValue(0.01f,
                        colors.getValue("Ascending")).setLabel("Ascending"),
                SliceValue(0.01f,
                        colors.getValue("Descending")).setLabel("Descending")
        )
    }

    private lateinit var defaultSlices: MutableList<SliceValue>

    private fun update(d: ActiveDay) {
        stepCountText?.text = "${d.stepCount}/10000"
        progressBar?.max = 10000
        progressBar?.min = 0
        progressBar?.progress = d.stepCount
        data.finish()
        data.values.apply {
            this[0].setTarget(d.walkingSeconds.toFloat())
                    .setLabel("Walking: ${d.walkingSeconds}s")
            this[1].setTarget(d.runningSeconds.toFloat())
                    .setLabel("Running: ${d.runningSeconds}s")
            this[2].setTarget(d.ascendingSeconds.toFloat())
                    .setLabel("Ascending: ${d.ascendingSeconds}s")
            this[3].setTarget(d.descendingSeconds.toFloat())
                    .setLabel("Descending: ${d.descendingSeconds}s")
        }
        pieChart?.pieChartData = data
        pieChart?.startDataAnimation(200L)
    }

    private val today = Date().stripTime()

    private fun loadDefaultPieData() {
        data.values = defaultSlices
        pieChart?.pieChartData = data
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val factory = context?.let { InjectorUtils.provideMainStubViewModelFactory(it) }
                ?: throw Exception("No Context!!!")
        viewModel = ViewModelProviders.of(this, factory).get(MainStubViewModel::class.java)
        viewModel.activeDay.observe(viewLifecycleOwner, Observer { activeDay ->
            if (activeDay != null) {
                update(activeDay)
            } else {
                loadDefaultPieData()
            }
        })

        loadDefaultPieData()

        data.apply {
            data.setHasLabels(true)
            data.slicesSpacing = 7
        }
        pieChart?.isChartRotationEnabled = false
        pieChart?.isInteractive = false

        toggleButton?.isChecked = serviceRunning
        toggleButton?.setOnClickListener {
            Intent()
            val intent = Intent(requireContext().applicationContext, AccelDataService::class.java)
                    .putExtra("FakeSensorInput", true)
            if (!serviceRunning) {
                requireContext().startService(intent)
                serviceRunning = true
                classification?.text = "Classifying"
                showCentre()
            } else {
                requireContext().stopService(intent)
                classification?.text = "No classification"
                serviceRunning = false
                hideCentre()
            }
        }
    }

    private fun showCentre() {
        classification?.visibility = VISIBLE
        spinner?.visibility = VISIBLE
        data.setHasCenterCircle(true)
        pieChart?.pieChartData = data
    }

    private fun hideCentre() {
        classification?.visibility = GONE
        spinner?.visibility = GONE
        data.setHasCenterCircle(false)
        pieChart?.pieChartData = data
    }
}
