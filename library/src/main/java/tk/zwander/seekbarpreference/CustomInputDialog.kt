package tk.zwander.seekbarpreference

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.value_selector_dialog.*
import java.text.DecimalFormat

open class CustomInputDialog(
    private val context: Context,
    minValue: Int,
    maxValue: Int,
    unscaledCurrent: Int,
    private val scale: Float,
    private val listener: ((progress: Int) -> Unit)? = null
) {
    private val minValue = minValue * scale
    private val maxValue = maxValue * scale
    private val currentValue = unscaledCurrent * scale

    private val dialog = object : BottomSheetDialog(context) {
        init {
            setContentView(R.layout.value_selector_dialog)
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            val maxWidth = context.resources.getDimensionPixelSize(R.dimen.seekbar_max_bottom_sheet_width)
            val screenWidth = context.resources.displayMetrics.widthPixels

            window.setLayout(if (screenWidth > maxWidth) maxWidth else ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
    }

    init {
        dialog.minValue.text = formatValue(this.minValue.toString())
        dialog.maxValue.text = formatValue(this.maxValue.toString())
        dialog.customValue.hint = formatValue(currentValue.toString())

//        dialog.dialog_color_area.setBackgroundColor(fetchAccentColor())

        dialog.btn_apply.setOnClickListener { tryApply() }
        dialog.btn_cancel.setOnClickListener { dialog.dismiss() }
    }

    private fun fetchAccentColor(): Int {
        val typedValue = TypedValue()

        val a = context.obtainStyledAttributes(typedValue.data, intArrayOf(R.attr.colorAccent))
        val color = a.getColor(0, 0)
        a.recycle()

        return color
    }

    fun show() {
        dialog.show()
    }

    private fun tryApply() {
        val value: Float

        try {
            value = dialog.customValue.text.toString().toFloat()

            if (value > maxValue) {
                notifyWrongInput()
                return
            } else if (value < minValue) {
                notifyWrongInput()
                return
            }
        } catch (e: Exception) {
            notifyWrongInput()
            return
        }

        listener?.invoke((value / scale).toInt())
        dialog.dismiss()
    }

    private fun notifyWrongInput() {
        with (dialog.customValue) {
            text = null
            hint = context.resources.getString(R.string.bad_input)
        }
    }

    private fun formatValue(value: String): String {
        return DecimalFormat("0.##").format(value.toDouble())
    }
}