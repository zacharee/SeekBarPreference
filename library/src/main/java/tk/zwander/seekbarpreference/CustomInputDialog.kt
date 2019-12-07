package tk.zwander.seekbarpreference

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.value_selector_dialog.view.*
import java.text.DecimalFormat

open class CustomInputDialog(
    private val activity: AppCompatActivity,
    minValue: Int,
    maxValue: Int,
    unscaledCurrent: Int,
    private val scale: Float,
    style: Int,
    private val listener: ((progress: Int) -> Unit)? = null
) {
    private val minValue = minValue * scale
    private val maxValue = maxValue * scale

    private val dialogFragment = InputDialogFragment.newInstance(
        minValue, maxValue, unscaledCurrent, scale, style
    )

    fun show() {
        dialogFragment.show(activity.supportFragmentManager, null)
    }

    fun tryApply(value: Float) {
        try {
            if (value > maxValue) {
                dialogFragment.notifyWrongInput()
                return
            } else if (value < minValue) {
                dialogFragment.notifyWrongInput()
                return
            }
        } catch (e: Exception) {
            dialogFragment.notifyWrongInput()
            return
        }

        listener?.invoke((value / scale).toInt())
        dialogFragment.dismiss()
    }
}