package com.yjy.data.repository.media

import android.graphics.Bitmap
import android.media.ExifInterface
import android.net.Uri
import com.yjy.data.repository.media.local.MediaLocalDataSource
import com.yjy.domain.repository.MediaRepository
import java.io.File
import javax.inject.Inject

class MediaRepositoryImpl @Inject constructor(
    private val mediaLocalDataSource: MediaLocalDataSource,
) : MediaRepository {

    override fun getFileFromUri(uri: Uri): File = mediaLocalDataSource.getFileFromUri(uri)
    override fun getUriFromFile(file: File): Uri = mediaLocalDataSource.getUriFromFile(file)
    override fun getBitmapFromUri(uri: Uri): Bitmap? = mediaLocalDataSource.getBitmapFromUri(uri)
    override fun getExifInterfaceFromUri(uri: Uri): ExifInterface? = mediaLocalDataSource.getExifInterfaceFromUri(uri)
    override fun createTempImageFile(): File = mediaLocalDataSource.createTempImageFile()
    override fun moveToOfficialDirectory(file: File) = mediaLocalDataSource.moveToOfficialDirectory(file)
}