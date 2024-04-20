package com.yjy.presentation.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

class HandOverlayView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private var results: HandLandmarkerResult? = null
    private var linePaint = Paint()
    private var pointPaint = Paint()

    init {
        initPaints()
    }

    private fun initPaints() {
        pointPaint.color = Color.RED
        pointPaint.strokeWidth = LANDMARK_STROKE_WIDTH
        pointPaint.style = Paint.Style.FILL
        linePaint.color = Color.GREEN
        linePaint.strokeWidth = LANDMARK_STROKE_WIDTH
        linePaint.style = Paint.Style.STROKE
    }

    fun setResults(handLandmarkerResults: HandLandmarkerResult) {
        results = handLandmarkerResults
        invalidate()
    }

    fun clear() {
        results = null
        linePaint.reset()
        pointPaint.reset()
        invalidate()
        initPaints()
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        val landmarks = results?.landmarks() ?: return
        for (landmark in landmarks) {
            drawConnections(canvas, landmark)
            drawPoints(canvas, landmark)
        }
    }

    private fun drawConnections(canvas: Canvas, landmark: MutableList<NormalizedLandmark>) {
        HandLandmarker.HAND_CONNECTIONS.forEach {
            canvas.drawLine(
                landmark[it.start()].x() * width,
                landmark[it.start()].y() * height,
                landmark[it.end()].x() * width,
                landmark[it.end()].y() * height,
                linePaint
            )
        }
    }

    private fun drawPoints(canvas: Canvas, landmark: MutableList<NormalizedLandmark>) {
        for (normalizedLandmark in landmark) {
            canvas.drawPoint(
                normalizedLandmark.x() * width,
                normalizedLandmark.y() * height,
                pointPaint
            )
        }
    }

    companion object {
        private const val LANDMARK_STROKE_WIDTH = 15F
    }
}
