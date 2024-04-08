package com.yjy.presentation.util

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import java.nio.ByteBuffer

// Bitmap 확장 함수
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

// Mat 확장 함수
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

// ImageProxy 확장 함수
fun ImageProxy.toMat(): Mat {
    val buffer: ByteBuffer = this.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val mat = Mat(this.height, this.width, CvType.CV_8UC1)
    mat.put(0, 0, bytes)
    return mat
}