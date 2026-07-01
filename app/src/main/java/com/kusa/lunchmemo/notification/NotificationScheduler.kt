package com.kusa.lunchmemo.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object NotificationScheduler {
    private const val REQUEST_CODE = 1001

    fun scheduleDailyNotification(context: Context, hour: Int = 12, minute: Int = 0) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = "com.kusa.lunchmemo.NOTIFICATION_ALARM"
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            
            // 設定時刻が現在時刻以前（または直後すぎる場合）は翌日に設定
            // 実行時のわずかなタイムラグで同日に再予約されるのを防ぐため、1分程度のバッファを持たせる
            if (timeInMillis <= System.currentTimeMillis() + 1000) {
                add(Calendar.DATE, 1)
            }
        }
        android.util.Log.d("NotifScheduler", "Now       : ${java.util.Date(System.currentTimeMillis())}")
        android.util.Log.d("NotifScheduler", "Scheduled : ${java.util.Date(calendar.timeInMillis)}")
        android.util.Log.d("NotifScheduler", "TimeZone  : ${calendar.timeZone.id}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12以上で正確なアラームの権限があるか確認
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                // 権限がない場合は不正確なアラームを使用。システムにより実行が遅れる可能性がある
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } else {
            // Android 12未満は常に正確なアラームを試行
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelNotification(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = "com.kusa.lunchmemo.NOTIFICATION_ALARM"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }
}
