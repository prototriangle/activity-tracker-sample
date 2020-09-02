package com.specknet.orientandroid

import android.app.Service
import android.content.Intent
import android.os.*
import android.util.Log
import android.widget.Toast
import com.opencsv.CSVReader
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.Timeout
import com.polidea.rxandroidble2.scan.ScanSettings
import com.specknet.orientandroid.data.ActiveDay
import com.specknet.orientandroid.data.ActiveDayRepository
import com.specknet.orientandroid.data.stripTime
import com.specknet.orientandroid.utilities.InjectorUtils
import com.specknet.orientandroid.utilities.MultiDimRingBuffer
import com.specknet.orientandroid.utilities.MultiDimRingBufferWithRollingMean
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.*
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.concurrent.TimeUnit

class AccelDataService : Service() {

    companion object {
        const val TAG = "ActivityTrack"
        const val rollingSize = 5
        const val windowSize = 54
        const val inputDim = 6
    }

    //region private properties
    private var mServiceLooper: Looper? = null
    private var mServiceHandler: ServiceHandler? = null

    private lateinit var packetData: ByteBuffer
    private lateinit var scanSubscription: Disposable

    private val ringBuffer = MultiDimRingBufferWithRollingMean(inputDim, windowSize, rollingSize)

    private var connected = false
    private var connectedTimestamp: Long = 0L

    private var fakeSensorInput: Boolean = false

    private var data = FloatArray(inputDim)

    private var lastPacketTime = 0L

    private val lock = Any()
    private var runClassifier = false

    private lateinit var repo: ActiveDayRepository
    private val today = Date().stripTime()
    private var currentActiveDay: ActiveDayFloats = ActiveDayFloats(ActiveDay(today))

    private val HANDLE_THREAD_NAME = "ClassifierBackground"
    /** An additional thread for running tasks that shouldn't block the UI.  */
    private var backgroundThread: HandlerThread? = null

    /** A [Handler] for running tasks in the background.  */
    private var backgroundHandler: Handler? = null

    private var fakeSensor = FakeSensor()

    private lateinit var classifier: WindowClassifier
    //endregion

    //region service handler
    private inner class ServiceHandler(looper: Looper) : Handler(looper) {

        override fun handleMessage(msg: Message) {
            // Normally we would do some work here, like download a file.
            // For our sample, we just sleep for 5 seconds.
            try {
                Log.i(TAG, "Thread sleeping..")
                Thread.sleep(1000)

                Log.i(TAG, "Thread classifying..")
                startBackgroundThread()
            } catch (e: InterruptedException) {
                // Restore interrupt status.
                Thread.currentThread().interrupt()
            }

            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
//            stopSelf(msg.arg1)
        }
    }
    //endregion

    //region orient connection and packet handling
    private fun ByteArray.toAccelAndGyroData(): FloatArray {
        val buffer = ByteBuffer.allocate(this.size * 4)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.clear()
        buffer.put(this)
        buffer.position(0)
        val output = FloatArray(6)
        for (i in 0 until 3)
            output[i] = buffer.short / 1024f
        for (i in 3 until 6)
            output[i] = buffer.short / 32f
        return output
    }

    var delta: Long = 0L
    val deltaBuffer = MultiDimRingBuffer(1, windowSize)

    private fun handlePacket(input: FloatArray) {
        val ts = System.currentTimeMillis()
        if (lastPacketTime > 0) {
            delta = ts - lastPacketTime
            deltaBuffer.putData(floatArrayOf(delta.toFloat()))
//            if (runIterationCounter >= windowSize)
//                Log.i("StepCount", "Rec. rate: ${1000f /deltaBuffer.mean()[0]}")
        }
        if (runIterationCounter >= windowSize) {
//            Log.i("StepCount", "runInterationCount reached windowSize: $windowSize")
            runIterationCounter = 0
            val stepCount: Int = stepCounter.countSteps(ringBuffer)
            addSteps(stepCount)
        }
        ringBuffer.putData(input)
        ++runIterationCounter


        lastPacketTime = ts
        /*
        Log.i("OrientAndroid", "Gyro:(" + gyro_x + ", " + gyro_y + ", " + gyro_z + ")");
        if (mag_x != 0f || mag_y != 0f || mag_z != 0f)
        Log.i("OrientAndroid", "Mag:(" + mag_x + ", " + mag_y + ", " + mag_z + ")");
        */
    }

    private fun handlePacket(bytes: ByteArray) = handlePacket(bytes.toAccelAndGyroData())

    lateinit var bleConnection: Disposable
    private fun connectToOrient(addr: String, rxBleClient: RxBleClient) {
        val orientDevice = rxBleClient.getBleDevice(addr)
        val characteristic: String = "00001527-1212-efde-1523-785feabcd125"//"8dd6a1b7-bc75-4741-8a26-264af75807de"//

        bleConnection = orientDevice.establishConnection(false, Timeout(5, TimeUnit.SECONDS))
                .flatMap { rxBleConnection -> rxBleConnection.setupNotification(UUID.fromString(characteristic)) }
                .doOnNext { notificationObservable ->
                    // Notification has been set up
                }
                .flatMap { notificationObservable -> notificationObservable } // <-- Notification has been set up, now observe value changes.
                .subscribe(
                        { bytes ->
                            //n += 1;
                            // Given characteristic has been changes, here is the value.

                            //Log.i("OrientAndroid", "Received " + bytes.length + " bytes");
                            if (!connected) {
                                connected = true
                                connectedTimestamp = System.currentTimeMillis()
                                Log.i("OrientAndroid", "SUCCESS: Connected")
                                // RECEIVING SENSOR DATA
//                                start_button.setEnabled(true)

                            }
                            GlobalScope.launch {
                                handlePacket(bytes)
                            }
                        },
                        { throwable ->
                            // Handle an error here.
                            Log.e("OrientAndroid", "Connection Error: " + throwable.toString())
                            throwable.printStackTrace()
                        }
                )
    }
    //endregion

    //region lifecycle methods
    override fun onCreate() {
        packetData = ByteBuffer.allocate(18)
        packetData.order(ByteOrder.LITTLE_ENDIAN)

    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
//        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show()
        fakeSensorInput = intent.extras.getBoolean("FakeSensorInput", false)

        repo = InjectorUtils.getActiveDayRepository(applicationContext)
        GlobalScope.launch(Dispatchers.IO) {
            val day = repo.getActiveDay(today)
            if (day != null)
                currentActiveDay = ActiveDayFloats(day)
        }

        if (fakeSensorInput)
            startFakeSensor()
        else {
            val ORIENT_BLE_ADDRESS = "C2:28:8B:24:8E:CB"//"C2:28:8B:24:8E:CB"//"EB:50:C6:52:DB:DB"//"C2:28:8B:24:8E:CB"//"F2:6D:63:1F:17:33" //"D9:6E:FC:43:7B:B1"

            Toast.makeText(this, "Looking for sensor with MAC addr.: $ORIENT_BLE_ADDRESS", Toast.LENGTH_LONG).show()

            val rxBleClient = RxBleClient.create(this)
            val startTime = System.currentTimeMillis()
            scanSubscription = rxBleClient.scanBleDevices(
                    ScanSettings.Builder()
                            // .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // change if needed
                            // .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES) // change if needed
                            .build()
                    // add filters if needed
            ).subscribe(
                    { scanResult ->
                        Log.i("OrientAndroid", "FOUND: " + scanResult.bleDevice.name + ", " +
                                scanResult.bleDevice.macAddress)
                        // Process scan result here.
                        if (scanResult.bleDevice.macAddress == ORIENT_BLE_ADDRESS) {
                            // CORRECT DEVICE FOUND
                            connectToOrient(ORIENT_BLE_ADDRESS, rxBleClient)
                            scanSubscription.dispose()
                        } else if (System.currentTimeMillis() - startTime > 20000) {
                            Log.i("OrientAndroid", "CANNOT FIND SENSOR")
                            this@AccelDataService.stopSelf()
                            scanSubscription.dispose()
                        }
                    },
                    { throwable ->
                        // Handle an error here.
                        Log.i("OrientAndroid", "BLE SCANNING ERROR: " +
                                throwable.message)
                        this.stopBackgroundThread()
                        stopSelf()
                        // BLUE SCANNING ERROR
                    }
            )
        }

        try {
            classifier = WindowClassifier(applicationContext)
            Log.d(TAG, "Classifier loaded.")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to initialize classifier.", e)
        }

        HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND).apply {
            start()
            // Get the HandlerThread'csvLines Looper and use it for our Handler
            mServiceLooper = looper
            mServiceHandler = ServiceHandler(looper)
        }


        mServiceHandler?.obtainMessage()?.also { msg ->
            msg.arg1 = startId
            mServiceHandler?.sendMessage(msg)
        }
        // If we get killed, after returning from here, restart
        return START_NOT_STICKY
    }

    class ActiveDayFloats(val activeDay: ActiveDay) {
        val id = activeDay.id
        var date = activeDay.date
        var stepCount
            get() = activeDay.stepCount
            set(value) {
                activeDay.stepCount = value
            }
        var walkingSecconds = activeDay.walkingSeconds.toFloat()
            set(value) {
                field = value
                activeDay.walkingSeconds = field.toInt()
            }
        var runningSeconds = activeDay.runningSeconds.toFloat()
            set(value) {
                field = value
                activeDay.runningSeconds = field.toInt()
            }
        var descendingSeconds = activeDay.descendingSeconds.toFloat()
            set(value) {
                field = value
                activeDay.descendingSeconds = field.toInt()
            }
        var ascendingSeconds = activeDay.ascendingSeconds.toFloat()
            set(value) {
                field = value
                activeDay.ascendingSeconds = field.toInt()
            }
    }


    override fun onBind(intent: Intent): IBinder? {
        // We don't provide binding, so return null
        return null
    }

    override fun onDestroy() {
        fakeSensor.stop()
        bleConnection.dispose()
        this.stopBackgroundThread()
        Toast.makeText(this, "Background classification service stopped", Toast.LENGTH_SHORT).show()
    }
    //endregion

    //region fake sensor
    private fun startFakeSensor() {
        fakeSensor.start()
    }

    private inner class FakeSensor {

        private var running: Boolean = false
        private lateinit var job: Job
        private lateinit var reader: CSVReader

        val csvLines by lazy { generateCSVLines() }

        fun generateCSVLines(): List<Array<String>> {
            reset()
            do {
                val l = reader.readNext()
            } while (l[0] != "timestamp")
            while (reader.peek() != null) {
                return reader.readAll()
            }
            return emptyList()
        }

        val dataFile = "fake_data/alex_100_wal_1.csv"

        fun reset() {
            val stream = applicationContext.resources.assets.open(dataFile)
            reader = CSVReader(stream.bufferedReader())
        }

        fun start() {
            if (running)
                return
            connected = true
            Toast.makeText(this@AccelDataService, "Starting fake sensor...", Toast.LENGTH_SHORT).show()
            reset()
            job = GlobalScope.launch(Dispatchers.IO) {
                running = true
                csvLines.forEach { stringArray ->
                    delay(38L)
                    handlePacket(stringArray.slice(2 until 8).toFloatArray())
                }
                running = false
            }
        }

        private fun List<String>.toFloatArray(): FloatArray = FloatArray(this.size) { i ->
            this[i].toFloat()
        }

        fun stop() {
            if (!running)
                return
            connected = false
            job.cancel()
            reader.close()
            running = false
        }


    }
//endregion

    //region thread control methods
    /** Starts a background thread and its [Handler].  */
    private fun startBackgroundThread() {
        backgroundThread = HandlerThread(HANDLE_THREAD_NAME)
        backgroundThread?.start()
        backgroundHandler = Handler(backgroundThread?.looper)
        synchronized(lock) {
            runClassifier = true
        }
        backgroundHandler?.post(periodicClassify)
    }

    /** Stops the background thread and its [Handler].  */
    private fun stopBackgroundThread() {
        backgroundHandler?.removeCallbacksAndMessages(null)
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
            synchronized(lock) {
                runClassifier = false
            }
        } catch (e: InterruptedException) {
            Log.e(TAG, "Interrupted when stopping background thread", e)
        }

    }
    //endregion

    //region classification
    private val periodicClassify = object : Runnable {
        override fun run() {
            synchronized(lock) {
                if (runClassifier) {
                    classify()
                    Thread.sleep(180)
                }
            }
            backgroundHandler?.post(this)
        }
    }

    private var lastClassifyTime: Long = 0L

    private val stepCounter = StepCounter()
    private var runIterationCounter = 0
    private val maxPacketWaitTime = 1000L

    private fun classify() {
        val time = System.currentTimeMillis()
        if (!connected) {
            return
        }
        var classification = "Unknown"
        if (time - lastPacketTime > maxPacketWaitTime) {
            lastClassifyTime = 0
            broadcastClassification(classification)
            return
        }
        val rolled = ringBuffer.getRolled()
        val d = rolled.toArrayOfFloatArrays()
        if (!idleDetector.isIdle(ringBuffer)) {
            classifier.classifyInput(d)
            val c = classifier.getProbabilities()
            classification = getClassName(c) { x -> argmax(x) }
        } else {
            classification = "Idle"
        }
        broadcastClassification(classification)
        val delta = if (lastClassifyTime <= 0) {
            10L
        } else {
            time - lastClassifyTime
        }
        updateRepo(classification, delta)
        lastClassifyTime = time
    }

    var stepCount = 0
    var newSteps = 0
    private fun addSteps(stepCount: Int) {
        newSteps += stepCount
        this.stepCount += stepCount
        Log.i("StepCount", "Step count: ${this.stepCount} (+$stepCount)")
        GlobalScope.launch(Dispatchers.IO) {
            updateRepo(stepCount)
        }
    }

    private fun updateRepo(classification: String, delta: Long) {
        val deltaSeconds = delta / 1000f
        when (classification) {
            "Walking" -> {
                currentActiveDay.walkingSecconds += deltaSeconds
            }
            "Running" -> {
                currentActiveDay.runningSeconds += deltaSeconds
            }
            "Descending" -> {
                currentActiveDay.descendingSeconds += deltaSeconds
            }
            "Ascending" -> {
                currentActiveDay.ascendingSeconds += deltaSeconds
            }
        }
        repo.update(currentActiveDay.activeDay)
    }

    private fun updateRepo(steps: Int) {
        currentActiveDay.stepCount += steps
        repo.update(currentActiveDay.activeDay)
    }

    private val idleDetector = IdleDetector()

    private fun broadcastClassification(prediction: String) {
        val intent = Intent("predicted_class")
                .putExtra("Prediction", prediction)
                .putExtra("Steps", newSteps)
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        newSteps = 0
    }
    //endregion

    //region helpers
    private fun <A> getClassName(probs: A, argSelect: (A) -> Int): String {
        return when (argSelect(probs)) {
            0 -> "Walking"
            1 -> "Running"
            2 -> "Descending"
            3 -> "Ascending"
            else -> "Unknown"
        }
    }

    private fun argmax(input: FloatArray): Int {
        var out = -1
        var max = Float.NEGATIVE_INFINITY
        for (i in 0 until input.size) {
            if (input[i] >= max) {
                max = input[i]
                out = i
            }
        }
        return out
    }

    private fun argmax(input: DoubleArray): Pair<Int, Double> {
        var out = -1
        var max = Double.NEGATIVE_INFINITY
        for (i in 0 until input.size) {
            if (input[i] >= max) {
                max = input[i]
                out = i
            }
        }
        return Pair(out, max)
    }

    private fun argmin(input: FloatArray): Pair<Int, Float> {
        var out = -1
        var min = Float.POSITIVE_INFINITY
        for (i in 0 until input.size) {
            if (input[i] <= min) {
                min = input[i]
                out = i
            }
        }
        return Pair(out, min)
    }

    private fun argmin(input: DoubleArray): Pair<Int, Double> {
        var out = -1
        var min = Double.POSITIVE_INFINITY
        for (i in 0 until input.size) {
            if (input[i] <= min) {
                min = input[i]
                out = i
            }
        }
        return Pair(out, min)
    }
//endregion
}