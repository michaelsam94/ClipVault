package com.michael.clipvault.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore by preferencesDataStore(name = "clip_vault_preferences")

class PreferencesManager(private val context: Context) {

    private val KEY_MONITOR_CLIPBOARD = booleanPreferencesKey("monitor_clipboard")
    private val KEY_SHOW_NOTIFICATIONS = booleanPreferencesKey("show_notifications")
    private val KEY_THEME_DARK = booleanPreferencesKey("theme_dark") // null = automatic, true = dark, false = light

    val monitorClipboardFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[KEY_MONITOR_CLIPBOARD] ?: true
        }

    val showNotificationsFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[KEY_SHOW_NOTIFICATIONS] ?: true
        }

    suspend fun setMonitorClipboard(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_MONITOR_CLIPBOARD] = enabled
        }
    }

    suspend fun setShowNotifications(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SHOW_NOTIFICATIONS] = enabled
        }
    }
}
