package com.vnamashko.understandme.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.vnamashko.understandme.data.dao.RecentLanguageDao
import com.vnamashko.understandme.data.model.RecentLanguage

@Database(entities = [RecentLanguage::class], version = 1)
abstract class AppDatabase: RoomDatabase() {
    abstract fun recentLanguageDao(): RecentLanguageDao
}