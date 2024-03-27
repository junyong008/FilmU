package com.yjy.presentation.util

sealed class UiState {
    data object Ready: UiState()
    data object Loading: UiState()
    data class Success<T>(val data: T): UiState()
    data class Error(val message: String): UiState()
}