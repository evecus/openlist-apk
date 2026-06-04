package com.openlist.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "openlist_prefs")

object PreferenceKeys {
    val ACTIVE_SERVER_ID = longPreferencesKey("active_server_id")
    val ACTIVE_SERVER_URL = stringPreferencesKey("active_server_url")
    val ACTIVE_SERVER_TOKEN = stringPreferencesKey("active_server_token")
    val ACTIVE_SERVER_NAME = stringPreferencesKey("active_server_name")
    val LAST_PATH = stringPreferencesKey("last_path")
    val SORT_BY = stringPreferencesKey("sort_by")
    val SORT_DESC = booleanPreferencesKey("sort_desc")
    val VIEW_MODE = stringPreferencesKey("view_mode") // "list" or "grid"
    val AUTO_PLAY_VIDEO = booleanPreferencesKey("auto_play_video")
    val AUTO_PLAY_AUDIO = booleanPreferencesKey("auto_play_audio")
    val DOWNLOAD_PATH = stringPreferencesKey("download_path")
}

class AppPreferences(private val context: Context) {

    val activeServerId: Flow<Long> = context.dataStore.data.map { it[PreferenceKeys.ACTIVE_SERVER_ID] ?: -1L }
    val activeServerUrl: Flow<String> = context.dataStore.data.map { it[PreferenceKeys.ACTIVE_SERVER_URL] ?: "" }
    val activeServerToken: Flow<String> = context.dataStore.data.map { it[PreferenceKeys.ACTIVE_SERVER_TOKEN] ?: "" }
    val activeServerName: Flow<String> = context.dataStore.data.map { it[PreferenceKeys.ACTIVE_SERVER_NAME] ?: "" }
    val viewMode: Flow<String> = context.dataStore.data.map { it[PreferenceKeys.VIEW_MODE] ?: "list" }
    val sortBy: Flow<String> = context.dataStore.data.map { it[PreferenceKeys.SORT_BY] ?: "name" }
    val sortDesc: Flow<Boolean> = context.dataStore.data.map { it[PreferenceKeys.SORT_DESC] ?: false }

    suspend fun setActiveServer(id: Long, url: String, token: String, name: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.ACTIVE_SERVER_ID] = id
            prefs[PreferenceKeys.ACTIVE_SERVER_URL] = url
            prefs[PreferenceKeys.ACTIVE_SERVER_TOKEN] = token
            prefs[PreferenceKeys.ACTIVE_SERVER_NAME] = name
        }
    }

    suspend fun clearActiveServer() {
        context.dataStore.edit { prefs ->
            prefs.remove(PreferenceKeys.ACTIVE_SERVER_ID)
            prefs.remove(PreferenceKeys.ACTIVE_SERVER_URL)
            prefs.remove(PreferenceKeys.ACTIVE_SERVER_TOKEN)
            prefs.remove(PreferenceKeys.ACTIVE_SERVER_NAME)
        }
    }

    suspend fun setViewMode(mode: String) {
        context.dataStore.edit { it[PreferenceKeys.VIEW_MODE] = mode }
    }

    suspend fun setSortBy(sortBy: String, desc: Boolean) {
        context.dataStore.edit {
            it[PreferenceKeys.SORT_BY] = sortBy
            it[PreferenceKeys.SORT_DESC] = desc
        }
    }

    suspend fun updateToken(token: String) {
        context.dataStore.edit { it[PreferenceKeys.ACTIVE_SERVER_TOKEN] = token }
    }
}
