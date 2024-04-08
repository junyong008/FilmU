package com.yjy.data.repository.media.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import com.yjy.domain.repository.MediaScanCompleteCallback
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class MediaLocalDataSourceImpl @Inject constructor(private val context: Context) : MediaLocalDataSource {

    override fun getBitmapFromUri(uri: Uri): Bitmap? {
        return safeOperation {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        }
    }

    override fun getExifInterfaceFromUri(uri: Uri): ExifInterface? {
        return safeOperation {
            context.contentResolver.openInputStream(uri)?.let { inputStream ->
                ExifInterface(inputStream)
            }
        }
    }

    override fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "FilmU_${timeStamp}.jpg"
        val storageDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "FilmU")
        if (!storageDir.exists()) { storageDir.mkdirs() }
        return File(storageDir, imageFileName)
    }

    override fun scanMediaFile(file: File, callback: MediaScanCompleteCallback?) {
        MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            null
        ) { path, uri ->
            callback?.invoke(path, uri)
        }
    }

    private fun <T> safeOperation(block: () -> T): T? {
        return try {
            block()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}