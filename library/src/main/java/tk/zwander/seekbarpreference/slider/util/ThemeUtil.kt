package tk.zwander.seekbarpreference.slider.util

import android.util.TypedValue
import android.content.Context
import android.content.res.TypedArray
import tk.zwander.seekbarpreference.R
import java.lang.Exception

object ThemeUtil {
    @JvmStatic
	fun dpToPx(context: Context, dp: Int): Int {
        return (TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ) + 0.5f).toInt()
    }

    private fun getColor(context: Context, id: Int, defaultValue: Int): Int {
        val value = TypedValue()
        try {
            val theme = context.theme
            if (theme != null && theme.resolveAttribute(id, value, true)) {
                if (value.type >= TypedValue.TYPE_FIRST_INT && value.type <= TypedValue.TYPE_LAST_INT) {
                    return value.data
                } else if (value.type == TypedValue.TYPE_STRING) {
                    return context.resources.getColor(value.resourceId)
                }
            }
        } catch (_: Exception) {}
        return defaultValue
    }

    @JvmStatic
    fun colorControlNormal(context: Context, defaultValue: Int): Int {
        return getColor(
            context,
            R.attr.colorControlNormal,
            defaultValue
        )
    }

    @JvmStatic
    fun colorControlActivated(context: Context, defaultValue: Int): Int {
        return getColor(
            context,
            R.attr.colorControlActivated,
            defaultValue
        )
    }

    @JvmStatic
    fun colorControlHighlight(context: Context, defaultValue: Int): Int {
        return getColor(
            context,
            R.attr.colorControlHighlight,
            defaultValue
        )
    }

    @JvmStatic
	fun getType(array: TypedArray, index: Int): Int {
        return array.getType(index)
    }
}