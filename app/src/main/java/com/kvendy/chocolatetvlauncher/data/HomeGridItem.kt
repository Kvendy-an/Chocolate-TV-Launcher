package com.kvendy.chocolatetvlauncher.data

import android.util.Base64
import java.util.UUID

/** One slot in the home grid's top-level order: either a single app or a folder of apps. */
sealed class HomeGridItem {
    /** Stable identity used for focus routing, animation keys, and move/merge targeting. */
    abstract val key: String

    data class AppItem(val packageName: String) : HomeGridItem() {
        override val key get() = "app:$packageName"
    }

    data class FolderItem(
        val id: String,
        // Null/blank means "unnamed" — distinct from ever having had a name.
        val name: String?,
        val packageNames: List<String>
    ) : HomeGridItem() {
        override val key get() = "folder:$id"
    }
}

fun newFolderId(): String = UUID.randomUUID().toString()

private const val FOLDER_PREFIX = "F:"
private const val ENTRY_DELIMITER = ","
private const val FOLDER_FIELD_DELIMITER = ":"
private const val FOLDER_APPS_DELIMITER = "|"

/**
 * Packs a grid order into one DataStore-friendly string. Deliberately avoids a JSON dependency:
 * app entries are just their bare package name (matching the legacy flat appOrder format, so old
 * stored values decode unchanged), and folder entries carry an id, a Base64-encoded name (so an
 * arbitrary rename can't collide with the delimiters below), and their member package names.
 * Package names can't contain ",", ":", or "|", so those are safe structural delimiters.
 */
fun encodeGridOrder(items: List<HomeGridItem>): String =
    items.joinToString(ENTRY_DELIMITER) { item ->
        when (item) {
            is HomeGridItem.AppItem -> item.packageName
            is HomeGridItem.FolderItem -> {
                val encodedName = Base64.encodeToString((item.name ?: "").toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                "$FOLDER_PREFIX${item.id}$FOLDER_FIELD_DELIMITER$encodedName$FOLDER_FIELD_DELIMITER${item.packageNames.joinToString(FOLDER_APPS_DELIMITER)}"
            }
        }
    }

fun decodeGridOrder(raw: String): List<HomeGridItem> =
    raw.split(ENTRY_DELIMITER)
        .filter { it.isNotBlank() }
        .mapNotNull { entry ->
            if (entry.startsWith(FOLDER_PREFIX)) {
                val fields = entry.removePrefix(FOLDER_PREFIX).split(FOLDER_FIELD_DELIMITER, limit = 3)
                if (fields.size != 3) return@mapNotNull null
                val (id, encodedName, appsField) = fields
                val name = try {
                    String(Base64.decode(encodedName, Base64.NO_WRAP), Charsets.UTF_8).ifBlank { null }
                } catch (e: IllegalArgumentException) {
                    null
                }
                val packageNames = appsField.split(FOLDER_APPS_DELIMITER).filter { it.isNotBlank() }
                if (packageNames.isEmpty()) null else HomeGridItem.FolderItem(id, name, packageNames)
            } else {
                HomeGridItem.AppItem(entry)
            }
        }

/** Preserve unavailable apps/folders and append each genuinely new package exactly once. */
internal fun appendDiscoveredApps(
    current: List<HomeGridItem>,
    packageNames: List<String>
): List<HomeGridItem> {
    val known = current.flatMap { item ->
        when (item) {
            is HomeGridItem.AppItem -> listOf(item.packageName)
            is HomeGridItem.FolderItem -> item.packageNames
        }
    }.toMutableSet()
    val additions = packageNames.filter { it.isNotBlank() && known.add(it) }
    return if (additions.isEmpty()) current else current + additions.map { HomeGridItem.AppItem(it) }
}
