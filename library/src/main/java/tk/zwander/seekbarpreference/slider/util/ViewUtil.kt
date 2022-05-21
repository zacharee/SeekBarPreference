package tk.zwander.seekbarpreference.slider.util

import tk.zwander.seekbarpreference.slider.util.TypefaceUtil.load
import android.annotation.SuppressLint
import android.content.res.TypedArray
import android.graphics.PorterDuff
import android.widget.TextView
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.AutoCompleteTextView
import android.view.ViewGroup
import android.widget.ImageView
import tk.zwander.seekbarpreference.R

object ViewUtil {
    const val FRAME_DURATION = (1000 / 60).toLong()

    @JvmStatic
    fun hasState(states: IntArray?, state: Int): Boolean {
        if (states == null) return false
        for (state1 in states) if (state1 == state) return true
        return false
    }

    @JvmStatic
    fun setBackground(v: View, drawable: Drawable?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) v.background =
            drawable else v.setBackgroundDrawable(drawable)
    }

    /**
     * Apply any View style attributes to a view.
     * @param v The view is applied.
     * @param resId The style resourceId.
     */
    fun applyStyle(v: View, resId: Int) {
        applyStyle(v, null, 0, resId)
    }

    /**
     * Apply any View style attributes to a view.
     * @param v The view is applied.
     * @param attrs
     * @param defStyleAttr
     * @param defStyleRes
     */
    fun applyStyle(v: View, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        val a = v.context.obtainStyledAttributes(attrs, R.styleable.View, defStyleAttr, defStyleRes)
        var leftPadding = -1
        var topPadding = -1
        var rightPadding = -1
        var bottomPadding = -1
        var startPadding = Int.MIN_VALUE
        var endPadding = Int.MIN_VALUE
        var padding = -1
        var startPaddingDefined = false
        var endPaddingDefined = false
        var leftPaddingDefined = false
        var rightPaddingDefined = false
        var i = 0
        val count = a.indexCount
        while (i < count) {
            when (val attr = a.getIndex(i)) {
                R.styleable.View_android_background -> {
                    val bg = a.getDrawable(attr)
                    setBackground(v, bg)
                }
                R.styleable.View_android_backgroundTint -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) v.backgroundTintList =
                        a.getColorStateList(attr)
                }
                R.styleable.View_android_backgroundTintMode -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        val value = a.getInt(attr, 3)
                        when (value) {
                            3 -> v.backgroundTintMode = PorterDuff.Mode.SRC_OVER
                            5 -> v.backgroundTintMode = PorterDuff.Mode.SRC_IN
                            9 -> v.backgroundTintMode = PorterDuff.Mode.SRC_ATOP
                            14 -> v.backgroundTintMode = PorterDuff.Mode.MULTIPLY
                            15 -> v.backgroundTintMode = PorterDuff.Mode.SCREEN
                            16 -> v.backgroundTintMode = PorterDuff.Mode.ADD
                        }
                    }
                }
                R.styleable.View_android_elevation -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) v.elevation =
                        a.getDimensionPixelOffset(attr, 0).toFloat()
                }
                R.styleable.View_android_padding -> {
                    padding = a.getDimensionPixelSize(attr, -1)
                    leftPaddingDefined = true
                    rightPaddingDefined = true
                }
                R.styleable.View_android_paddingLeft -> {
                    leftPadding = a.getDimensionPixelSize(attr, -1)
                    leftPaddingDefined = true
                }
                R.styleable.View_android_paddingTop -> topPadding =
                    a.getDimensionPixelSize(
                        attr,
                        -1
                    )
                R.styleable.View_android_paddingRight -> {
                    rightPadding = a.getDimensionPixelSize(attr, -1)
                    rightPaddingDefined = true
                }
                R.styleable.View_android_paddingBottom -> bottomPadding =
                    a.getDimensionPixelSize(
                        attr,
                        -1
                    )
                R.styleable.View_android_paddingStart -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        startPadding = a.getDimensionPixelSize(attr, Int.MIN_VALUE)
                        startPaddingDefined = startPadding != Int.MIN_VALUE
                    }
                }
                R.styleable.View_android_paddingEnd -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        endPadding = a.getDimensionPixelSize(attr, Int.MIN_VALUE)
                        endPaddingDefined = endPadding != Int.MIN_VALUE
                    }
                }
                R.styleable.View_android_fadeScrollbars -> v.isScrollbarFadingEnabled =
                    a.getBoolean(
                        attr,
                        true
                    )
                R.styleable.View_android_fadingEdgeLength -> v.fadingEdgeLength =
                    a.getDimensionPixelOffset(
                        attr,
                        0
                    )
                R.styleable.View_android_minHeight -> v.minimumHeight =
                    a.getDimensionPixelSize(
                        attr,
                        0
                    )
                R.styleable.View_android_minWidth -> v.minimumWidth =
                    a.getDimensionPixelSize(
                        attr,
                        0
                    )
                R.styleable.View_android_requiresFadingEdge -> v.isVerticalFadingEdgeEnabled =
                    a.getBoolean(
                        attr,
                        true
                    )
                R.styleable.View_android_scrollbarDefaultDelayBeforeFade -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) v.scrollBarDefaultDelayBeforeFade =
                        a.getInteger(attr, 0)
                }
                R.styleable.View_android_scrollbarFadeDuration -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) v.scrollBarFadeDuration =
                        a.getInteger(attr, 0)
                }
                R.styleable.View_android_scrollbarSize -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) v.scrollBarSize =
                        a.getDimensionPixelSize(attr, 0)
                }
                R.styleable.View_android_scrollbarStyle -> {
                    when (a.getInteger(attr, 0)) {
                        0x0 -> v.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
                        0x01000000 -> v.scrollBarStyle = View.SCROLLBARS_INSIDE_INSET
                        0x02000000 -> v.scrollBarStyle = View.SCROLLBARS_OUTSIDE_OVERLAY
                        0x03000000 -> v.scrollBarStyle = View.SCROLLBARS_OUTSIDE_INSET
                    }
                }
                R.styleable.View_android_soundEffectsEnabled -> v.isSoundEffectsEnabled =
                    a.getBoolean(attr, true)
                R.styleable.View_android_textAlignment -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        when (a.getInteger(attr, 0)) {
                            0 -> v.textAlignment = View.TEXT_ALIGNMENT_INHERIT
                            1 -> v.textAlignment = View.TEXT_ALIGNMENT_GRAVITY
                            2 -> v.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
                            3 -> v.textAlignment = View.TEXT_ALIGNMENT_TEXT_END
                            4 -> v.textAlignment = View.TEXT_ALIGNMENT_CENTER
                            5 -> v.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                            6 -> v.textAlignment = View.TEXT_ALIGNMENT_VIEW_END
                        }
                    }
                }
                R.styleable.View_android_textDirection -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        when (a.getInteger(attr, 0)) {
                            0 -> v.textDirection = View.TEXT_DIRECTION_INHERIT
                            1 -> v.textDirection = View.TEXT_DIRECTION_FIRST_STRONG
                            2 -> v.textDirection = View.TEXT_DIRECTION_ANY_RTL
                            3 -> v.textDirection = View.TEXT_DIRECTION_LTR
                            4 -> v.textDirection = View.TEXT_DIRECTION_RTL
                            5 -> v.textDirection = View.TEXT_DIRECTION_LOCALE
                        }
                    }
                }
                R.styleable.View_android_visibility -> {
                    when (a.getInteger(attr, 0)) {
                        0 -> v.visibility = View.VISIBLE
                        1 -> v.visibility = View.INVISIBLE
                        2 -> v.visibility = View.GONE
                    }
                }
                R.styleable.View_android_layoutDirection -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        when (a.getInteger(attr, 0)) {
                            0 -> v.layoutDirection = View.LAYOUT_DIRECTION_LTR
                            1 -> v.layoutDirection = View.LAYOUT_DIRECTION_RTL
                            2 -> v.layoutDirection = View.LAYOUT_DIRECTION_INHERIT
                            3 -> v.layoutDirection = View.LAYOUT_DIRECTION_LOCALE
                        }
                    }
                }
                R.styleable.View_android_src -> {
                    if (v is ImageView) {
                        val resId = a.getResourceId(attr, 0)
                        v.setImageResource(resId)
                    }
                }
            }
            i++
        }
        if (padding >= 0) v.setPadding(
            padding,
            padding,
            padding,
            padding
        ) else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            if (startPaddingDefined) leftPadding = startPadding
            if (endPaddingDefined) rightPadding = endPadding
            v.setPadding(
                if (leftPadding >= 0) leftPadding else v.paddingLeft,
                if (topPadding >= 0) topPadding else v.paddingTop,
                if (rightPadding >= 0) rightPadding else v.paddingRight,
                if (bottomPadding >= 0) bottomPadding else v.paddingBottom
            )
        } else {
            if (leftPaddingDefined || rightPaddingDefined) v.setPadding(
                if (leftPaddingDefined) leftPadding else v.paddingLeft,
                if (topPadding >= 0) topPadding else v.paddingTop,
                if (rightPaddingDefined) rightPadding else v.paddingRight,
                if (bottomPadding >= 0) bottomPadding else v.paddingBottom
            )
            if (startPaddingDefined || endPaddingDefined) v.setPaddingRelative(
                if (startPaddingDefined) startPadding else v.paddingStart,
                if (topPadding >= 0) topPadding else v.paddingTop,
                if (endPaddingDefined) endPadding else v.paddingEnd,
                if (bottomPadding >= 0) bottomPadding else v.paddingBottom
            )
        }
        a.recycle()
        if (v is TextView) applyStyle(v, attrs, defStyleAttr, defStyleRes)
    }

    /**
     * Apply any TextView style attributes to a view.
     * @param v
     * @param attrs
     * @param defStyleAttr
     * @param defStyleRes
     */
    private fun applyStyle(v: TextView, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        var fontFamily: String? = null
        var typefaceIndex = -1
        var styleIndex = -1
        var shadowColor = 0
        var dx = 0f
        var dy = 0f
        var r = 0f
        var drawableLeft: Drawable? = null
        var drawableTop: Drawable? = null
        var drawableRight: Drawable? = null
        var drawableBottom: Drawable? = null
        var drawableStart: Drawable? = null
        var drawableEnd: Drawable? = null
        var drawableDefined = false
        var drawableRelativeDefined = false

        /*
         * Look the appearance up without checking first if it exists because
         * almost every TextView has one and it greatly simplifies the logic
         * to be able to parse the appearance first and then let specific tags
         * for this View override it.
         */
        var a = v.context.obtainStyledAttributes(
            attrs,
            R.styleable.TextViewAppearance,
            defStyleAttr,
            defStyleRes
        )
        var appearance: TypedArray? = null
        val ap = a.getResourceId(R.styleable.TextViewAppearance_android_textAppearance, 0)
        a.recycle()
        if (ap != 0) appearance = v.context.obtainStyledAttributes(ap, R.styleable.TextAppearance)
        if (appearance != null) {
            val n = appearance.indexCount
            for (i in 0 until n) {
                when (val attr: Int = appearance.getIndex(i)) {
                    R.styleable.TextAppearance_android_textColorHighlight -> {
                        v.highlightColor = appearance.getColor(attr, 0)
                    }
                    R.styleable.TextAppearance_android_textColor -> {
                        v.setTextColor(appearance.getColorStateList(attr))
                    }
                    R.styleable.TextAppearance_android_textColorHint -> {
                        v.setHintTextColor(appearance.getColorStateList(attr))
                    }
                    R.styleable.TextAppearance_android_textColorLink -> {
                        v.setLinkTextColor(appearance.getColorStateList(attr))
                    }
                    R.styleable.TextAppearance_android_textSize -> {
                        v.setTextSize(
                            TypedValue.COMPLEX_UNIT_PX,
                            appearance.getDimensionPixelSize(attr, 0).toFloat()
                        )
                    }
                    R.styleable.TextAppearance_android_typeface -> {
                        typefaceIndex = appearance.getInt(attr, -1)
                    }
                    R.styleable.TextAppearance_android_fontFamily -> {
                        fontFamily = appearance.getString(attr)
                    }
                    R.styleable.TextAppearance_tv_fontFamily -> {
                        fontFamily = appearance.getString(attr)
                    }
                    R.styleable.TextAppearance_android_textStyle -> {
                        styleIndex = appearance.getInt(attr, -1)
                    }
                    R.styleable.TextAppearance_android_textAllCaps -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) v.isAllCaps =
                            appearance.getBoolean(attr, false)
                    }
                    R.styleable.TextAppearance_android_shadowColor -> {
                        shadowColor = appearance.getInt(attr, 0)
                    }
                    R.styleable.TextAppearance_android_shadowDx -> {
                        dx = appearance.getFloat(attr, 0f)
                    }
                    R.styleable.TextAppearance_android_shadowDy -> {
                        dy = appearance.getFloat(attr, 0f)
                    }
                    R.styleable.TextAppearance_android_shadowRadius -> {
                        r = appearance.getFloat(attr, 0f)
                    }
                    R.styleable.TextAppearance_android_elegantTextHeight -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) v.isElegantTextHeight =
                            appearance.getBoolean(attr, false)
                    }
                    R.styleable.TextAppearance_android_letterSpacing -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) v.letterSpacing =
                            appearance.getFloat(attr, 0f)
                    }
                    R.styleable.TextAppearance_android_fontFeatureSettings -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) v.fontFeatureSettings =
                            appearance.getString(attr)
                    }
                }
            }
            appearance.recycle()
        }
        a = v.context.obtainStyledAttributes(attrs, R.styleable.TextView, defStyleAttr, defStyleRes)
        val n = a.indexCount
        for (i in 0 until n) {
            when (val attr = a.getIndex(i)) {
                R.styleable.TextView_android_drawableLeft -> {
                    drawableLeft = a.getDrawable(attr)
                    drawableDefined = true
                }
                R.styleable.TextView_android_drawableTop -> {
                    drawableTop = a.getDrawable(attr)
                    drawableDefined = true
                }
                R.styleable.TextView_android_drawableRight -> {
                    drawableRight = a.getDrawable(attr)
                    drawableDefined = true
                }
                R.styleable.TextView_android_drawableBottom -> {
                    drawableBottom = a.getDrawable(attr)
                    drawableDefined = true
                }
                R.styleable.TextView_android_drawableStart -> {
                    drawableStart = a.getDrawable(attr)
                    drawableRelativeDefined = true
                }
                R.styleable.TextView_android_drawableEnd -> {
                    drawableEnd = a.getDrawable(attr)
                    drawableRelativeDefined = true
                }
                R.styleable.TextView_android_drawablePadding -> {
                    v.compoundDrawablePadding = a.getDimensionPixelSize(attr, 0)
                }
                R.styleable.TextView_android_maxLines -> {
                    v.maxLines = a.getInt(attr, -1)
                }
                R.styleable.TextView_android_maxHeight -> {
                    v.maxHeight = a.getDimensionPixelSize(attr, -1)
                }
                R.styleable.TextView_android_lines -> {
                    v.setLines(a.getInt(attr, -1))
                }
                R.styleable.TextView_android_height -> {
                    v.height = a.getDimensionPixelSize(attr, -1)
                }
                R.styleable.TextView_android_minLines -> {
                    v.minLines = a.getInt(attr, -1)
                }
                R.styleable.TextView_android_minHeight -> {
                    v.minHeight = a.getDimensionPixelSize(attr, -1)
                }
                R.styleable.TextView_android_maxEms -> {
                    v.maxEms = a.getInt(attr, -1)
                }
                R.styleable.TextView_android_maxWidth -> {
                    v.maxWidth = a.getDimensionPixelSize(attr, -1)
                }
                R.styleable.TextView_android_ems -> {
                    v.setEms(a.getInt(attr, -1))
                }
                R.styleable.TextView_android_width -> {
                    v.width = a.getDimensionPixelSize(attr, -1)
                }
                R.styleable.TextView_android_minEms -> {
                    v.minEms = a.getInt(attr, -1)
                }
                R.styleable.TextView_android_minWidth -> {
                    v.minWidth = a.getDimensionPixelSize(attr, -1)
                }
                R.styleable.TextView_android_gravity -> {
                    v.gravity = a.getInt(attr, -1)
                }
                R.styleable.TextView_android_scrollHorizontally -> {
                    v.horizontallyScrolling = a.getBoolean(attr, false)
                }
                R.styleable.TextView_android_includeFontPadding -> {
                    v.includeFontPadding = a.getBoolean(attr, true)
                }
                R.styleable.TextView_android_cursorVisible -> {
                    v.isCursorVisible = a.getBoolean(attr, true)
                }
                R.styleable.TextView_android_textScaleX -> {
                    v.textScaleX = a.getFloat(attr, 1.0f)
                }
                R.styleable.TextView_android_shadowColor -> {
                    shadowColor = a.getInt(attr, 0)
                }
                R.styleable.TextView_android_shadowDx -> {
                    dx = a.getFloat(attr, 0f)
                }
                R.styleable.TextView_android_shadowDy -> {
                    dy = a.getFloat(attr, 0f)
                }
                R.styleable.TextView_android_shadowRadius -> {
                    r = a.getFloat(attr, 0f)
                }
                R.styleable.TextView_android_textColorHighlight -> {
                    v.highlightColor = a.getColor(attr, 0)
                }
                R.styleable.TextView_android_textColor -> {
                    v.setTextColor(a.getColorStateList(attr))
                }
                R.styleable.TextView_android_textColorHint -> {
                    v.setHintTextColor(a.getColorStateList(attr))
                }
                R.styleable.TextView_android_textColorLink -> {
                    v.setLinkTextColor(a.getColorStateList(attr))
                }
                R.styleable.TextView_android_textSize -> {
                    v.setTextSize(
                        TypedValue.COMPLEX_UNIT_PX,
                        a.getDimensionPixelSize(attr, 0).toFloat()
                    )
                }
                R.styleable.TextView_android_typeface -> {
                    typefaceIndex = a.getInt(attr, -1)
                }
                R.styleable.TextView_android_textStyle -> {
                    styleIndex = a.getInt(attr, -1)
                }
                R.styleable.TextView_android_fontFamily -> {
                    fontFamily = a.getString(attr)
                }
                R.styleable.TextView_tv_fontFamily -> {
                    fontFamily = a.getString(attr)
                }
                R.styleable.TextView_android_textAllCaps -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) v.isAllCaps =
                        a.getBoolean(attr, false)
                }
                R.styleable.TextView_android_elegantTextHeight -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) v.isElegantTextHeight =
                        a.getBoolean(attr, false)
                }
                R.styleable.TextView_android_letterSpacing -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) v.letterSpacing =
                        a.getFloat(attr, 0f)
                }
                R.styleable.TextView_android_fontFeatureSettings -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) v.fontFeatureSettings =
                        a.getString(attr)
                }
            }
        }
        a.recycle()
        if (shadowColor != 0) v.setShadowLayer(r, dx, dy, shadowColor)
        if (drawableDefined) {
            val drawables = v.compoundDrawables
            if (drawableStart != null) drawables[0] =
                drawableStart else if (drawableLeft != null) drawables[0] = drawableLeft
            if (drawableTop != null) drawables[1] = drawableTop
            if (drawableEnd != null) drawables[2] =
                drawableEnd else if (drawableRight != null) drawables[2] = drawableRight
            if (drawableBottom != null) drawables[3] = drawableBottom
            v.setCompoundDrawablesWithIntrinsicBounds(
                drawables[0],
                drawables[1],
                drawables[2],
                drawables[3]
            )
        }
        if (drawableRelativeDefined && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val drawables = v.compoundDrawablesRelative
            if (drawableStart != null) drawables[0] = drawableStart
            if (drawableEnd != null) drawables[2] = drawableEnd
            v.setCompoundDrawablesRelativeWithIntrinsicBounds(
                drawables[0],
                drawables[1],
                drawables[2],
                drawables[3]
            )
        }
        var tf: Typeface? = null
        if (fontFamily != null) {
            tf = load(v.context, fontFamily, styleIndex)
            if (tf != null) v.typeface = tf
        }
        if (tf != null) {
            when (typefaceIndex) {
                1 -> tf = Typeface.SANS_SERIF
                2 -> tf = Typeface.SERIF
                3 -> tf = Typeface.MONOSPACE
            }
            v.setTypeface(tf, styleIndex)
        }
        if (v is AutoCompleteTextView) applyStyle(v, attrs, defStyleAttr, defStyleRes)
    }

    /**
     * Apply any AutoCompleteTextView style attributes to a view.
     * @param v
     * @param attrs
     * @param defStyleAttr
     * @param defStyleRes
     */
    private fun applyStyle(
        v: AutoCompleteTextView,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) {
        val a = v.context.obtainStyledAttributes(
            attrs,
            R.styleable.AutoCompleteTextView,
            defStyleAttr,
            defStyleRes
        )
        val n = a.indexCount
        for (i in 0 until n) {
            when (val attr = a.getIndex(i)) {
                R.styleable.AutoCompleteTextView_android_completionHint -> v.completionHint =
                    a.getString(attr)
                R.styleable.AutoCompleteTextView_android_completionThreshold -> v.threshold =
                    a.getInteger(
                        attr,
                        0
                    )
                R.styleable.AutoCompleteTextView_android_dropDownAnchor -> v.dropDownAnchor =
                    a.getResourceId(
                        attr,
                        0
                    )
                R.styleable.AutoCompleteTextView_android_dropDownHeight -> v.dropDownHeight =
                    a.getLayoutDimension(
                        attr,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                R.styleable.AutoCompleteTextView_android_dropDownWidth -> v.dropDownWidth =
                    a.getLayoutDimension(
                        attr,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                R.styleable.AutoCompleteTextView_android_dropDownHorizontalOffset -> v.dropDownHorizontalOffset =
                    a.getDimensionPixelSize(
                        attr,
                        0
                    )
                R.styleable.AutoCompleteTextView_android_dropDownVerticalOffset -> v.dropDownVerticalOffset =
                    a.getDimensionPixelSize(
                        attr,
                        0
                    )
                R.styleable.AutoCompleteTextView_android_popupBackground -> v.setDropDownBackgroundDrawable(
                    a.getDrawable(attr)
                )
            }
        }
        a.recycle()
    }
}