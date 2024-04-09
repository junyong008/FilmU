package com.yjy.presentation.feature.main

import android.app.Activity
import android.content.Intent
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.yjy.presentation.R
import com.yjy.presentation.base.BaseActivity
import com.yjy.presentation.databinding.ActivityMainBinding
import com.yjy.presentation.feature.camera.CameraActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>(R.layout.activity_main) {

    private val mainViewModel: MainViewModel by viewModels()

    override fun initViewModel() {
        binding.mainViewModel = mainViewModel
    }

    override fun setListener() {
        binding.buttonNewProject.setOnClickListener {
            selectPhotoFromGallery()
        }
    }

    // 임시 함수. 원래는 맨 처음 프로젝트에서 새로운 사진을 촬영하고, 그 다음 해당 프로젝트 내에서 버튼을 클릭하면 바로 최신거에 이어서 하도록 할 예정.
    private fun selectPhotoFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
        selectPhotoResultLauncher.launch(intent)
    }

    private val selectPhotoResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            (result.data?.data)?.let {
                val intent = Intent(this, CameraActivity::class.java)
                intent.putExtra("beforeImage", it)
                startActivity(intent)
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}