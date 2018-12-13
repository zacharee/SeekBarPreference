package tk.zwander.seekbarpreference

import android.content.Context
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
import kotlin.math.min

class SeekBarView : ConstraintLayout, View.OnClickListener, Slider.OnPositionChangeListener {
    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)
    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int) : super(
        context,
        attributeSet,
        defStyleAttr
    )

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
    var maxValue = 100
    var progress = 0
    var scale = 1f

    var scaledProgress: Float
        get() = progress * scale
        set(value) {
            progress = (progress / scale ).toInt()
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
            }
            v.id == R.id.down -> {
                listener?.onProgressSubtracted()
            }
            v.id == R.id.reset -> {
                listener?.onProgressReset()
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

    fun onBind(minValue: Int, maxValue: Int, progress: Int, scale: Float, units: String?) {
        this.minValue = minValue
        this.maxValue = maxValue
        this.units = units
        this.scale = scale
        this.progress = progress

        seekBar.setValueRange(minValue, maxValue, false)
        setValue(progress.toFloat(), false)
        seekBar.setOnPositionChangeListener(this)
    }

    fun setValue(value: Float, animate: Boolean) {
        seekBar.setOnPositionChangeListener(null)
        seekBar.setValue(value, true)
        seekBar.setOnPositionChangeListener(this)

        progress = value.toInt()

        valueView.text = formatProgress(progress * scale)

        updateFill(value.toInt())
    }

    fun setValueRange(min: Int, max: Int, animate: Boolean) {
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

    interface SeekBarListener {
        fun onProgressChanged(newValue: Int)
        fun onProgressReset()
        fun onProgressAdded()
        fun onProgressSubtracted()
    }
}