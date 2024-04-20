package com.yjy.presentation.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.yjy.presentation.R

class CircularProgressBar(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var progress = 0f
    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
    }
    private val backgroundPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CircularProgressBar,
            0, 0).apply {
            try {
                paint.color = getColor(R.styleable.CircularProgressBar_progressColor, Color.BLUE)
                paint.strokeWidth = getDimension(R.styleable.CircularProgressBar_strokeWidth, 20f)
                backgroundPaint.color = getColor(R.styleable.CircularProgressBar_backgroundColor, Color.GRAY)
                backgroundPaint.strokeWidth = getDimension(R.styleable.CircularProgressBar_strokeWidth, 20f)
            } finally {
                recycle()
            }
        }
    }

    fun setProgress(value: Float) {
        this.progress = value.coerceIn(0f, 100f)
        invalidate()
    }

    fun getProgress() = progress

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = Math.min(width, height) / 2f * 0.8f
        canvas.drawCircle(centerX, centerY, radius, backgroundPaint)
        val sweepAngle = (progress / 100) * 360
        canvas.drawArc(centerX - radius, centerY - radius, centerX + radius, centerY + radius,
            -90f, sweepAngle, false, paint)
    }
}
