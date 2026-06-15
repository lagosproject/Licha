package com.lakescorp.twitchchattts.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.SharedPreferencesMigration
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    private const val LEGACY_PREFS_NAME = "TwitchChatTTSSettings"
    private const val DATASTORE_NAME = "TwitchChatTTSDataStore"

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            migrations = listOf(
                // Automatically migrates all keys from the legacy SharedPreferences
                // to DataStore on first access. One-shot migration.
                SharedPreferencesMigration(context, LEGACY_PREFS_NAME)
            ),
            produceFile = { context.preferencesDataStoreFile(DATASTORE_NAME) }
        )
    }
}
