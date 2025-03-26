package com.vnamashko.understandme.tts.di

import com.vnamashko.understandme.tts.Tts
import com.vnamashko.understandme.tts.TtsImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TtsModule {

    @Binds
    @Singleton
    abstract fun bindTts(tts: TtsImpl): Tts
}