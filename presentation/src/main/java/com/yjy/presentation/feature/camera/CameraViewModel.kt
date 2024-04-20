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
import com.yjy.presentation.util.HandDetector
import com.yjy.presentation.util.ImageProcessor
import com.yjy.presentation.util.ImageUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
    private val handDetector: HandDetector,
) : ViewModel() {

    private var progressJob: Job? = null
    private var timerJob: Job? = null
    private var checkExtendedFingerJob: Job? = null
    private var recentExtendedFingerCount = 0

    private val _message = MutableSharedFlow<CameraMessage>(replay = 0)
    val message: SharedFlow<CameraMessage> = _message

    private val _pastImage = MutableStateFlow<PastImage?>(null)
    val pastImage: StateFlow<PastImage?> = _pastImage.asStateFlow()

    private val _pastOriginalImage = MutableStateFlow<Bitmap?>(null)
    val pastOriginalImage: StateFlow<Bitmap?> = _pastOriginalImage.asStateFlow()

    private val _pastContourImage = MutableStateFlow<Bitmap?>(null)
    val pastContourImage: StateFlow<Bitmap?> = _pastContourImage.asStateFlow()

    private val _pastImageForAnalyze = MutableStateFlow<Mat?>(null)
    val pastImageForAnalyze: StateFlow<Mat?> = _pastImageForAnalyze.asStateFlow()

    private val _presentBlurImage = MutableStateFlow<Bitmap?>(null)
    val presentBlurImage: StateFlow<Bitmap?> = _presentBlurImage.asStateFlow()

    private val _autoCaptureProgress = MutableStateFlow(0f)
    val autoCaptureProgress: StateFlow<Float> = _autoCaptureProgress.asStateFlow()

    private val _takePhoto = MutableSharedFlow<Boolean>(replay = 0)
    val takePhoto: SharedFlow<Boolean> = _takePhoto

    private val _timer = MutableStateFlow<Int?>(null)
    val timer: StateFlow<Int?> = _timer.asStateFlow()

    private val _timerSecond = MutableStateFlow<Int?>(null)
    val timerSecond: StateFlow<Int?> = _timerSecond.asStateFlow()

    private val _aspectRatio = MutableStateFlow(AspectRatio.RATIO_FULL)
    val aspectRatio: StateFlow<AspectRatio> = _aspectRatio.asStateFlow()

    private val _cameraSelector = MutableStateFlow(CameraSelector.DEFAULT_BACK_CAMERA)
    val cameraSelector: StateFlow<CameraSelector> = _cameraSelector.asStateFlow()

    /*private val _handData = MutableStateFlow<HandLandmarkerResult?>(null)
    val handData: StateFlow<HandLandmarkerResult?> = _handData*/

    fun initPastImage(initImage: Uri) {
        if (pastImage.value != null) return
        viewModelScope.launch {
            val bitmap = mediaRepository.getBitmapFromUri(initImage)
            val exifInterface = mediaRepository.getExifInterfaceFromUri(initImage)

            if (bitmap == null || exifInterface == null) {
                postMessage(CameraMessage.FailedToAccessFile)
                return@launch
            }

            val orientation = exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )

            // 전면 촬영, 회전 정보를 바탕으로 눈으로 보이는 바와 같이 로드하기 위한 보정 작업.
            val originalImage = imageProcessor.rotateBitmapAccordingToExif(bitmap, orientation)
            val contourImage = imageProcessor.getContourImageFromBitmap(originalImage)

            setAspectRatio(originalImage)
            _pastOriginalImage.value = originalImage
            _pastContourImage.value = contourImage
            _pastImage.value = PastImage.Original(originalImage)
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

    fun changePastImage(
        pastImage: PastImage?,
        pastOriginalImage: Bitmap?,
        pastContourImage: Bitmap?
    ) {
        if (pastImage == null || pastOriginalImage == null || pastContourImage == null) return
        _pastImage.value = when (pastImage) {
            is PastImage.Original -> PastImage.Contour(pastContourImage)
            is PastImage.Contour -> PastImage.None
            is PastImage.None -> PastImage.Original(pastOriginalImage)
        }
    }

    fun changeTimer(timer: Int?) {
        _timer.value = when(timer) {
            null ->  3
            3 -> 5
            5 -> 10
            else -> null
        }
    }

    fun takePhoto(timer: Int?) {
        viewModelScope.launch {
            if (timer != null) {
                startTimer(timer)
            } else {
                resetAutoCaptureProgress()
                _takePhoto.emit(true)
            }
        }
    }

    fun analysisImageProxy(
        imageProxy: ImageProxy,
        pastImage: PastImage?,
        pastImageForAnalyze: Mat?,
    ) {
        viewModelScope.launch {
            val presentImage = imageProcessor.adjustImageProxy(imageProxy, cameraSelector.value, aspectRatio.value) ?: return@launch
            try {
                createBlurImage(presentImage)
                analyzeImageSimilarity(presentImage, pastImage, pastImageForAnalyze)
                checkFingerCountMaintain(presentImage)
                // _handData.value = handDetector.detectInImage(presentBitmap)
            } finally {
                presentImage.release()
                imageProxy.close()
            }
        }
    }

    private suspend fun createBlurImage(presentImage: Mat) {
        _presentBlurImage.value = imageProcessor.getBlurImageFromMat(presentImage)
    }

    private suspend fun analyzeImageSimilarity(presentImage: Mat, pastImage: PastImage?, pastImageForAnalyze: Mat?) {
        if (pastImage != null && pastImage !is PastImage.None && timerJob == null) {
            if (isPresentImageSameWithPastImage(presentImage, pastImageForAnalyze))
                startAutoCaptureProgress()
            else
                resetAutoCaptureProgress()
        }
    }

    private suspend fun checkFingerCountMaintain(presentImage: Mat) {
        val presentBitmap = imageUtils.matToBitmap(presentImage)
        val currentCount = handDetector.getExtendedFingerCount(presentBitmap)
        if (checkExtendedFingerJob == null || recentExtendedFingerCount != currentCount) {
            checkExtendedFingerJob?.cancel()
            recentExtendedFingerCount = currentCount
            checkExtendedFingerJob = viewModelScope.launch {
                delay(MAINTAIN_TIME_FOR_RECOGNIZE_TIMER)
                if (recentExtendedFingerCount == currentCount && currentCount >= MIN_SECOND_FOR_TIMER && timerJob == null) {
                    startTimer(currentCount)
                }
            }
        }
    }

    private suspend fun isPresentImageSameWithPastImage(presentImage: Mat, pastImageForAnalyze: Mat?): Boolean {
        val present = imageProcessor.createThresholdMat(presentImage)
        val past = pastImageForAnalyze ?: createPastImageForAnalyze(presentImage)
        val similarity = imageProcessor.calculateImageSimilarity(present, past)
        present.release()
        return similarity < MAX_SIMILARITY_TO_MATCH
    }
    private suspend fun createPastImageForAnalyze(presentImage: Mat): Mat {
        val (resizeW, resizeH) = imageUtils.getMatSize(presentImage)
        val resizedOriginalBitmap = imageUtils.resizeBitmap(pastOriginalImage.value!!, resizeW, resizeH)
        val resizedOriginalMat = imageUtils.bitmapToMat(resizedOriginalBitmap)
        val result = imageProcessor.createThresholdMat(resizedOriginalMat)
        resizedOriginalMat.release()
        _pastImageForAnalyze.value = result
        return result
    }

    private fun startAutoCaptureProgress() {
        if (progressJob != null) return
        progressJob = viewModelScope.launch {
            var elapsedTime = 0L
            while (elapsedTime < AUTO_CAPTURE_DURATION) {
                if (pastImage.value is PastImage.None) resetAutoCaptureProgress()
                delay(PROGRESS_UPDATE_INTERVAL)
                elapsedTime += PROGRESS_UPDATE_INTERVAL
                _autoCaptureProgress.value = (elapsedTime.toFloat() / AUTO_CAPTURE_DURATION * 100)
            }
            _takePhoto.emit(true)
            delay(1200)
            resetAutoCaptureProgress()
        }
    }
    private fun resetAutoCaptureProgress() {
        progressJob?.cancel()
        progressJob = null
        _autoCaptureProgress.value = 0f
    }

    private fun startTimer(second: Int) {
        if (timerJob != null) return
        resetAutoCaptureProgress()
        timerJob = viewModelScope.launch {
            var remainTime = second
            while (remainTime > 0) {
                _timerSecond.value = remainTime
                delay(1000)
                remainTime--
            }
            _takePhoto.emit(true)
            resetTimer()
        }
    }
    fun resetTimer() {
        timerJob?.cancel()
        timerJob = null
        _timerSecond.value = null
    }

    fun prepareImageCapture(cameraSelector: CameraSelector): Pair<ImageCapture.OutputFileOptions, File> {
        val tempImageFile = mediaRepository.createTempImageFile()
        val metadata = ImageCapture.Metadata().apply {
            isReversedHorizontal = (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA)
        }
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(tempImageFile)
            .setMetadata(metadata)
            .build()

        return Pair(outputFileOptions, tempImageFile)
    }

    fun changeAspectRatio(ratio: AspectRatio) {
        if (pastImage.value != null) return
        _aspectRatio.value = ratio
    }

    fun flipCameraSelector(cameraSelector: CameraSelector) {
        _cameraSelector.value = when (cameraSelector) {
            CameraSelector.DEFAULT_FRONT_CAMERA -> CameraSelector.DEFAULT_BACK_CAMERA
            else -> CameraSelector.DEFAULT_FRONT_CAMERA
        }
    }

    fun getImageCaptureConfig(aspectRatio: AspectRatio): Pair<AspectRatioStrategy, Rational?> {
        var aspectRatioStrategy: AspectRatioStrategy? = null
        var cropAspectRatio: Rational? = null
        when (aspectRatio) {
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

    fun getImageAnalysisConfig(aspectRatio: AspectRatio): AspectRatioStrategy {
        return when (aspectRatio) {
            AspectRatio.RATIO_16_9 -> AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY
            AspectRatio.RATIO_3_4 -> AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY
            AspectRatio.RATIO_1_1 -> AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY
            AspectRatio.RATIO_FULL -> AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY
        }
    }

    override fun onCleared() {
        super.onCleared()
        pastImageForAnalyze.value?.release()
    }

    private suspend fun postMessage(msg: CameraMessage) {
        _message.emit(msg)
    }

    sealed class CameraMessage {
        data object FailedToAccessFile : CameraMessage()
    }

    sealed class PastImage {
        data class Original(val bitmap: Bitmap) : PastImage()
        data class Contour(val bitmap: Bitmap) : PastImage()
        data object None : PastImage()

        fun getImage(): Bitmap? = when (this) {
            is Original -> bitmap
            is Contour -> bitmap
            else -> null
        }
    }

    enum class AspectRatio { RATIO_16_9, RATIO_3_4, RATIO_1_1, RATIO_FULL }

    companion object {
        private const val MAX_SIMILARITY_TO_MATCH = 0.03
        private const val AUTO_CAPTURE_DURATION = 5000L
        private const val PROGRESS_UPDATE_INTERVAL = 100L
        private const val MAINTAIN_TIME_FOR_RECOGNIZE_TIMER = 1500L
        private const val MIN_SECOND_FOR_TIMER = 3
    }
}