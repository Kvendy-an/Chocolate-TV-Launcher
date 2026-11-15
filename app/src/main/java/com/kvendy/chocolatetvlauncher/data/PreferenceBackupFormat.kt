package com.kvendy.chocolatetvlauncher.data

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.ACCENT_COLOR
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.APP_LABEL_SCALE
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.APP_ORDER
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.CONTENT_CARD_SCALE
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.EXCLUDED_CONTENT_APPS
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.FOLDER_GRID_SIZE
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.GLOW_POSITION
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.GLOW_RADIUS
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.GRADIENT_BACKGROUND
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.GRID_SIZE
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.GRID_SQUARE_ICONS
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.HIDDEN_APPS
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.ICON_CORNER_RADIUS
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.IDLE_DIM_ENABLED
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.IDLE_DIM_STRENGTH
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.IDLE_DIM_TIMEOUT_SECONDS
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.MAX_RECENT_APPS
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.NAVIGATION_SOUND_ENABLED
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.RECENT_APPS_CORNER_RADIUS
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.RECENT_APPS_SCALE
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.RECENT_APPS_SQUARE_ICONS
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.SECTION_ORDER
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.SECTION_TITLE_SCALE
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.SHOW_APP_LABELS
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.SHOW_BLUETOOTH_SHORTCUT
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.SHOW_CLOCK
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.SHOW_FEATURED_TILE
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.SHOW_GLOW_EFFECT
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.SHOW_HIDDEN_APPS_IN_SEARCH
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.SHOW_INPUT_SHORTCUT
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.SHOW_NOTIFICATIONS
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.SHOW_NOTIFICATIONS_ON_HOME
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.SHOW_PAGE_TAB_LABELS
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.SHOW_RECENT_APPS
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.SHOW_RECOMMENDATIONS_ON_HOME
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.SHOW_SEARCH_SHORTCUT
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.SHOW_SECTION_ICONS
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.SHOW_SECTION_TITLES
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.SHOW_SETTINGS_SHORTCUT
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.SHOW_TOP_BAR
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.SHOW_WIFI_SHORTCUT
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.THEME
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.UI_SCALE
import com.kvendy.chocolatetvlauncher.data.PreferencesData.Companion.WALLPAPER_PATH
import com.kvendy.chocolatetvlauncher.ui.components.GlowPosition
import com.kvendy.chocolatetvlauncher.ui.components.id
import com.kvendy.chocolatetvlauncher.ui.theme.AccentColor
import com.kvendy.chocolatetvlauncher.ui.theme.GradientBackground
import com.kvendy.chocolatetvlauncher.ui.theme.id
import com.kvendy.chocolatetvlauncher.remote.decodeRemoteMapping
import com.kvendy.chocolatetvlauncher.remote.overrideHomeKey
import com.kvendy.chocolatetvlauncher.remote.remoteMappingsKey
import org.json.JSONArray
import org.json.JSONObject

private class BackupField<T : Any>(
    val key: Preferences.Key<T>,
    private val default: T,
    private val decode: (Any) -> T
) {
    fun write(settings: JSONObject, source: Preferences) {
        val value = source[key] ?: default
        settings.put(key.name, if (value is Set<*>) JSONArray(value.sortedBy { it.toString() }) else value)
    }

    fun read(settings: JSONObject, target: MutablePreferences) {
        if (settings.has(key.name)) target[key] = decode(settings.get(key.name))
    }

    fun copy(source: Preferences, target: MutablePreferences) {
        source[key]?.let { target[key] = it }
    }
}

private fun boolean(key: Preferences.Key<Boolean>, default: Boolean) =
    BackupField(key, default) { requireNotNull(it as? Boolean) }

private fun integer(key: Preferences.Key<Int>, default: Int, allowed: IntRange) =
    BackupField(key, default) {
        val number = requireNotNull(it as? Number).toDouble()
        number.toInt().also { result -> require(number == result.toDouble() && result in allowed) }
    }

private fun decimal(key: Preferences.Key<Float>, default: Float, allowed: ClosedFloatingPointRange<Float>) =
    BackupField(key, default) {
        requireNotNull(it as? Number).toFloat().also { value -> require(value.isFinite() && value in allowed) }
    }

private fun choice(key: Preferences.Key<String>, default: String, allowed: List<String>) =
    BackupField(key, default) { requireNotNull(it as? String).also { value -> require(value in allowed) } }

private val packagePattern = Regex("[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)*")
private fun isPackageName(name: String) = name.length <= 255 && packagePattern.matches(name)

private fun packages(key: Preferences.Key<Set<String>>) =
    BackupField(key, emptySet()) {
        val array = requireNotNull(it as? JSONArray)
        (0 until array.length()).mapTo(mutableSetOf()) { index ->
            requireNotNull(array.get(index) as? String).also { name -> require(isPackageName(name)) }
        }
    }

private fun validatedGridOrder(raw: String): String {
    val seenKeys = mutableSetOf<String>()
    val seenPackages = mutableSetOf<String>()
    val items = decodeGridOrder(raw)
    require(items.size == raw.split(',').count { it.isNotBlank() })
    for (item in items) {
        require(seenKeys.add(item.key))
        when (item) {
            is HomeGridItem.AppItem -> require(isPackageName(item.packageName) && seenPackages.add(item.packageName))
            is HomeGridItem.FolderItem -> {
                require(item.id.isNotBlank())
                item.packageNames.forEach { require(isPackageName(it) && seenPackages.add(it)) }
            }
        }
    }
    return encodeGridOrder(items)
}

internal enum class BackupCategory {
    Preferences, GridOrder, HiddenApps, TileColors, RemoteActions
}

internal object PreferenceBackupFormat {
    private const val format = "chocolate-tv-preferences"
    private const val version = 1

    private val fields = listOf(
        decimal(ICON_CORNER_RADIUS, LauncherDefaults.ICON_CORNER_RADIUS, 0f..100f),
        boolean(SHOW_APP_LABELS, true),
        boolean(overrideHomeKey, false),
        BackupField(remoteMappingsKey, emptySet()) {
            val array = requireNotNull(it as? JSONArray)
            val keys = mutableSetOf<Int>()
            (0 until array.length()).mapTo(mutableSetOf()) { index ->
                val entry = requireNotNull(array.get(index) as? String)
                val mapping = requireNotNull(decodeRemoteMapping(entry))
                require(keys.add(mapping.keyCode) && mapping.packageName?.let(::isPackageName) != false)
                entry
            }
        },
        decimal(APP_LABEL_SCALE, 0.85f, 0.5f..1.2f),
        integer(GRID_SIZE, 6, 2..12),
        integer(FOLDER_GRID_SIZE, 4, 2..8),
        boolean(GRID_SQUARE_ICONS, false),
        boolean(SHOW_HIDDEN_APPS_IN_SEARCH, false),
        boolean(SHOW_FEATURED_TILE, false),
        boolean(SHOW_RECOMMENDATIONS_ON_HOME, true),
        boolean(SHOW_NOTIFICATIONS, false),
        boolean(SHOW_NOTIFICATIONS_ON_HOME, true),
        decimal(CONTENT_CARD_SCALE, 0.7f, 0.2f..2f),
        packages(EXCLUDED_CONTENT_APPS),
        boolean(SHOW_SECTION_TITLES, LauncherDefaults.SHOW_SECTION_TITLES),
        boolean(SHOW_SECTION_ICONS, LauncherDefaults.SHOW_SECTION_ICONS),
        decimal(SECTION_TITLE_SCALE, LauncherDefaults.SECTION_TITLE_SCALE, 0.5f..1.5f),
        BackupField(SECTION_ORDER, HomeSection.DEFAULT_ORDER.joinToString(",")) {
            val sections = requireNotNull(it as? String).split(',').distinct()
            require(sections.all { section -> section in HomeSection.DEFAULT_ORDER })
            (sections + HomeSection.DEFAULT_ORDER.filterNot { section -> section in sections }).joinToString(",")
        },
        decimal(UI_SCALE, 1f, 0.7f..1.3f),
        choice(THEME, "system", listOf("system", "light", "dark")),
        choice(ACCENT_COLOR, "default", AccentColor.entries.map { it.id }),
        choice(GRADIENT_BACKGROUND, "none", GradientBackground.entries.map { it.id }),
        boolean(SHOW_TOP_BAR, true),
        boolean(SHOW_PAGE_TAB_LABELS, LauncherDefaults.SHOW_PAGE_TAB_LABELS),
        boolean(SHOW_CLOCK, true),
        boolean(SHOW_WIFI_SHORTCUT, true),
        boolean(SHOW_BLUETOOTH_SHORTCUT, true),
        boolean(SHOW_INPUT_SHORTCUT, true),
        boolean(SHOW_SEARCH_SHORTCUT, true),
        boolean(SHOW_SETTINGS_SHORTCUT, true),
        boolean(SHOW_RECENT_APPS, true),
        decimal(RECENT_APPS_SCALE, LauncherDefaults.RECENT_APPS_SCALE, 0.5f..2f),
        boolean(RECENT_APPS_SQUARE_ICONS, LauncherDefaults.RECENT_APPS_SQUARE_ICONS),
        decimal(RECENT_APPS_CORNER_RADIUS, LauncherDefaults.RECENT_APPS_CORNER_RADIUS, 0f..100f),
        integer(MAX_RECENT_APPS, LauncherDefaults.MAX_RECENT_APPS, 5..20),
        packages(HIDDEN_APPS),
        BackupField(APP_ORDER, "") { validatedGridOrder(requireNotNull(it as? String)) },
        boolean(SHOW_GLOW_EFFECT, true),
        decimal(GLOW_RADIUS, LauncherDefaults.GLOW_RADIUS, 10f..100f),
        choice(GLOW_POSITION, "topright", GlowPosition.entries.map { it.id }),
        boolean(NAVIGATION_SOUND_ENABLED, true),
        boolean(IDLE_DIM_ENABLED, true),
        integer(IDLE_DIM_TIMEOUT_SECONDS, 30, 30..300),
        integer(IDLE_DIM_STRENGTH, LauncherDefaults.IDLE_DIM_STRENGTH, 5..100)
    )

    private fun category(key: Preferences.Key<*>): BackupCategory = when (key) {
        APP_ORDER -> BackupCategory.GridOrder
        HIDDEN_APPS -> BackupCategory.HiddenApps
        TILE_BACKGROUNDS -> BackupCategory.TileColors
        remoteMappingsKey, overrideHomeKey -> BackupCategory.RemoteActions
        else -> BackupCategory.Preferences
    }

    fun categories(source: Preferences): Set<BackupCategory> =
        source.asMap().keys.mapTo(mutableSetOf(), ::category)

    fun encode(source: Preferences, selected: Set<BackupCategory>): String {
        val settings = JSONObject()
        fields.filter { category(it.key) in selected }.forEach { it.write(settings, source) }
        val backgrounds = JSONObject()
        decodeTileBackgrounds(source[TILE_BACKGROUNDS].orEmpty()).toSortedMap().forEach { (name, color) ->
            backgrounds.put(name, color.id)
        }
        return JSONObject().put("format", format).put("version", version).put("settings", settings).apply {
            if (BackupCategory.TileColors in selected) put("tileBackgrounds", backgrounds)
        }.toString(2)
    }

    fun decode(text: String): Preferences {
        val json = JSONObject(text)
        require(json.getString("format") == format && json.getInt("version") == version)
        val settings = json.getJSONObject("settings")
        val target = mutablePreferencesOf()
        fields.forEach { it.read(settings, target) }
        if (json.has("tileBackgrounds")) {
            val backgrounds = json.getJSONObject("tileBackgrounds")
            target[TILE_BACKGROUNDS] = encodeTileBackgrounds(backgrounds.keys().asSequence().associateWith { name ->
                require(isPackageName(name))
                TileBackground.fromId(backgrounds.getString(name)).also { require(it != TileBackground.None) }
            })
        }
        require(target.asMap().isNotEmpty())
        return target.toPreferences()
    }

    fun apply(
        source: Preferences,
        target: MutablePreferences,
        selected: Set<BackupCategory> = BackupCategory.entries.toSet()
    ) {
        fields.filter { category(it.key) in selected }.forEach { it.copy(source, target) }
        if (BackupCategory.TileColors in selected) source[TILE_BACKGROUNDS]?.let { target[TILE_BACKGROUNDS] = it }
        if (BackupCategory.Preferences in selected && source[GRADIENT_BACKGROUND]?.let { it != "none" } == true) {
            target.remove(WALLPAPER_PATH)
        }
    }
}

internal class LauncherPreferenceMigration(private val context: Context) : DataMigration<Preferences> {
    override suspend fun shouldMigrate(currentData: Preferences): Boolean = !currentData.contains(TILE_BACKGROUNDS)

    override suspend fun migrate(currentData: Preferences): Preferences {
        val migrated = currentData.toMutablePreferences()
        if (currentData.asMap().isEmpty()) {
            val bundled = context.assets.open("default-preferences.json").bufferedReader().use { it.readText() }
            PreferenceBackupFormat.apply(PreferenceBackupFormat.decode(bundled), migrated)
        }
        if (!migrated.contains(TILE_BACKGROUNDS)) {
            migrated[TILE_BACKGROUNDS] = encodeTileBackgrounds(CustomImageCache.legacyTileBackgrounds(context))
        }
        return migrated.toPreferences()
    }

    override suspend fun cleanUp() = Unit
}
