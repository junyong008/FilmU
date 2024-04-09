package com.yjy.presentation.feature.camera

import android.graphics.Bitmap
import android.media.ExifInterface
import android.net.Uri
import android.util.Rational
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yjy.domain.repository.MediaRepository
import com.yjy.presentation.util.DisplayManager
import com.yjy.presentation.util.ImageProcessor
import com.yjy.presentation.util.ImageUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.opencv.core.Mat
import java.io.File
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val displayManager: DisplayManager,
    private val imageProcessor: ImageProcessor,
    private val imageUtils: ImageUtils,
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

    private val _currentBlurImage = MutableStateFlow<Bitmap?>(null)
    val currentBlurImage: StateFlow<Bitmap?> = _currentBlurImage.asStateFlow()

    private val _autoCaptureProgress = MutableStateFlow(0f)
    val autoCaptureProgress: StateFlow<Float> = _autoCaptureProgress.asStateFlow()

    private val _aspectRatio = MutableStateFlow(AspectRatio.RATIO_FULL)
    val aspectRatio: StateFlow<AspectRatio> = _aspectRatio.asStateFlow()

    private val _cameraSelector = MutableStateFlow(CameraSelector.DEFAULT_BACK_CAMERA)
    val cameraSelector: StateFlow<CameraSelector> = _cameraSelector.asStateFlow()

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
            val rotatedImage = imageUtils.rotateBitmapAccordingToExif(bitmap, orientation)

            oriBeforeImage = rotatedImage
            contourBeforeImage = imageProcessor.getEdgeImageFromBitmap(rotatedImage)

            setAspectRatio(oriBeforeImage!!)
            _beforeImage.value = BeforeImage.Original(oriBeforeImage!!)
        }
    }

    private fun setAspectRatio(bitmap: Bitmap) {
        val width = bitmap.width
        val height = bitmap.height
        val ratio = ((height.toFloat() / width.toFloat()) * 10).roundToInt() / 10f
        _aspectRatio.value = when (ratio) {
            1f -> AspectRatio.RATIO_1_1
            ((4f / 3f) * 10).roundToInt() / 10f -> AspectRatio.RATIO_3_4
            ((16f / 9f) * 10).roundToInt() / 10f -> AspectRatio.RATIO_16_9
            else -> AspectRatio.RATIO_FULL
        }
    }

    fun changeBeforeImage() {
        if(beforeImage.value == null) return
        _beforeImage.value = when(beforeImage.value) {
            is BeforeImage.Original -> BeforeImage.Contour(contourBeforeImage!!)
            is BeforeImage.Contour -> BeforeImage.None
            else -> BeforeImage.Original(oriBeforeImage!!)
        }
    }

    fun analysisImageProxy(imageProxy: ImageProxy) {

        val currentMat = imageProcessor.adjustImageProxy(imageProxy, cameraSelector.value, aspectRatio.value) ?: return
        _currentBlurImage.value = imageProcessor.getBlurredImageFromMat(currentMat)

        // 유사도 분석이 가능하면 유사도 분석 실시.
        if (beforeImage.value != null && beforeImage.value !is BeforeImage.None) {
            compareWithBeforeImage(currentMat)
        }

        // TODO: yolo 손바닥 인식
        currentMat.release()
    }

    private fun compareWithBeforeImage(currentMat: Mat) {

        // 분석용 이미지 생성 (원본 beforeImage로부터 자료형 변환 + 크기 조절 + Gray)
        if (analyzeImage == null) {
            val (resizeW, resizeH) = imageUtils.getMatSize(currentMat)
            val resizedImage = imageUtils.resizeBitmap(oriBeforeImage!!, resizeW, resizeH)
            analyzeImage = imageUtils.bitmapToMat(resizedImage)
            imageProcessor.applyGrayscaleOtsuThreshold(analyzeImage!!)
        }

        // 현재 이미지 또한 분석용 이미지와의 색감차이를 이진화로 개선
        imageProcessor.applyGrayscaleOtsuThreshold(currentMat)

        val similarity = imageProcessor.calculateImageSimilarity(currentMat, analyzeImage!!)
        isImageSame = (similarity < MAX_SIMILARITY_TO_MATCH)
        if (isImageSame && !isProgressStarted && beforeImage.value !is BeforeImage.None) startProgress()
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
        if (oriBeforeImage != null) return
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
        private const val MAX_SIMILARITY_TO_MATCH = 0.03
        private const val AUTO_CAPTURE_DURATION = 5000L
        private const val PROGRESS_UPDATE_INTERVAL = 100L
    }
}