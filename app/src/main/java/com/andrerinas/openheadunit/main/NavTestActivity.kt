package com.andrerinas.openheadunit.main

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.graphics.Color
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.graphics.Typeface
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.andrerinas.openheadunit.aap.protocol.proto.NavigationStatus.NavigationManeuver.NavigationType
import com.andrerinas.openheadunit.hud.ClusterIconSet
import com.andrerinas.openheadunit.hud.SaicCall
import com.andrerinas.openheadunit.vehicle.AdasProperties
import com.evsuite.hardware.EVHardware
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import com.andrerinas.openheadunit.hud.SaicGuidance
import com.andrerinas.openheadunit.hud.SaicNavigationMapper
import com.andrerinas.openheadunit.hud.SaicNavigationOutputs
import com.andrerinas.openheadunit.utils.Settings

/**
 * Test bench for the instrument cluster: pick any Android Auto maneuver type (with the roundabout
 * exit number or angle), press Send, and the cluster shows the icon the mapper picks for it, sent
 * through the same path as a real guidance. Without an Android Auto session.
 *
 * The cluster draws its icons from the set the system setting `navigation_type` names (TeleNav, NNG or
 * ISA), each with its own numbering. This screen can set it, to see every set on the cluster; what was
 * there before is put back when the screen closes.
 */
class NavTestActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var current: SaicGuidance? = null
    private lateinit var result: TextView
    private lateinit var maneuver: Spinner
    private lateinit var iconView: ManeuverIconView
    private lateinit var iconFile: TextView
    private lateinit var exitNumber: EditText
    private lateinit var exitAngle: EditText
    private lateinit var distance: EditText
    private lateinit var road: EditText
    private lateinit var lanes: EditText
    private lateinit var carMode: TextView
    private lateinit var speedLimit: EditText
    private lateinit var speedUnit: EditText
    private lateinit var coverage: EditText
    private lateinit var positioned: EditText
    private lateinit var confidence: EditText
    private lateinit var speedResult: TextView
    private lateinit var adasGroup: Spinner
    private lateinit var adasAuto: CheckBox
    private lateinit var adasOutput: TextView
    private val adasExecutor = Executors.newSingleThreadExecutor()
    private val adasReading = AtomicBoolean(false)
    private val adasRefresh = object : Runnable {
        override fun run() {
            readAdas()
            if (adasAuto.isChecked) handler.postDelayed(this, 1000)
        }
    }
    private lateinit var iconSetChoice: Spinner
    private lateinit var iconSetStatus: TextView

    // The navigation_type the car had before this screen changed it, put back on exit.
    private var navigationTypeChanged = false
    private var originalNavigationType: String? = null

    // The cluster drops guidance that is not renewed for 30 s, so a test frame is renewed while shown.
    private val keepAlive = object : Runnable {
        override fun run() {
            current?.let {
                current = it.copy(updatedNs = System.nanoTime())
                SaicNavigationOutputs.update(current)
                handler.postDelayed(this, 10_000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (16 * resources.displayMetrics.density).toInt()
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        fun label(text: String) = TextView(this).apply { this.text = text; setPadding(0, pad / 2, 0, 0) }
        fun field(hint: String, value: String, numeric: Boolean) = EditText(this).apply {
            this.hint = hint
            setText(value)
            setSingleLine()
            if (numeric) inputType = InputType.TYPE_CLASS_NUMBER
        }

        column.addView(TextView(this).apply {
            text = "Cluster navigation test"
            textSize = 22f
        })

        column.addView(label("Android Auto maneuver type"))
        maneuver = Spinner(this)
        maneuver.adapter = whiteAdapter(NavigationType.values().map { "${it.name} (${it.number})" })
        iconView = ManeuverIconView(this)
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        top.addView(maneuver, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        top.addView(iconView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        column.addView(top)
        iconFile = TextView(this)
        column.addView(iconFile)

        column.addView(label("Roundabout exit number (optional)"))
        exitNumber = field("e.g. 2", "", true)
        column.addView(exitNumber)
        column.addView(label("Roundabout exit angle, 1-360 (optional, used by *_WITH_ANGLE)"))
        exitAngle = field("e.g. 90", "", true)
        column.addView(exitAngle)
        column.addView(label("Distance to the maneuver (meters)"))
        distance = field("meters", "300", true)
        column.addView(distance)
        column.addView(label("Street"))
        road = field("street", "Rue de Test", false)
        column.addView(road)
        column.addView(label("Lane patterns, 1-33 separated by commas (optional, e.g. 1,2,3)"))
        lanes = field("lanes", "", false)
        column.addView(lanes)

        column.addView(label("Icon set of the cluster (the system's navigation_type)"))
        iconSetChoice = Spinner(this)
        iconSetChoice.adapter = whiteAdapter(
            listOf("Leave the cluster's setting as it is") +
                ClusterIconSet.values().map { "${it.label} (${it.navigationType})" }
        )
        column.addView(iconSetChoice)
        iconSetStatus = TextView(this)
        column.addView(iconSetStatus)

        column.addView(label("Speed limit through the car's speed assist (test only)"))
        column.addView(TextView(this).apply {
            text = "This writes to the driver assistance system, like the car's own speed limit service does. " +
                "Use it with the car stopped; that service may write over the value."
        })
        carMode = TextView(this)
        column.addView(Button(this).apply {
            text = "Read the car model"
            setOnClickListener {
                SaicNavigationOutputs.start(this@NavTestActivity)
                SaicNavigationOutputs.readCarMode { model -> runOnUiThread { carMode.text = "Car model: $model" } }
            }
        })
        column.addView(carMode)
        val speedRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        speedLimit = field("limit", "50", true)
        speedUnit = field("unit", "0", true)
        speedRow.addView(speedLimit, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        speedRow.addView(speedUnit, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        column.addView(label("Limit (km/h) and unit (raw value the car expects)"))
        column.addView(speedRow)
        val validityRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        coverage = field("coverage", "1", true)
        positioned = field("positioned", "1", true)
        confidence = field("confidence", "10", true)
        validityRow.addView(coverage, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        validityRow.addView(positioned, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        validityRow.addView(confidence, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        column.addView(label("Validity: map coverage, positioned, confidence (2 = no for the first two)"))
        column.addView(validityRow)
        val sendLimit = Button(this).apply { text = "Send the limit"; isEnabled = false }
        val sendNone = Button(this).apply { text = "Send \"no position\""; isEnabled = false }
        val stopped = CheckBox(this).apply {
            text = "The car is stopped"
            setOnCheckedChangeListener { _, checked -> sendLimit.isEnabled = checked; sendNone.isEnabled = checked }
        }
        column.addView(stopped)
        sendLimit.setOnClickListener { sendSpeedLimit() }
        sendNone.setOnClickListener {
            // What the car's own service sends when it has no position: nothing valid to show.
            runSpeedCalls(listOf(SaicCall.IsaSelfDiag(0, 2, 0)))
        }
        val speedButtons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        speedButtons.addView(sendLimit)
        speedButtons.addView(sendNone)
        column.addView(speedButtons)
        speedResult = TextView(this)
        column.addView(speedResult)

        column.addView(label("Driver assistance properties from the car (read only)"))
        adasGroup = Spinner(this)
        adasGroup.adapter = whiteAdapter(AdasProperties.groups.map { it.title })
        column.addView(adasGroup)
        val adasRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        adasRow.addView(Button(this).apply {
            text = "Read now"
            setOnClickListener { readAdas() }
        })
        adasAuto = CheckBox(this).apply {
            text = "Every second"
            setOnCheckedChangeListener { _, checked ->
                handler.removeCallbacks(adasRefresh)
                if (checked) handler.post(adasRefresh)
            }
        }
        adasRow.addView(adasAuto)
        column.addView(adasRow)
        adasOutput = TextView(this).apply {
            typeface = Typeface.MONOSPACE
            textSize = 12f
            setTextIsSelectable(true)
            text = "Pick a group and read it. Lane lines and objects only carry values while the car drives with the camera on."
        }
        column.addView(adasOutput)

        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        buttons.addView(Button(this).apply {
            text = "Send to cluster"
            setOnClickListener { send() }
        })
        buttons.addView(Button(this).apply {
            text = "Stop"
            setOnClickListener { stopGuidance() }
        })
        column.addView(buttons)

        val refresh = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = updatePreview()
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        }
        exitNumber.addTextChangedListener(refresh)
        exitAngle.addTextChangedListener(refresh)
        maneuver.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = updatePreview()
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        iconSetChoice.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = chooseIconSet(position)
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        result = TextView(this).apply { textSize = 18f; setPadding(0, pad, 0, 0) }
        column.addView(result)

        whiten(column)
        setContentView(ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(PAGE_BACKGROUND)
            addView(column)
        })
        showIconSet()
        SaicNavigationOutputs.start(this)
        if (!SaicNavigationOutputs.available(this)) {
            result.text = "com.saicmotor.adapterservice not found: no cluster on this device."
        }
    }

    /** The car's day/night theme can leave dark text on a dark page: the page is dark and its text white. */
    private fun whiten(view: View) {
        when (view) {
            is Button -> return
            is Spinner -> return
            is TextView -> {
                view.setTextColor(Color.WHITE)
                if (view is EditText) view.setHintTextColor(0xFFB0B0B0.toInt())
            }
            is ViewGroup -> for (i in 0 until view.childCount) whiten(view.getChildAt(i))
        }
    }

    /** A drop-down list whose lines are white on the dark page, in the list as well as when closed. */
    private fun whiteAdapter(items: List<String>) = object : ArrayAdapter<String>(
        this, android.R.layout.simple_spinner_dropdown_item, items
    ) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
            (super.getView(position, convertView, parent) as TextView).apply { setTextColor(Color.WHITE) }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
            (super.getDropDownView(position, convertView, parent) as TextView).apply {
                setTextColor(Color.WHITE)
                setBackgroundColor(0xFF303030.toInt())
            }
    }

    private fun chooseIconSet(position: Int) {
        if (position == 0) {
            restoreNavigationType()
            showIconSet()
            return
        }
        val set = ClusterIconSet.values()[position - 1]
        try {
            if (!navigationTypeChanged) {
                originalNavigationType = android.provider.Settings.Global.getString(contentResolver, ClusterIconSet.SETTING)
                navigationTypeChanged = true
            }
            android.provider.Settings.Global.putString(contentResolver, ClusterIconSet.SETTING, set.navigationType)
            showIconSet()
        } catch (e: Exception) {
            iconSetStatus.text = "Could not set navigation_type: ${e.message}"
        }
    }

    private fun restoreNavigationType() {
        if (!navigationTypeChanged) return
        navigationTypeChanged = false
        try {
            android.provider.Settings.Global.putString(contentResolver, ClusterIconSet.SETTING, originalNavigationType)
        } catch (_: Exception) {
        }
    }

    private fun showIconSet() {
        val raw = runCatching { android.provider.Settings.Global.getString(contentResolver, ClusterIconSet.SETTING) }.getOrNull()
        iconSetStatus.text = "navigation_type = ${raw ?: "(not set)"} -> icons of the ${ClusterIconSet.fromSetting(raw).label} set. " +
            "The cluster may read it only when its navigation page changes, so the icons can follow a moment later."
    }

    private fun sendSpeedLimit() {
        val limit = speedLimit.text.toString().toIntOrNull()
        if (limit == null || limit !in 5..200) {
            speedResult.text = "Give a limit between 5 and 200."
            return
        }
        val unit = speedUnit.text.toString().toIntOrNull() ?: 0
        val cover = coverage.text.toString().toIntOrNull() ?: 1
        val position = positioned.text.toString().toIntOrNull() ?: 1
        val conf = confidence.text.toString().toIntOrNull() ?: 10
        // The order of the car's own service: the unit, the value, then whether the value can be trusted.
        runSpeedCalls(listOf(SaicCall.IsaSpeedUnit(unit), SaicCall.IsaSpeedLimit(limit), SaicCall.IsaSelfDiag(cover, position, conf)))
    }

    /** Reads the chosen group off the main thread: each read is a call to the car's service. */
    private fun readAdas() {
        if (!adasReading.compareAndSet(false, true)) return
        val group = AdasProperties.groups[adasGroup.selectedItemPosition]
        val context = applicationContext
        adasExecutor.execute {
            try {
                EVHardware.init(context)
                val text = group.properties.joinToString("\n") { (name, id) -> "$name = ${EVHardware.describeProperty(id)}" }
                runOnUiThread { adasOutput.text = group.title + "\n" + text }
            } catch (e: Exception) {
                runOnUiThread { adasOutput.text = "Could not read: ${e.message}" }
            } finally {
                adasReading.set(false)
            }
        }
    }

    private fun runSpeedCalls(calls: List<SaicCall>) {
        SaicNavigationOutputs.start(this)
        speedResult.text = "Sending..."
        SaicNavigationOutputs.sendTestCalls(calls) { outcome -> runOnUiThread { speedResult.text = outcome } }
    }

    private fun updatePreview() {
        val file = iconView.show(
            NavigationType.values()[maneuver.selectedItemPosition],
            exitNumber.text.toString().toIntOrNull(),
            exitAngle.text.toString().toIntOrNull()
        )
        iconFile.text = file
    }

    private fun send() {
        val type = NavigationType.values()[maneuver.selectedItemPosition]
        val number = exitNumber.text.toString().toIntOrNull()
        val angle = exitAngle.text.toString().toIntOrNull()
        val icon = SaicNavigationMapper.icon(type, number, angle, Settings(this).rightHandDrive)
        val set = ClusterIconSet.current(contentResolver)
        if (icon == null) {
            stopGuidance()
            result.text = "${type.name} -> no icon in the table: the cluster shows nothing."
            return
        }
        val meters = distance.text.toString().toIntOrNull() ?: 0
        val street = road.text.toString()
        val laneList = lanes.text.toString().replace(" ", "")
        current = SaicGuidance(icon, meters, street, lanes = laneList)
        SaicNavigationOutputs.start(this)
        SaicNavigationOutputs.update(current)
        handler.removeCallbacks(keepAlive)
        handler.postDelayed(keepAlive, 10_000)
        result.text = "${type.name} (exit ${number ?: "-"}, angle ${angle ?: "-"}) -> " +
            "${icon.name} (${set.label} tbtType ${set.idFor(icon)}), $meters m, \"$street\"" +
            (if (laneList.isEmpty()) "" else ", lanes $laneList")
    }

    private fun stopGuidance() {
        handler.removeCallbacks(keepAlive)
        current = null
        SaicNavigationOutputs.stop()
        result.text = "Guidance stopped."
    }

    private companion object {
        const val PAGE_BACKGROUND = 0xFF121212.toInt()
    }

    override fun onDestroy() {
        handler.removeCallbacks(keepAlive)
        handler.removeCallbacks(adasRefresh)
        adasExecutor.shutdown()
        if (current != null) SaicNavigationOutputs.stop()
        restoreNavigationType()
        super.onDestroy()
    }
}
