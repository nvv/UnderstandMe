package com.vnamashko.understandme.camera.di

import com.vnamashko.understandme.camera.image.ImageLoader
import com.vnamashko.understandme.camera.image.ImageLoaderImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ImageModule {

    @Binds
    @Singleton
    abstract fun bindImageLoader(loader: ImageLoaderImpl): ImageLoader
}
