package com.yjy.domain.repository

import android.graphics.Bitmap
import android.media.ExifInterface
import android.net.Uri
import java.io.File

typealias MediaScanCompleteCallback = (String, Uri) -> Unit

interface MediaRepository {
    fun getFileFromUri(uri: Uri): File
    fun getUriFromFile(file: File): Uri
    fun getBitmapFromUri(uri: Uri): Bitmap?
    fun getExifInterfaceFromUri(uri: Uri): ExifInterface?
    fun createTempImageFile(): File
    fun moveToOfficialDirectory(file: File): File
}