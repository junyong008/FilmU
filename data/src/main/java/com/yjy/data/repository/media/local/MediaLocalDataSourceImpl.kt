package com.yjy.data.repository.media.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import androidx.core.net.toFile
import androidx.core.net.toUri
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class MediaLocalDataSourceImpl @Inject constructor(private val context: Context) : MediaLocalDataSource {

    override fun getFileFromUri(uri: Uri): File = uri.toFile()

    override fun getUriFromFile(file: File): Uri = file.toUri()

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

    override fun createTempImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "TEMP_FilmU_$timeStamp.jpg"
        val storageDir = context.cacheDir
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    override fun moveToOfficialDirectory(file: File): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "FilmU_$timeStamp.jpg"
        val storageDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "FilmU")
        if (!storageDir.exists()) { storageDir.mkdirs() }

        val officialFile = File(storageDir, imageFileName)
        file.copyTo(officialFile, overwrite = true)
        file.delete()

        scanMediaFile(officialFile)
        return officialFile
    }

    private fun scanMediaFile(file: File) {
        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
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