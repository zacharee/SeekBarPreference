package tk.zwander.seekbarpreference

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialog
import tk.zwander.seekbarpreference.databinding.ValueSelectorDialogBinding
import java.text.DecimalFormat

open class CustomInputDialog(
    private val context: Context,
    minValue: Float,
    maxValue: Float,
    unscaledCurrent: Float,
    private val scale: Float,
    private val listener: ((progress: Float) -> Unit)? = null
) {
    private val minValue = minValue * scale
    private val maxValue = maxValue * scale
    private val currentValue = unscaledCurrent * scale

    private val dialogBinding = ValueSelectorDialogBinding.inflate(LayoutInflater.from(context))

    private val dialog = object : BottomSheetDialog(context) {
        init {
            setContentView(dialogBinding.root)
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            val maxWidth = context.resources.getDimensionPixelSize(R.dimen.seekbar_max_bottom_sheet_width)
            val screenWidth = context.resources.displayMetrics.widthPixels

            window?.setLayout(if (screenWidth > maxWidth) maxWidth else ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
    }

    init {
        dialogBinding.minValue.text = formatValue(this.minValue.toString())
        dialogBinding.maxValue.text = formatValue(this.maxValue.toString())
        dialogBinding.customValue.hint = formatValue(currentValue.toString())

        dialogBinding.btnApply.setOnClickListener { tryApply() }
        dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }
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
            value = dialogBinding.customValue.text.toString().toFloat()

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

        listener?.invoke(value / scale)
        dialog.dismiss()
    }

    private fun notifyWrongInput() {
        with(dialogBinding.customValue) {
            text = null
            hint = context.resources.getString(R.string.bad_input)
        }
    }

    private fun formatValue(value: String): String {
        return DecimalFormat("0.##").format(value.toDouble())
    }
}