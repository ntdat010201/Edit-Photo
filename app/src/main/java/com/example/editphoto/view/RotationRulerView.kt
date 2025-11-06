package com.example.editphoto.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min

class RotationRulerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        strokeWidth = dpToPx(1f)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = spToPx(9f)
        textAlign = Paint.Align.CENTER
    }

    private val centerLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        strokeWidth = dpToPx(1.5f)
    }

    private var offsetX = 0f
    private var startX = 0f
    private var degreePerTick = 5
    private var tickSpacing = 0f
    private var currentDegree = 0f
    private var viewWidth = 0f

    var onDegreeChange: ((Float) -> Unit)? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewWidth = w.toFloat()
        tickSpacing = viewWidth / 18f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val baseY = height / 2f
        val lineHeightShort = dpToPx(12f)
        val lineHeightLong = dpToPx(24f)

        for (i in -9..9) {
            val x = centerX + i * tickSpacing + offsetX
            if (x in -50f..width + 50f) {
                val degree = i * degreePerTick
                val lineHeight = if (degree % 15 == 0) lineHeightLong else lineHeightShort

                // vẽ vạch
                canvas.drawLine(x, baseY - lineHeight / 2, x, baseY + lineHeight / 2, linePaint)

                if (degree % 15 == 0) {
                    canvas.drawText(
                        degree.toString(),
                        x,
                        baseY + dpToPx(22f),
                        textPaint
                    )
                }
            }
        }

        // Vẽ line giữa (0 độ)
        canvas.drawLine(centerX, 0f, centerX, height.toFloat(), centerLinePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> startX = event.x
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - startX
                startX = event.x
                offsetX += dx

                val maxOffset = 9 * tickSpacing
                offsetX = max(-maxOffset, min(maxOffset, offsetX))

                currentDegree = -offsetX / tickSpacing * degreePerTick
                onDegreeChange?.invoke(currentDegree)
                invalidate()
            }
        }
        return true
    }

    fun getCurrentDegree(): Float = currentDegree

    private fun dpToPx(dp: Float): Float =
        dp * context.resources.displayMetrics.density

    private fun spToPx(sp: Float): Float =
        sp * context.resources.displayMetrics.scaledDensity

    fun setDegree(degree: Float) {
        offsetX = -degree / degreePerTick * tickSpacing
        currentDegree = degree
        invalidate()
    }

}
