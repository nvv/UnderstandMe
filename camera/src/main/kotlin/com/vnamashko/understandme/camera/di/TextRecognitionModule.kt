package com.vnamashko.understandme.camera.di

import com.vnamashko.understandme.camera.recognition.TextBlockRecognizer
import com.vnamashko.understandme.camera.recognition.TextBlockRecognizerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
abstract class TextRecognitionModule {

    @Binds
    @ViewModelScoped
    abstract fun bindTextBlockRecognizer(recognizer: TextBlockRecognizerImpl): TextBlockRecognizer
}
