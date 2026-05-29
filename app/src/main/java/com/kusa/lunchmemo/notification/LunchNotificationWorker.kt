package com.kusa.lunchmemo.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kusa.lunchmemo.R
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
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        
        // 全てのメモを取得して今日の日付のものを探す（簡易的な実装）
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

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // 本来はアプリアイコン
            .setContentTitle("今日のランチ")
            .setContentText(memoContent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}
