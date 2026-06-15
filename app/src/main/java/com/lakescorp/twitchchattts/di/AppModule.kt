package com.lakescorp.twitchchattts.di

import com.lakescorp.twitchchattts.data.auth.AuthManager
import com.lakescorp.twitchchattts.data.auth.AuthManagerImpl
import com.lakescorp.twitchchattts.domain.tts.TtsManager
import com.lakescorp.twitchchattts.domain.tts.TtsManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindTtsManager(impl: TtsManagerImpl): TtsManager

    @Binds
    @Singleton
    abstract fun bindAuthManager(impl: AuthManagerImpl): AuthManager
}
