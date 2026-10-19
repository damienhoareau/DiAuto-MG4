package com.andrerinas.openheadunit.main

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.andrerinas.openheadunit.aap.protocol.proto.NavigationStatus.NavigationManeuver.NavigationType
import com.andrerinas.openheadunit.hud.ClusterIconSet
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
    private lateinit var imageWidth: EditText
    private lateinit var imageHeight: EditText
    private lateinit var imageResult: TextView
    private var imageCount = 0
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
        maneuver.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            NavigationType.values().map { "${it.name} (${it.number})" }
        )
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
        iconSetChoice.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            listOf("Leave the cluster's setting as it is") +
                ClusterIconSet.values().map { "${it.label} (${it.navigationType})" }
        )
        column.addView(iconSetChoice)
        iconSetStatus = TextView(this)
        column.addView(iconSetStatus)

        column.addView(label("Picture for the adapter (onUpdateImage): size in pixels"))
        val sizes = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        imageWidth = field("width", "400", true)
        imageHeight = field("height", "240", true)
        sizes.addView(imageWidth, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        sizes.addView(imageHeight, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        column.addView(sizes)
        column.addView(Button(this).apply {
            text = "Send a test picture"
            setOnClickListener { sendTestImage() }
        })
        imageResult = TextView(this)
        column.addView(imageResult)

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

        setContentView(ScrollView(this).apply {
            isFillViewport = true
            addView(column)
        })
        showIconSet()
        SaicNavigationOutputs.start(this)
        if (!SaicNavigationOutputs.available(this)) {
            result.text = "com.saicmotor.adapterservice not found: no cluster on this device."
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

    /** A picture that shows, wherever it ends up, that it is this one: its size, a counter and the time. */
    private fun testPicture(width: Int, height: Int, number: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), 0xFF0D47A1.toInt(), 0xFF00897B.toInt(), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.color = Color.WHITE
        paint.strokeWidth = maxOf(2f, height / 60f)
        canvas.drawRect(paint.strokeWidth, paint.strokeWidth, width - paint.strokeWidth, height - paint.strokeWidth, paint)
        canvas.drawLine(0f, 0f, width.toFloat(), height.toFloat(), paint)
        canvas.drawLine(0f, height.toFloat(), width.toFloat(), 0f, paint)
        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.CENTER
        paint.setShadowLayer(4f, 0f, 0f, Color.BLACK)
        paint.textSize = height / 8f
        canvas.drawText("DiAuto cluster test", width / 2f, height * 0.38f, paint)
        canvas.drawText("#$number  " + java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date()), width / 2f, height * 0.58f, paint)
        canvas.drawText("${width}x$height", width / 2f, height * 0.78f, paint)
        return bitmap
    }

    private fun sendTestImage() {
        val width = imageWidth.text.toString().toIntOrNull() ?: 0
        val height = imageHeight.text.toString().toIntOrNull() ?: 0
        if (width < 16 || height < 16) {
            imageResult.text = "Give a width and a height of at least 16."
            return
        }
        // A call carries about a megabyte for everything: stay well under it.
        if (width.toLong() * height * 4 > MAX_PICTURE_BYTES) {
            imageResult.text = "Too big for one call: ${width}x$height is ${width.toLong() * height * 4 / 1000} kB, " +
                "the limit here is ${MAX_PICTURE_BYTES / 1000} kB."
            return
        }
        SaicNavigationOutputs.start(this)
        val bitmap = testPicture(width, height, ++imageCount)
        imageResult.text = "Sending picture #$imageCount..."
        SaicNavigationOutputs.sendImage(bitmap) { outcome ->
            bitmap.recycle()
            runOnUiThread { imageResult.text = "Picture #$imageCount: $outcome" }
        }
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
        const val MAX_PICTURE_BYTES = 800_000L
    }

    override fun onDestroy() {
        handler.removeCallbacks(keepAlive)
        if (current != null) SaicNavigationOutputs.stop()
        restoreNavigationType()
        super.onDestroy()
    }
}
