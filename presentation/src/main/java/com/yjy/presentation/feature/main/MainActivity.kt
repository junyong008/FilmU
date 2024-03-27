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

    override fun initViewModel() {
        binding.mainViewModel = mainViewModel
    }

    override fun initView(savedInstanceState: Bundle?) {
        if (!OpenCVLoader.initDebug()) {
            Log.e("MainActivity", "OpenCV 초기화 실패")
        } else {
            Log.d("MainActivity", "OpenCV 초기화 성공")
        }
    }
}