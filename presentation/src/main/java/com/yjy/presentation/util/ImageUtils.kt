package com.yjy.presentation.util

import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.Rational
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.imgproc.Imgproc
import kotlin.math.max

interface ImageUtils {
    fun bitmapToMat(bitmap: Bitmap): Mat
    fun rotateBitmap(bitmap: Bitmap, angle: Float): Bitmap
    fun resizeBitmap(bitmap: Bitmap, width: Int, height: Int): Bitmap
    fun flipBitmapHorizontally(bitmap: Bitmap): Bitmap
    fun rotateBitmapAccordingToExif(bitmap: Bitmap, orientation: Int): Bitmap
    fun cropMat(source: Mat, target: Mat, aspectRatio: Rational)
    fun rotateMat(source: Mat, target: Mat, degree: Int)
    fun matToBitmap(mat: Mat): Bitmap
    fun getMatSize(mat: Mat): Pair<Int, Int>
    fun imageProxyToMat(imageProxy: ImageProxy): Mat?
}

class ImageUtilsImpl : ImageUtils {

    // Bitmap
    override fun bitmapToMat(bitmap: Bitmap): Mat {
        val mat = Mat(bitmap.height, bitmap.width, CvType.CV_8UC1)
        Utils.bitmapToMat(bitmap, mat)
        return mat
    }

    override fun rotateBitmap(bitmap: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(angle) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    override fun resizeBitmap(bitmap: Bitmap, width: Int, height: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    override fun flipBitmapHorizontally(bitmap: Bitmap): Bitmap {
        val matrix = Matrix().apply {
            postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    override fun rotateBitmapAccordingToExif(bitmap: Bitmap, orientation: Int): Bitmap {
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                rotateBitmap(bitmap, 270f)
                flipBitmapHorizontally(bitmap)
            }
            else -> bitmap
        }
    }

    // Mat
    override fun cropMat(source: Mat, target: Mat, aspectRatio: Rational) {
        val originalWidth = source.width()
        val originalHeight = source.height()
        val targetRatio = aspectRatio.toFloat()

        val (cropWidth, cropHeight) = if (originalWidth.toFloat() / originalHeight < targetRatio) {
            val adjustedHeight = (originalWidth / targetRatio).toInt()
            Pair(originalWidth, adjustedHeight)
        } else {
            val adjustedWidth = (originalHeight * targetRatio).toInt()
            Pair(adjustedWidth, originalHeight)
        }

        val x = max(0, (originalWidth - cropWidth) / 2)
        val y = max(0, (originalHeight - cropHeight) / 2)
        val rect = Rect(x, y, cropWidth, cropHeight)
        val cropped = Mat(source, rect)
        target.create(cropHeight, cropWidth, source.type())
        cropped.copyTo(target)
        cropped.release()
    }

    override fun rotateMat(source: Mat, target: Mat, degree: Int) {
        var rotatedMat = Mat()
        when (degree) {
            90 -> Core.rotate(source, rotatedMat, Core.ROTATE_90_CLOCKWISE)
            180 -> Core.rotate(source, rotatedMat, Core.ROTATE_180)
            270 -> Core.rotate(source, rotatedMat, Core.ROTATE_90_COUNTERCLOCKWISE)
            else -> rotatedMat = source.clone() // 회전이 필요 없는 경우, 원본 Mat 복제
        }
        rotatedMat.copyTo(target)
        rotatedMat.release()
    }

    override fun matToBitmap(mat: Mat): Bitmap {
        val bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, bitmap)
        return bitmap
    }

    override fun getMatSize(mat: Mat): Pair<Int, Int> = Pair(mat.width(), mat.height())

    // ImageProxy
    @OptIn(ExperimentalGetImage::class)
    override fun imageProxyToMat(imageProxy: ImageProxy): Mat? {
        val image = imageProxy.image ?: return null

        val yBuffer = image.planes[0].buffer // Y 플레인
        val uBuffer = image.planes[1].buffer // U 플레인
        val vBuffer = image.planes[2].buffer // V 플레인

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21Bytes = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21Bytes, 0, ySize)
        vBuffer.get(nv21Bytes, ySize, vSize)
        uBuffer.get(nv21Bytes, ySize + vSize, uSize)

        val matYuv = Mat(image.height + image.height / 2, image.width, CvType.CV_8UC1)
        matYuv.put(0, 0, nv21Bytes)

        val mat = Mat()
        Imgproc.cvtColor(matYuv, mat, Imgproc.COLOR_YUV2RGBA_NV21, 4)

        return mat
    }
}