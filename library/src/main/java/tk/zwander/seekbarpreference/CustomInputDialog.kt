package tk.zwander.seekbarpreference

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import kotlinx.android.synthetic.main.value_selector_dialog.view.*
import java.text.DecimalFormat

class CustomInputDialog(
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

    private val dialogView = LayoutInflater.from(context)
        .inflate(R.layout.value_selector_dialog, null)

    private val dialog = AlertDialog.Builder(context)
        .setView(dialogView)
        .create()

    init {
        dialogView.minValue.text = formatValue(minValue.toString())
        dialogView.maxValue.text = formatValue(maxValue.toString())
        dialogView.customValue.hint = formatValue(currentValue.toString())

        dialogView.dialog_color_area.setBackgroundColor(fetchAccentColor())

        dialogView.btn_apply.setOnClickListener { tryApply() }
        dialogView.btn_cancel.setOnClickListener { dialog?.dismiss() }
    }

    private fun fetchAccentColor(): Int {
        val typedValue = TypedValue()

        val a = context.obtainStyledAttributes(typedValue.data, intArrayOf(R.attr.colorAccent))
        val color = a.getColor(0, 0)
        a.recycle()

        return color
    }

    fun show() {
        dialog?.show()
    }

    private fun tryApply() {
        val value: Float

        try {
            value = dialogView.customValue.text.toString().toFloat()

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
        dialog?.dismiss()
    }

    private fun notifyWrongInput() {
        with (dialogView.customValue) {
            text = null
            hint = context.resources.getString(R.string.bad_input)
        }
    }

    private fun formatValue(value: String): String {
        return DecimalFormat("0.##").format(value.toDouble())
    }
}