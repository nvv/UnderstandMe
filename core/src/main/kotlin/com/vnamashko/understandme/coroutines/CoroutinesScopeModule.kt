package com.vnamashko.understandme.coroutines

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Coroutines scope tied to lifecycle of the app, should be used for operations that are not supposed to
 * be cancelled when a view is destroying, for instance, downloading a newspaper
 */
@Qualifier
annotation class AppCoroutinesScope

@Module
@InstallIn(SingletonComponent::class)
object CoroutinesScopeModule {

    @Provides
    @Singleton
    @AppCoroutinesScope
    fun providesAppCoroutinesScope(
        @DefaultDispatcher defaultDispatcher: CoroutineDispatcher
    ): CoroutineScope = CoroutineScope(SupervisorJob() + defaultDispatcher)
}