package tk.zwander.seekbarpreference

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.ColorUtils
import com.rey.material.widget.Slider
import kotlinx.android.synthetic.main.seekbar_guts.view.*
import java.text.DecimalFormat

open class SeekBarView : ConstraintLayout, View.OnClickListener, Slider.OnPositionChangeListener {
    constructor(context: Context) : super(context) { init(null) }
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) { init(attributeSet) }
    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int) : super(
        context,
        attributeSet,
        defStyleAttr
    ) { init(attributeSet) }

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

    private var _progress = 0
    private var progress: Int
        get() = _progress
        set(value) {
            _progress = value
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
            value_holder.isClickable = value
            value_holder.isEnabled = value
            value_holder.setOnClickListener(if (value) this else null)
            bottom_line.visibility = if (value) View.VISIBLE else View.INVISIBLE
        }

    var dialogStyle = 0

    private var sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    var listener: SeekBarListener? = null

    private fun init(attributeSet: AttributeSet?) {
        if (attributeSet != null) {
            val array = context.theme.obtainStyledAttributes(attributeSet, R.styleable.SeekBarView, 0, 0)

            var min = minValue
            var max = maxValue
            var scl = scale
            var unt = units
            var style = dialogStyle

            for (i in 0 until array.indexCount) {
                when (val a = array.getIndex(i)) {
                    R.styleable.SeekBarView_view_defaultValue -> defaultValue = array.getInteger(a, defaultValue)
                }
            }

            for (i in 0 until array.indexCount) {
                when (val a = array.getIndex(i)) {
                    R.styleable.SeekBarView_minValue -> min = array.getInteger(a, minValue)
                    R.styleable.SeekBarView_maxValue -> max = array.getInteger(a, maxValue)
                    R.styleable.SeekBarView_scale -> scl = array.getFloat(a, scale)
                    R.styleable.SeekBarView_units -> unt = array.getString(a)
                    R.styleable.SeekBarView_dialogStyle -> style = array.getInt(a, style)
                }
            }

            val isPref = array.getBoolean(R.styleable.SeekBarView_isPreference, false)

            _progress = defaultValue

            View.inflate(context, R.layout.seekbar_guts, this)

            if (!isPref) {
                onBind(min, max, progress, defaultValue, scl, unt, "", style, null)
            }

            array.recycle()
        } else View.inflate(context, R.layout.seekbar_guts, this)

        onFinishInflate()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        val colorAttr = context.theme.obtainStyledAttributes(TypedValue().data, intArrayOf(R.attr.colorAccent))
        val color = colorAttr.getColor(0, 0)
        colorAttr.recycle()

        seekbar.setPrimaryColor(color)
        seekbar.setSecondaryColor(ColorUtils.setAlphaComponent(color, 0x33))

        up.setOnClickListener(this)
        down.setOnClickListener(this)
        reset.setOnClickListener(this)
        value_holder.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when(v.id) {
            R.id.value_holder -> CustomInputDialog(
                context, minValue,
                maxValue, progress, scale,
                dialogStyle
            ) { value ->
                listener?.onProgressChanged(value, value * scale)
                setValue(value.toFloat(), true)
            }
                .show()
            R.id.up -> {
                val newValue = progress + 1
                if (newValue <= maxValue) {
                    listener?.onProgressAdded()
                    listener?.onProgressChanged(newValue, newValue * scale)
                    setValue(newValue.toFloat(), true)
                }
            }
            R.id.down -> {
                val newValue = progress - 1
                if (newValue >= minValue) {
                    listener?.onProgressSubtracted()
                    listener?.onProgressChanged(newValue, newValue * scale)
                    setValue(newValue.toFloat(), true)
                }
            }
            R.id.reset -> {
                listener?.onProgressReset()
                listener?.onProgressChanged(defaultValue, defaultValue * scale)
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
        listener?.onProgressChanged(newValue, newValue * scale)
        seekbar_value.text = formatProgress(newValue * scale)

        updateFill(newValue)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        seekbar_value.isEnabled = enabled
        value_holder.isClickable = enabled
        value_holder.isEnabled = enabled

        seekbar.isEnabled = enabled
        seekbar.isClickable = enabled

        measurement_unit.isEnabled = enabled
        bottom_line.isEnabled = enabled
        button_holder.isEnabled = enabled
        up.isEnabled = enabled
        down.isEnabled = enabled
        reset.isEnabled = enabled
    }

    fun onBind(minValue: Int,
               maxValue: Int,
               progress: Int,
               defaultValue: Int,
               scale: Float,
               units: String?,
               key: String,
               style: Int,
               listener: SeekBarListener?,
               prefs: SharedPreferences = sharedPreferences) {
        this.key = key
        this._progress = progress
        this.minValue = minValue
        this.maxValue = maxValue
        this.defaultValue = defaultValue
        this.units = units
        this.scale = scale
        this.listener = listener
        this.sharedPreferences = prefs
        this.dialogStyle = style

        seekbar.setValueRange(minValue, maxValue, false)
        setValue(progress.toFloat(), false)
        seekbar.setOnPositionChangeListener(this)
    }

    fun setValue(value: Float, animate: Boolean) {
        seekbar.setOnPositionChangeListener(null)
        seekbar.setValue(value, true)
        seekbar.setOnPositionChangeListener(this)

        progress = if (value > maxValue) maxValue else if (value < minValue) minValue else value.toInt()

        seekbar_value.text = formatProgress(value * scale)

        updateFill(value.toInt())
    }

    fun getCurrentProgress() = progress

    fun setValueRange(min: Int, max: Int, animate: Boolean) {
        minValue = min
        maxValue = max
        seekbar.setValueRange(min, max, animate)
    }

    private fun updateFill(value: Int) {
        if (!seekbar.isThumbStrokeAnimatorRunning) {
            if (value == defaultValue)
                seekbar.setThumbFillPercent(0)
            else
                seekbar.setThumbFillPercent(1)
        }
    }

    private fun formatProgress(progress: Float): String {
        return DecimalFormat("0.##").format(progress.toDouble()) + (if (units != null) " $units" else "")
    }

    private fun persistProgress(progress: Int) {
        if (key.isNotBlank())
            sharedPreferences
                .edit()
                .putInt(key, progress)
                .apply()
    }

    interface SeekBarListener {
        fun onProgressChanged(newValue: Int, newScaledValue: Float)
        fun onProgressReset()
        fun onProgressAdded()
        fun onProgressSubtracted()
    }
}