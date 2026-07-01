package com.kusa.lunchmemo.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.kusa.lunchmemo.MainActivity
import com.kusa.lunchmemo.data.LunchMemoDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == "com.kusa.lunchmemo.NOTIFICATION_ALARM") {
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val database = LunchMemoDatabase.getDatabase(context)
                    val dao = database.lunchMemoDao()
                    val settings = dao.getSettings().first()
                    val hour = settings?.notificationHour ?: 12
                    val minute = settings?.notificationMinute ?: 0

                    // 翌日のアラームを再スケジュール
                    NotificationScheduler.scheduleDailyNotification(context, hour, minute)

                    // WorkManagerを使わず直接通知
                    if (action == "com.kusa.lunchmemo.NOTIFICATION_ALARM") {
                        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                        val memos = dao.getAllMemos().first()
                        val todayMemo = memos.find { it.date == today }?.memo
                        if (!todayMemo.isNullOrBlank()) {
                            showNotification(context, todayMemo)
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun showNotification(context: Context, memoContent: String) {
        val channelId = "lunch_memo_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            "ランチ通知",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "ランチメモの通知チャンネル"
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
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("ランチメモ")
            .setContentText(memoContent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}