package com.kvendy.chocolatetvlauncher.data

import org.junit.Assert.assertEquals
import org.junit.Test

class GridDiscoveryTest {
    @Test fun incompleteScanPreservesFolderAndUnavailableApps() {
        val folder = HomeGridItem.FolderItem("folder", "Favorites", listOf("app.a", "app.b"))
        val original = listOf(folder, HomeGridItem.AppItem("app.c"))
        assertEquals(
            original + HomeGridItem.AppItem("app.new"),
            appendDiscoveredApps(original, listOf("app.a", "app.new"))
        )
    }

    @Test fun repeatedDiscoveryDoesNotDuplicateEntriesOrFolderMembers() {
        val original = listOf(HomeGridItem.FolderItem("folder", null, listOf("app.a")))
        val discovered = listOf("app.a", "app.new", "app.new", "")
        val updated = appendDiscoveredApps(original, discovered)
        assertEquals(original + HomeGridItem.AppItem("app.new"), updated)
        assertEquals(updated, appendDiscoveredApps(updated, discovered))
    }

    @Test fun emptyScanDoesNotEraseLayout() {
        val original = listOf(HomeGridItem.AppItem("app.a"))
        assertEquals(original, appendDiscoveredApps(original, emptyList()))
    }
}
