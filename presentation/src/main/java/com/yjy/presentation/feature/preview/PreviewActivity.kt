package com.yjy.presentation.feature.preview

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import com.yjy.presentation.R
import com.yjy.presentation.base.BaseActivity
import com.yjy.presentation.databinding.ActivityPreviewBinding
import com.yjy.presentation.util.collectLatestSharedFlow
import com.yjy.presentation.util.collectLatestStateFlow
import com.yjy.presentation.util.parcelable
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PreviewActivity : BaseActivity<ActivityPreviewBinding>(R.layout.activity_preview) {

    private val previewViewModel: PreviewViewModel by viewModels()

    private fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, message, duration).apply { show() }
    }

    override fun initViewModel() {
        binding.previewViewModel = previewViewModel
    }

    override fun initView(savedInstanceState: Bundle?) {
        setDisplayCutoutMode()
        initPreviewImage()
    }
    private fun setDisplayCutoutMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }
    private fun initPreviewImage() {
        val image: Uri = intent.parcelable("capturedImage") ?: return onBackPressedCallback.handleOnBackPressed()
        previewViewModel.initPreviewImage(image)
    }

    override fun observeFlows() {
        collectLatestStateFlow(previewViewModel.previewImage) {
            binding.imageViewPreview.setImageURI(it)
        }
        collectLatestSharedFlow(previewViewModel.officialImage) {
            // 임시 코드
            showToast("사진 저장 완료")
            onBackPressedCallback.handleOnBackPressed()
        }
    }
}