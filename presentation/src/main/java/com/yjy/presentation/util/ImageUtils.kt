package com.yjy.presentation.util

import android.graphics.Bitmap
import android.graphics.Matrix
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

object ImageUtils {

    // Bitmap
    fun Bitmap.toMat(): Mat {
        val mat = Mat(this.height, this.width, CvType.CV_8UC1)
        Utils.bitmapToMat(this, mat)
        return mat
    }

    fun Bitmap.rotate(angle: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(angle) }
        return Bitmap.createBitmap(this, 0, 0, this.width, this.height, matrix, true)
    }

    fun Bitmap.flipHorizontally(): Bitmap {
        val matrix = Matrix().apply {
            postScale(-1f, 1f, this@flipHorizontally.width / 2f, this@flipHorizontally.height / 2f)
        }
        return Bitmap.createBitmap(this, 0, 0, this.width, this.height, matrix, true)
    }

    // Mat
    fun cropMat(source: Mat, target: Mat, aspectRatio: Rational) {
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

    fun rotateMat(source: Mat, target: Mat, degree: Int) {
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

    fun Mat.toBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(this.cols(), this.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(this, bitmap)
        return bitmap
    }

    fun Mat.rotate(degree: Int): Mat {
        var rotatedMat = Mat()
        when (degree) {
            90 -> Core.rotate(this, rotatedMat, Core.ROTATE_90_CLOCKWISE)
            180 -> Core.rotate(this, rotatedMat, Core.ROTATE_180)
            270 -> Core.rotate(this, rotatedMat, Core.ROTATE_90_COUNTERCLOCKWISE)
            else -> rotatedMat = this.clone() // 회전이 필요 없는 경우, 원본 Mat 복제
        }
        this.release()
        return rotatedMat
    }

    // ImageProxy
    @OptIn(ExperimentalGetImage::class)
    fun ImageProxy.toMat(): Mat? {
        val image = this.image ?: return null

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