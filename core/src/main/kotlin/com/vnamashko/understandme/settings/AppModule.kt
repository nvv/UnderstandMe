package com.vnamashko.understandme.settings

import android.app.Application
import android.content.Context
import android.content.res.Resources
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providesAppContext(application: Application): Context = application.applicationContext
}

@Module
@InstallIn(ViewModelComponent::class)
object AppViewModelModule {

    @Provides
    fun providesAppContext(application: Application): Resources = application.applicationContext.resources
}

