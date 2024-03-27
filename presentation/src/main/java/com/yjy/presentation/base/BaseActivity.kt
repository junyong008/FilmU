package com.yjy.presentation.base

import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

abstract class BaseActivity<B : ViewDataBinding>(@LayoutRes val layoutResId: Int) :
    AppCompatActivity() {

    protected lateinit var binding: B
    private var toast: Toast? = null

    protected open val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finish()
        }
    }

    protected fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        toast?.cancel()
        toast = Toast.makeText(this, message, duration).apply { show() }
    }

    fun <T> collectLatestFlow(flow: Flow<T>, collect: suspend (T) -> Unit) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                flow.collectLatest(collect)
            }
        }
    }

    fun <T> collectLatestStateFlow(stateFlow: StateFlow<T>, collect: suspend (T) -> Unit) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                stateFlow.collectLatest(collect)
            }
        }
    }

    fun <T> collectLatestSharedFlow(sharedFlow: SharedFlow<T>, collect: suspend (T) -> Unit) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedFlow.collectLatest(collect)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, layoutResId)
        binding.lifecycleOwner = this

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        initViewModel()
        initView(savedInstanceState)
        setListener()
        observeFlows()
        observeStateFlows()
        observeSharedFlow()
    }

    protected open fun initViewModel() {}
    protected open fun initView(savedInstanceState: Bundle?) {}
    protected open fun setListener() {}
    protected open fun observeFlows() {}
    protected open fun observeStateFlows() {}
    protected open fun observeSharedFlow() {}
}