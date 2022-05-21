package tk.zwander.seekbarpreference

import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.ColorUtils
import androidx.preference.PreferenceManager
import com.google.android.material.slider.Slider
import tk.zwander.seekbarpreference.databinding.SeekbarGutsNewBinding
import java.text.DecimalFormat
import kotlin.math.max

open class SeekBarViewNew : ConstraintLayout, View.OnClickListener, Slider.OnChangeListener {
    constructor(context: Context) : super(context) { init(null) }
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) { init(attributeSet) }
    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int) : super(
        context,
        attributeSet,
        defStyleAttr
    ) { init(attributeSet) }

    var units: String? = null
    var defaultValue = 0f
    var minValue = 0f
        set(value) {
            field = value
            if (progress < value) progress = value
        }
    var maxValue = 100f
        set(value) {
            field = value
            if (progress > value) progress = value
        }
    var stepSize = 0f

    private var _progress = 0f
    private var progress: Float
        get() = _progress
        private set(value) {
            _progress = value
            persistProgress(value)
        }
    private var key: String = ""

    var dialogEnabled = true
        set(value) {
            field = value
            binding.valueHolder.isClickable = value
            binding.valueHolder.isEnabled = value
            binding.valueHolder.setOnClickListener(if (value) this else null)
            binding.bottomLine.visibility = if (value) View.VISIBLE else View.INVISIBLE
        }

    private var sharedPreferences: SharedPreferences? = PreferenceManager.getDefaultSharedPreferences(context)
    var listener: SeekBarListener? = null

    private val binding by lazy { SeekbarGutsNewBinding.inflate(LayoutInflater.from(context), this) }

    private fun init(attributeSet: AttributeSet?) {
        if (attributeSet != null) {
            val array = context.theme.obtainStyledAttributes(attributeSet, R.styleable.SeekBarViewNew, 0, 0)

            val min = array.getFloat(R.styleable.SeekBarViewNew_minValueFloat, minValue)
            val max = array.getFloat(R.styleable.SeekBarViewNew_maxValueFloat, maxValue)
            val unt = array.getString(R.styleable.SeekBarViewNew_units) ?: units
            val scl = array.getFloat(R.styleable.SeekBarViewNew_stepSize, stepSize)

            defaultValue = array.getFloat(R.styleable.SeekBarViewNew_view_defaultValueFloat, defaultValue)

            val isPref = array.getBoolean(R.styleable.SeekBarViewNew_isPreference, false)

            _progress = defaultValue

            binding

            if (!isPref) {
                onBind(min, max, progress, scl, defaultValue, unt, "", null)
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

        binding.seekbar.thumbStrokeColor = ColorStateList.valueOf(color)
        binding.seekbar.trackActiveTintList = ColorStateList.valueOf(color)
        binding.seekbar.trackInactiveTintList = ColorStateList.valueOf(ColorUtils.setAlphaComponent(color, 0x33))
        binding.seekbar.isTickVisible = false
        binding.seekbar.haloTintList = ColorStateList.valueOf(ColorUtils.setAlphaComponent(color, 0x22))
        binding.seekbar.thumbTintList = ColorStateList.valueOf(Color.TRANSPARENT)

//        binding.seekbar.setPrimaryColor(color)
//        binding.seekbar.setSecondaryColor(ColorUtils.setAlphaComponent(color, 0x33))

        binding.up.setOnClickListener(this)
        binding.down.setOnClickListener(this)
        binding.reset.setOnClickListener(this)
        binding.valueHolder.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when(v.id) {
            R.id.value_holder -> {
                CustomInputDialog(
                    context, minValue,
                    maxValue, progress, 1f
                ) { value ->
                    listener?.onProgressChanged(value)
                    setValue(value)
                }.show()
            }
            R.id.up -> {
                val newValue = progress + stepSize
                if (newValue <= maxValue) {
                    listener?.onProgressAdded()
                    listener?.onProgressChanged(newValue)
                    setValue(newValue)
                }
            }
            R.id.down -> {
                val newValue = progress - stepSize
                if (newValue >= minValue) {
                    listener?.onProgressSubtracted()
                    listener?.onProgressChanged(newValue)
                    setValue(newValue)
                }
            }
            R.id.reset -> {
                listener?.onProgressReset()
                listener?.onProgressChanged(defaultValue)
                setValue(defaultValue)
            }
        }
    }

    override fun onValueChange(slider: Slider, value: Float, fromUser: Boolean) {
        this.progress = value
        listener?.onProgressChanged(value)
        binding.seekbarValue.text = formatProgress(value)

        updateFill(value)
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

    fun onBind(minValue: Float,
               maxValue: Float,
               progress: Float,
               stepSize: Float,
               defaultValue: Float,
               units: String?,
               key: String,
               listener: SeekBarListener?,
               prefs: SharedPreferences? = sharedPreferences) {
        this.key = key
        this._progress = progress
        this.minValue = minValue
        this.maxValue = maxValue
        this.defaultValue = defaultValue
        this.units = units
        this.listener = listener
        this.sharedPreferences = prefs
        this.stepSize = stepSize

        binding.seekbar.valueFrom = minValue
        binding.seekbar.valueTo = maxValue
        binding.seekbar.stepSize = stepSize
        setValue(progress)
        binding.seekbar.addOnChangeListener(this)
    }

    fun setValue(value: Float) {
        binding.seekbar.removeOnChangeListener(this)
        binding.seekbar.value = value.coerceIn(minValue, maxValue)
        binding.seekbar.addOnChangeListener(this)

        progress = value.coerceIn(minValue, maxValue)

        binding.seekbarValue.text = formatProgress(value)

        updateFill(value)
    }

    fun setValueRange(min: Float, max: Float) {
        minValue = min
        maxValue = max
        binding.seekbar.valueFrom = min
        binding.seekbar.valueTo = maxValue
    }

    private fun updateFill(value: Float) {
        binding.seekbar.thumbStrokeWidth = if (value != defaultValue) binding.seekbar.thumbRadius.toFloat() else binding.seekbar.thumbRadius.toFloat() / 4f
    }

    private fun formatProgress(progress: Float): String {
        return DecimalFormat("0.##").format(progress.toDouble()) + (if (units != null) " $units" else "")
    }

    private fun persistProgress(progress: Float) {
        if (key.isNotBlank())
            sharedPreferences
                ?.edit()
                ?.putFloat(key, progress)
                ?.apply()
    }

    interface SeekBarListener {
        fun onProgressChanged(newValue: Float)
        fun onProgressReset()
        fun onProgressAdded()
        fun onProgressSubtracted()
    }
}