package com.kusa.lunchmemo.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.kusa.lunchmemo.data.LunchMemoDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "com.kusa.lunchmemo.NOTIFICATION_ALARM") {
            // 通知設定を取得して再予約（BOOT_COMPLETEDまたはアラーム発生時の次回の予約）
            val database = LunchMemoDatabase.getDatabase(context)
            val dao = database.lunchMemoDao()
            
            CoroutineScope(Dispatchers.IO).launch {
                val settings = dao.getSettings().first()
                settings?.let {
                    NotificationScheduler.scheduleDailyNotification(
                        context, it.notificationHour, it.notificationMinute
                    )
                }
            }

            // アラームが発生した場合は通知ワーカーを起動
            if (intent.action == "com.kusa.lunchmemo.NOTIFICATION_ALARM") {
                val workRequest = OneTimeWorkRequestBuilder<LunchNotificationWorker>().build()
                WorkManager.getInstance(context).enqueue(workRequest)
            }
        }
    }
}
