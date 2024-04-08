package com.yjy.data.repository.media.local

import android.graphics.Bitmap
import android.media.ExifInterface
import android.net.Uri
import com.yjy.domain.repository.MediaScanCompleteCallback
import java.io.File

interface MediaLocalDataSource {
    fun getBitmapFromUri(uri: Uri): Bitmap?
    fun getExifInterfaceFromUri(uri: Uri): ExifInterface?
    fun createImageFile(): File
    fun scanMediaFile(file: File, callback: MediaScanCompleteCallback?)
}