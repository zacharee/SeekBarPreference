package tk.zwander.seekbarpreference

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.AttributeSet
import androidx.core.content.edit
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import kotlin.math.max

open class SeekBarPreferenceNew : Preference, SharedPreferences.OnSharedPreferenceChangeListener {
    var minValue = 0f
        set(value) {
            field = value
            notifyChanged()
        }
    var maxValue = 100f
        set(value) {
            field = value
            notifyChanged()
        }
    var units: String? = null
        set(value) {
            field = value
            notifyChanged()
        }
    var progress: Float
        get() = safeGetPersistedFloat(defaultValue)
        set(value) {
            if (progress != value
                && value >= minValue && value <= maxValue
            ) {
                callChangeListener(value)
                safePersistFloat(value)
                notifyChanged()
            }
        }
    var defaultValue = 0f
        set(value) {
            field = value
            super.setDefaultValue(value)
            notifyChanged()
        }
    var stepSize = 0f
        set(value) {
            field = value
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
        layoutResource = R.layout.seekbar_preference_layout_new
        widgetLayoutResource = R.layout.seekbar_new

        if (attributeSet != null) {
            val defArray = context.theme.obtainStyledAttributes(attributeSet, androidx.preference.R.styleable.Preference, 0, 0)

            defaultValue = defArray.getFloat(androidx.preference.R.styleable.Preference_android_defaultValue, defaultValue)

            defArray.recycle()

            val array = context.theme.obtainStyledAttributes(attributeSet, R.styleable.SeekBarPreferenceNew, 0, 0)

            minValue = array.getFloat(R.styleable.SeekBarPreferenceNew_minValueFloat, minValue)
            maxValue = array.getFloat(R.styleable.SeekBarPreferenceNew_maxValueFloat, maxValue)
            stepSize = array.getFloat(R.styleable.SeekBarPreferenceNew_stepSize, stepSize)
            units = array.getString(R.styleable.SeekBarPreferenceNew_units)

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
                callChangeListener(progress)
            }
        }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val seekbar = holder.itemView.findViewById(R.id.seekbar_root) as SeekBarViewNew
        seekbar.onBind(minValue, maxValue, progress, stepSize, defaultValue, units, key, null, sharedPreferences)
    }

    override fun setDefaultValue(defaultValue: Any?) {
        this.defaultValue = defaultValue?.toString()?.toFloat() ?: return
    }

    private fun safeGetPersistedFloat(defaultValue: Float): Float {
        return try {
            getPersistedFloat(defaultValue)
        } catch (e: Exception) {
            defaultValue
        }
    }

    private fun safePersistFloat(newValue: Float): Boolean {
        return try {
            persistFloat(newValue)
        } catch (e: Exception) {
            sharedPreferences?.edit {
                remove(key)
            }
            persistFloat(newValue)
        }
    }
}