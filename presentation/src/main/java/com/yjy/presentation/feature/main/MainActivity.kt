package com.yjy.presentation.feature.main

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import com.yjy.presentation.R
import com.yjy.presentation.base.BaseActivity
import com.yjy.presentation.databinding.ActivityExampleBinding
import com.yjy.presentation.databinding.ActivityMainBinding
import com.yjy.presentation.feature.example.ExampleViewModel
import dagger.hilt.android.AndroidEntryPoint
import org.opencv.android.OpenCVLoader


@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>(R.layout.activity_main) {

    private val mainViewModel: MainViewModel by viewModels()
    private val tag = "MainActivity"

    override fun initViewModel() {
        binding.mainViewModel = mainViewModel
    }

    override fun initView(savedInstanceState: Bundle?) {
        if (!OpenCVLoader.initDebug()) {
            Log.e(tag, "OpenCV 초기화 실패")
        } else {
            Log.d(tag, "OpenCV 초기화 성공")
        }
    }

    override fun setListener() {
        binding.buttonNewProject.setOnClickListener {
            Log.d(tag, "새 프로젝트 버튼 클릭")
        }
    }
}