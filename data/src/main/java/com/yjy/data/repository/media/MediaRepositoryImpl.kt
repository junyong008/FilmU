package com.yjy.data.repository.media

import android.graphics.Bitmap
import android.media.ExifInterface
import android.net.Uri
import com.yjy.data.repository.media.local.MediaLocalDataSource
import com.yjy.domain.repository.MediaRepository
import com.yjy.domain.repository.MediaScanCompleteCallback
import java.io.File
import javax.inject.Inject

class MediaRepositoryImpl @Inject constructor(
    private val mediaLocalDataSource: MediaLocalDataSource,
) : MediaRepository {

    override fun getBitmapFromUri(uri: Uri): Bitmap? = mediaLocalDataSource.getBitmapFromUri(uri)

    override fun getExifInterfaceFromUri(uri: Uri): ExifInterface? =
        mediaLocalDataSource.getExifInterfaceFromUri(uri)

    override fun createImageFile(): File = mediaLocalDataSource.createImageFile()

    override fun scanMediaFile(file: File, callback: MediaScanCompleteCallback?) =
        mediaLocalDataSource.scanMediaFile(file, callback)
}