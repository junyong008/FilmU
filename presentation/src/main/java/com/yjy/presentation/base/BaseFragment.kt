package com.yjy.presentation.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment

abstract class BaseFragment<B : ViewDataBinding>(@LayoutRes val layoutResId: Int) : Fragment() {

    protected lateinit var binding: B

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)

        binding = DataBindingUtil.inflate(inflater, layoutResId, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        initViewModel()
        initView(savedInstanceState)
        setListener()
        observeFlows()

        return binding.root
    }

    protected open fun initViewModel() {}
    protected open fun initView(savedInstanceState: Bundle?) {}
    protected open fun setListener() {}
    protected open fun observeFlows() {}
}