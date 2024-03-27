package com.yjy.presentation.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.yjy.presentation.R

class NotificationHelper(private val context: Context) {

    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createExampleNotificationChannel()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createExampleNotificationChannel() {
        val name = "Name"
        val description = "Description"
        val importance = NotificationManager.IMPORTANCE_HIGH

        val exampleChannel = NotificationChannel(CHANNEL_ID_EXAMPLE, name, importance)
        exampleChannel.description = description
        notificationManager.createNotificationChannel(exampleChannel)
    }

    fun showExampleNotification(title: String, content: String) {
        val requestCode = System.currentTimeMillis().toInt()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_EXAMPLE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .build()

        notificationManager.notify(requestCode, notification)
    }

    companion object {
        private const val CHANNEL_ID_EXAMPLE = "channel_id_example"
    }
}