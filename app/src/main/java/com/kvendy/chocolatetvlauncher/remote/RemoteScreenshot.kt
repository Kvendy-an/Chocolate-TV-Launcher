package com.kvendy.chocolatetvlauncher.remote

import android.accessibilityservice.AccessibilityService
import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.Display
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.kvendy.chocolatetvlauncher.R
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

@RequiresApi(Build.VERSION_CODES.R)
internal fun AccessibilityService.saveRemoteScreenshot() {
    takeScreenshot(
        Display.DEFAULT_DISPLAY,
        Dispatchers.IO.asExecutor(),
        object : AccessibilityService.TakeScreenshotCallback {
            override fun onSuccess(result: AccessibilityService.ScreenshotResult) {
                val saved = try {
                    result.hardwareBuffer.use { buffer ->
                        val captured = requireNotNull(Bitmap.wrapHardwareBuffer(buffer, result.colorSpace))
                        try {
                            saveScreenshotImage(captured)
                        } finally {
                            captured.recycle()
                        }
                    }
                    true
                } catch (_: IOException) {
                    false
                } catch (_: RuntimeException) {
                    false
                }
                showResult(if (saved) R.string.remote_screenshot_saved else R.string.remote_action_unavailable)
            }

            override fun onFailure(errorCode: Int) {
                showResult(R.string.remote_action_unavailable)
            }

            private fun showResult(message: Int) {
                mainExecutor.execute { Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show() }
            }
        }
    )
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun AccessibilityService.saveScreenshotImage(bitmap: Bitmap) {
    val resolver = contentResolver
    val image = resolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "Chocolate-${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Screenshots")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    ) ?: throw IOException("Could not create screenshot")
    var published = false
    try {
        val written = resolver.openOutputStream(image)?.use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        if (written != true) throw IOException("Could not write screenshot")
        published = resolver.update(
            image,
            ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
            null,
            null
        ) > 0
        if (!published) throw IOException("Could not publish screenshot")
    } finally {
        if (!published) resolver.delete(image, null, null)
    }
}
