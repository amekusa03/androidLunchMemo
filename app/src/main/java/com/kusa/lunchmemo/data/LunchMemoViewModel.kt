package com.kusa.lunchmemo.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class LunchMemoViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = LunchMemoDatabase.getDatabase(application).lunchMemoDao()
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    val memos = dao.getAllMemos().map { list ->
        list.associate { LocalDate.parse(it.date, formatter) to it.memo }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val settings = dao.getSettings().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AppSettingsEntity()
    )

    fun saveMemo(date: LocalDate, memo: String) {
        viewModelScope.launch {
            dao.insertMemo(LunchMemoEntity(date.format(formatter), memo))
        }
    }

    fun updateNotificationTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            val current = settings.value ?: AppSettingsEntity()
            dao.saveSettings(current.copy(notificationHour = hour, notificationMinute = minute))
        }
    }

    fun updateAlphanumericOnly(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value ?: AppSettingsEntity()
            dao.saveSettings(current.copy(alphanumericOnly = enabled))
        }
    }

    fun updateComponentConfig(index: Int, config: ComponentConfig) {
        viewModelScope.launch {
            val current = settings.value ?: AppSettingsEntity()
            val updated = when (index) {
                1 -> current.copy(component1ConfigJson = config.serialize())
                2 -> current.copy(component2ConfigJson = config.serialize())
                3 -> current.copy(component3ConfigJson = config.serialize())
                else -> current
            }
            dao.saveSettings(updated)
        }
    }

    fun cleanOldMemos() {
        viewModelScope.launch {
            val yesterday = LocalDate.now().minusDays(1).format(formatter)
            dao.deleteMemosOlderThan(yesterday)
        }
    }
}
