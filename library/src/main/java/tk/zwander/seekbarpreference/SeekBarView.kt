package tk.zwander.seekbarpreference

import android.content.Context
import android.preference.PreferenceManager
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.ColorUtils
import com.rey.material.widget.Slider
import java.text.DecimalFormat

class SeekBarView : ConstraintLayout, View.OnClickListener, Slider.OnPositionChangeListener {
    constructor(context: Context) : super(context) { init(null) }
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) { init(attributeSet) }
    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int) : super(
        context,
        attributeSet,
        defStyleAttr
    ) { init(attributeSet) }

    val seekBar: Slider by lazy { findViewById<Slider>(R.id.seekbar) }
    val valueView: TextView by lazy { findViewById<TextView>(R.id.seekbar_value) }
    val measurementView: TextView by lazy { findViewById<TextView>(R.id.measurement_unit) }
    val valueHolderView: LinearLayout by lazy { findViewById<LinearLayout>(R.id.value_holder) }
    val buttonHolderView: LinearLayout by lazy { findViewById<LinearLayout>(R.id.button_holder) }
    val bottomLineView: FrameLayout by lazy { findViewById<FrameLayout>(R.id.bottom_line) }
    val up: ImageView by lazy { findViewById<ImageView>(R.id.up) }
    val down: ImageView by lazy { findViewById<ImageView>(R.id.down) }
    val reset: ImageView by lazy { findViewById<ImageView>(R.id.reset) }

    var units: String? = null
    var defaultValue = 0
    var minValue = 0
        set(value) {
            field = value
            if (progress < value) progress = value
        }
    var maxValue = 100
        set(value) {
            field = value
            if (progress > value) progress = value
        }
    private var progress = 0
        set(value) {
            field = value
            persistProgress(value)
        }
    private var key: String = ""
    var scale = 1f

    var scaledProgress: Float
        get() = progress * scale
        set(value) {
            setValue(value / scale, true)
        }

    var dialogEnabled = true
        set(value) {
            field = value
            valueHolderView.isClickable = value
            valueHolderView.isEnabled = value
            valueHolderView.setOnClickListener(if (value) this else null)
            bottomLineView.visibility = if (value) View.VISIBLE else View.INVISIBLE
        }

    var listener: SeekBarListener? = null

    private fun init(attributeSet: AttributeSet?) {
        if (attributeSet != null) {
            val array = context.theme.obtainStyledAttributes(attributeSet, R.styleable.SeekBarView, 0, 0)

            var min = minValue
            var max = maxValue
            var scl = scale
            var unt = units

            for (i in 0 until array.length()) {
                val a = array.getIndex(i)

                when (a) {
                    R.styleable.SeekBarPreference_minValue -> min = array.getInteger(a, minValue)
                    R.styleable.SeekBarPreference_maxValue -> max = array.getInteger(a, maxValue)
                    R.styleable.SeekBarPreference_scale -> scl = array.getFloat(a, scale)
                    R.styleable.SeekBarPreference_units -> unt = array.getString(a)
                    R.styleable.SeekBarView_view_defaultValue -> defaultValue = array.getInteger(a, defaultValue)
                }
            }

            progress = defaultValue

            View.inflate(context, R.layout.seekbar_guts, this)

            onBind(min, max, progress, defaultValue, scl, unt, key)
        } else View.inflate(context, R.layout.seekbar_guts, this)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        val colorAttr = context.theme.obtainStyledAttributes(TypedValue().data, intArrayOf(R.attr.colorAccent))
        val color = colorAttr.getColor(0, 0)
        colorAttr.recycle()

        seekBar.setPrimaryColor(color)
        seekBar.setSecondaryColor(ColorUtils.setAlphaComponent(color, 0x33))

        up.setOnClickListener(this)
        down.setOnClickListener(this)
        reset.setOnClickListener(this)
        valueHolderView.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when {
            v.id == R.id.value_holder -> CustomInputDialog(
                context, minValue,
                maxValue, progress, scale
            ) { value ->
                listener?.onProgressChanged(value)
                setValue(value.toFloat(), true)
            }
                .show()
            v.id == R.id.up -> {
                listener?.onProgressAdded()
                setValue(progress + 1f, true)
            }
            v.id == R.id.down -> {
                listener?.onProgressSubtracted()
                setValue(progress - 1f, true)
            }
            v.id == R.id.reset -> {
                listener?.onProgressReset()
                setValue(defaultValue.toFloat(), true)
            }
        }
    }

    override fun onPositionChanged(
        view: Slider?,
        fromUser: Boolean,
        oldPos: Float,
        newPos: Float,
        oldValue: Int,
        newValue: Int
    ) {
        this.progress = newValue
        listener?.onProgressChanged(newValue)
        valueView.text = formatProgress(newValue * scale)

        updateFill(newValue)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        valueView.isEnabled = enabled
        valueHolderView.isClickable = enabled
        valueHolderView.isEnabled = enabled

        seekBar.isEnabled = enabled
        seekBar.isClickable = enabled

        measurementView.isEnabled = enabled
        bottomLineView.isEnabled = enabled
        buttonHolderView.isEnabled = enabled
        up.isEnabled = enabled
        down.isEnabled = enabled
        reset.isEnabled = enabled
    }

    fun onBind(minValue: Int, maxValue: Int, progress: Int, defaultValue: Int, scale: Float, units: String?, key: String) {
        this.key = key
        this.progress = progress
        this.minValue = minValue
        this.maxValue = maxValue
        this.defaultValue = defaultValue
        this.units = units
        this.scale = scale

        seekBar.setValueRange(minValue, maxValue, false)
        setValue(progress.toFloat(), false)
        seekBar.setOnPositionChangeListener(this)
    }

    fun setValue(value: Float, animate: Boolean) {
        seekBar.setOnPositionChangeListener(null)
        seekBar.setValue(value, true)
        seekBar.setOnPositionChangeListener(this)

        progress = if (value > maxValue) maxValue else if (value < minValue) minValue else value.toInt()

        valueView.text = formatProgress(value * scale)

        updateFill(value.toInt())
    }

    fun getCurrentProgress() = progress

    fun setValueRange(min: Int, max: Int, animate: Boolean) {
        minValue = min
        maxValue = max
        seekBar.setValueRange(min, max, animate)
    }

    private fun updateFill(value: Int) {
        if (!seekBar.isThumbStrokeAnimatorRunning) {
            if (value == defaultValue)
                seekBar.setThumbFillPercent(0)
            else
                seekBar.setThumbFillPercent(1)
        }
    }

    private fun formatProgress(progress: Float): String {
        return DecimalFormat("0.##").format(progress.toDouble()) + (if (units != null) " $units" else "")
    }

    private fun persistProgress(progress: Int) {
        if (!key.isBlank()) PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putInt(key, progress)
                .apply()
    }

    interface SeekBarListener {
        fun onProgressChanged(newValue: Int)
        fun onProgressReset()
        fun onProgressAdded()
        fun onProgressSubtracted()
    }
}