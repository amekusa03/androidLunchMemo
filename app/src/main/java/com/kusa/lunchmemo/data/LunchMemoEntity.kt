package com.kusa.lunchmemo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lunch_memos")
data class LunchMemoEntity(
    @PrimaryKey val date: String, // yyyy-MM-dd
    val memo: String
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 0,
    val notificationHour: Int = 12,
    val notificationMinute: Int = 0,
    val alphanumericOnly: Boolean = false
)
