package com.yjy.presentation.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

abstract class BaseFragment<B : ViewDataBinding>(@LayoutRes val layoutResId: Int) : Fragment() {

    protected lateinit var binding: B
    private var toast: Toast? = null

    protected fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        toast?.cancel()
        toast = Toast.makeText(requireContext(), message, duration).apply { show() }
    }

    fun <T> collectLatestFlow(flow: Flow<T>, collect: suspend (T) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                flow.collectLatest(collect)
            }
        }
    }

    fun <T> collectLatestStateFlow(stateFlow: StateFlow<T>, collect: suspend (T) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                stateFlow.collectLatest(collect)
            }
        }
    }

    fun <T> collectLatestSharedFlow(sharedFlow: SharedFlow<T>, collect: suspend (T) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedFlow.collectLatest(collect)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)

        binding = DataBindingUtil.inflate(inflater, layoutResId, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        initViewModel()
        initView(savedInstanceState)
        setListener()
        observeFlows()
        observeStateFlows()
        observeSharedFlow()

        return binding.root
    }

    protected open fun initViewModel() {}
    protected open fun initView(savedInstanceState: Bundle?) {}
    protected open fun setListener() {}
    protected open fun observeFlows() {}
    protected open fun observeStateFlows() {}
    protected open fun observeSharedFlow() {}
}