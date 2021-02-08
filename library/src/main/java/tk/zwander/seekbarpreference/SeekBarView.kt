package tk.zwander.seekbarpreference

import android.content.Context
import android.content.SharedPreferences
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.ColorUtils
import androidx.preference.PreferenceManager
import com.rey.material.widget.Slider
import tk.zwander.seekbarpreference.databinding.SeekbarGutsBinding
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
            binding.valueHolder.isClickable = value
            binding.valueHolder.isEnabled = value
            binding.valueHolder.setOnClickListener(if (value) this else null)
            binding.bottomLine.visibility = if (value) View.VISIBLE else View.INVISIBLE
        }

    private var sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    var listener: SeekBarListener? = null

    private val binding by lazy { SeekbarGutsBinding.inflate(LayoutInflater.from(context), this) }

    private fun init(attributeSet: AttributeSet?) {
        if (attributeSet != null) {
            val array = context.theme.obtainStyledAttributes(attributeSet, R.styleable.SeekBarView, 0, 0)

            var min = minValue
            var max = maxValue
            var scl = scale
            var unt = units

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
                }
            }

            val isPref = array.getBoolean(R.styleable.SeekBarView_isPreference, false)

            _progress = defaultValue

            binding

            if (!isPref) {
                onBind(min, max, progress, defaultValue, scl, unt, "", null)
            }

            array.recycle()
        } else binding

        onFinishInflate()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        val colorAttr = context.theme.obtainStyledAttributes(TypedValue().data, intArrayOf(R.attr.colorAccent))
        val color = colorAttr.getColor(0, 0)
        colorAttr.recycle()

        binding.seekbar.setPrimaryColor(color)
        binding.seekbar.setSecondaryColor(ColorUtils.setAlphaComponent(color, 0x33))

        binding.up.setOnClickListener(this)
        binding.down.setOnClickListener(this)
        binding.reset.setOnClickListener(this)
        binding.valueHolder.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when(v.id) {
            R.id.value_holder -> CustomInputDialog(
                context, minValue,
                maxValue, progress, scale
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
        binding.seekbarValue.text = formatProgress(newValue * scale)

        updateFill(newValue)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        binding.seekbarValue.isEnabled = enabled
        binding.valueHolder.isClickable = enabled
        binding.valueHolder.isEnabled = enabled

        binding.seekbar.isEnabled = enabled
        binding.seekbar.isClickable = enabled

        binding.measurementUnit.isEnabled = enabled
        binding.bottomLine.isEnabled = enabled
        binding.buttonHolder.isEnabled = enabled
        binding.up.isEnabled = enabled
        binding.down.isEnabled = enabled
        binding.reset.isEnabled = enabled
    }

    fun onBind(minValue: Int,
               maxValue: Int,
               progress: Int,
               defaultValue: Int,
               scale: Float,
               units: String?,
               key: String,
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

        binding.seekbar.setValueRange(minValue, maxValue, false)
        setValue(progress.toFloat(), false)
        binding.seekbar.setOnPositionChangeListener(this)
    }

    fun setValue(value: Float, animate: Boolean) {
        binding.seekbar.setOnPositionChangeListener(null)
        binding.seekbar.setValue(value.coerceIn(minValue.toFloat(), maxValue.toFloat()), animate)
        binding.seekbar.setOnPositionChangeListener(this)

        progress = value.coerceIn(minValue.toFloat(), maxValue.toFloat()).toInt()

        binding.seekbarValue.text = formatProgress(value * scale)

        updateFill(value.toInt())
    }

    fun getCurrentProgress() = progress

    fun setValueRange(min: Int, max: Int, animate: Boolean) {
        minValue = min
        maxValue = max
        binding.seekbar.setValueRange(min, max, animate)
    }

    private fun updateFill(value: Int) {
        if (!binding.seekbar.isThumbStrokeAnimatorRunning) {
            if (value == defaultValue)
                binding.seekbar.setThumbFillPercent(0)
            else
                binding.seekbar.setThumbFillPercent(1)
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