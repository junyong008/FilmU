package com.yjy.data.repository.media.local

import android.graphics.Bitmap
import android.media.ExifInterface
import android.net.Uri
import java.io.File

interface MediaLocalDataSource {
    fun getFileFromUri(uri: Uri): File
    fun getUriFromFile(file: File): Uri
    fun getBitmapFromUri(uri: Uri): Bitmap?
    fun getExifInterfaceFromUri(uri: Uri): ExifInterface?
    fun createTempImageFile(): File
    fun moveToOfficialDirectory(file: File): File
}