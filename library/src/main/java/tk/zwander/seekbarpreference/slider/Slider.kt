package tk.zwander.seekbarpreference.slider

import android.content.Context
import tk.zwander.seekbarpreference.slider.util.ThemeUtil.colorControlActivated
import tk.zwander.seekbarpreference.slider.util.ThemeUtil.colorControlNormal
import tk.zwander.seekbarpreference.slider.util.ThemeUtil.dpToPx
import tk.zwander.seekbarpreference.slider.util.TypefaceUtil.load
import tk.zwander.seekbarpreference.slider.RippleManager.Companion.cancelRipple
import tk.zwander.seekbarpreference.slider.util.ColorUtil.getMiddleColor
import tk.zwander.seekbarpreference.slider.util.ColorUtil.getColor
import android.graphics.Paint.Cap
import android.view.Gravity
import android.view.ViewConfiguration
import android.graphics.*
import android.graphics.drawable.Drawable
import tk.zwander.seekbarpreference.slider.util.ViewUtil
import android.view.MotionEvent
import android.os.Parcelable
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import kotlinx.parcelize.Parcelize
import tk.zwander.seekbarpreference.R
import tk.zwander.seekbarpreference.slider.drawable.RippleDrawable
import kotlin.math.*

/**
 * Created by Ret on 3/18/2015.
 */
class Slider : View {
    private var styleId = 0
    private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val drawRect: RectF = RectF()
    private val tempRect: RectF = RectF()
    private val leftTrackPath: Path = Path()
    private val rightTrackPath: Path = Path()
    private var markPath: Path = Path()

    /**
     * @return The minimum selectable value.
     */
    var minValue = 0
        private set

    /**
     * @return The maximum selectable value.
     */
    var maxValue = 100
        private set

    /**
     * @return The step value.
     */
    var stepValue = 1
        private set

    var isThumbRadiusAnimatorRunning = false
        private set

    var isThumbStrokeAnimatorRunning = false
        private set

    private var discreteMode = false
    private var primaryColor = 0
    private var secondaryColor = 0
    private var trackSize = -1
    private var trackCap = Cap.BUTT
    private var thumbBorderSize = -1
    private var thumbRadius = -1
    private var thumbFocusRadius = -1
    private var thumbTouchRadius = -1
    private var thumbPosition = -1f
    private var typeface = Typeface.DEFAULT
    private var textSize = -1
    private var textColor = -0x1
    private var gravity = Gravity.CENTER
    private var travelAnimationDuration = -1
    private var transformAnimationDuration = -1
    private var interpolator: Interpolator? = null
    private var baselineOffset = 0
    private var touchSlop = 0
    var isDragging = false
        private set
    private var thumbCurrentRadius = 0f
    private var thumbFillPercent = 0f
    private var alwaysFillThumb = false
    private var textHeight = 0
    private var memoValue = 0
    private var isRtl = false

    private val memoPoint: PointF = PointF()
    private val thumbRadiusAnimator: ThumbRadiusAnimator = ThumbRadiusAnimator()
    private val thumbStrokeAnimator: ThumbStrokeAnimator = ThumbStrokeAnimator()
    private val thumbMoveAnimator: ThumbMoveAnimator = ThumbMoveAnimator()

    /**
     * Interface definition for a callback to be invoked when thumb's position changed.
     */
    interface OnPositionChangeListener {
        /**
         * Called when thumb's position changed.
         *
         * @param view The view fire this event.
         * @param fromUser Indicate the change is from user touch event or not.
         * @param oldPos The old position of thumb.
         * @param newPos The new position of thumb.
         * @param oldValue The old value.
         * @param newValue The new value.
         */
        fun onPositionChanged(
            view: Slider?,
            fromUser: Boolean,
            oldPos: Float,
            newPos: Float,
            oldValue: Int,
            newValue: Int
        )
    }

    private var onPositionChangeListener: OnPositionChangeListener? = null

    interface ValueDescriptionProvider {
        fun getDescription(value: Int): String?
    }

    private var valueDescriptionProvider: ValueDescriptionProvider? = null

    constructor(context: Context) : super(context) {
        init(context, null, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs, defStyleAttr, 0)
    }

    private fun init(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) {
        //default color
        primaryColor = colorControlActivated(context, -0x1000000)
        secondaryColor = colorControlNormal(context, -0x1000000)
        touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        applyStyle(context, attrs, defStyleAttr, defStyleRes)
        val a = context.obtainStyledAttributes(
            attrs,
            R.styleable.ThemableView,
            defStyleAttr,
            defStyleRes
        )
        val styleId = a.getResourceId(R.styleable.ThemableView_v_styleId, 0)
        a.recycle()
        this.styleId = styleId
    }

    fun applyStyle(resId: Int) {
        applyStyle(context, null, 0, resId)
    }

    protected fun applyStyle(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) {
        rippleManager.onCreate(this, context, attrs, defStyleAttr, defStyleRes)
        val a = context.obtainStyledAttributes(attrs, R.styleable.Slider, defStyleAttr, defStyleRes)
        var minValue = minValue
        var maxValue = maxValue
        var valueRangeDefined = false
        var value = -1
        var valueDefined = false
        var familyName: String? = null
        var style = Typeface.NORMAL
        var textStyleDefined = false
        var i = 0
        val count = a.indexCount
        while (i < count) {
            val attr = a.getIndex(i)
            if (attr == R.styleable.Slider_sl_discreteMode) discreteMode = a.getBoolean(
                attr,
                false
            ) else if (attr == R.styleable.Slider_sl_primaryColor) primaryColor = a.getColor(
                attr,
                0
            ) else if (attr == R.styleable.Slider_sl_secondaryColor) secondaryColor =
                a.getColor(attr, 0) else if (attr == R.styleable.Slider_sl_trackSize) trackSize =
                a.getDimensionPixelSize(attr, 0) else if (attr == R.styleable.Slider_sl_trackCap) {
                val cap = a.getInteger(attr, 0)
                trackCap = if (cap == 0) Cap.BUTT else if (cap == 1) Cap.ROUND else Cap.SQUARE
            } else if (attr == R.styleable.Slider_sl_thumbBorderSize) thumbBorderSize =
                a.getDimensionPixelSize(
                    attr,
                    0
                ) else if (attr == R.styleable.Slider_sl_thumbRadius) thumbRadius =
                a.getDimensionPixelSize(
                    attr,
                    0
                ) else if (attr == R.styleable.Slider_sl_thumbFocusRadius) thumbFocusRadius =
                a.getDimensionPixelSize(
                    attr,
                    0
                ) else if (attr == R.styleable.Slider_sl_thumbTouchRadius) thumbTouchRadius =
                a.getDimensionPixelSize(
                    attr,
                    0
                ) else if (attr == R.styleable.Slider_sl_travelAnimDuration) {
                travelAnimationDuration = a.getInteger(attr, 0)
                transformAnimationDuration = travelAnimationDuration
            } else if (attr == R.styleable.Slider_sl_alwaysFillThumb) {
                alwaysFillThumb = a.getBoolean(R.styleable.Slider_sl_alwaysFillThumb, false)
            } else if (attr == R.styleable.Slider_sl_interpolator) {
                val resId = a.getResourceId(R.styleable.Slider_sl_interpolator, 0)
                interpolator = AnimationUtils.loadInterpolator(context, resId)
            } else if (attr == R.styleable.Slider_android_gravity) gravity =
                a.getInteger(attr, 0) else if (attr == R.styleable.Slider_sl_minValue) {
                minValue = a.getInteger(attr, 0)
                valueRangeDefined = true
            } else if (attr == R.styleable.Slider_sl_maxValue) {
                maxValue = a.getInteger(attr, 0)
                valueRangeDefined = true
            } else if (attr == R.styleable.Slider_sl_stepValue) stepValue =
                a.getInteger(attr, 0) else if (attr == R.styleable.Slider_sl_value) {
                value = a.getInteger(attr, 0)
                valueDefined = true
            } else if (attr == R.styleable.Slider_sl_fontFamily) {
                familyName = a.getString(attr)
                textStyleDefined = true
            } else if (attr == R.styleable.Slider_sl_textStyle) {
                style = a.getInteger(attr, 0)
                textStyleDefined = true
            } else if (attr == R.styleable.Slider_sl_textColor) textColor =
                a.getColor(attr, 0) else if (attr == R.styleable.Slider_sl_textSize) textSize =
                a.getDimensionPixelSize(
                    attr,
                    0
                ) else if (attr == R.styleable.Slider_android_enabled) isEnabled = a.getBoolean(
                attr,
                true
            ) else if (attr == R.styleable.Slider_sl_baselineOffset) baselineOffset =
                a.getDimensionPixelOffset(attr, 0)
            i++
        }
        a.recycle()
        if (trackSize < 0) trackSize = dpToPx(context, 2)
        if (thumbBorderSize < 0) thumbBorderSize = dpToPx(context, 2)
        if (thumbRadius < 0) thumbRadius = dpToPx(context, 10)
        if (thumbFocusRadius < 0) thumbFocusRadius = dpToPx(context, 14)
        if (travelAnimationDuration < 0) {
            travelAnimationDuration =
                context.resources.getInteger(android.R.integer.config_mediumAnimTime)
            transformAnimationDuration = travelAnimationDuration
        }
        if (interpolator == null) interpolator = DecelerateInterpolator()
        if (valueRangeDefined) setValueRange(minValue, maxValue, false)
        if (valueDefined) setValue(value.toFloat(), false) else if (thumbPosition < 0) setValue(
            minValue.toFloat(), false
        )
        if (textStyleDefined) typeface = load(context, familyName, style)
        if (textSize < 0) textSize =
            context.resources.getDimensionPixelOffset(com.google.android.material.R.dimen.abc_text_size_small_material)
        paint.textSize = textSize.toFloat()
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = typeface
        measureText()
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelRipple(this)
    }

    private fun measureText() {
        if (_valueText == null) return
        val temp = Rect()
        paint.textSize = textSize.toFloat()
        val width = paint.measureText(_valueText)
        val maxWidth = (thumbRadius * Math.sqrt(2.0) * 2 - dpToPx(context, 8)).toFloat()
        if (width > maxWidth) {
            val textSize = textSize * maxWidth / width
            paint.textSize = textSize
        }
        paint.getTextBounds(_valueText, 0, _valueText!!.length, temp)
        textHeight = temp.height()
    }

    private var _valueText: String? = null
    private val valueText: String?
        get() {
            val value = value
            if (_valueText == null || memoValue != value) {
                memoValue = value
                _valueText = if (valueDescriptionProvider == null) {
                    memoValue.toString()
                } else {
                    valueDescriptionProvider?.getDescription(
                        memoValue
                    )
                }
                measureText()
            }
            return _valueText
        }

    /**
     * Set the randge of selectable value.
     * @param min The minimum selectable value.
     * @param max The maximum selectable value.
     * @param animation Indicate that should show animation when thumb's current position changed.
     */
    fun setValueRange(min: Int, max: Int, animation: Boolean) {
        if (max < min || min == minValue && max == maxValue) return
        val oldValue = exactValue
        val oldPosition = position
        minValue = min
        maxValue = max
        setValue(oldValue, animation)
        if (onPositionChangeListener != null && oldPosition == position && oldValue != exactValue) {
            onPositionChangeListener?.onPositionChanged(
                this,
                false,
                oldPosition,
                oldPosition,
                oldValue.roundToInt(),
                value
            )
        }
    }

    /**
     * @return The selected value.
     */
    val value: Int
        get() = Math.round(exactValue)

    /**
     * @return The exact selected value.
     */
    val exactValue: Float
        get() = (maxValue - minValue) * position + minValue

    /**
     * @return The current position of thumb in [0..1] range.
     */
    val position: Float
        get() = if (thumbMoveAnimator.isRunning) thumbMoveAnimator.position else thumbPosition

    /**
     * Set current position of thumb.
     * @param pos The position in [0..1] range.
     * @param animation Indicate that should show animation when change thumb's position.
     */
    fun setPosition(pos: Float, animation: Boolean) {
        setPosition(pos, animation, animation, false)
    }

    private fun setPosition(
        pos: Float,
        moveAnimation: Boolean,
        transformAnimation: Boolean,
        fromUser: Boolean
    ) {
        val change = position != pos
        val oldValue = value
        val oldPos = position
        if (!moveAnimation || !thumbMoveAnimator.startAnimation(pos)) {
            thumbPosition = pos
            if (transformAnimation) {
                if (!isDragging) thumbRadiusAnimator.startAnimation(thumbRadius)
                //                mThumbStrokeAnimator.startAnimation(pos == 0 ? 0 : 1);
            } else {
                thumbCurrentRadius = thumbRadius.toFloat()
                //                mThumbFillPercent = (mAlwaysFillThumb || mThumbPosition != 0) ? 1 : 0;
                invalidate()
            }
        }
        val newValue = value
        val newPos = position
        if (change && onPositionChangeListener != null) {
            onPositionChangeListener?.onPositionChanged(
                this,
                fromUser,
                oldPos,
                newPos,
                oldValue,
                newValue
            )
        }
    }

    /**
     * Changes the primary color and invalidates the view to force a redraw.
     * @param color New color to assign to mPrimaryColor.
     */
    fun setPrimaryColor(color: Int) {
        primaryColor = color
        invalidate()
    }

    /**
     * Changes the secondary color and invalidates the view to force a redraw.
     * @param color New color to assign to mSecondaryColor.
     */
    fun setSecondaryColor(color: Int) {
        secondaryColor = color
        invalidate()
    }

    /**
     * Set if we want the thumb to always be filled.
     * @param alwaysFillThumb Do we want it to always be filled.
     */
    fun setAlwaysFillThumb(alwaysFillThumb: Boolean) {
        this.alwaysFillThumb = alwaysFillThumb
    }

    /**
     * Set the selected value of this Slider.
     * @param value The selected value.
     * @param animation Indicate that should show animation when change thumb's position.
     */
    fun setValue(value: Float, animation: Boolean) {
        val newValue = maxValue.toFloat().coerceAtMost(value.coerceAtLeast(minValue.toFloat()))
        setPosition((newValue - minValue) / (maxValue - minValue), animation)
    }

    /**
     * Set a listener will be called when thumb's position changed.
     * @param listener The [OnPositionChangeListener] will be called.
     */
    fun setOnPositionChangeListener(listener: OnPositionChangeListener?) {
        onPositionChangeListener = listener
    }

    fun setValueDescriptionProvider(provider: ValueDescriptionProvider?) {
        valueDescriptionProvider = provider
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun setBackgroundDrawable(drawable: Drawable) {
        val background = background
        if (background is RippleDrawable && drawable !is RippleDrawable) background.backgroundDrawable =
            drawable else super.setBackgroundDrawable(drawable)
    }

    override fun setBackground(drawable: Drawable) {
        val background = background
        if (background is RippleDrawable && drawable !is RippleDrawable) background.backgroundDrawable =
            drawable else super.setBackground(drawable)
    }

    private var _rippleManager: RippleManager? = null
    private val rippleManager: RippleManager
        get() {
            if (_rippleManager == null) {
                synchronized(RippleManager::class.java) {
                    if (_rippleManager == null) _rippleManager = RippleManager()
                }
            }
            return _rippleManager!!
        }

    override fun setOnClickListener(l: OnClickListener?) {
        val rippleManager = rippleManager
        if (l === rippleManager) {
            super.setOnClickListener(l)
        } else {
            rippleManager.setOnClickListener(l)
            setOnClickListener(rippleManager)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        var heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)

        when (widthMode) {
            MeasureSpec.UNSPECIFIED -> widthSize = suggestedMinimumWidth
            MeasureSpec.AT_MOST -> widthSize = widthSize.coerceAtMost(suggestedMinimumWidth)
            else -> {}
        }

        when (heightMode) {
            MeasureSpec.UNSPECIFIED -> heightSize = suggestedMinimumHeight
            MeasureSpec.AT_MOST -> heightSize = heightSize.coerceAtMost(suggestedMinimumHeight)
            else -> {}
        }

        setMeasuredDimension(widthSize, heightSize)
    }

    public override fun getSuggestedMinimumWidth(): Int {
        return (if (discreteMode) (thumbRadius * sqrt(2.0)).toInt() else thumbFocusRadius) * 4 + paddingLeft + paddingRight
    }

    public override fun getSuggestedMinimumHeight(): Int {
        return (if (discreteMode) (thumbRadius * (4 + sqrt(2.0))).toInt() else thumbFocusRadius * 2) + paddingTop + paddingBottom
    }

    override fun onRtlPropertiesChanged(layoutDirection: Int) {
        val rtl = layoutDirection == LAYOUT_DIRECTION_RTL
        if (isRtl != rtl) {
            isRtl = rtl
            invalidate()
        }
    }

    override fun getBaseline(): Int {
        val align = gravity and Gravity.VERTICAL_GRAVITY_MASK
        val baseline = if (discreteMode) {
            val fullHeight = (thumbRadius * (4 + sqrt(2.0))).toInt()
            val height = thumbRadius * 2
            when (align) {
                Gravity.TOP -> paddingTop.coerceAtLeast(fullHeight - height) + thumbRadius
                Gravity.BOTTOM -> measuredHeight - paddingBottom
                else -> (((measuredHeight - height) / 2f).coerceAtLeast((fullHeight - height).toFloat()) + thumbRadius).roundToInt()
            }
        } else {
            val height = thumbFocusRadius * 2
            when (align) {
                Gravity.TOP -> paddingTop + thumbFocusRadius
                Gravity.BOTTOM -> measuredHeight - paddingBottom
                else -> ((measuredHeight - height) / 2f + thumbFocusRadius).roundToInt()
            }
        }
        return baseline + baselineOffset
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        drawRect.left = (paddingLeft + thumbRadius).toFloat()
        drawRect.right = (w - paddingRight - thumbRadius).toFloat()
        val align = gravity and Gravity.VERTICAL_GRAVITY_MASK
        if (discreteMode) {
            val fullHeight = (thumbRadius * (4 + sqrt(2.0))).toInt()
            val height = thumbRadius * 2
            when (align) {
                Gravity.TOP -> {
                    drawRect.top = paddingTop.coerceAtLeast(fullHeight - height).toFloat()
                    drawRect.bottom = drawRect.top + height
                }
                Gravity.BOTTOM -> {
                    drawRect.bottom = (h - paddingBottom).toFloat()
                    drawRect.top = drawRect.bottom - height
                }
                else -> {
                    drawRect.top = ((h - height) / 2f).coerceAtLeast((fullHeight - height).toFloat())
                    drawRect.bottom = drawRect.top + height
                }
            }
        } else {
            val height = thumbFocusRadius * 2
            when (align) {
                Gravity.TOP -> {
                    drawRect.top = paddingTop.toFloat()
                    drawRect.bottom = drawRect.top + height
                }
                Gravity.BOTTOM -> {
                    drawRect.bottom = (h - paddingBottom).toFloat()
                    drawRect.top = drawRect.bottom - height
                }
                else -> {
                    drawRect.top = (h - height) / 2f
                    drawRect.bottom = drawRect.top + height
                }
            }
        }
    }

    private fun isThumbHit(x: Float, y: Float, radius: Float): Boolean {
        val cx = drawRect.width() * thumbPosition + drawRect.left
        val cy = drawRect.centerY()
        return x >= cx - radius && x <= cx + radius && y >= cy - radius && y < cy + radius
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Double {
        return sqrt((x1 - x2).toDouble().pow(2.0) + (y1 - y2).toDouble().pow(2.0))
    }

    private fun correctPosition(position: Float): Float {
        
        if (!discreteMode) return position
        
        val totalOffset = maxValue - minValue
        val valueOffset = (totalOffset * position).roundToInt()
        val stepOffset = valueOffset / stepValue
        val lowerValue = stepOffset * stepValue
        val higherValue = totalOffset.coerceAtMost((stepOffset + 1) * stepValue)
        
        return if (valueOffset - lowerValue < higherValue - valueOffset) lowerValue / totalOffset.toFloat() else higherValue / totalOffset.toFloat()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)
        rippleManager.onTouchEvent(this, event)
        if (!isEnabled) return false
        var x = event.x
        val y = event.y
        if (isRtl) x = 2 * drawRect.centerX() - x
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = isThumbHit(
                    x,
                    y,
                    if (thumbTouchRadius > 0) thumbTouchRadius.toFloat() else thumbRadius.toFloat()
                ) && !thumbMoveAnimator.isRunning
                memoPoint[x] = y
                if (isDragging) {
                    thumbRadiusAnimator.startAnimation(if (discreteMode) 0 else thumbFocusRadius)
                    if (parent != null) parent.requestDisallowInterceptTouchEvent(true)
                }
            }
            MotionEvent.ACTION_MOVE -> if (isDragging) {
                if (discreteMode) {
                    val position = correctPosition(
                        1f.coerceAtMost(0f.coerceAtLeast((x - drawRect.left) / drawRect.width()))
                    )
                    setPosition(position,
                        moveAnimation = true,
                        transformAnimation = true,
                        fromUser = true
                    )
                } else {
                    val offset = (x - memoPoint.x) / drawRect.width()
                    val position = 1f.coerceAtMost(0f.coerceAtLeast(thumbPosition + offset))
                    setPosition(position,
                        moveAnimation = false,
                        transformAnimation = true,
                        fromUser = true
                    )
                    memoPoint.x = x
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> if (isDragging) {
                isDragging = false
                setPosition(position,
                    moveAnimation = true,
                    transformAnimation = true,
                    fromUser = true
                )
                if (parent != null) parent.requestDisallowInterceptTouchEvent(false)
            } else if (distance(memoPoint.x, memoPoint.y, x, y) <= touchSlop) {
                val position = correctPosition(
                    1f.coerceAtMost(0f.coerceAtLeast((x - drawRect.left) / drawRect.width()))
                )
                setPosition(position, moveAnimation = true, transformAnimation = true, fromUser = true)
            }
            MotionEvent.ACTION_CANCEL -> if (isDragging) {
                isDragging = false
                setPosition(position,
                    moveAnimation = true,
                    transformAnimation = true,
                    fromUser = true
                )
                if (parent != null) parent.requestDisallowInterceptTouchEvent(false)
            }
        }
        return true
    }

    private fun getTrackPath(x: Float, y: Float, radius: Float) {
        val halfStroke = trackSize / 2f
        leftTrackPath.reset()
        rightTrackPath.reset()
        if (radius - 1f < halfStroke) {
            if (trackCap != Cap.ROUND) {
                if (x > drawRect.left) {
                    leftTrackPath.moveTo(drawRect.left, y - halfStroke)
                    leftTrackPath.lineTo(x, y - halfStroke)
                    leftTrackPath.lineTo(x, y + halfStroke)
                    leftTrackPath.lineTo(drawRect.left, y + halfStroke)
                    leftTrackPath.close()
                }
                if (x < drawRect.right) {
                    rightTrackPath.moveTo(drawRect.right, y + halfStroke)
                    rightTrackPath.lineTo(x, y + halfStroke)
                    rightTrackPath.lineTo(x, y - halfStroke)
                    rightTrackPath.lineTo(drawRect.right, y - halfStroke)
                    rightTrackPath.close()
                }
            } else {
                if (x > drawRect.left) {
                    tempRect[drawRect.left, y - halfStroke, drawRect.left + trackSize] =
                        y + halfStroke
                    leftTrackPath.arcTo(tempRect, 90f, 180f)
                    leftTrackPath.lineTo(x, y - halfStroke)
                    leftTrackPath.lineTo(x, y + halfStroke)
                    leftTrackPath.close()
                }
                if (x < drawRect.right) {
                    tempRect[drawRect.right - trackSize, y - halfStroke, drawRect.right] =
                        y + halfStroke
                    rightTrackPath.arcTo(tempRect, 270f, 180f)
                    rightTrackPath.lineTo(x, y + halfStroke)
                    rightTrackPath.lineTo(x, y - halfStroke)
                    rightTrackPath.close()
                }
            }
        } else {
            if (trackCap != Cap.ROUND) {
                tempRect[x - radius + 1f, y - radius + 1f, x + radius - 1f] = y + radius - 1f
                val angle =
                    (asin((halfStroke / (radius - 1f)).toDouble()) / Math.PI * 180).toFloat()
                if (x - radius > drawRect.left) {
                    leftTrackPath.moveTo(drawRect.left, y - halfStroke)
                    leftTrackPath.arcTo(tempRect, 180 + angle, -angle * 2)
                    leftTrackPath.lineTo(drawRect.left, y + halfStroke)
                    leftTrackPath.close()
                }
                if (x + radius < drawRect.right) {
                    rightTrackPath.moveTo(drawRect.right, y - halfStroke)
                    rightTrackPath.arcTo(tempRect, -angle, angle * 2)
                    rightTrackPath.lineTo(drawRect.right, y + halfStroke)
                    rightTrackPath.close()
                }
            } else {
                val angle = (asin((halfStroke / (radius - 1f)).toDouble()) / Math.PI * 180).toFloat()
                if (x - radius > drawRect.left) {
                    val angle2 = (acos(0f.coerceAtLeast((drawRect.left + halfStroke - x + radius) / halfStroke).toDouble()
                    ) / Math.PI * 180).toFloat()
                    tempRect[drawRect.left, y - halfStroke, drawRect.left + trackSize] =
                        y + halfStroke
                    leftTrackPath.arcTo(tempRect, 180 - angle2, angle2 * 2)
                    tempRect[x - radius + 1f, y - radius + 1f, x + radius - 1f] = y + radius - 1f
                    leftTrackPath.arcTo(tempRect, 180 + angle, -angle * 2)
                    leftTrackPath.close()
                }
                if (x + radius < drawRect.right) {
                    var angle2 = acos(0f.coerceAtLeast((x + radius - drawRect.right + halfStroke) / halfStroke)
                            .toDouble()).toFloat()
                    rightTrackPath.moveTo(
                        (drawRect.right - halfStroke + cos(angle2.toDouble()) * halfStroke).toFloat(),
                        (y + sin(angle2.toDouble()) * halfStroke).toFloat()
                    )
                    angle2 = (angle2 / Math.PI * 180).toFloat()
                    tempRect[drawRect.right - trackSize, y - halfStroke, drawRect.right] =
                        y + halfStroke
                    rightTrackPath.arcTo(tempRect, angle2, -angle2 * 2)
                    tempRect[x - radius + 1f, y - radius + 1f, x + radius - 1f] = y + radius - 1f
                    rightTrackPath.arcTo(tempRect, -angle, angle * 2)
                    rightTrackPath.close()
                }
            }
        }
    }

    private fun getMarkPath(path: Path, cx: Float, cy: Float, radius: Float, factor: Float): Path {
        path.reset()

        val x1 = cx - radius
        val x2 = cx + radius
        val y3 = cy + radius
        var nCx = cx
        var nCy = cy - radius * factor

        // calculate first arc
        var angle =
            (atan2((cy - nCy).toDouble(), (x2 - nCx).toDouble()) * 180 / Math.PI).toFloat()
        var nRadius = distance(nCx, nCy, x1, cy).toFloat()
        tempRect[nCx - nRadius, nCy - nRadius, nCx + nRadius] = nCy + nRadius
        path.moveTo(x1, cy)
        path.arcTo(tempRect, 180 - angle, 180 + angle * 2)
        if (factor > 0.9f) path.lineTo(cx, y3) else {
            // find center point for second arc
            val x4 = (x2 + cx) / 2
            val y4 = (cy + y3) / 2
            val d1 = distance(x2, cy, x4, y4)
            val d2 = d1 / tan(Math.PI * (1f - factor) / 4)
            nCx = (x4 - cos(Math.PI / 4) * d2).toFloat()
            nCy = (y4 - sin(Math.PI / 4) * d2).toFloat()

            // calculate second arc
            angle =
                (atan2((cy - nCy).toDouble(), (x2 - nCx).toDouble()) * 180 / Math.PI).toFloat()
            var angle2 =
                (atan2((y3 - nCy).toDouble(), (cx - nCx).toDouble()) * 180 / Math.PI).toFloat()
            nRadius = distance(nCx, nCy, x2, cy).toFloat()
            tempRect[nCx - nRadius, nCy - nRadius, nCx + nRadius] = nCy + nRadius
            path.arcTo(tempRect, angle, angle2 - angle)

            // calculate third arc
            nCx = cx * 2 - nCx
            angle =
                (atan2((y3 - nCy).toDouble(), (cx - nCx).toDouble()) * 180 / Math.PI).toFloat()
            angle2 =
                (atan2((cy - nCy).toDouble(), (x1 - nCx).toDouble()) * 180 / Math.PI).toFloat()
            tempRect[nCx - nRadius, nCy - nRadius, nCx + nRadius] = nCy + nRadius
            path.arcTo(tempRect, angle + Math.PI.toFloat() / 4, angle2 - angle)
        }
        path.close()
        return path
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        var x = drawRect.width() * thumbPosition + drawRect.left
        if (isRtl) x = 2 * drawRect.centerX() - x
        val y = drawRect.centerY()
        val filledPrimaryColor = getMiddleColor(
            secondaryColor,
            if (isEnabled) primaryColor else secondaryColor,
            thumbFillPercent
        )
        getTrackPath(x, y, thumbCurrentRadius)
        paint.style = Paint.Style.FILL
        paint.color = if (isRtl) filledPrimaryColor else secondaryColor
        canvas.drawPath(rightTrackPath, paint)
        paint.color = if (isRtl) secondaryColor else filledPrimaryColor
        canvas.drawPath(leftTrackPath, paint)
        paint.color = filledPrimaryColor
        if (discreteMode) {
            val factor = 1f - thumbCurrentRadius / thumbRadius
            if (factor > 0) {
                markPath = getMarkPath(markPath, x, y, thumbRadius.toFloat(), factor)
                paint.style = Paint.Style.FILL
                val saveCount = canvas.save()
                canvas.translate(0f, -thumbRadius * 2 * factor)
                canvas.drawPath(markPath, paint)
                paint.color = getColor(textColor, factor)
                canvas.drawText(valueText!!, x, y + textHeight / 2f - thumbRadius * factor, paint)
                canvas.restoreToCount(saveCount)
            }
            val radius = if (isEnabled) thumbCurrentRadius else thumbCurrentRadius - thumbBorderSize
            if (radius > 0) {
                paint.color = filledPrimaryColor
                canvas.drawCircle(x, y, radius, paint)
            }
        } else {
            var radius =
                if (isEnabled) thumbCurrentRadius else thumbCurrentRadius - thumbBorderSize
            if (thumbFillPercent == 1f) paint.style = Paint.Style.FILL else {
                val strokeWidth = (radius - thumbBorderSize) * thumbFillPercent + thumbBorderSize
                radius -= strokeWidth / 2f
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = strokeWidth
            }
            canvas.drawCircle(x, y, radius, paint)
        }
    }

    fun setThumbFillPercent(fillPercent: Int): Boolean {
        return thumbStrokeAnimator.startAnimation(fillPercent)
    }

    fun setThumbRadius(radius: Int): Boolean {
        return thumbRadiusAnimator.startAnimation(radius)
    }

    internal inner class ThumbRadiusAnimator : Runnable {
        var mStartTime: Long = 0
        var mStartRadius = 0f
        var mRadius = 0

        fun resetAnimation() {
            mStartTime = SystemClock.uptimeMillis()
            mStartRadius = thumbCurrentRadius
        }

        fun startAnimation(radius: Int): Boolean {
            if (thumbCurrentRadius == radius.toFloat()) return false
            mRadius = radius
            return if (handler != null) {
                resetAnimation()
                isThumbRadiusAnimatorRunning = true
                handler.postAtTime(this, SystemClock.uptimeMillis() + ViewUtil.FRAME_DURATION)
                invalidate()
                true
            } else {
                thumbCurrentRadius = mRadius.toFloat()
                invalidate()
                false
            }
        }

        fun stopAnimation() {
            isThumbRadiusAnimatorRunning = false
            thumbCurrentRadius = mRadius.toFloat()
            if (handler != null) handler.removeCallbacks(this)
            invalidate()
        }

        override fun run() {
            val curTime = SystemClock.uptimeMillis()
            val progress = 1f.coerceAtMost((curTime - mStartTime).toFloat() / transformAnimationDuration)
            val value = interpolator?.getInterpolation(progress) ?: progress
            thumbCurrentRadius = (mRadius - mStartRadius) * value + mStartRadius
            if (progress == 1f) stopAnimation()
            if (isThumbRadiusAnimatorRunning) {
                if (handler != null) handler.postAtTime(
                    this,
                    SystemClock.uptimeMillis() + ViewUtil.FRAME_DURATION
                ) else stopAnimation()
            }
            invalidate()
        }
    }

    internal inner class ThumbStrokeAnimator : Runnable {
        var startTime: Long = 0
        var startFillPercent = 0f
        var fillPercent = 0

        fun resetAnimation() {
            startTime = SystemClock.uptimeMillis()
            startFillPercent = thumbFillPercent
        }

        fun startAnimation(fillPercent: Int): Boolean {
            if (thumbFillPercent == fillPercent.toFloat()) return false
            this.fillPercent = fillPercent
            return if (handler != null) {
                resetAnimation()
                isThumbStrokeAnimatorRunning = true
                handler.postAtTime(this, SystemClock.uptimeMillis() + ViewUtil.FRAME_DURATION)
                invalidate()
                true
            } else {
                thumbFillPercent = if (alwaysFillThumb) 1f else this.fillPercent.toFloat()
                invalidate()
                false
            }
        }

        fun stopAnimation() {
            isThumbStrokeAnimatorRunning = false
            thumbFillPercent = if (alwaysFillThumb) 1f else fillPercent.toFloat()
            if (handler != null) handler.removeCallbacks(this)
            invalidate()
        }

        override fun run() {
            val curTime = SystemClock.uptimeMillis()
            val progress = 1f.coerceAtMost((curTime - startTime).toFloat() / transformAnimationDuration)
            val value = interpolator?.getInterpolation(progress) ?: progress
            thumbFillPercent = if (alwaysFillThumb) 1f else (fillPercent - startFillPercent) * value + startFillPercent
            if (progress == 1f) stopAnimation()
            if (isThumbStrokeAnimatorRunning) {
                if (handler != null) handler.postAtTime(
                    this,
                    SystemClock.uptimeMillis() + ViewUtil.FRAME_DURATION
                ) else stopAnimation()
            }
            invalidate()
        }
    }

    internal inner class ThumbMoveAnimator : Runnable {
        var isRunning = false
        var mStartTime: Long = 0
        var mStartFillPercent = 0f
        var mStartRadius = 0f
        var mStartPosition = 0f
        var position = 0f
        var mDuration = 0

        fun resetAnimation() {
            mStartTime = SystemClock.uptimeMillis()
            mStartPosition = thumbPosition
            mStartFillPercent = thumbFillPercent
            mStartRadius = thumbCurrentRadius
            mDuration =
                if (discreteMode && !isDragging) transformAnimationDuration * 2 + travelAnimationDuration else travelAnimationDuration
        }

        fun startAnimation(position: Float): Boolean {
            if (thumbPosition == position) return false
            this.position = position
            return if (handler != null) {
                resetAnimation()
                isRunning = true
                handler.postAtTime(this, SystemClock.uptimeMillis() + ViewUtil.FRAME_DURATION)
                invalidate()
                true
            } else {
                thumbPosition = position
                invalidate()
                false
            }
        }

        fun stopAnimation() {
            isRunning = false
            thumbCurrentRadius = if (discreteMode && isDragging) 0f else thumbRadius.toFloat()
            thumbPosition = this.position
            if (handler != null) handler.removeCallbacks(this)
            invalidate()
        }

        override fun run() {
            val curTime = SystemClock.uptimeMillis()
            val progress = 1f.coerceAtMost((curTime - mStartTime).toFloat() / mDuration)
            var value = interpolator?.getInterpolation(progress) ?: progress
            if (discreteMode) {
                if (isDragging) {
                    thumbPosition = (this.position - mStartPosition) * value + mStartPosition
                } else {
                    val p1 = travelAnimationDuration.toFloat() / mDuration
                    val p2 = (travelAnimationDuration + transformAnimationDuration).toFloat() / mDuration
                    if (progress < p1) {
                        value = interpolator?.getInterpolation(progress / p1) ?: (progress / p1)
                        thumbCurrentRadius = mStartRadius * (1f - value)
                        thumbPosition = (this.position - mStartPosition) * value + mStartPosition
                    } else if (progress > p2) {
                        thumbCurrentRadius = thumbRadius * (progress - p2) / (1 - p2)
                    }
                }
            } else {
                thumbPosition = (this.position - mStartPosition) * value + mStartPosition
                if (progress < 0.2) {
                    thumbCurrentRadius = (thumbRadius + thumbBorderSize * progress * 5).coerceAtLeast(thumbCurrentRadius)
                } else if (progress >= 0.8) {
                    thumbCurrentRadius = thumbRadius + thumbBorderSize * (5f - progress * 5)
                }
            }
            if (progress == 1f) stopAnimation()
            if (isRunning) {
                if (handler != null) handler.postAtTime(
                    this,
                    SystemClock.uptimeMillis() + ViewUtil.FRAME_DURATION
                ) else stopAnimation()
            }
            invalidate()
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        return SavedState(position, superState)
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        val ss = state as SavedState
        super.onRestoreInstanceState(ss.superState)
        setPosition(ss.position, false)
        requestLayout()
    }

    @Parcelize
    internal data class SavedState(val position: Float = 0f, val source: Parcelable?) : BaseSavedState(source)
}