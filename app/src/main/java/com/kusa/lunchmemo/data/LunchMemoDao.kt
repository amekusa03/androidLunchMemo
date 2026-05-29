package com.kusa.lunchmemo.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LunchMemoDao {
    @Query("SELECT * FROM lunch_memos")
    fun getAllMemos(): Flow<List<LunchMemoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemo(memo: LunchMemoEntity)

    @Query("DELETE FROM lunch_memos WHERE date < :date")
    suspend fun deleteMemosOlderThan(date: String)
}
