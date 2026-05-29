package com.kusa.lunchmemo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lunch_memos")
data class LunchMemoEntity(
    @PrimaryKey val date: String, // yyyy-MM-dd
    val memo: String
)
