package com.thanes.vending

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.app.Notification
import android.provider.Settings

class BootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
      Log.d("BootReceiver", "Device booted")

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Log.d("BootReceiver", "Android 12+ detected - showing notification")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          val permission = Manifest.permission.POST_NOTIFICATIONS
          val granted = ContextCompat.checkSelfPermission(context, permission)

          if (granted != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.w("BootReceiver", "Notification permission not granted - cannot show notification")
            return
          }
        }

        showNotification(context)
      } else {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        try {
          context.startActivity(launchIntent)
          Log.d("BootReceiver", "App launched successfully")
        } catch (e: Exception) {
          Log.e("BootReceiver", "Failed to launch app: ${e.message}")
        }
      }
    }
  }

  private fun showNotification(context: Context) {
    val notificationManager =
      context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "boot_channel"

    val channel = NotificationChannel(
      channelId,
      "Boot Notifications",
      NotificationManager.IMPORTANCE_HIGH
    ).apply {
      description = "Show when device boots"
      enableVibration(true)
      enableLights(true)
      setSound(
        Settings.System.DEFAULT_NOTIFICATION_URI,
        Notification.AUDIO_ATTRIBUTES_DEFAULT
      )
    }
    notificationManager.createNotificationChannel(channel)

    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }

    val pendingIntent = PendingIntent.getActivity(
      context,
      0,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, channelId)
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setContentTitle("Vending Machine")
      .setContentText("แตะเพื่อเปิดแอพหลังจากเครื่องบู๊ต")
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setDefaults(Notification.DEFAULT_ALL)
      .setContentIntent(pendingIntent)
      .setAutoCancel(true)
      .build()

    notificationManager.notify(1, notification)
  }
}
