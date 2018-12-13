package tk.zwander.seekbarpreference

import android.app.Dialog
import android.content.Context
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import java.text.DecimalFormat

class CustomInputDialog(
    context: Context,
    minValue: Int,
    maxValue: Int,
    currentValue: Int,
    private val scale: Float,
    private val listener: ((progress: Int) -> Unit)? = null
) {

    private val TAG = javaClass.simpleName

    private var dialog: Dialog? = null
    private var customValueView: EditText? = null

    private val minValue = minValue * scale
    private val maxValue = maxValue * scale
    private val currentValue = currentValue * scale

    init {
        init(AlertDialog.Builder(context))
    }

    private fun init(dialogBuilder: AlertDialog.Builder) {
        val dialogView = LayoutInflater.from(dialogBuilder.context).inflate(R.layout.value_selector_dialog, null)
        dialog = dialogBuilder.setView(dialogView).create()

        val minValueView = dialogView.findViewById<TextView>(R.id.minValue)
        val maxValueView = dialogView.findViewById<TextView>(R.id.maxValue)
        customValueView = dialogView.findViewById(R.id.customValue)

        minValueView.text = formatValue(minValue.toString())
        maxValueView.text = formatValue(maxValue.toString())
        customValueView?.hint = formatValue(currentValue.toString())

        val colorView = dialogView.findViewById<LinearLayout>(R.id.dialog_color_area)
        colorView.setBackgroundColor(fetchAccentColor(dialogBuilder.context))

        val applyButton = dialogView.findViewById<Button>(R.id.btn_apply)
        val cancelButton = dialogView.findViewById<Button>(R.id.btn_cancel)

        applyButton.setOnClickListener { tryApply() }

        cancelButton.setOnClickListener { dialog?.dismiss() }
    }

    private fun fetchAccentColor(context: Context): Int {
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
            value = java.lang.Float.parseFloat(customValueView!!.text.toString())
            if (value > maxValue) {
                Log.e(TAG, "wrong input( > than required): " + customValueView!!.text.toString())
                notifyWrongInput()
                return
            } else if (value < minValue) {
                Log.e(TAG, "wrong input( < then required): " + customValueView!!.text.toString())
                notifyWrongInput()
                return
            }
        } catch (e: Exception) {
            Log.e(TAG, "wrong input(non-integer): " + customValueView!!.text.toString())
            notifyWrongInput()
            return
        }

        listener?.invoke((value / scale).toInt())
        dialog?.dismiss()
    }

    private fun notifyWrongInput() {
        customValueView!!.setText("")
        customValueView!!.hint = "Wrong Input!"
    }

    private fun formatValue(value: String): String {
        return DecimalFormat("0.##").format(value.toDouble())
    }
}