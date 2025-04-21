package com.vnamashko.understandme.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vnamashko.understandme.data.model.RecentLanguage
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentLanguageDao {
    @Query("SELECT * FROM RecentLanguage ORDER BY rowid DESC LIMIT 10")
    fun getRecentLanguages(): Flow<List<RecentLanguage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLanguage(language: RecentLanguage)

    @Query("DELETE FROM RecentLanguage WHERE code NOT IN (SELECT code FROM RecentLanguage ORDER BY rowid DESC LIMIT 10)")
    suspend fun trimLanguages()
}