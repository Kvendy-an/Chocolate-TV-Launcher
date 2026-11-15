package com.kvendy.chocolatetvlauncher.data

import android.content.ComponentName

data class AppInfo(
    val packageName: String,
    val label: String,
    val componentName: ComponentName,
    // Whether this app declares a real android:banner — computed once at scan time (a resource id
    // check, not a drawable decode) so tiles can decide their render mode synchronously instead of
    // silently discovering the fallback only once the image finishes loading.
    val hasBanner: Boolean = false
)