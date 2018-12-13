package tk.zwander.seekbarpreference

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.annotation.IntegerRes
import androidx.preference.DialogPreference
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import kotlinx.android.synthetic.main.seekbar.view.*
import tk.zwander.seekbarpreference.R.attr.*
import tk.zwander.seekbarpreference.R.id.*
import kotlin.math.max

class SeekBarPreference : Preference {
    private var progressInternal = 0
        set(value) {
            if (field != value
                && value >= minValue && value <= maxValue
            ) {
                field = value
                persistInt(value)
            }
        }

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
    var scale = 1f
    var units: String? = null
        set(value) {
            field = value
            notifyChanged()
        }
    var progress: Int
        get() = progressInternal
        set(value) {
            if (progressInternal != value
                && value >= minValue && value <= maxValue
            ) {
                progressInternal = value
                notifyChanged()
                callChangeListener(value)
                persistInt(value)
            }
        }
    var scaledProgress: Float
        get() = progress * scale
        set(value) {
            progress = (value / scale).toInt()
        }

    val defaultValue: Any?
        get() = Preference::class.java.getDeclaredField("mDefaultValue")
            .apply { isAccessible = true }
            .get(this)

    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {
        init(attributeSet)
    }

    private fun init(attributeSet: AttributeSet?) {
        isPersistent = true
        layoutResource = R.layout.seekbar_view_layout
        widgetLayoutResource = R.layout.seekbar

        if (attributeSet != null) {
            val array = context.theme.obtainStyledAttributes(attributeSet, R.styleable.SeekBarPreference, 0, 0)

            for (i in 0 until array.length()) {
                val a = array.getIndex(i)

                when (a) {
                    R.styleable.SeekBarPreference_minValue -> {
                        minValue = array.getInteger(a, minValue)
                    }
                    R.styleable.SeekBarPreference_maxValue -> {
                        maxValue = array.getInteger(a, maxValue)
                    }
                    R.styleable.SeekBarPreference_units -> {
                        units = array.getString(a)
                    }
                    R.styleable.SeekBarPreference_scale -> {
                        scale = array.getFloat(a, scale)
                    }
                }
            }
        }
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        progressInternal = if (isPersistent) getPersistedInt(defaultValue?.toString()?.toInt() ?: return)
        else defaultValue?.toString()?.toInt() ?: return
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        progressInternal = getPersistedInt(defaultValue?.toString()?.toInt() ?: progressInternal)

        (holder.itemView.seekbar_root as SeekBarView).apply {
            onBind(minValue, maxValue, progress, scale, units)
            listener = object : SeekBarView.SeekBarListener {
                override fun onProgressAdded() {
                    this@SeekBarPreference.progress++
                    setValue(progress.toFloat(), true)
                }

                override fun onProgressSubtracted() {
                    this@SeekBarPreference.progress--
                    setValue(progress.toFloat(), true)
                }

                override fun onProgressReset() {
                    this@SeekBarPreference.progress = defaultValue?.toString()?.toInt() ?: this@SeekBarPreference.minValue
                }

                override fun onProgressChanged(newValue: Int) {
                    progressInternal = newValue
                    callChangeListener(progressInternal)
                }
            }
        }
    }
}