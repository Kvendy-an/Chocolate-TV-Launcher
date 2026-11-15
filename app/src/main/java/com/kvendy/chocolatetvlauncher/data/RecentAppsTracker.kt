package com.kvendy.chocolatetvlauncher.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class RecentAppsTracker(private val context: Context) {
    private val dataStore = context.dataStore

    companion object {
        val RECENT_APPS_LIST = stringPreferencesKey("recent_apps_list")
    }

    // dataStore is shared with PreferencesData (same "settings" file), so its underlying .data
    // Flow re-emits on ANY preference changing, not just this one — distinctUntilChanged() means
    // this Flow (and everything combining it, see Launcher.kt) only actually reacts when the
    // recent-apps list itself changed.
    val recentApps: Flow<List<String>> = dataStore.data.map { preferences ->
        preferences[RECENT_APPS_LIST]?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
    }.distinctUntilChanged()

    suspend fun addRecentApp(packageName: String, maxApps: Int = 20) {
        dataStore.edit { preferences ->
            val currentList = preferences[RECENT_APPS_LIST]?.split(",")?.filter { it.isNotEmpty() }?.toMutableList() ?: mutableListOf()

            currentList.remove(packageName)

            currentList.add(0, packageName)

            while (currentList.size > maxApps) {
                currentList.removeAt(currentList.lastIndex)
            }

            preferences[RECENT_APPS_LIST] = currentList.joinToString(",")
        }
    }

    suspend fun removeRecentApp(packageName: String) {
        dataStore.edit { preferences ->
            val current = preferences[RECENT_APPS_LIST].orEmpty().split(',')
            preferences[RECENT_APPS_LIST] = current.filter { it.isNotEmpty() && it != packageName }.joinToString(",")
        }
    }

    suspend fun clearHistory() {
        dataStore.edit { preferences ->
            preferences[RECENT_APPS_LIST] = ""
        }
    }
}