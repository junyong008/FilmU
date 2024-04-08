package com.yjy.presentation.feature.camera

import android.graphics.Bitmap
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import android.util.Rational
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yjy.domain.repository.MediaRepository
import com.yjy.presentation.util.DisplayManager
import com.yjy.presentation.util.flipHorizontally
import com.yjy.presentation.util.rotate
import com.yjy.presentation.util.toBitmap
import com.yjy.presentation.util.toMat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.opencv.android.OpenCVLoader
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfFloat
import org.opencv.core.MatOfInt
import org.opencv.core.MatOfPoint
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.io.File
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val displayManager: DisplayManager,
) : ViewModel() {

    private var analyzeImage: Mat? = null
    private var oriBeforeImage: Bitmap? = null
    private var contourBeforeImage: Bitmap? = null
    private var isImageSame = false
    private var isProgressStarted = false

    private val _message = MutableSharedFlow<CameraMessage>(replay = 0)
    val message: SharedFlow<CameraMessage> = _message

    private val _imageCaptured = MutableSharedFlow<Uri>(replay = 0)
    val imageCaptured: SharedFlow<Uri> = _imageCaptured

    private val _beforeImage = MutableStateFlow<BeforeImage?>(null)
    val beforeImage: StateFlow<BeforeImage?> = _beforeImage.asStateFlow()

    private val _autoCaptureProgress = MutableStateFlow(0f)
    val autoCaptureProgress: StateFlow<Float> = _autoCaptureProgress.asStateFlow()

    private val _aspectRatio = MutableStateFlow(AspectRatio.RATIO_16_9)
    val aspectRatio: StateFlow<AspectRatio> = _aspectRatio.asStateFlow()

    private val _cameraSelector = MutableStateFlow(CameraSelector.DEFAULT_BACK_CAMERA)
    val cameraSelector: StateFlow<CameraSelector> = _cameraSelector.asStateFlow()

    init {
        OpenCVLoader.initDebug()
    }

    fun initBeforeImage(uri: Uri) {
        if (beforeImage.value != null) return
        viewModelScope.launch {
            val bitmap = mediaRepository.getBitmapFromUri(uri)
            val exifInterface = mediaRepository.getExifInterfaceFromUri(uri)

            if (bitmap == null || exifInterface == null) {
                postMessage(CameraMessage.FailedToAccessFile)
                return@launch
            }

            val orientation = exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )

            // 전면 촬영, 회전 정보를 바탕으로 눈으로 보이는 바와 같이 로드하기 위한 보정 작업.
            val rotatedImage = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> bitmap.rotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> bitmap.rotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> bitmap.rotate(270f)
                ExifInterface.ORIENTATION_TRANSVERSE -> bitmap.rotate(270f).flipHorizontally()
                else -> bitmap
            }

            oriBeforeImage = rotatedImage
            contourBeforeImage = getEdgeImageFromBitmap(rotatedImage)

            _beforeImage.value = BeforeImage.Original(oriBeforeImage!!)
        }
    }

    fun changeBeforeImage() {
        if(beforeImage.value == null) return
        _beforeImage.value = when(beforeImage.value) {
            is BeforeImage.Original -> BeforeImage.Contour(contourBeforeImage!!)
            is BeforeImage.Contour ->  {
                resetProgress()
                BeforeImage.None
            }
            else -> BeforeImage.Original(oriBeforeImage!!)
        }
    }

    private fun getEdgeImageFromBitmap(bitmap: Bitmap): Bitmap {
        val mat = bitmap.toMat()
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY)
        Imgproc.GaussianBlur(mat, mat, Size(17.0, 17.0), 3.0)
        Imgproc.Canny(mat, mat, 0.0, 100.0)
        val contoursMat = getContours(mat)
        val resultBitmap = contoursMat.toBitmap()
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

    fun analysisImageProxy(imageProxy: ImageProxy) {

        // imageProxy를 Mat로 변환 + 보이는 바와 같이 보정작업
        val currentImage = imageProxy.toMat().rotate(imageProxy.imageInfo.rotationDegrees)
        if (cameraSelector.value == CameraSelector.DEFAULT_FRONT_CAMERA)
            Core.flip(currentImage, currentImage, 1)

        if (beforeImage.value != null && beforeImage.value !is BeforeImage.None) {
            compareWithBeforeImage(currentImage)
        }

        // TODO: yolo 손바닥 인식

        currentImage.release()
    }

    private fun compareWithBeforeImage(currentMat: Mat) {

        // 분석용 이미지 생성 (원본 beforeImage로부터 자료형 변환 + 크기 조절 + Gray)
        if (analyzeImage == null) {
            val resizedImage = Bitmap.createScaledBitmap(oriBeforeImage!!, currentMat.width(), currentMat.height(), true)
            analyzeImage = resizedImage.toMat()
            Imgproc.cvtColor(analyzeImage, analyzeImage, Imgproc.COLOR_BGR2GRAY)
        }

        val similarity = calculateImageSimilarity(currentMat, analyzeImage!!)
        isImageSame = (similarity < MAX_SIMILARITY_TO_MATCH)
        if (isImageSame && !isProgressStarted && beforeImage.value !is BeforeImage.None) startProgress()
        Log.d("SMSM", "$similarity")
    }

    private fun calculateImageSimilarity(image1: Mat, image2: Mat): Double {
        val histSize = MatOfInt(256)
        val ranges = MatOfFloat(0f, 256f)
        val hist1 = Mat()
        val hist2 = Mat()
        val accumulate = false
        Imgproc.calcHist(listOf(image1), MatOfInt(0), Mat(), hist1, histSize, ranges, accumulate)
        Imgproc.calcHist(listOf(image2), MatOfInt(0), Mat(), hist2, histSize, ranges, accumulate)
        return Imgproc.compareHist(hist1, hist2, Imgproc.CV_COMP_BHATTACHARYYA)
    }

    private fun startProgress() {
        isProgressStarted = true
        viewModelScope.launch {
            var elapsedTime = 0L
            while (elapsedTime < AUTO_CAPTURE_DURATION) {
                if (!isImageSame || beforeImage.value is BeforeImage.None) {
                    resetProgress()
                    return@launch
                }
                delay(PROGRESS_UPDATE_INTERVAL)
                elapsedTime += PROGRESS_UPDATE_INTERVAL
                _autoCaptureProgress.value = (elapsedTime.toFloat() / AUTO_CAPTURE_DURATION * 100)
            }
        }
    }

    private fun resetProgress() {
        isProgressStarted = false
        _autoCaptureProgress.value = 0f
    }


    fun prepareImageCapture(): Pair<ImageCapture.OutputFileOptions, File> {
        val imageFile = mediaRepository.createImageFile()
        val metadata = ImageCapture.Metadata().apply {
            isReversedHorizontal = cameraSelector.value == CameraSelector.DEFAULT_FRONT_CAMERA
        }

        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(imageFile)
            .setMetadata(metadata)
            .build()

        return Pair(outputFileOptions, imageFile)
    }

    fun scanMediaFile(file: File) {
        mediaRepository.scanMediaFile(file) { _, uri ->
            viewModelScope.launch {
                _imageCaptured.emit(uri)
            }
        }
    }

    fun selectAspectRatio(ratio: AspectRatio) {
        _aspectRatio.value = ratio
    }

    fun flipCameraSelector() {
        _cameraSelector.value = when (cameraSelector.value) {
            CameraSelector.DEFAULT_FRONT_CAMERA -> CameraSelector.DEFAULT_BACK_CAMERA
            else -> CameraSelector.DEFAULT_FRONT_CAMERA
        }
    }

    fun getImageCaptureConfig(): Pair<AspectRatioStrategy, Rational?> {
        var aspectRatioStrategy: AspectRatioStrategy? = null
        var cropAspectRatio: Rational? = null

        when(aspectRatio.value) {
            AspectRatio.RATIO_16_9 -> aspectRatioStrategy = AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY
            AspectRatio.RATIO_3_4 -> aspectRatioStrategy = AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY
            AspectRatio.RATIO_1_1 -> {
                aspectRatioStrategy = AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY
                cropAspectRatio = Rational(1, 1)
            }
            AspectRatio.RATIO_FULL -> {
                aspectRatioStrategy = AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY
                val (screenWidth, screenHeight) = displayManager.getScreenSize()
                cropAspectRatio = Rational(screenWidth, screenHeight)
            }
        }

        return Pair(aspectRatioStrategy, cropAspectRatio)
    }

    fun getImageAnalysisConfig(): AspectRatioStrategy {
        return when(aspectRatio.value) {
            AspectRatio.RATIO_16_9 -> AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY
            AspectRatio.RATIO_3_4 -> AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY
            AspectRatio.RATIO_1_1 -> AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY
            AspectRatio.RATIO_FULL -> AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY
        }
    }

    fun calculatePreviewViewSize(): Pair<Int, Int> {
        var previewViewWidth = 0
        var previewViewHeight = 0
        val (screenWidth, screenHeight) = displayManager.getScreenSize()
        when(aspectRatio.value) {
            AspectRatio.RATIO_16_9 -> previewViewHeight = (screenWidth * 16) / 9
            AspectRatio.RATIO_3_4 -> previewViewHeight = (screenWidth * 4) / 3
            AspectRatio.RATIO_1_1 -> previewViewHeight = screenWidth
            AspectRatio.RATIO_FULL -> {
                previewViewWidth = screenWidth
                previewViewHeight = screenHeight
            }
        }
        return Pair(previewViewWidth, previewViewHeight)
    }

    override fun onCleared() {
        super.onCleared()
        analyzeImage?.release()
    }

    private suspend fun postMessage(msg: CameraMessage) {
        _message.emit(msg)
    }

    sealed class CameraMessage {
        data object FailedToAccessFile : CameraMessage()
    }

    // image - 사용자에게 보여줄 원본 가이드 이미지
    // analyzeImage - 현재 카메라 이미지를 비교 분석할 때 사용할 이미지(크기 조절)
    sealed class BeforeImage {
        data class Original(val image: Bitmap) : BeforeImage()
        data class Contour(val image: Bitmap) : BeforeImage()
        data object None : BeforeImage()

        fun getOriginalImage(): Bitmap? {
            return when (this) {
                is Original -> image
                is Contour -> image
                else -> null
            }
        }
    }

    enum class AspectRatio { RATIO_16_9, RATIO_3_4, RATIO_1_1, RATIO_FULL }

    companion object {
        private const val MAX_SIMILARITY_TO_MATCH = 0.25
        private const val AUTO_CAPTURE_DURATION = 5000L
        private const val PROGRESS_UPDATE_INTERVAL = 100L
    }
}