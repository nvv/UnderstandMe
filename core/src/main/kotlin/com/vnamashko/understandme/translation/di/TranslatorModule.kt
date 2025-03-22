package com.vnamashko.understandme.translation.di

import com.vnamashko.understandme.translation.Translator
import com.vnamashko.understandme.translation.TranslatorImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class TranslatorModule {

    @Binds
    abstract fun bindTranslator(translator: TranslatorImpl): Translator
}