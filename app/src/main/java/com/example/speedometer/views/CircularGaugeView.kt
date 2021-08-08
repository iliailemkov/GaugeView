package com.example.speedometer.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.animation.doOnEnd
import com.example.speedometer.R
import java.lang.Math.PI
import kotlin.math.cos
import kotlin.math.sin

open class CircularGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val animator = ValueAnimator.ofFloat().apply {
        interpolator = AccelerateDecelerateInterpolator()
        addUpdateListener {
            gaugeValue = it.animatedValue as Int
            invalidate()
        }
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 10f
    }
    private val borderRectF = RectF()
    private val criticalSectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 20f
        color = Color.RED
    }
    private val criticalSectionRectF = RectF()

    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
    }
    private val arrowPath = Path()
    private val arrowBasePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val arrowBaseRectF = RectF()
    private val circlePath = Path()

    private val markPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 10f
    }
    private val markPath = Path()
    private val markRectList: MutableList<FloatArray> = mutableListOf()

    private val minorMarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 6f
    }
    private val minorMarkRectList: MutableList<FloatArray> = mutableListOf()

    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 40f
    }
    private val textBounds = Rect()
    private val textMap: MutableMap<String, Pair<Float, Float>> = mutableMapOf()

    private var attachedToWindow: Boolean = false
    private var marksNumber = 10

    private var startAngle = -240f
    private var endAngle = 60f

    private var centerX = 0f
    private var centerY = 0f

    /**
     * @return size of speedometer.
     */
    val size: Int
        get() {
            return width
        }

    var borderWidth = 10f
        set(value) {
            field = value
            borderPaint.strokeWidth = value
            markPaint.strokeWidth = value
            if (isAttachedToWindow) {
                invalidate()
            }
        }

    var minValue: Int = 0
    var maxValue: Int = 220
    var valueStep: Int = 20
    var minorMarkStep: Int = 5
    var gaugeValue = 0
        private set

    var criticalSectionPercent = .2f

    init {
        init()
        obtainStyledAttributes(attrs, defStyleAttr)
    }

    fun setGaugeValue(s: Int, d: Long, onEnd: (() -> Unit)? = null) {
        animator.apply {
            duration = d
            setIntValues(gaugeValue, s)
            doOnEnd {
                onEnd?.invoke()
            }
            start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        borderRectF.set(
            borderWidth * .5f,
            borderWidth * .5f,
            width.toFloat() - borderWidth * .5f,
            width.toFloat() - borderWidth * .5f
        )
        arrowBaseRectF.set(
            width * .5f - 30f,
            width * .5f - 30f,
            width * .5f + 30f,
            width * .5f + 30f
        )
        criticalSectionRectF.set(
            borderWidth * .5f + 20f,
            borderWidth * .5f + 20f,
            width.toFloat() - 20f,
            width.toFloat() - 20f
        )
        calculateMarks()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        centerX = (width).toFloat()
        centerY = (width).toFloat()
        calculateMarks()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBorder()
        canvas.drawMarks()
        canvas.drawTextNearMarks()
        canvas.drawArrow()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attachedToWindow = true
        if (!isInEditMode) {
            invalidate()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        attachedToWindow = false
    }

    private fun obtainStyledAttributes(attrs: AttributeSet?, defStyleAttr: Int) {
        val typedArray = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CircularGaugeView,
            defStyleAttr,
            0
        )

        try {
            with(typedArray) {
                maxValue = getInt(R.styleable.CircularGaugeView_maxValue, 220)
                minValue = getInt(R.styleable.CircularGaugeView_minValue, 0)
                valueStep = getInt(R.styleable.CircularGaugeView_markStep, 20)
                minorMarkStep = getInt(R.styleable.CircularGaugeView_minorMarkStep, 5)
                startAngle = getFloat(R.styleable.CircularGaugeView_startAngle, 220f)
                endAngle = getFloat(R.styleable.CircularGaugeView_endAngle, -40f)
                criticalSectionPercent =
                    getFloat(R.styleable.CircularGaugeView_criticalSectionPercent, .2f)
            }
        } catch (_: Exception) {
        } finally {
            typedArray.recycle()
        }
    }

    private fun calculateMarks() {
        markRectList.clear()
        minorMarkRectList.clear()
        textMap.clear()
        for (speed in minValue..maxValue step valueStep) {
            val angleRadian = (mapSpeedToAngle(speed)).toRadian()
            markRectList.add(
                arrayOf(
                    (width * .5f + (width * .5f - MAJOR_TICK_SIZE) * cos(angleRadian)),
                    (width * .5f - (width * .5f - MAJOR_TICK_SIZE) * sin(angleRadian)),
                    (width * .5f + (width * .5f - TICK_MARGIN) * cos(angleRadian)),
                    (width * .5f - (width * .5f - TICK_MARGIN) * sin(angleRadian))
                ).toFloatArray()
            )
            textMap[(speed).toString()] = Pair(
                width * .5f + (width * .5f - MAJOR_TICK_SIZE - TICK_MARGIN - TICK_TEXT_MARGIN) * cos(
                    angleRadian
                ),
                width * .5f - (width * .5f - MAJOR_TICK_SIZE - TICK_MARGIN - TICK_TEXT_MARGIN) * sin(
                    angleRadian
                )
            )
        }

        for (step in minValue..maxValue step minorMarkStep) {
            val angleRadian = (mapSpeedToAngle(step)).toRadian()
            minorMarkRectList.add(
                arrayOf(
                    (width * .5f + (width * .5f - MINOR_TICK_SIZE) * cos(angleRadian)),
                    (width * .5f - (width * .5f - MINOR_TICK_SIZE) * sin(angleRadian)),
                    (width * .5f + (width * .5f - TICK_MARGIN) * cos(angleRadian)),
                    (width * .5f - (width * .5f - TICK_MARGIN) * sin(angleRadian))
                ).toFloatArray()
            )
        }
    }

    private fun mapSpeedToAngle(speed: Int): Float {
        return startAngle + ((endAngle - startAngle) / (maxValue - minValue)) * (speed - minValue)
    }

    private fun init() {
        markPaint.color = Color.BLACK
        arrowPaint.color = Color.BLACK
        arrowPaint.strokeWidth = 3f
    }

    private fun Canvas.drawBorder() {
        drawArc(borderRectF, -endAngle, -(startAngle - endAngle), false, borderPaint)
        drawArc(
            criticalSectionRectF,
            -endAngle,
            (-(startAngle - endAngle) * .2f),
            false,
            criticalSectionPaint
        )
    }

    private fun Canvas.drawMarks() {
        markRectList.forEach {
            drawLine(it[0], it[1], it[2], it[3], markPaint)
        }
        minorMarkRectList.forEach {
            drawLine(it[0], it[1], it[2], it[3], minorMarkPaint)
        }
    }

    private fun Canvas.drawTextNearMarks() {
        textMap.forEach {
            textPaint.getTextBounds(it.key, 0, it.key.length, textBounds)
            drawText(
                it.key,
                it.value.first - textBounds.exactCenterX(),
                it.value.second - textBounds.exactCenterY(),
                textPaint
            )
        }
    }

    private fun Canvas.drawArrow() {
        drawArc(arrowBaseRectF, 0f, 360f, false, arrowBasePaint)
        arrowPath.reset()
        arrowPath.moveTo(width * .5f - 60f, width * .5f + 10f)
        arrowPath.lineTo(width * .5f + width * .5f - 150f, width * .5f)
        arrowPath.lineTo(width * .5f - 60f, width * .5f - 10f)
        save()
        rotate(-mapSpeedToAngle(gaugeValue), width * .5f, width * .5f)
        drawPath(arrowPath, arrowPaint)
        restore()
    }

    private fun Float.toRadian(): Float = this * (PI / 180).toFloat()

    companion object {
        private const val TICK_MARGIN = 10f
        private const val TICK_TEXT_MARGIN = 30f
        private const val MAJOR_TICK_SIZE = 50f
        private const val MINOR_TICK_SIZE = 25f
    }
}