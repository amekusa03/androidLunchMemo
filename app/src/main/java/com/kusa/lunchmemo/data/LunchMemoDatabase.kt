package com.kusa.lunchmemo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [LunchMemoEntity::class, AppSettingsEntity::class], version = 3)
abstract class LunchMemoDatabase : RoomDatabase() {
    abstract fun lunchMemoDao(): LunchMemoDao

    companion object {
        @Volatile
        private var INSTANCE: LunchMemoDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `app_settings` (" +
                            "`id` INTEGER NOT NULL, " +
                            "`notificationHour` INTEGER NOT NULL, " +
                            "`notificationMinute` INTEGER NOT NULL, " +
                            "PRIMARY KEY(`id`))"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE `app_settings` ADD COLUMN `alphanumericOnly` INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        fun getDatabase(context: Context): LunchMemoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LunchMemoDatabase::class.java,
                    "lunch_memo_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
