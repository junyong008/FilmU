package com.yjy.presentation.feature.camera

import android.graphics.Bitmap
import android.media.ExifInterface
import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import com.yjy.domain.repository.MediaRepository
import com.yjy.presentation.util.DisplayManager
import com.yjy.presentation.util.ImageProcessor
import com.yjy.presentation.util.ImageUtils
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.opencv.core.Mat
import java.io.File

@ExperimentalCoroutinesApi
class CameraViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: CameraViewModel
    private lateinit var mediaRepository: MediaRepository
    private lateinit var displayManager: DisplayManager
    private lateinit var imageProcessor: ImageProcessor
    private lateinit var imageUtils: ImageUtils

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mediaRepository = mockk(relaxed = true)
        displayManager = mockk(relaxed = true)
        imageProcessor = mockk(relaxed = true)
        imageUtils = mockk(relaxed = true)
        viewModel = CameraViewModel(mediaRepository, displayManager, imageProcessor, imageUtils)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initPastImage_uri를 받아 pastImage와 aspectRatio를 변경`() = runTest(testDispatcher) {
        // Given
        val uri = mockk<Uri>(relaxed = true)
        val bitmap = mockk<Bitmap>(relaxed = true)
        val exifInterface = mockk<ExifInterface>(relaxed = true)
        val rotatedImage = mockk<Bitmap>(relaxed = true)
        val contourImage = mockk<Bitmap>(relaxed = true)
        coEvery { mediaRepository.getBitmapFromUri(uri) } returns bitmap
        coEvery { mediaRepository.getExifInterfaceFromUri(uri) } returns exifInterface
        coEvery { imageUtils.rotateBitmapAccordingToExif(bitmap, any()) } returns rotatedImage
        coEvery { rotatedImage.width } returns 900
        coEvery { rotatedImage.height } returns 1600
        coEvery { imageProcessor.getContourImageFromBitmap(rotatedImage) } returns contourImage
        coEvery { exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED) } returns ExifInterface.ORIENTATION_ROTATE_90
        // When
        viewModel.initPastImage(uri)
        advanceUntilIdle() // 비동기 작업이 끝날때 까지 대기
        // Then
        val expectedPastImage = CameraViewModel.PastImage.Original(rotatedImage)
        val expectedAspectRatio = CameraViewModel.AspectRatio.RATIO_16_9
        assertEquals(expectedPastImage, viewModel.pastImage.value)
        assertEquals(expectedAspectRatio, viewModel.aspectRatio.value)
    }

    @Test
    fun `initPastImage_getBitmapFromUri 또는 getExifInterfaceFromUri 가 null이면 message를 CameraMessage_FailedToAccessFile로 변경`() = runTest(testDispatcher) {
        // Given
        val uri = mockk<Uri>(relaxed = true)
        coEvery { mediaRepository.getBitmapFromUri(uri) } returns null
        coEvery { mediaRepository.getExifInterfaceFromUri(uri) } returns null
        val messages = mutableListOf<CameraViewModel.CameraMessage>()
        val job = launch {
            viewModel.message.collect { messages.add(it) }
        }
        // When
        viewModel.initPastImage(uri)
        advanceUntilIdle()
        // Then
        assertTrue(messages.contains(CameraViewModel.CameraMessage.FailedToAccessFile))
        job.cancel()
    }

    @Test
    fun `changePastImage_호출할 때마다 beforeImage 값을 순환`() = runTest {
        // Given
        val pastOriginalImage = mockk<Bitmap>(relaxed = true)
        val pastContourImage = mockk<Bitmap>(relaxed = true)
        val pastImage = CameraViewModel.PastImage.None
        // When & Then
        viewModel.changePastImage(pastImage, pastOriginalImage, pastContourImage)
        assertTrue(viewModel.pastImage.value is CameraViewModel.PastImage.Original)
        viewModel.changePastImage(viewModel.pastImage.value, pastOriginalImage, pastContourImage)
        assertTrue(viewModel.pastImage.value is CameraViewModel.PastImage.Contour)
        viewModel.changePastImage(viewModel.pastImage.value, pastOriginalImage, pastContourImage)
        assertTrue(viewModel.pastImage.value is CameraViewModel.PastImage.None)
    }

    @Test
    fun `analysisImageProxy_이미지 프록시를 분석하여 현재 블러 이미지를 설정`() = runTest {
        // Given
        val imageProxy: ImageProxy = mockk(relaxed = true)
        val pastImage: CameraViewModel.PastImage = mockk(relaxed = true)
        val pastImageForAnalyze: Mat = mockk(relaxed = true)
        val presentImage: Mat = mockk(relaxed = true)
        val presentBlurImage: Bitmap = mockk(relaxed = true)
        every { imageProcessor.adjustImageProxy(imageProxy, any(), any()) } returns presentImage
        every { imageProcessor.getBlurImageFromMat(presentImage) } returns presentBlurImage
        // When
        viewModel.analysisImageProxy(imageProxy, pastImage, pastImageForAnalyze)
        // Then
        assertEquals(presentBlurImage, viewModel.presentBlurImage.value)
    }

    @Test
    fun `analysisImageProxy_유사도 분석 후 일치율이 유지되면 진행율 증가`() = runTest {
        // Given
        val imageProxy: ImageProxy = mockk(relaxed = true)
        val pastImage: CameraViewModel.PastImage = mockk(relaxed = true)
        val pastImageForAnalyze: Mat = mockk(relaxed = true)
        val presentImage: Mat = mockk(relaxed = true)
        val presentBlurImage: Bitmap = mockk(relaxed = true)
        every { imageProcessor.adjustImageProxy(imageProxy, any(), any()) } returns presentImage
        every { imageProcessor.getBlurImageFromMat(presentImage) } returns presentBlurImage
        every { imageProcessor.applyGrayscaleOtsuThreshold(any()) } returns presentImage
        every { imageProcessor.calculateImageSimilarity(any(), any()) } returns 0.01
        // When
        viewModel.analysisImageProxy(imageProxy, pastImage, pastImageForAnalyze)
        advanceTimeBy(6000) // 6초 동안 진행률 업데이트
        // Then
        assertEquals(100f, viewModel.autoCaptureProgress.value)
    }

    @Test
    fun `prepareImageCapture_이미지 파일 및 메타 데이터 생성`() = runTest {
        // Given
        val expectedFile = File("path/to/image.jpg")
        every { mediaRepository.createTempImageFile() } returns expectedFile
        // When
        val (outputFileOptions, imageFile) = viewModel.prepareImageCapture(CameraSelector.DEFAULT_FRONT_CAMERA)
        // Then
        assertEquals(expectedFile, imageFile)
        assertTrue(outputFileOptions.metadata.isReversedHorizontal)
    }

    @Test
    fun `flipCameraSelector_cameraSelector를 반대로 변경`() {
        // Back to Front
        viewModel.flipCameraSelector(CameraSelector.DEFAULT_BACK_CAMERA)
        assertEquals(CameraSelector.DEFAULT_FRONT_CAMERA, viewModel.cameraSelector.value)
        // Front to Back
        viewModel.flipCameraSelector(CameraSelector.DEFAULT_FRONT_CAMERA)
        assertEquals(CameraSelector.DEFAULT_BACK_CAMERA, viewModel.cameraSelector.value)
    }

    @Test
    fun `getImageCaptureConfig_aspectRatio에 따른 적절한 AspectRatioStrategy와 cropAspectRatio 결정`() = runTest {
        // Given
        val displayWidth = 1080
        val displayHeight = 1920
        every { displayManager.getScreenSize() } returns Pair(displayWidth, displayHeight)

        // When & Then
        var config = viewModel.getImageCaptureConfig(CameraViewModel.AspectRatio.RATIO_16_9)
        assertEquals(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY, config.first)
        assertNull(config.second)

        config = viewModel.getImageCaptureConfig(CameraViewModel.AspectRatio.RATIO_3_4)
        assertEquals(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY, config.first)
        assertNull(config.second)

        config = viewModel.getImageCaptureConfig(CameraViewModel.AspectRatio.RATIO_1_1)
        assertEquals(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY, config.first)
        assertNotNull(config.second)

        config = viewModel.getImageCaptureConfig(CameraViewModel.AspectRatio.RATIO_FULL)
        assertEquals(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY, config.first)
        assertNotNull(config.second)
    }

    @Test
    fun `getImageAnalysisConfig_aspectRatio에 따른 적절한 AspectRatioStrategy 반환`() {
        // Given
        val scenarios = listOf(
            CameraViewModel.AspectRatio.RATIO_16_9 to AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY,
            CameraViewModel.AspectRatio.RATIO_3_4 to AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY,
            CameraViewModel.AspectRatio.RATIO_1_1 to AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY,
            CameraViewModel.AspectRatio.RATIO_FULL to AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY
        )
        scenarios.forEach { (inputAspectRatio, expectedStrategy) ->
            // When
            val result = viewModel.getImageAnalysisConfig(inputAspectRatio)
            // Then
            assertEquals(expectedStrategy, result)
        }
    }
}