package com.kusa.lunchmemo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [LunchMemoEntity::class, AppSettingsEntity::class], version = 4)
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

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE `app_settings` ADD COLUMN `component1ConfigJson` TEXT NOT NULL DEFAULT ''"
                )
                database.execSQL(
                    "ALTER TABLE `app_settings` ADD COLUMN `component2ConfigJson` TEXT NOT NULL DEFAULT ''"
                )
                database.execSQL(
                    "ALTER TABLE `app_settings` ADD COLUMN `component3ConfigJson` TEXT NOT NULL DEFAULT ''"
                )
                
                // Set default values for the new columns
                val defaultConfig1 = ComponentConfig(ComponentType.SELECTION, listOf("Aランチ", "Bランチ", "Cランチ")).serialize()
                val defaultConfig2 = ComponentConfig(ComponentType.NUMERIC, digitLimit = 2).serialize()
                val defaultConfig3 = ComponentConfig(ComponentType.TEXT).serialize()
                
                database.execSQL("UPDATE `app_settings` SET `component1ConfigJson` = '$defaultConfig1'")
                database.execSQL("UPDATE `app_settings` SET `component2ConfigJson` = '$defaultConfig2'")
                database.execSQL("UPDATE `app_settings` SET `component3ConfigJson` = '$defaultConfig3'")
            }
        }

        fun getDatabase(context: Context): LunchMemoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LunchMemoDatabase::class.java,
                    "lunch_memo_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
