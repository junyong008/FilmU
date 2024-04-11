package com.yjy.presentation.feature.camera

import android.Manifest
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
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
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.transition.AutoTransition
import androidx.transition.Transition
import androidx.transition.TransitionListenerAdapter
import androidx.transition.TransitionManager
import com.github.logansdk.permission.PermissionManager
import com.yjy.presentation.R
import com.yjy.presentation.base.BaseActivity
import com.yjy.presentation.databinding.ActivityCameraBinding
import com.yjy.presentation.feature.preview.PreviewActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@AndroidEntryPoint
class CameraActivity : BaseActivity<ActivityCameraBinding>(R.layout.activity_camera) {

    private val cameraViewModel: CameraViewModel by viewModels()
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    override fun initViewModel() {
        binding.cameraViewModel = cameraViewModel
    }

    override fun initView(savedInstanceState: Bundle?) {
        setDisplayCutoutMode()
        initPastImage()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    // 상태바 영역까지 확장하여 사용
    private fun setDisplayCutoutMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    // 기존 이미지가 있는지 체크 (새 프로젝트 or 기존 프로젝트인지 확인하는 겸)
    private fun initPastImage() {
        val pastImage: Uri = intent.extras?.getParcelable("pastImage") ?: return
        cameraViewModel.initPastImage(pastImage)
        listOf(
            binding.linearLayoutAspectRatio
        ).forEach { it.isVisible = false }
        listOf(
            binding.buttonChangePastImage
        ).forEach { it.isVisible = true }
    }

    private fun requestPermissions() {
        PermissionManager.with(this, REQUIRE_PERMISSIONS).check { granted, _, _ ->
            if (granted.size != REQUIRE_PERMISSIONS.size) {
                showToast(getString(R.string.camera_permission_denied))
                onBackPressedCallback.handleOnBackPressed()
            }
        }
    }

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
        val (aspectRatioStrategy, cropAspectRatio) = cameraViewModel.getImageCaptureConfig(cameraViewModel.aspectRatio.value)

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
        val aspectRatioStrategy = cameraViewModel.getImageAnalysisConfig(cameraViewModel.aspectRatio.value)

        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(aspectRatioStrategy)
            .build()

        val imageAnalyzer = ImageAnalysis.Builder()
            .setResolutionSelector(resolutionSelector)
            .build()

        imageAnalyzer.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer { imageProxy ->
            cameraViewModel.analysisImageProxy(
                imageProxy,
                cameraViewModel.pastImage.value,
                cameraViewModel.pastImageForAnalyze.value
            )
            imageProxy.close()
        })
        return imageAnalyzer
    }

    override fun setListener() {
        binding.buttonTakePhoto.setOnClickListener { view ->
            animateShrink(view)
            takePhoto()
        }
        binding.buttonChangePastImage.setOnClickListener {
            cameraViewModel.changePastImage(
                cameraViewModel.pastImage.value,
                cameraViewModel.pastOriginalImage.value,
                cameraViewModel.pastContourImage.value
            )
        }
        binding.buttonFlipCameraSelector.setOnClickListener {
            cameraViewModel.flipCameraSelector(cameraViewModel.cameraSelector.value)
        }
    }

    private fun animateShrink(view: View) {
        val animation = AnimationUtils.loadAnimation(this, R.anim.shrink)
        view.startAnimation(animation)
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        vibrate()
        flashScreen()
        val (outputFileOptions, imageFile) = cameraViewModel.prepareImageCapture(cameraViewModel.cameraSelector.value)
        imageCapture.takePicture(
            outputFileOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    startPreviewActivity(imageFile)
                }
                override fun onError(exception: ImageCaptureException) {
                    showToast(getString(R.string.fail_to_take_photo))
                }
            }
        )
    }

    private fun vibrate() {
        val vibrator = getSystemService(Vibrator::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(VIBRATION_DURATION, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator?.vibrate(VIBRATION_DURATION) //deprecated in API 26
        }
    }

    private fun flashScreen() {
        binding.flashView.visibility = View.VISIBLE
        Handler(Looper.getMainLooper()).postDelayed({
            binding.flashView.visibility = View.GONE
        }, CAPTURE_EFFECT_DURATION)
    }

    private fun startPreviewActivity(imageFile: File) {
        val imageUri = Uri.fromFile(imageFile)
        val intent = Intent(this, PreviewActivity::class.java)
        intent.putExtra("capturedImage", imageUri)
        startActivity(intent)
    }

    override fun observeStateFlows() {
        collectLatestStateFlow(cameraViewModel.aspectRatio) {
            // 뷰가 완전히 렌더링되기 전에 updatePreviewViewSize하면 transition가 작동하지 않음. 고로 post 사용.
            binding.viewTransform.post { updatePreviewViewSize(it) }
            updateCameraConfiguration()
        }
        collectLatestStateFlow(cameraViewModel.cameraSelector) {
            updateCameraConfiguration()
        }
        collectLatestStateFlow(cameraViewModel.pastImage) { pastImage ->
            val pastView = binding.imageViewPast
            pastView.alpha = if (pastImage is CameraViewModel.PastImage.Original) 0.5f else 1f
            pastView.setImageBitmap(pastImage?.getImage())
        }
        collectLatestStateFlow(cameraViewModel.autoCaptureProgress) {
            binding.progressSimilarity.setProgress(it)
        }
    }

    private fun updatePreviewViewSize(aspectRatio: CameraViewModel.AspectRatio) {
        val transformView = binding.viewTransform
        val constraintLayout = transformView.parent as ConstraintLayout
        val transition = AutoTransition().apply {
            duration = 300
            addListener(object : TransitionListenerAdapter() {
                override fun onTransitionEnd(transition: Transition) {
                    super.onTransitionEnd(transition)
                    lifecycleScope.launch {
                        delay(620)
                        transformView.visibility = View.INVISIBLE
                    }
                }
            })
        }

        // 현재 보이는 이미지의 블러화된 Bitmap을 받아와서 viewTransform을 변경.
        val presentBlurImage = cameraViewModel.presentBlurImage.value
        transformView.background = BitmapDrawable(transformView.resources, presentBlurImage)

        transformView.visibility = View.VISIBLE
        TransitionManager.beginDelayedTransition(constraintLayout, transition)
        transformView.layoutParams = when (aspectRatio) {
            CameraViewModel.AspectRatio.RATIO_16_9 -> binding.view916.layoutParams
            CameraViewModel.AspectRatio.RATIO_3_4 -> binding.view34.layoutParams
            CameraViewModel.AspectRatio.RATIO_1_1 -> binding.view11.layoutParams
            CameraViewModel.AspectRatio.RATIO_FULL -> binding.viewFull.layoutParams
        }
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
        collectLatestSharedFlow(cameraViewModel.isProgressCharged) { isProgressCharged ->
            if (isProgressCharged) takePhoto()
        }
    }

    companion object {
        private const val TAG = "CameraActivity"
        private val REQUIRE_PERMISSIONS = mutableListOf(Manifest.permission.CAMERA).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }.toTypedArray()
        private const val VIBRATION_DURATION = 10L
        private const val CAPTURE_EFFECT_DURATION = 10L
    }
}