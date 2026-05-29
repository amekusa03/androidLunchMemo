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

    fun saveMemo(date: LocalDate, memo: String) {
        viewModelScope.launch {
            dao.insertMemo(LunchMemoEntity(date.format(formatter), memo))
        }
    }

    fun cleanOldMemos() {
        viewModelScope.launch {
            val yesterday = LocalDate.now().minusDays(1).format(formatter)
            dao.deleteMemosOlderThan(yesterday)
        }
    }
}
