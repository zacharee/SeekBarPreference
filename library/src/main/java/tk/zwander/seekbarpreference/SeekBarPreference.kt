package tk.zwander.seekbarpreference

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

open class SeekBarPreference : Preference, SharedPreferences.OnSharedPreferenceChangeListener {
    var minValue = 0
        set(value) {
            field = value
            notifyChanged()
        }
    var maxValue = 100
        set(value) {
            field = value
            notifyChanged()
        }
    var scale = 1f
    var units: String? = null
        set(value) {
            field = value
            notifyChanged()
        }
    var progress: Int
        get() = getPersistedInt(defaultValue)
        set(value) {
            if (progress != value
                && value >= minValue && value <= maxValue
            ) {
                callChangeListener(value * scale)
                persistInt(value)
                notifyChanged()
            }
        }
    var scaledProgress: Float
        get() = progress * scale
        set(value) {
            progress = (value / scale).toInt()
        }

    var defaultValue = 0
        set(value) {
            field = value
            super.setDefaultValue(value)
            notifyChanged()
        }

    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {
        init(attributeSet)
    }

    @SuppressLint("PrivateResource")
    private fun init(attributeSet: AttributeSet?) {
        isPersistent = true
        layoutResource = R.layout.seekbar_preference_layout
        widgetLayoutResource = R.layout.seekbar

        if (attributeSet != null) {
            val defArray = context.theme.obtainStyledAttributes(attributeSet, R.styleable.Preference, 0, 0)

            defaultValue = defArray.getInteger(R.styleable.Preference_android_defaultValue, defaultValue)

            defArray.recycle()

            val array = context.theme.obtainStyledAttributes(attributeSet, R.styleable.SeekBarPreference, 0, 0)

            for (i in 0 until array.length()) {
                when (val a = array.getIndex(i)) {
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

            array.recycle()
        }
    }

    override fun onAttached() {
        super.onAttached()

        sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDetached() {
        super.onDetached()

        sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            this.key -> {
                callChangeListener(scaledProgress)
            }
        }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val seekbar = holder.itemView.findViewById(R.id.seekbar_root) as SeekBarView
        seekbar.onBind(minValue, maxValue, progress, defaultValue, scale, units, key, null, sharedPreferences)
    }

    override fun setDefaultValue(defaultValue: Any?) {
        this.defaultValue = defaultValue?.toString()?.toInt() ?: return
    }
}