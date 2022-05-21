package tk.zwander.seekbarpreference.slider

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import tk.zwander.seekbarpreference.R
import tk.zwander.seekbarpreference.slider.drawable.RippleDrawable

class RippleManager : View.OnClickListener {
    private var clickListener: View.OnClickListener? = null
    private var clickScheduled = false

    /**
     * Should be called in the construction method of view to create a RippleDrawable.
     * @param v
     * @param context
     * @param attrs
     * @param defStyleAttr
     * @param defStyleRes
     */
    fun onCreate(
        v: View,
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) {
        if (v.isInEditMode) return
        val a = context.obtainStyledAttributes(attrs, R.styleable.RippleView, defStyleAttr, defStyleRes)
        val rippleStyle = a.getResourceId(R.styleable.RippleView_rd_style, 0)
        var drawable: RippleDrawable? = null
        if (rippleStyle != 0) {
            drawable = RippleDrawable.Builder(context, rippleStyle).backgroundDrawable(getBackground(v))
                .build()
        } else {
            val rippleEnable = a.getBoolean(R.styleable.RippleView_rd_enable, false)
            if (rippleEnable) drawable =
                RippleDrawable.Builder(context, attrs, defStyleAttr, defStyleRes)
                    .backgroundDrawable(getBackground(v)).build()
        }
        a.recycle()
        if (drawable != null) {
            v.background = drawable
        }
    }

    private fun getBackground(v: View): Drawable? {
        val background = v.background ?: return null
        return if (background is RippleDrawable) background.backgroundDrawable else background
    }

    fun setOnClickListener(l: View.OnClickListener?) {
        clickListener = l
    }

    fun onTouchEvent(v: View, event: MotionEvent?): Boolean {
        val background = v.background
        return background != null && background is RippleDrawable && background.onTouch(v, event!!)
    }

    override fun onClick(v: View) {
        val background = v.background
        var delay: Long = 0
        if (background != null) {
            if (background is RippleDrawable) delay = background.clickDelayTime
        }
        if (delay > 0 && v.handler != null) {
            if (!clickScheduled) {
                clickScheduled = true
                v.handler.postDelayed(ClickRunnable(v), delay)
            }
        } else dispatchClickEvent(v)
    }

    private fun dispatchClickEvent(v: View) {
        if (clickListener != null) clickListener!!.onClick(v)
    }

    internal inner class ClickRunnable(private val view: View) : Runnable {
        override fun run() {
            clickScheduled = false
            dispatchClickEvent(view)
        }
    }

    companion object {
        /**
         * Cancel the ripple effect of this view and all of it's children.
         * @param v
         */
		@JvmStatic
		fun cancelRipple(v: View) {
            val background = v.background
            if (background is RippleDrawable) background.cancel()
            if (v is ViewGroup) {
                var i = 0
                val count = v.childCount
                while (i < count) {
                    cancelRipple(v.getChildAt(i))
                    i++
                }
            }
        }
    }
}