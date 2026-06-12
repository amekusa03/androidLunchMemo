package com.kusa.lunchmemo.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kusa.lunchmemo.MainActivity
import com.kusa.lunchmemo.data.LunchMemoDatabase
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.first

class LunchNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = LunchMemoDatabase.getDatabase(applicationContext)
        val dao = database.lunchMemoDao()
        
        // 実行時刻にかかわらず「今日」のメモを探す
        // AlarmManagerで指定時刻（例：12:00）に起動された直後に実行される想定
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        
        val memos = dao.getAllMemos().first()
        val todayMemo = memos.find { it.date == today }?.memo

        if (!todayMemo.isNullOrBlank()) {
            showNotification(todayMemo)
        }

        return Result.success()
    }

    private fun showNotification(memoContent: String) {
        val channelId = "lunch_memo_channel"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            "ランチ通知",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "お昼のメニューをお知らせします"
        }
        notificationManager.createNotificationChannel(channel)

        // 通知タップ時にアプリ（MainActivity）を開くためのIntent
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("今日のランチ")
            .setContentText(memoContent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent) // タップ時のIntentを設定
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}
