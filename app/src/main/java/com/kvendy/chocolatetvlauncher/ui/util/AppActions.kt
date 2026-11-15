package com.kvendy.chocolatetvlauncher.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/** Opens the system "App info" screen for [packageName]. */
fun Context.openAppInfo(packageName: String) {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
    )
}

/** Launches the system uninstall confirmation dialog for [packageName]. */
fun Context.requestUninstall(packageName: String) {
    val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.fromParts("package", packageName, null))
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}

/** Launches [packageName]'s default activity, if it has one. */
fun Context.launchApp(packageName: String) {
    packageManager.getLaunchIntentForPackage(packageName)?.let { startActivity(it) }
}
