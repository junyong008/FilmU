package com.yjy.presentation.feature.preview

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yjy.domain.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PreviewViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
) : ViewModel() {

    private val _officialImage = MutableSharedFlow<Uri>(replay = 0)
    val officialImage: SharedFlow<Uri> = _officialImage

    private val _previewImage = MutableStateFlow<Uri?>(null)
    val previewImage: StateFlow<Uri?> = _previewImage.asStateFlow()

    fun initPreviewImage(imageUri: Uri) {
        _previewImage.value = imageUri
    }

    fun saveImageOfficially() {
        val currentTempImage = previewImage.value ?: return
        viewModelScope.launch {
            val tempImageFile = mediaRepository.getFileFromUri(currentTempImage)
            val officialFile = mediaRepository.moveToOfficialDirectory(tempImageFile)
            val officialUri = mediaRepository.getUriFromFile(officialFile)
            _officialImage.emit(officialUri)
        }
    }
}