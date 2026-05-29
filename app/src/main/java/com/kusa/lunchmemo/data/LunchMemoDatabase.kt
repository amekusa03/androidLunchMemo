package com.kusa.lunchmemo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [LunchMemoEntity::class], version = 1)
abstract class LunchMemoDatabase : RoomDatabase() {
    abstract fun lunchMemoDao(): LunchMemoDao

    companion object {
        @Volatile
        private var INSTANCE: LunchMemoDatabase? = null

        fun getDatabase(context: Context): LunchMemoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LunchMemoDatabase::class.java,
                    "lunch_memo_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
