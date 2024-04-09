package com.yjy.presentation.util

import android.graphics.Bitmap
import android.util.Rational
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageProxy
import com.yjy.presentation.feature.camera.CameraViewModel
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

interface ImageProcessor {
    fun getEdgeImageFromBitmap(bitmap: Bitmap): Bitmap
    fun getBlurredImageFromMat(mat: Mat): Bitmap
    fun adjustImageProxy(imageProxy: ImageProxy, cameraSelector: CameraSelector, aspectRatio: CameraViewModel.AspectRatio): Mat?
    fun calculateImageSimilarity(image1: Mat, image2: Mat): Double
    fun applyGrayscaleOtsuThreshold(mat: Mat)
}

class ImageProcessorImpl @Inject constructor(
    private val imageUtils: ImageUtils,
    private val displayManager: DisplayManager,
) : ImageProcessor {
    override fun getEdgeImageFromBitmap(bitmap: Bitmap): Bitmap {
        val mat = imageUtils.bitmapToMat(bitmap)
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY)
        // Imgproc.GaussianBlur(mat, mat, Size(17.0, 17.0), 3.0)
        Imgproc.medianBlur(mat, mat, 15)
        Imgproc.Canny(mat, mat, 0.0, 100.0)
        val contoursMat = getContours(mat)
        val resultBitmap = imageUtils.matToBitmap(contoursMat)
        mat.release()
        contoursMat.release()
        return resultBitmap
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
    override fun adjustImageProxy(
        imageProxy: ImageProxy,
        cameraSelector: CameraSelector,
        aspectRatio: CameraViewModel.AspectRatio
    ): Mat? {
        val currentMat = imageUtils.imageProxyToMat(imageProxy) ?: return null
        imageUtils.rotateMat(currentMat, currentMat, imageProxy.imageInfo.rotationDegrees)
        if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) Core.flip(currentMat, currentMat, 1)
        when (aspectRatio) {
            CameraViewModel.AspectRatio.RATIO_1_1 -> Rational(1, 1)
            CameraViewModel.AspectRatio.RATIO_FULL -> Rational(displayManager.getScreenSize().first, displayManager.getScreenSize().second)
            else -> null
        }?.let { imageUtils.cropMat(currentMat, currentMat, it) }
        return currentMat
    }

    override fun getBlurredImageFromMat(mat: Mat): Bitmap {
        val blurMat = Mat()
        mat.copyTo(blurMat)
        Imgproc.blur(blurMat, blurMat, Size(200.0, 200.0))
        val result = imageUtils.matToBitmap(blurMat)
        blurMat.release()
        return result
    }

    override fun calculateImageSimilarity(image1: Mat, image2: Mat): Double {
        val histSize = MatOfInt(256)
        val ranges = MatOfFloat(0f, 256f)
        val hist1 = Mat()
        val hist2 = Mat()
        val accumulate = false
        Imgproc.calcHist(listOf(image1), MatOfInt(0), Mat(), hist1, histSize, ranges, accumulate)
        Imgproc.calcHist(listOf(image2), MatOfInt(0), Mat(), hist2, histSize, ranges, accumulate)
        return Imgproc.compareHist(hist1, hist2, Imgproc.CV_COMP_BHATTACHARYYA)
    }

    override fun applyGrayscaleOtsuThreshold(mat: Mat) {
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY)
        Imgproc.threshold(mat, mat, 0.0, 255.0, Imgproc.THRESH_OTSU)
    }
}