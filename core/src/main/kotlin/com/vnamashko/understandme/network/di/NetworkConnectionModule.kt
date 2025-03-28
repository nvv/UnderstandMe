package com.vnamashko.understandme.network.di

import com.vnamashko.understandme.network.NetworkConnectionManager
import com.vnamashko.understandme.network.NetworkConnectionManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkConnectionModule {

    @Binds
    @Singleton
    abstract fun bindNetworkConnection(tts: NetworkConnectionManagerImpl): NetworkConnectionManager
}