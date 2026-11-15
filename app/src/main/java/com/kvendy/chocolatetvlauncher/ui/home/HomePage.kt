package com.kvendy.chocolatetvlauncher.ui.home

/** Which top-level screen is currently shown — switched via the tabs next to the status bar clock. */
enum class HomePage(val label: String) {
    Home("Home"),
    Recommendations("Recommendations"),
    Notifications("Notifications")
}
