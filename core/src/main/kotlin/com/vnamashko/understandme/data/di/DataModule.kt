package com.vnamashko.understandme.data.di

import android.content.Context
import androidx.room.Room
import com.vnamashko.understandme.data.AppDatabase
import com.vnamashko.understandme.data.dao.RecentLanguageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "app_database").build()
    }

    @Provides
    fun provideRecentLanguageDao(database: AppDatabase): RecentLanguageDao {
        return database.recentLanguageDao()
    }
}
