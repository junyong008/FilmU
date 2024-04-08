package com.yjy.presentation.feature.camera

import android.Manifest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.github.logansdk.permission.PermissionManager
import com.yjy.presentation.R
import com.yjy.presentation.base.BaseActivity
import com.yjy.presentation.databinding.ActivityCameraBinding
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@AndroidEntryPoint
class CameraActivity : BaseActivity<ActivityCameraBinding>(R.layout.activity_camera) {

    private val cameraViewModel: CameraViewModel by viewModels()
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private var progressHandler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null

    override fun initViewModel() {
        binding.cameraViewModel = cameraViewModel
    }

    override fun initView(savedInstanceState: Bundle?) {

        // 상태바 영역까지 확장하여 사용
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        // 기존 이미지가 넘어오는지 체크.
        val beforeImage: Uri? = intent.extras?.getParcelable("beforeImage")
        beforeImage?.let { cameraViewModel.initBeforeImage(it) }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun requestPermissions() {
        PermissionManager.with(this, REQUIRE_PERMISSIONS).check { granted, _, _ ->
            if (granted.size != REQUIRE_PERMISSIONS.size) {
                showToast(getString(R.string.camera_permission_denied))
                onBackPressedCallback.handleOnBackPressed()
            }
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun updateCameraConfiguration() {
        requestPermissions()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({

            val cameraSelector = cameraViewModel.cameraSelector.value

            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            imageCapture = createImageCapture()
            val imageAnalyzer = createImageAnalysis()

            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this@CameraActivity,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalyzer,
                )
            } catch (e: Exception) {
                Log.e(TAG, "바인딩 실패", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun createImageCapture(): ImageCapture {
        val (aspectRatioStrategy, cropAspectRatio) = cameraViewModel.getImageCaptureConfig()

        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(aspectRatioStrategy)
            .build()

        val imageCaptureBuilder = ImageCapture.Builder()
            .setResolutionSelector(resolutionSelector)

        val imageCapture = imageCaptureBuilder.build()

        cropAspectRatio?.let { imageCapture.setCropAspectRatio(it) }
        return imageCapture
    }

    private fun createImageAnalysis(): ImageAnalysis {
        val aspectRatioStrategy = cameraViewModel.getImageAnalysisConfig()

        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(aspectRatioStrategy)
            .build()

        val imageAnalyzer = ImageAnalysis.Builder()
            .setResolutionSelector(resolutionSelector)
            .build()

        imageAnalyzer.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer { imageProxy ->
            cameraViewModel.analysisImageProxy(imageProxy)
            imageProxy.close()
        })
        return imageAnalyzer
    }

    override fun setListener() {
        binding.buttonTakePhoto.setOnClickListener { view ->
            vibrate()
            animateTakePhoto(view)
            flashScreen()
            takePhoto()
        }
    }

    private fun vibrate() {
        val vibrator = getSystemService(Vibrator::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(VIBRATION_DURATION, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator?.vibrate(VIBRATION_DURATION) //deprecated in API 26
        }
    }

    private fun animateTakePhoto(view: View) {
        val animation = AnimationUtils.loadAnimation(this, R.anim.take_photo)
        view.startAnimation(animation)
    }

    private fun flashScreen() {
        binding.flashView.visibility = View.VISIBLE
        Handler(Looper.getMainLooper()).postDelayed({
            binding.flashView.visibility = View.GONE
        }, CAPTURE_EFFECT_DURATION)
    }


    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val (outputFileOptions, imageFile) = cameraViewModel.prepareImageCapture()

        imageCapture.takePicture(
            outputFileOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    cameraViewModel.scanMediaFile(imageFile)
                }
                override fun onError(exception: ImageCaptureException) {
                    showToast("${getString(R.string.fail_to_take_photo)} $exception")
                }
            }
        )
    }

    override fun observeStateFlows() {
        collectLatestStateFlow(cameraViewModel.aspectRatio) {
            updatePreviewViewSize()
            updateCameraConfiguration()
        }
        collectLatestStateFlow(cameraViewModel.cameraSelector) {
            updateCameraConfiguration()
        }
        collectLatestStateFlow(cameraViewModel.beforeImage) { beforeImage ->
            beforeImage?.let {
                val beforeView = binding.imageViewBefore
                when(beforeImage) {
                    is CameraViewModel.BeforeImage.Original -> beforeView.alpha = 0.5f
                    else -> beforeView.alpha = 1f
                }
                beforeView.setImageBitmap(beforeImage.getOriginalImage())
            }
        }
        collectLatestStateFlow(cameraViewModel.isImageSame) { isImageSame ->
            if (isImageSame) startProgress() else resetProgress()
        }
    }

    private fun updatePreviewViewSize() {
        val (newWidth, newHeight) = cameraViewModel.calculatePreviewViewSize()
        binding.previewView.layoutParams = binding.previewView.layoutParams.apply {
            if (newWidth > 0) this.width = newWidth
            if (newHeight > 0) this.height = newHeight
        }
    }

    private fun startProgress() {
        progressHandler.removeCallbacksAndMessages(null) // 이전에 실행된 모든 콜백 및 메시지 제거
        progressRunnable = object : Runnable {
            override fun run() {
                var currentProgressValue = binding.progressSimilarity.getProgress()
                currentProgressValue += (PROGRESS_UPDATE_INTERVAL * 100 / AUTO_CAPTURE_DURATION)
                binding.progressSimilarity.setProgress(currentProgressValue)

                if (currentProgressValue < 100) {
                    progressHandler.postDelayed(this, PROGRESS_UPDATE_INTERVAL)
                }
            }
        }.also { progressHandler.postDelayed(it, PROGRESS_UPDATE_INTERVAL) }
    }

    private fun resetProgress() {
        progressHandler.removeCallbacksAndMessages(null)
        binding.progressSimilarity.setProgress(0f)
    }

    override fun observeSharedFlow() {
        collectLatestSharedFlow(cameraViewModel.message) {
            when(it) {
                is CameraViewModel.CameraMessage.FailedToAccessFile -> {
                    showToast(getString(R.string.fail_to_access_file))
                    onBackPressedCallback.handleOnBackPressed()
                }
            }
        }
        collectLatestSharedFlow(cameraViewModel.imageCaptured) {
            showToast("사진 촬영 완료.")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        progressHandler.removeCallbacksAndMessages(null)
    }

    companion object {
        private const val TAG = "CameraActivity"
        private val REQUIRE_PERMISSIONS = mutableListOf(Manifest.permission.CAMERA).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }.toTypedArray()
        private const val VIBRATION_DURATION = 10L
        private const val CAPTURE_EFFECT_DURATION = 10L
        private const val AUTO_CAPTURE_DURATION = 5000L
        private const val PROGRESS_UPDATE_INTERVAL = 100L
    }
}