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
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
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
    fun `initBeforeImage_uri를 받아 beforeImage와 aspectRatio를 변경`() = runTest(testDispatcher) {
        // Given
        val uri = mockk<Uri>(relaxed = true)
        val bitmap = mockk<Bitmap>(relaxed = true)
        val exifInterface = mockk<ExifInterface>(relaxed = true)
        val rotatedImage = mockk<Bitmap>(relaxed = true)
        val edgeImage = mockk<Bitmap>(relaxed = true)
        coEvery { mediaRepository.getBitmapFromUri(uri) } returns bitmap
        coEvery { mediaRepository.getExifInterfaceFromUri(uri) } returns exifInterface
        coEvery { imageUtils.rotateBitmapAccordingToExif(bitmap, any()) } returns rotatedImage
        coEvery { rotatedImage.width } returns 900
        coEvery { rotatedImage.height } returns 1600
        coEvery { imageProcessor.getEdgeImageFromBitmap(rotatedImage) } returns edgeImage
        coEvery { exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED) } returns ExifInterface.ORIENTATION_ROTATE_90
        // When
        viewModel.initBeforeImage(uri)
        advanceUntilIdle() // 비동기 작업이 끝날때 까지 대기
        // Then
        val expectedBeforeImage = CameraViewModel.BeforeImage.Original(rotatedImage)
        val expectedAspectRatio = CameraViewModel.AspectRatio.RATIO_16_9
        assertEquals(expectedBeforeImage, viewModel.beforeImage.value)
        assertEquals(expectedAspectRatio, viewModel.aspectRatio.value)
    }

    @Test
    fun `initBeforeImage_getBitmapFromUri 또는 getExifInterfaceFromUri 가 null이면 message를 CameraMessage_FailedToAccessFile로 변경`() = runTest(testDispatcher) {
        // Given
        val uri = mockk<Uri>(relaxed = true)
        coEvery { mediaRepository.getBitmapFromUri(uri) } returns null
        coEvery { mediaRepository.getExifInterfaceFromUri(uri) } returns null
        val messages = mutableListOf<CameraViewModel.CameraMessage>()
        val job = launch {
            viewModel.message.collect { messages.add(it) }
        }
        // When
        viewModel.initBeforeImage(uri)
        advanceUntilIdle()
        // Then
        assertTrue(messages.contains(CameraViewModel.CameraMessage.FailedToAccessFile))

        job.cancel()
    }

    @Test
    fun `changeBeforeImage_호출할 때마다 beforeImage 값을 순환`() = runTest {
        // Given
        val contourImage = mockk<Bitmap>(relaxed = true)
        val originalImage = mockk<Bitmap>(relaxed = true)
        viewModel.setBeforeImagesForTest(CameraViewModel.BeforeImage.Original(originalImage), contourImage, originalImage)
        // When & Then
        // Original to Contour
        viewModel.changeBeforeImage()
        assertTrue(viewModel.beforeImage.value is CameraViewModel.BeforeImage.Contour)
        // Contour to None
        viewModel.changeBeforeImage()
        assertTrue(viewModel.beforeImage.value is CameraViewModel.BeforeImage.None)
        // None to Original
        viewModel.changeBeforeImage()
        assertTrue(viewModel.beforeImage.value is CameraViewModel.BeforeImage.Original)
    }

    @Test
    fun `analysisImageProxy_이미지 프록시를 분석하여 현재 블러 이미지를 설정`() = runTest {
        // Given
        val imageProxy: ImageProxy = mockk(relaxed = true)
        val currentMat: Mat = mockk(relaxed = true)
        val blurredImage: Bitmap = mockk(relaxed = true)
        every { imageProcessor.adjustImageProxy(any(), any(), any()) } returns currentMat
        every { imageProcessor.getBlurredImageFromMat(currentMat) } returns blurredImage
        // When
        viewModel.analysisImageProxy(imageProxy)
        // Then
        assertEquals(blurredImage, viewModel.currentBlurImage.value)
    }

    @Test
    fun `analysisImageProxy_이미지 프록시 분석 시 기존 이미지가 있으면 유사도 분석을 실행`() = runTest {
        // Given
        val beforeImage: Bitmap = mockk(relaxed = true)
        val imageProxy: ImageProxy = mockk(relaxed = true)
        val currentMat: Mat = mockk(relaxed = true)
        val resizedImage: Bitmap = mockk(relaxed = true)
        val analyzeImage: Mat = mockk(relaxed = true)
        val resizedSize = Pair(800, 600)
        viewModel.setBeforeImagesForTest(CameraViewModel.BeforeImage.Original(beforeImage), null, beforeImage)
        every { imageUtils.getMatSize(currentMat) } returns resizedSize
        every { imageUtils.resizeBitmap(beforeImage, resizedSize.first, resizedSize.second) } returns resizedImage
        every { imageUtils.bitmapToMat(any()) } returns analyzeImage
        every { imageProcessor.applyGrayscaleOtsuThreshold(any()) } just Runs
        every { imageProcessor.adjustImageProxy(any(), any(), any()) } returns currentMat
        every { imageProcessor.calculateImageSimilarity(any(), any()) } returns 0.01
        // When
        viewModel.analysisImageProxy(imageProxy)
        // Then
        assertTrue(viewModel.getIsImageSameForTest())
    }

    @Test
    fun `analysisImageProxy_유사도 분석 후 일치율이 유지되면 진행율 증가`() = runTest {
        // Given
        val beforeImage: Bitmap = mockk(relaxed = true)
        val imageProxy: ImageProxy = mockk(relaxed = true)
        val currentMat: Mat = mockk(relaxed = true)
        val resizedImage: Bitmap = mockk(relaxed = true)
        val analyzeImage: Mat = mockk(relaxed = true)
        val resizedSize = Pair(800, 600)
        viewModel.setBeforeImagesForTest(CameraViewModel.BeforeImage.Original(beforeImage), null, beforeImage)
        every { imageUtils.getMatSize(currentMat) } returns resizedSize
        every { imageUtils.resizeBitmap(beforeImage, resizedSize.first, resizedSize.second) } returns resizedImage
        every { imageUtils.bitmapToMat(any()) } returns analyzeImage
        every { imageProcessor.applyGrayscaleOtsuThreshold(any()) } just Runs
        every { imageProcessor.adjustImageProxy(any(), any(), any()) } returns currentMat
        every { imageProcessor.calculateImageSimilarity(any(), any()) } returns 0.01
        // When
        viewModel.analysisImageProxy(imageProxy)
        advanceTimeBy(6000) // 6초 동안 진행률 업데이트
        // Then
        assertEquals(100f, viewModel.autoCaptureProgress.value)
    }

    @Test
    fun `prepareImageCapture_이미지 파일 및 메타 데이터 생성`() = runTest {
        // Given
        val expectedFile = File("path/to/image.jpg")
        every { mediaRepository.createImageFile() } returns expectedFile
        viewModel.setCameraSelectorForTest(CameraSelector.DEFAULT_FRONT_CAMERA)
        // When
        val (outputFileOptions, imageFile) = viewModel.prepareImageCapture()
        // Then
        assertEquals(expectedFile, imageFile)
        assertTrue(outputFileOptions.metadata.isReversedHorizontal)
    }

    @Test
    fun `selectAspectRatio_비율 선택`() {
        // Given
        val expectedAspectRatio = CameraViewModel.AspectRatio.RATIO_16_9
        // When
        viewModel.selectAspectRatio(expectedAspectRatio)
        // Then
        assertEquals(expectedAspectRatio, viewModel.aspectRatio.value)
    }

    @Test
    fun `flipCameraSelector_cameraSelector를 반대로 변경`() {
        // Given
        viewModel.setCameraSelectorForTest(CameraSelector.DEFAULT_BACK_CAMERA)

        // When & Then
        // Back to Front
        viewModel.flipCameraSelector()
        assertEquals(CameraSelector.DEFAULT_FRONT_CAMERA, viewModel.cameraSelector.value)
        // Front to Back
        viewModel.flipCameraSelector()
        assertEquals(CameraSelector.DEFAULT_BACK_CAMERA, viewModel.cameraSelector.value)
    }

    @Test
    fun `getImageCaptureConfig_현재 aspectRatio에 따른 적절한 AspectRatioStrategy와 cropAspectRatio 결정`() = runTest {
        // Given
        val displayWidth = 1080
        val displayHeight = 1920
        every { displayManager.getScreenSize() } returns Pair(displayWidth, displayHeight)

        // When & Then
        viewModel.setAspectRatioForTest(CameraViewModel.AspectRatio.RATIO_16_9)
        var config = viewModel.getImageCaptureConfig()
        assertEquals(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY, config.first)
        assertNull(config.second)

        viewModel.setAspectRatioForTest(CameraViewModel.AspectRatio.RATIO_3_4)
        config = viewModel.getImageCaptureConfig()
        assertEquals(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY, config.first)
        assertNull(config.second)

        viewModel.setAspectRatioForTest(CameraViewModel.AspectRatio.RATIO_1_1)
        config = viewModel.getImageCaptureConfig()
        assertEquals(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY, config.first)
        assertNotNull(config.second)

        viewModel.setAspectRatioForTest(CameraViewModel.AspectRatio.RATIO_FULL)
        config = viewModel.getImageCaptureConfig()
        assertEquals(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY, config.first)
        assertNotNull(config.second)
    }

    @Test
    fun `getImageAnalysisConfig_현재 aspectRatio에 따른 적절한 AspectRatioStrategy 반환`() {
        val scenarios = listOf(
            CameraViewModel.AspectRatio.RATIO_16_9 to AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY,
            CameraViewModel.AspectRatio.RATIO_3_4 to AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY,
            CameraViewModel.AspectRatio.RATIO_1_1 to AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY,
            CameraViewModel.AspectRatio.RATIO_FULL to AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY
        )

        scenarios.forEach { (inputAspectRatio, expectedStrategy) ->
            // Given
            viewModel.setAspectRatioForTest(inputAspectRatio)
            // When
            val result = viewModel.getImageAnalysisConfig()
            // Then
            assertEquals(expectedStrategy, result)
        }
    }
}