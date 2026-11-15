package com.kvendy.chocolatetvlauncher.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

internal data class PreferenceBackupFile(val name: String, val uri: Uri)

internal class PreferenceBackups(context: Context) {
    private val context = context.applicationContext
    private val resolver = this.context.contentResolver

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun exportToDownloads(selected: Set<BackupCategory>): PreferenceBackupFile = withContext(Dispatchers.IO) {
        writeDownload(selected)
    }

    suspend fun exportTo(uri: Uri, selected: Set<BackupCategory>) = withContext(Dispatchers.IO) {
        write(uri, PreferenceBackupFormat.encode(context.dataStore.data.first(), selected))
    }

    suspend fun read(uri: Uri): Preferences = withContext(Dispatchers.IO) {
        val input = resolver.openInputStream(uri) ?: throw IOException("Unable to open backup")
        val text = input.use {
            val bytes = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = it.read(buffer)
                if (count < 0) break
                require(bytes.size() + count <= maxBackupBytes)
                bytes.write(buffer, 0, count)
            }
            bytes.toString(Charsets.UTF_8.name())
        }
        PreferenceBackupFormat.decode(text)
    }

    suspend fun restore(restored: Preferences, selected: Set<BackupCategory>) {
        context.dataStore.edit { PreferenceBackupFormat.apply(restored, it, selected) }
    }

    suspend fun downloads(): List<PreferenceBackupFile> = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) listDownloads() else emptyList()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun writeDownload(selected: Set<BackupCategory>): PreferenceBackupFile {
        val text = PreferenceBackupFormat.encode(context.dataStore.data.first(), selected)
        val name = newFileName()
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Unable to create backup")
        try {
            write(uri, text)
            val ready = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
            if (resolver.update(uri, ready, null, null) != 1) throw IOException("Unable to publish backup")
        } catch (error: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            throw error
        }
        return PreferenceBackupFile(name, uri)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun listDownloads(): List<PreferenceBackupFile> = buildList {
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val id = MediaStore.Downloads._ID
        val name = MediaStore.Downloads.DISPLAY_NAME
        resolver.query(
            collection, arrayOf(id, name),
            "$name LIKE ? AND ${MediaStore.Downloads.RELATIVE_PATH} = ? AND ${MediaStore.Downloads.IS_PENDING} = 0",
            arrayOf("$filePrefix%.json", "${Environment.DIRECTORY_DOWNLOADS}/"), "$name DESC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                add(PreferenceBackupFile(cursor.getString(1), ContentUris.withAppendedId(collection, cursor.getLong(0))))
            }
        }
    }

    private fun write(uri: Uri, text: String) {
        val output = resolver.openOutputStream(uri, "wt") ?: throw IOException("Unable to write backup")
        output.bufferedWriter(Charsets.UTF_8).use { it.write(text) }
    }

    companion object {
        const val mimeType = "application/json"
        private const val filePrefix = "ChocolateTVLauncher-"
        private const val maxBackupBytes = 512 * 1024

        fun newFileName(): String {
            val stamp = SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.ROOT).format(Date())
            return "$filePrefix$stamp.json"
        }
    }
}
