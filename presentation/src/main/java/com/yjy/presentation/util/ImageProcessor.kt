package com.yjy.presentation.util

import android.graphics.Bitmap
import android.media.ExifInterface
import android.util.Rational
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageProxy
import com.yjy.presentation.feature.camera.CameraViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfFloat
import org.opencv.core.MatOfInt
import org.opencv.core.MatOfPoint
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import javax.inject.Inject
import javax.inject.Named

interface ImageProcessor {
    suspend fun rotateBitmapAccordingToExif(bitmap: Bitmap, orientation: Int): Bitmap
    suspend fun getContourImageFromBitmap(bitmap: Bitmap): Bitmap
    suspend fun getBlurImageFromMat(mat: Mat): Bitmap
    suspend fun adjustImageProxy(imageProxy: ImageProxy, cameraSelector: CameraSelector, aspectRatio: CameraViewModel.AspectRatio): Mat?
    suspend fun calculateImageSimilarity(image1: Mat, image2: Mat): Double
    suspend fun createThresholdMat(mat: Mat): Mat
}

class ImageProcessorImpl @Inject constructor(
    private val imageUtils: ImageUtils,
    private val displayManager: DisplayManager,
    @Named("defaultDispatcher") private val defaultDispatcher: CoroutineDispatcher,
) : ImageProcessor {
    override suspend fun rotateBitmapAccordingToExif(bitmap: Bitmap, orientation: Int): Bitmap = withContext(defaultDispatcher) {
        return@withContext when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> imageUtils.rotateBitmap(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> imageUtils.rotateBitmap(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> imageUtils.rotateBitmap(bitmap, 270f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                imageUtils.rotateBitmap(bitmap, 270f)
                imageUtils.flipBitmapHorizontally(bitmap)
            }
            else -> bitmap
        }
    }

    override suspend fun getContourImageFromBitmap(bitmap: Bitmap): Bitmap = withContext(defaultDispatcher) {
        val mat = imageUtils.bitmapToMat(bitmap)
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY)
        // Imgproc.GaussianBlur(mat, mat, Size(17.0, 17.0), 3.0)
        Imgproc.medianBlur(mat, mat, 15)
        Imgproc.Canny(mat, mat, 0.0, 100.0)
        val contoursMat = getContours(mat)
        val resultBitmap = imageUtils.matToBitmap(contoursMat)
        mat.release()
        contoursMat.release()
        return@withContext resultBitmap
    }

    private fun getContours(mat: Mat): Mat {
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(mat, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        val contoursMat = Mat.zeros(mat.size(), CvType.CV_8UC4)
        contoursMat.setTo(Scalar(0.0, 0.0, 0.0, 0.0))

        for (contour in contours) {
            Imgproc.drawContours(contoursMat, listOf(contour), -1, Scalar(255.0, 255.0, 255.0, 255.0), 15)
        }

        hierarchy.release()
        contours.forEach { it.release() }
        return contoursMat
    }

    // imageProxy를 preview로 보는 바와 같이 보정작업 (자료형 변환 + 회전 + 크롭)
    override suspend fun adjustImageProxy(
        imageProxy: ImageProxy,
        cameraSelector: CameraSelector,
        aspectRatio: CameraViewModel.AspectRatio
    ): Mat? = withContext(defaultDispatcher) {
        val currentMat = imageUtils.imageProxyToMat(imageProxy) ?: return@withContext null
        imageUtils.rotateMat(currentMat, currentMat, imageProxy.imageInfo.rotationDegrees)
        if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) Core.flip(currentMat, currentMat, 1)
        when (aspectRatio) {
            CameraViewModel.AspectRatio.RATIO_1_1 -> Rational(1, 1)
            CameraViewModel.AspectRatio.RATIO_FULL -> Rational(displayManager.getScreenSize().first, displayManager.getScreenSize().second)
            else -> null
        }?.let { imageUtils.cropMat(currentMat, currentMat, it) }
        return@withContext currentMat
    }

    override suspend fun getBlurImageFromMat(mat: Mat): Bitmap = withContext(defaultDispatcher) {
        val blurMat = Mat()
        Imgproc.blur(mat, blurMat, Size(200.0, 200.0))
        val result = imageUtils.matToBitmap(blurMat)
        blurMat.release()
        return@withContext result
    }

    override suspend fun calculateImageSimilarity(image1: Mat, image2: Mat): Double = withContext(defaultDispatcher) {
        val histSize = MatOfInt(256)
        val ranges = MatOfFloat(0f, 256f)
        val hist1 = Mat()
        val hist2 = Mat()
        val accumulate = false
        Imgproc.calcHist(listOf(image1), MatOfInt(0), Mat(), hist1, histSize, ranges, accumulate)
        Imgproc.calcHist(listOf(image2), MatOfInt(0), Mat(), hist2, histSize, ranges, accumulate)
        return@withContext Imgproc.compareHist(hist1, hist2, Imgproc.CV_COMP_BHATTACHARYYA)
    }

    override suspend fun createThresholdMat(mat: Mat): Mat = withContext(defaultDispatcher) {
        val resultMat = Mat()
        Imgproc.cvtColor(mat, resultMat, Imgproc.COLOR_BGR2GRAY)
        Imgproc.threshold(resultMat, resultMat, 0.0, 255.0, Imgproc.THRESH_OTSU)
        return@withContext resultMat
    }
}