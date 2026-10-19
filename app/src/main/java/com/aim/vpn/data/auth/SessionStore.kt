package com.aim.vpn.data.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "secure_session")

@Singleton
class SessionStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val KEY_SESSION_ID = stringPreferencesKey("session_id")
    // Never store plaintext access key

    suspend fun saveSession(keyId: String) {
        context.sessionDataStore.edit { prefs ->
            prefs[KEY_SESSION_ID] = keyId
        }
    }

    suspend fun hasValidSession(): Boolean {
        val id = context.sessionDataStore.data.map { it[KEY_SESSION_ID] }.first()
        return !id.isNullOrBlank()
    }

    suspend fun clear() {
        context.sessionDataStore.edit { it.clear() }
    }
}
