package tk.zwander.seekbarpreference

import android.app.Dialog
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.value_selector_dialog.view.*
import java.text.DecimalFormat

class InputDialogFragment : DialogFragment() {
    companion object {
        const val ARG_MIN_VALUE = "min_value"
        const val ARG_MAX_VALUE = "max_value"
        const val ARG_UNSCALED = "unscaled"
        const val ARG_SCALE = "scale"
        const val ARG_STYLE = "style"

        fun newInstance(min: Int, max: Int, unscaled: Int, scale: Float, style: Int): InputDialogFragment {
            return InputDialogFragment()
                .apply {
                    arguments = Bundle().apply {
                        putInt(ARG_MIN_VALUE, min)
                        putInt(ARG_MAX_VALUE, max)
                        putInt(ARG_UNSCALED, unscaled)
                        putFloat(ARG_SCALE, scale)
                        putInt(ARG_STYLE, style)
                    }
                }
        }
    }

    val minValue by lazy { arguments!!.getInt(ARG_MIN_VALUE) }
    val maxValue by lazy { arguments!!.getInt(ARG_MAX_VALUE) }
    val unscaled by lazy { arguments!!.getInt(ARG_UNSCALED) }
    val style by lazy { arguments!!.getInt(ARG_STYLE) }

    val scale by lazy { arguments!!.getFloat(ARG_SCALE) }

    var callback: CustomInputDialog? = null

    private val dialogView by lazy {
        LayoutInflater.from(context)
            .inflate(R.layout.value_selector_dialog, null).apply {
                minValue.text = formatValue(this.minValue.toString())
                maxValue.text = formatValue(this.maxValue.toString())
                customValue.hint = formatValue((unscaled * scale).toString())

                dialog_color_area.setBackgroundColor(fetchAccentColor())

                btn_apply.setOnClickListener { callback?.tryApply(customValue.text.toString().toFloat()) }
                btn_cancel.setOnClickListener { dismiss() }
            }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext(), style)
            .setView(dialogView)
            .create()
    }

    private fun formatValue(value: String): String {
        return DecimalFormat("0.##").format(value.toDouble())
    }

    private fun fetchAccentColor(): Int {
        val typedValue = TypedValue()

        val a = requireContext().obtainStyledAttributes(typedValue.data, intArrayOf(R.attr.colorAccent))
        val color = a.getColor(0, 0)
        a.recycle()

        return color
    }

    fun notifyWrongInput() {
        with (dialogView.customValue) {
            text = null
            hint = context.resources.getString(R.string.bad_input)
        }
    }
}