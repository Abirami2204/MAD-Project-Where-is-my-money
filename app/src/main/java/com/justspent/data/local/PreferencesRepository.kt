package com.justspent.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "justspent_prefs")

class PreferencesRepository(private val context: Context) {

    companion object {
        val LAST_SMS_SYNC_TIMESTAMP = longPreferencesKey("last_sms_sync_timestamp")
    }

    val lastSmsSyncTimestamp: Flow<Long> = context.dataStore.data
        .map { preferences ->
            // Default to 0 (all time) if never synced
            preferences[LAST_SMS_SYNC_TIMESTAMP] ?: 0L
        }

    suspend fun updateLastSmsSyncTimestamp(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[LAST_SMS_SYNC_TIMESTAMP] = timestamp
        }
    }
}
