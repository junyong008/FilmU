package com.yjy.presentation.util

import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.yjy.domain.model.Joke

object BindingAdapter {

    @BindingAdapter("imageSrc")
    @JvmStatic
    fun ImageView.setImageSrc(imageSrc: String) {
        Glide.with(context)
            .load(imageSrc)
            .into(this)
    }

    @BindingAdapter("uiState")
    @JvmStatic
    fun ProgressBar.setUiState(uiState: UiState) {
        isVisible = (uiState is UiState.Loading)
    }

    @BindingAdapter("uiState")
    @JvmStatic
    fun Button.setUiState(uiState: UiState) {
        isEnabled = (uiState !is UiState.Loading)
    }

    @BindingAdapter("jokeUiState")
    @JvmStatic
    fun TextView.setJokeUiState(jokeUiState: UiState) {
        isVisible = jokeUiState !is UiState.Loading
        text = when(jokeUiState) {
            is UiState.Success<*> -> (jokeUiState.data as Joke).content
            is UiState.Error -> "Error"
            else -> ""
        }
    }
}