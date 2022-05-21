package tk.zwander.seekbarpreference.slider.drawable

import android.content.Context
import tk.zwander.seekbarpreference.slider.util.ColorUtil.getColor
import tk.zwander.seekbarpreference.slider.util.ThemeUtil.getType
import tk.zwander.seekbarpreference.slider.util.ThemeUtil.dpToPx
import tk.zwander.seekbarpreference.slider.util.ThemeUtil.colorControlHighlight
import android.view.View.OnTouchListener
import android.view.MotionEvent
import tk.zwander.seekbarpreference.slider.util.ViewUtil
import android.graphics.*
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import tk.zwander.seekbarpreference.R

class RippleDrawable private constructor(
    backgroundDrawable: Drawable?,
    private val backgroundAnimDuration: Int,
    private val backgroundColor: Int,
    private var rippleType: Int,
    private var delayClickType: Int,
    private val delayRippleTime: Int,
    private var maxRippleRadius: Int,
    private val rippleAnimDuration: Int,
    private val rippleColor: Int,
    private val inInterpolator: Interpolator,
    private val outInterpolator: Interpolator,
    type: Int,
    topLeftCornerRadius: Int,
    topRightCornerRadius: Int,
    bottomRightCornerRadius: Int,
    bottomLeftCornerRadius: Int,
    left: Int,
    top: Int,
    right: Int,
    bottom: Int
) : Drawable(), Animatable, OnTouchListener {
    private var running = false
    private val shaderPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val fillPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private var mask: Mask? = null
    private val inShader: RadialGradient = RadialGradient(
        0f,
        0f,
        GRADIENT_RADIUS,
        intArrayOf(rippleColor, rippleColor, 0),
        GRADIENT_STOPS,
        Shader.TileMode.CLAMP
    )
    private var outShader: RadialGradient? = if (rippleType == TYPE_WAVE) RadialGradient(
        0f,
        0f,
        GRADIENT_RADIUS,
        intArrayOf(0, getColor(rippleColor, 0f), rippleColor),
        GRADIENT_STOPS,
        Shader.TileMode.CLAMP
    ) else null
    private val matrix: Matrix = Matrix()
    private var myAlpha = 255
    private val backgroundBounds: RectF = RectF()
    private val background: Path = Path()
    private var backgroundAlphaPercent = 0f
    private val ripplePoint: PointF = PointF()
    private var rippleRadius = 0f
    private var rippleAlphaPercent = 0f
    private var startTime: Long = 0
    private var touchTime: Long = 0
    private var myState = STATE_OUT
    
    var backgroundDrawable: Drawable? = null
        set(backgroundDrawable) {
            field = backgroundDrawable
            field?.bounds = bounds
        }

    fun setMask(
        type: Int,
        topLeftCornerRadius: Int,
        topRightCornerRadius: Int,
        bottomRightCornerRadius: Int,
        bottomLeftCornerRadius: Int,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ) {
        mask = Mask(
            type,
            topLeftCornerRadius,
            topRightCornerRadius,
            bottomRightCornerRadius,
            bottomLeftCornerRadius,
            left,
            top,
            right,
            bottom
        )
    }

    override fun setAlpha(alpha: Int) {
        myAlpha = alpha
        backgroundDrawable?.alpha = alpha
    }

    override fun setColorFilter(filter: ColorFilter?) {
        backgroundDrawable?.colorFilter = filter
    }

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    val clickDelayTime: Long
        get() {
            when (delayClickType) {
                DELAY_CLICK_NONE -> return -1
                DELAY_CLICK_UNTIL_RELEASE -> if (myState == STATE_RELEASE_ON_HOLD) return Math.max(
                    backgroundAnimDuration,
                    rippleAnimDuration
                ) - (SystemClock.uptimeMillis() - startTime)
                DELAY_CLICK_AFTER_RELEASE -> if (myState == STATE_RELEASE_ON_HOLD) return 2 * Math.max(
                    backgroundAnimDuration,
                    rippleAnimDuration
                ) - (SystemClock.uptimeMillis() - startTime) else if (myState == STATE_RELEASE) return Math.max(
                    backgroundAnimDuration,
                    rippleAnimDuration
                ) - (SystemClock.uptimeMillis() - startTime)
            }
            return -1
        }

    private fun setRippleState(state: Int) {
        if (myState != state) {
            //fix bug incorrect state switch
            if (myState == STATE_OUT && state != STATE_PRESS) return

            myState = state
            if (myState == STATE_OUT || myState == STATE_HOVER) stop() else start()
        }
    }

    private fun setRippleEffect(x: Float, y: Float, radius: Float): Boolean {
        var radius = radius
        if (ripplePoint.x != x || ripplePoint.y != y || rippleRadius != radius) {
            ripplePoint[x] = y
            rippleRadius = radius
            radius = rippleRadius / GRADIENT_RADIUS
            matrix.reset()
            matrix.postTranslate(x, y)
            matrix.postScale(radius, radius, x, y)
            inShader.setLocalMatrix(matrix)
            outShader?.setLocalMatrix(matrix)
            return true
        }
        return false
    }

    override fun onBoundsChange(bounds: Rect) {
        if (backgroundDrawable != null) backgroundDrawable!!.bounds = bounds
        backgroundBounds[(bounds.left + mask!!.left).toFloat(), (bounds.top + mask!!.top).toFloat(), (bounds.right - mask!!.right).toFloat()] =
            (bounds.bottom - mask!!.bottom).toFloat()
        background.reset()
        when (mask!!.type) {
            Mask.TYPE_OVAL -> background.addOval(backgroundBounds, Path.Direction.CW)
            Mask.TYPE_RECTANGLE -> background.addRoundRect(
                backgroundBounds,
                mask!!.cornerRadius,
                Path.Direction.CW
            )
        }
    }

    override fun isStateful(): Boolean {
        return backgroundDrawable != null && backgroundDrawable!!.isStateful
    }

    override fun onStateChange(state: IntArray): Boolean {
        return backgroundDrawable != null && backgroundDrawable!!.setState(state)
    }

    override fun draw(canvas: Canvas) {
        if (backgroundDrawable != null) backgroundDrawable!!.draw(canvas)
        when (rippleType) {
            TYPE_TOUCH, TYPE_TOUCH_MATCH_VIEW -> drawTouch(canvas)
            TYPE_WAVE -> drawWave(canvas)
        }
    }

    private fun drawTouch(canvas: Canvas) {
        if (myState != STATE_OUT) {
            if (backgroundAlphaPercent > 0) {
                fillPaint.color = backgroundColor
                fillPaint.alpha = Math.round(myAlpha * backgroundAlphaPercent)
                canvas.drawPath(background, fillPaint)
            }
            if (rippleRadius > 0 && rippleAlphaPercent > 0) {
                shaderPaint.alpha = Math.round(myAlpha * rippleAlphaPercent)
                shaderPaint.shader = inShader
                canvas.drawPath(background, shaderPaint)
            }
        }
    }

    private fun drawWave(canvas: Canvas) {
        if (myState != STATE_OUT) {
            if (myState == STATE_RELEASE) {
                if (rippleRadius == 0f) {
                    fillPaint.color = rippleColor
                    canvas.drawPath(background, fillPaint)
                } else {
                    shaderPaint.shader = outShader
                    canvas.drawPath(background, shaderPaint)
                }
            } else if (rippleRadius > 0) {
                shaderPaint.shader = inShader
                canvas.drawPath(background, shaderPaint)
            }
        }
    }

    private fun getMaxRippleRadius(x: Float, y: Float): Int {
        val x1 =
            if (x < backgroundBounds.centerX()) backgroundBounds.right else backgroundBounds.left
        val y1 =
            if (y < backgroundBounds.centerY()) backgroundBounds.bottom else backgroundBounds.top
        return Math.round(
            Math.sqrt(
                Math.pow(
                    (x1 - x).toDouble(),
                    2.0
                ) + Math.pow((y1 - y).toDouble(), 2.0)
            )
        ).toInt()
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
//        Log.v(RippleDrawable.class.getSimpleName(), "touch: " + event.getAction() + " " + mState);
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> if (myState == STATE_OUT || myState == STATE_RELEASE) {
                val time = SystemClock.uptimeMillis()
                if (touchTime == 0L) touchTime = time
                setRippleEffect(event.x, event.y, 0f)
                if (touchTime <= time - delayRippleTime) {
                    if (rippleType == TYPE_WAVE || rippleType == TYPE_TOUCH_MATCH_VIEW) maxRippleRadius =
                        getMaxRippleRadius(event.x, event.y)
                    setRippleState(STATE_PRESS)
                }
            } else if (rippleType == TYPE_TOUCH) {
                if (setRippleEffect(event.x, event.y, rippleRadius)) invalidateSelf()
            }
            MotionEvent.ACTION_UP -> {
                if (touchTime > 0 && myState == STATE_OUT) {
                    if (rippleType == TYPE_WAVE || rippleType == TYPE_TOUCH_MATCH_VIEW) maxRippleRadius =
                        getMaxRippleRadius(event.x, event.y)
                    setRippleState(STATE_PRESS)
                }
                touchTime = 0
                if (myState != STATE_OUT) {
                    if (myState == STATE_HOVER) {
                        if (rippleType == TYPE_WAVE || rippleType == TYPE_TOUCH_MATCH_VIEW) setRippleEffect(
                            ripplePoint.x,
                            ripplePoint.y,
                            0f
                        )
                        setRippleState(STATE_RELEASE)
                    } else setRippleState(STATE_RELEASE_ON_HOLD)
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                touchTime = 0
                if (myState != STATE_OUT) {
                    if (myState == STATE_HOVER) {
                        if (rippleType == TYPE_WAVE || rippleType == TYPE_TOUCH_MATCH_VIEW) setRippleEffect(
                            ripplePoint.x,
                            ripplePoint.y,
                            0f
                        )
                        setRippleState(STATE_RELEASE)
                    } else setRippleState(STATE_RELEASE_ON_HOLD)
                }
            }
        }
        return true
    }

    //Animation: based on http://cyrilmottier.com/2012/11/27/actionbar-on-the-move/
    fun cancel() {
        setRippleState(STATE_OUT)
    }

    private fun resetAnimation() {
        startTime = SystemClock.uptimeMillis()
    }

    override fun start() {
        if (isRunning) return
        resetAnimation()
        scheduleSelf(mUpdater, SystemClock.uptimeMillis() + ViewUtil.FRAME_DURATION)
        invalidateSelf()
    }

    override fun stop() {
        running = false
        unscheduleSelf(mUpdater)
        invalidateSelf()
    }

    override fun isRunning(): Boolean {
        return myState != STATE_OUT && myState != STATE_HOVER && running
    }

    override fun scheduleSelf(what: Runnable, `when`: Long) {
        running = true
        super.scheduleSelf(what, `when`)
    }

    private val mUpdater = Runnable {
        when (rippleType) {
            TYPE_TOUCH, TYPE_TOUCH_MATCH_VIEW -> updateTouch()
            TYPE_WAVE -> updateWave()
        }
    }

    init {
        this.backgroundDrawable = backgroundDrawable
        if (rippleType == TYPE_TOUCH && maxRippleRadius <= 0) {
            rippleType = TYPE_TOUCH_MATCH_VIEW
        }
        setMask(
            type,
            topLeftCornerRadius,
            topRightCornerRadius,
            bottomRightCornerRadius,
            bottomLeftCornerRadius,
            left,
            top,
            right,
            bottom
        )
    }

    private fun updateTouch() {
        if (myState != STATE_RELEASE) {
            val backgroundProgress = Math.min(
                1f,
                (SystemClock.uptimeMillis() - startTime).toFloat() / backgroundAnimDuration
            )
            backgroundAlphaPercent =
                inInterpolator.getInterpolation(backgroundProgress) * Color.alpha(backgroundColor) / 255f
            val touchProgress = Math.min(
                1f,
                (SystemClock.uptimeMillis() - startTime).toFloat() / rippleAnimDuration
            )
            rippleAlphaPercent = inInterpolator.getInterpolation(touchProgress)
            setRippleEffect(
                ripplePoint.x,
                ripplePoint.y,
                maxRippleRadius * inInterpolator.getInterpolation(touchProgress)
            )
            if (backgroundProgress == 1f && touchProgress == 1f) {
                startTime = SystemClock.uptimeMillis()
                setRippleState(if (myState == STATE_PRESS) STATE_HOVER else STATE_RELEASE)
            }
        } else {
            val backgroundProgress = Math.min(
                1f,
                (SystemClock.uptimeMillis() - startTime).toFloat() / backgroundAnimDuration
            )
            backgroundAlphaPercent =
                (1f - outInterpolator.getInterpolation(backgroundProgress)) * Color.alpha(
                    backgroundColor
                ) / 255f
            val touchProgress = Math.min(
                1f,
                (SystemClock.uptimeMillis() - startTime).toFloat() / rippleAnimDuration
            )
            rippleAlphaPercent = 1f - outInterpolator.getInterpolation(touchProgress)
            setRippleEffect(
                ripplePoint.x,
                ripplePoint.y,
                maxRippleRadius * (1f + 0.5f * outInterpolator.getInterpolation(touchProgress))
            )
            if (backgroundProgress == 1f && touchProgress == 1f) setRippleState(STATE_OUT)
        }
        if (isRunning) scheduleSelf(mUpdater, SystemClock.uptimeMillis() + ViewUtil.FRAME_DURATION)
        invalidateSelf()
    }

    private fun updateWave() {
        val progress =
            Math.min(1f, (SystemClock.uptimeMillis() - startTime).toFloat() / rippleAnimDuration)
        if (myState != STATE_RELEASE) {
            setRippleEffect(
                ripplePoint.x,
                ripplePoint.y,
                maxRippleRadius * inInterpolator.getInterpolation(progress)
            )
            if (progress == 1f) {
                startTime = SystemClock.uptimeMillis()
                if (myState == STATE_PRESS) setRippleState(STATE_HOVER) else {
                    setRippleEffect(ripplePoint.x, ripplePoint.y, 0f)
                    setRippleState(STATE_RELEASE)
                }
            }
        } else {
            setRippleEffect(
                ripplePoint.x,
                ripplePoint.y,
                maxRippleRadius * outInterpolator.getInterpolation(progress)
            )
            if (progress == 1f) setRippleState(STATE_OUT)
        }
        if (isRunning) scheduleSelf(mUpdater, SystemClock.uptimeMillis() + ViewUtil.FRAME_DURATION)
        invalidateSelf()
    }

    class Mask(
        val type: Int,
        topLeftCornerRadius: Int,
        topRightCornerRadius: Int,
        bottomRightCornerRadius: Int,
        bottomLeftCornerRadius: Int,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ) {
        val cornerRadius = FloatArray(8)
        val left: Int
        val top: Int
        val right: Int
        val bottom: Int

        init {
            cornerRadius[0] = topLeftCornerRadius.toFloat()
            cornerRadius[1] = topLeftCornerRadius.toFloat()
            cornerRadius[2] = topRightCornerRadius.toFloat()
            cornerRadius[3] = topRightCornerRadius.toFloat()
            cornerRadius[4] = bottomRightCornerRadius.toFloat()
            cornerRadius[5] = bottomRightCornerRadius.toFloat()
            cornerRadius[6] = bottomLeftCornerRadius.toFloat()
            cornerRadius[7] = bottomLeftCornerRadius.toFloat()
            this.left = left
            this.top = top
            this.right = right
            this.bottom = bottom
        }

        companion object {
            const val TYPE_RECTANGLE = 0
            const val TYPE_OVAL = 1
        }
    }

    class Builder {
        private var mBackgroundDrawable: Drawable? = null
        private var mBackgroundAnimDuration = 200
        private var mBackgroundColor = 0
        private var mRippleType = 0
        private var mMaxRippleRadius = 0
        private var mRippleAnimDuration = 400
        private var mRippleColor = 0
        private var mDelayClickType = 0
        private var mDelayRippleTime = 0
        private var mInInterpolator: Interpolator? = null
        private var mOutInterpolator: Interpolator? = null
        private var mMaskType = 0
        private var mMaskTopLeftCornerRadius = 0
        private var mMaskTopRightCornerRadius = 0
        private var mMaskBottomLeftCornerRadius = 0
        private var mMaskBottomRightCornerRadius = 0
        private var mMaskLeft = 0
        private var mMaskTop = 0
        private var mMaskRight = 0
        private var mMaskBottom = 0

        constructor() {}
        constructor(context: Context, defStyleRes: Int) : this(context, null, 0, defStyleRes) {}
        constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
            val a = context.obtainStyledAttributes(
                attrs,
                R.styleable.RippleDrawable,
                defStyleAttr,
                defStyleRes
            )
            val type: Int
            var resId: Int
            backgroundColor(a.getColor(R.styleable.RippleDrawable_rd_backgroundColor, 0))
            backgroundAnimDuration(
                a.getInteger(
                    R.styleable.RippleDrawable_rd_backgroundAnimDuration,
                    context.resources.getInteger(android.R.integer.config_mediumAnimTime)
                )
            )
            rippleType(a.getInteger(R.styleable.RippleDrawable_rd_rippleType, TYPE_TOUCH))
            delayClickType(a.getInteger(R.styleable.RippleDrawable_rd_delayClick, DELAY_CLICK_NONE))
            delayRippleTime(a.getInteger(R.styleable.RippleDrawable_rd_delayRipple, 0))
            type = getType(a, R.styleable.RippleDrawable_rd_maxRippleRadius)
            if (type >= TypedValue.TYPE_FIRST_INT && type <= TypedValue.TYPE_LAST_INT) maxRippleRadius(
                a.getInteger(
                    R.styleable.RippleDrawable_rd_maxRippleRadius, -1
                )
            ) else maxRippleRadius(
                a.getDimensionPixelSize(
                    R.styleable.RippleDrawable_rd_maxRippleRadius, dpToPx(context, 48)
                )
            )
            rippleColor(
                a.getColor(
                    R.styleable.RippleDrawable_rd_rippleColor,
                    colorControlHighlight(context, 0)
                )
            )
            rippleAnimDuration(
                a.getInteger(
                    R.styleable.RippleDrawable_rd_rippleAnimDuration,
                    context.resources.getInteger(android.R.integer.config_mediumAnimTime)
                )
            )
            if (a.getResourceId(R.styleable.RippleDrawable_rd_inInterpolator, 0)
                    .also { resId = it } != 0
            ) inInterpolator(
                AnimationUtils.loadInterpolator(context, resId)
            )
            if (a.getResourceId(R.styleable.RippleDrawable_rd_outInterpolator, 0)
                    .also { resId = it } != 0
            ) outInterpolator(
                AnimationUtils.loadInterpolator(context, resId)
            )
            maskType(a.getInteger(R.styleable.RippleDrawable_rd_maskType, Mask.TYPE_RECTANGLE))
            cornerRadius(a.getDimensionPixelSize(R.styleable.RippleDrawable_rd_cornerRadius, 0))
            topLeftCornerRadius(
                a.getDimensionPixelSize(
                    R.styleable.RippleDrawable_rd_topLeftCornerRadius,
                    mMaskTopLeftCornerRadius
                )
            )
            topRightCornerRadius(
                a.getDimensionPixelSize(
                    R.styleable.RippleDrawable_rd_topRightCornerRadius,
                    mMaskTopRightCornerRadius
                )
            )
            bottomRightCornerRadius(
                a.getDimensionPixelSize(
                    R.styleable.RippleDrawable_rd_bottomRightCornerRadius,
                    mMaskBottomRightCornerRadius
                )
            )
            bottomLeftCornerRadius(
                a.getDimensionPixelSize(
                    R.styleable.RippleDrawable_rd_bottomLeftCornerRadius,
                    mMaskBottomLeftCornerRadius
                )
            )
            padding(a.getDimensionPixelSize(R.styleable.RippleDrawable_rd_padding, 0))
            left(a.getDimensionPixelSize(R.styleable.RippleDrawable_rd_leftPadding, mMaskLeft))
            right(a.getDimensionPixelSize(R.styleable.RippleDrawable_rd_rightPadding, mMaskRight))
            top(a.getDimensionPixelSize(R.styleable.RippleDrawable_rd_topPadding, mMaskTop))
            bottom(
                a.getDimensionPixelSize(
                    R.styleable.RippleDrawable_rd_bottomPadding,
                    mMaskBottom
                )
            )
            a.recycle()
        }

        fun build(): RippleDrawable {
            if (mInInterpolator == null) mInInterpolator = AccelerateInterpolator()
            if (mOutInterpolator == null) mOutInterpolator = DecelerateInterpolator()
            return RippleDrawable(
                mBackgroundDrawable,
                mBackgroundAnimDuration,
                mBackgroundColor,
                mRippleType,
                mDelayClickType,
                mDelayRippleTime,
                mMaxRippleRadius,
                mRippleAnimDuration,
                mRippleColor,
                mInInterpolator!!,
                mOutInterpolator!!,
                mMaskType,
                mMaskTopLeftCornerRadius,
                mMaskTopRightCornerRadius,
                mMaskBottomRightCornerRadius,
                mMaskBottomLeftCornerRadius,
                mMaskLeft,
                mMaskTop,
                mMaskRight,
                mMaskBottom
            )
        }

        fun backgroundDrawable(drawable: Drawable?): Builder {
            mBackgroundDrawable = drawable
            return this
        }

        fun backgroundAnimDuration(duration: Int): Builder {
            mBackgroundAnimDuration = duration
            return this
        }

        fun backgroundColor(color: Int): Builder {
            mBackgroundColor = color
            return this
        }

        fun rippleType(type: Int): Builder {
            mRippleType = type
            return this
        }

        fun delayClickType(type: Int): Builder {
            mDelayClickType = type
            return this
        }

        fun delayRippleTime(time: Int): Builder {
            mDelayRippleTime = time
            return this
        }

        fun maxRippleRadius(radius: Int): Builder {
            mMaxRippleRadius = radius
            return this
        }

        fun rippleAnimDuration(duration: Int): Builder {
            mRippleAnimDuration = duration
            return this
        }

        fun rippleColor(color: Int): Builder {
            mRippleColor = color
            return this
        }

        fun inInterpolator(interpolator: Interpolator?): Builder {
            mInInterpolator = interpolator
            return this
        }

        fun outInterpolator(interpolator: Interpolator?): Builder {
            mOutInterpolator = interpolator
            return this
        }

        fun maskType(type: Int): Builder {
            mMaskType = type
            return this
        }

        fun cornerRadius(radius: Int): Builder {
            mMaskTopLeftCornerRadius = radius
            mMaskTopRightCornerRadius = radius
            mMaskBottomLeftCornerRadius = radius
            mMaskBottomRightCornerRadius = radius
            return this
        }

        fun topLeftCornerRadius(radius: Int): Builder {
            mMaskTopLeftCornerRadius = radius
            return this
        }

        fun topRightCornerRadius(radius: Int): Builder {
            mMaskTopRightCornerRadius = radius
            return this
        }

        fun bottomLeftCornerRadius(radius: Int): Builder {
            mMaskBottomLeftCornerRadius = radius
            return this
        }

        fun bottomRightCornerRadius(radius: Int): Builder {
            mMaskBottomRightCornerRadius = radius
            return this
        }

        fun padding(padding: Int): Builder {
            mMaskLeft = padding
            mMaskTop = padding
            mMaskRight = padding
            mMaskBottom = padding
            return this
        }

        fun left(padding: Int): Builder {
            mMaskLeft = padding
            return this
        }

        fun top(padding: Int): Builder {
            mMaskTop = padding
            return this
        }

        fun right(padding: Int): Builder {
            mMaskRight = padding
            return this
        }

        fun bottom(padding: Int): Builder {
            mMaskBottom = padding
            return this
        }
    }

    companion object {
        const val DELAY_CLICK_NONE = 0
        const val DELAY_CLICK_UNTIL_RELEASE = 1
        const val DELAY_CLICK_AFTER_RELEASE = 2
        private const val STATE_OUT = 0
        private const val STATE_PRESS = 1
        private const val STATE_HOVER = 2
        private const val STATE_RELEASE_ON_HOLD = 3
        private const val STATE_RELEASE = 4
        private const val TYPE_TOUCH_MATCH_VIEW = -1
        private const val TYPE_TOUCH = 0
        private const val TYPE_WAVE = 1
        private val GRADIENT_STOPS = floatArrayOf(0f, 0.99f, 1f)
        private const val GRADIENT_RADIUS = 16f
    }
}