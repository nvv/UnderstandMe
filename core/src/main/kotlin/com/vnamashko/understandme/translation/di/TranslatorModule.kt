package com.vnamashko.understandme.translation.di

import com.vnamashko.understandme.translation.Translator
import com.vnamashko.understandme.translation.TranslatorImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TranslatorModule {

    @Binds
    @Singleton
    abstract fun bindTranslator(translator: TranslatorImpl): Translator
}