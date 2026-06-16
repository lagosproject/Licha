package com.lakescorp.twitchchattts.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Qualifier for the application-lifetime [CoroutineScope] used for fire-and-forget
 * background work (e.g. warming up KeyStore-backed encrypted storage off the main thread).
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FUNCTION
)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object CoroutinesModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        // SupervisorJob so one failed child never cancels the whole app scope.
        // Dispatchers.IO because every consumer of this scope does disk/crypto I/O.
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
