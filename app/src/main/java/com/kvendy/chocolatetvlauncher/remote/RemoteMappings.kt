package com.kvendy.chocolatetvlauncher.remote

import android.content.Context
import android.os.Build
import androidx.annotation.StringRes
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.kvendy.chocolatetvlauncher.R
import com.kvendy.chocolatetvlauncher.data.dataStore
import java.io.IOException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal enum class RemoteAction(val id: String, @StringRes val title: Int) {
    LaunchApp("app", R.string.remote_launch_app),
    OpenLauncher("launcher", R.string.remote_open_launcher),
    OpenPreferences("preferences", R.string.remote_open_preferences),
    Screenshot("screenshot", R.string.remote_take_screenshot),
    Back("back", R.string.back),
    Settings("settings", R.string.remote_system_settings),
    ToggleMute("mute", R.string.remote_toggle_mute),
    Ignore("ignore", R.string.remote_ignore);

    val isSupported: Boolean
        get() = this != Screenshot || Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
}

internal enum class RemoteScope(val id: String, @StringRes val title: Int) {
    LauncherOnly("launcher", R.string.remote_launcher_only),
    Everywhere("everywhere", R.string.remote_everywhere);
}

internal data class RemoteMapping(
    val keyCode: Int,
    val action: RemoteAction,
    val scope: RemoteScope,
    val packageName: String? = null
)

internal val remoteMappingsKey = stringSetPreferencesKey("remote_button_mappings")
internal val overrideHomeKey = booleanPreferencesKey("remote_override_home")

internal class RemoteMappings(context: Context) {
    private val store = context.applicationContext.dataStore
    private val preferences = store.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }

    val overrideHome = preferences.map { it[overrideHomeKey] ?: false }.distinctUntilChanged()

    val mappings = preferences
        .map { it[remoteMappingsKey].orEmpty() }
        .distinctUntilChanged()
        .map { saved -> saved.mapNotNull(::decodeRemoteMapping).associateBy { it.keyCode } }

    suspend fun setOverrideHome(enabled: Boolean) = store.edit { it[overrideHomeKey] = enabled }

    suspend fun save(mapping: RemoteMapping) = store.edit { prefs ->
        val others = prefs[remoteMappingsKey].orEmpty().filterNot { storedKey(it) == mapping.keyCode }
        val entry = "${mapping.keyCode}|${mapping.action.id}|${mapping.scope.id}|${mapping.packageName.orEmpty()}"
        prefs[remoteMappingsKey] = others.toSet() + entry
    }

    suspend fun remove(keyCode: Int) = store.edit { prefs ->
        prefs[remoteMappingsKey] = prefs[remoteMappingsKey].orEmpty().filterNot { storedKey(it) == keyCode }.toSet()
    }

    private fun storedKey(entry: String) = entry.substringBefore('|').toIntOrNull()

}

internal fun decodeRemoteMapping(entry: String): RemoteMapping? {
    val parts = entry.split('|', limit = 4)
    if (parts.size != 4) return null
    val keyCode = parts[0].toIntOrNull()?.takeIf { it > 0 } ?: return null
    val action = RemoteAction.entries.firstOrNull { it.id == parts[1] } ?: return null
    val scope = RemoteScope.entries.firstOrNull { it.id == parts[2] } ?: return null
    val packageName = parts[3].takeIf { it.isNotEmpty() }
    if (action == RemoteAction.LaunchApp && packageName == null) return null
    return RemoteMapping(keyCode, action, scope, packageName)
}
