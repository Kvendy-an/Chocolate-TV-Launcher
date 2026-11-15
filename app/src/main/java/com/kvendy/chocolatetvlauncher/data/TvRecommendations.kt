package com.kvendy.chocolatetvlauncher.data

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.BaseColumns
import android.util.Log
import android.media.tv.TvContract
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** A piece of content (a movie, show, episode, etc.) another app published for the home screen. */
data class RecommendedContent(
    val id: Long,
    val title: String,
    val description: String?,
    val posterArtUri: Uri?,
    val intentUri: Uri?,
    val sourcePackageName: String?
)

private const val MAX_PROGRAMS = 100
private const val TAG = "TvRecommendations"

// Query the public contract columns we actually display, rather than restricted library
// projections and model getters that materialize all program metadata.
private val programProjection = arrayOf(
    BaseColumns._ID,
    TvContract.PreviewPrograms.COLUMN_TITLE,
    TvContract.PreviewPrograms.COLUMN_SHORT_DESCRIPTION,
    TvContract.PreviewPrograms.COLUMN_POSTER_ART_URI,
    TvContract.PreviewPrograms.COLUMN_INTENT_URI,
    TvContract.PreviewPrograms.COLUMN_PACKAGE_NAME
)

private suspend fun Cursor.readPrograms(fallbackPackage: String? = null, limit: Int = MAX_PROGRAMS): List<RecommendedContent> {
    val results = mutableListOf<RecommendedContent>()
    while (results.size < limit && moveToNext()) {
        currentCoroutineContext().ensureActive()
        val title = getString(1) ?: continue
        results.add(RecommendedContent(
            id = getLong(0),
            title = title,
            description = getString(2),
            posterArtUri = getString(3)?.let(Uri::parse),
            intentUri = getString(4)?.let(Uri::parse),
            sourcePackageName = getString(5) ?: fallbackPackage
        ))
    }
    return results
}

/** Content published to the system Watch Next table; unsupported/denied feeds remain optional. */
suspend fun loadWatchNextPrograms(context: Context): List<RecommendedContent> = withContext(Dispatchers.IO) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return@withContext emptyList()
    try {
        context.contentResolver.query(
            TvContract.WatchNextPrograms.CONTENT_URI,
            programProjection, null, null, null
        )?.use { it.readPrograms() } ?: emptyList()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Watch Next unavailable", e)
        emptyList()
    }
}

/** Read browsable preview channels with a bounded working set and cancellation between rows. */
suspend fun loadTopPicks(context: Context): List<RecommendedContent> = withContext(Dispatchers.IO) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return@withContext emptyList()
    val channels = try {
        val result = mutableListOf<Pair<Long, String?>>()
        context.contentResolver.query(
            TvContract.Channels.CONTENT_URI,
            arrayOf(BaseColumns._ID, TvContract.Channels.COLUMN_PACKAGE_NAME, TvContract.Channels.COLUMN_BROWSABLE),
            null, null, null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                currentCoroutineContext().ensureActive()
                if (cursor.getInt(2) == 1) result.add(cursor.getLong(0) to cursor.getString(1))
            }
        }
        result
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Recommendation channels unavailable", e)
        emptyList()
    }

    val results = mutableListOf<RecommendedContent>()
    for ((id, packageName) in channels) {
        currentCoroutineContext().ensureActive()
        if (results.size >= MAX_PROGRAMS) break
        try {
            context.contentResolver.query(
                TvContract.buildPreviewProgramsUriForChannel(id),
                programProjection, null, null, null
            )?.use { cursor ->
                results.addAll(cursor.readPrograms(packageName, MAX_PROGRAMS - results.size))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Recommendation channel $id unavailable", e)
        }
    }
    results
}
