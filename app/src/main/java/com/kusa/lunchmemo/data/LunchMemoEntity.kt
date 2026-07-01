package com.kusa.lunchmemo.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "lunch_memos")
data class LunchMemoEntity(
    @PrimaryKey val date: String, // yyyy-MM-dd
    val memo: String
)

enum class ComponentType { SELECTION, NUMERIC, TEXT }

data class ComponentConfig(
    val type: ComponentType = ComponentType.TEXT,
    val options: List<String> = emptyList(),
    val digitLimit: Int = 1
) {
    fun serialize(): String = Gson().toJson(this)
    companion object {
        fun deserialize(json: String): ComponentConfig = try {
            Gson().fromJson(json, object : TypeToken<ComponentConfig>() {}.type)
        } catch (e: Exception) {
            ComponentConfig()
        }
    }
}

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 0,
    val notificationHour: Int = 12,
    val notificationMinute: Int = 0,
    val alphanumericOnly: Boolean = false,
    val component1ConfigJson: String = ComponentConfig(ComponentType.SELECTION, listOf("Aランチ", "Bランチ", "Cランチ")).serialize(),
    val component2ConfigJson: String = ComponentConfig(ComponentType.NUMERIC, digitLimit = 2).serialize(),
    val component3ConfigJson: String = ComponentConfig(ComponentType.TEXT).serialize()
)
