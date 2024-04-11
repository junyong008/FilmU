package com.yjy.presentation.feature.preview

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.viewModels
import com.yjy.presentation.R
import com.yjy.presentation.base.BaseActivity
import com.yjy.presentation.databinding.ActivityPreviewBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PreviewActivity : BaseActivity<ActivityPreviewBinding>(R.layout.activity_preview) {

    private val previewViewModel: PreviewViewModel by viewModels()

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
        val image: Uri? = intent.getParcelableExtra("capturedImage")
        if (image == null) onBackPressedCallback.handleOnBackPressed()
        previewViewModel.initPreviewImage(image!!)
    }

    override fun observeStateFlows() {
        collectLatestStateFlow(previewViewModel.previewImage) {
            binding.imageViewPreview.setImageURI(it)
        }
    }

    override fun observeSharedFlow() {
        collectLatestSharedFlow(previewViewModel.officialImage) {
            // 임시 코드
            showToast("사진 저장 완료")
            onBackPressedCallback.handleOnBackPressed()
        }
    }
}