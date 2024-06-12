package com.yjy.presentation.util

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmark
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

interface HandDetector {
    fun detectInImage(bitmap: Bitmap): HandLandmarkerResult?
    suspend fun getExtendedFingerCount(bitmap: Bitmap): Int
}

class HandDetectorImpl @Inject constructor(
    private val context: Context,
    private val defaultDispatcher: CoroutineDispatcher,
) : HandDetector {

    private val handLandmarker: HandLandmarker? by lazy {
        setupHandLandmarker()
    }

    private fun setupHandLandmarker(): HandLandmarker? {
        // HandLandmarker 설정 로직
        return try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("hand_landmarker.task")
                .setDelegate(Delegate.GPU)
                .build()

            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setNumHands(2)
                .build()

            HandLandmarker.createFromOptions(context, options)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "초기화 실패. ${e.message}")
            null
        }
    }

    override fun detectInImage(bitmap: Bitmap): HandLandmarkerResult? {
        val mpImage = BitmapImageBuilder(bitmap).build()
        return handLandmarker?.detect(mpImage)
    }

    override suspend fun getExtendedFingerCount(bitmap: Bitmap): Int = withContext(defaultDispatcher) {
        val landmarks = detectInImage(bitmap)?.landmarks() ?: return@withContext 0
        var count = 0

        for (landmark in landmarks) {
            if (landmark[HandLandmark.WRIST].y() < landmark[HandLandmark.PINKY_DIP].y()) continue

            val fingers = listOf(
                HandLandmark.INDEX_FINGER_TIP to HandLandmark.INDEX_FINGER_PIP,
                HandLandmark.MIDDLE_FINGER_TIP to HandLandmark.MIDDLE_FINGER_PIP,
                HandLandmark.RING_FINGER_TIP to HandLandmark.RING_FINGER_PIP,
                HandLandmark.PINKY_TIP to HandLandmark.PINKY_PIP
            )

            for ((tip, pip) in fingers) {
                if (isFingerExtended(landmark[tip], landmark[pip], landmark[HandLandmark.WRIST])) count++
            }

            if (isThumbExtended(
                    landmark[HandLandmark.THUMB_TIP],
                    landmark[HandLandmark.THUMB_IP],
                    landmark[HandLandmark.THUMB_CMC],
                    landmark[HandLandmark.INDEX_FINGER_MCP],
                    landmark[HandLandmark.PINKY_MCP]
                )
            ) {
                count++
            }
        }
        return@withContext count
    }

    private fun isFingerExtended(
        tip: NormalizedLandmark,
        pip: NormalizedLandmark,
        wrist: NormalizedLandmark
    ): Boolean {
        val tipToWrist = calculateDistance(tip, wrist)
        val pipToWrist = calculateDistance(pip, wrist)
        return tipToWrist > pipToWrist
    }

    private fun isThumbExtended(
        tip: NormalizedLandmark,
        ip: NormalizedLandmark,
        cmc: NormalizedLandmark,
        idxMcp: NormalizedLandmark,
        pkyMcp: NormalizedLandmark
    ): Boolean {
        val tipToIdxMcp = calculateDistance(tip, idxMcp)
        val tipToPkyMcp = calculateDistance(tip, pkyMcp)
        return tipToIdxMcp < tipToPkyMcp && isStraightAngle(tip, ip, cmc)
    }

    private fun calculateDistance(p1: NormalizedLandmark, p2: NormalizedLandmark): Double {
        val xDiff = p1.x() - p2.x()
        val yDiff = p1.y() - p2.y()
        return sqrt((xDiff * xDiff + yDiff * yDiff).toDouble())
    }

    private fun isStraightAngle(
        A: NormalizedLandmark,
        B: NormalizedLandmark,
        C: NormalizedLandmark
    ): Boolean {
        val angle = calculateAngle(A, B, C)
        return angle > 140 && angle < 220
    }

    private fun calculateAngle(
        A: NormalizedLandmark,
        B: NormalizedLandmark,
        C: NormalizedLandmark
    ): Double {
        val ABx = B.x() - A.x()
        val ABy = B.y() - A.y()
        val CBx = B.x() - C.x()
        val CBy = B.y() - C.y()
        val angleAB = atan2(ABy, ABx)
        val angleCB = atan2(CBy, CBx)
        var angle = (angleAB - angleCB).toDouble()
        if (angle < 0) angle += 2 * PI
        return Math.toDegrees(angle)
    }

    companion object {
        private const val TAG = "HandDetector"
    }
}