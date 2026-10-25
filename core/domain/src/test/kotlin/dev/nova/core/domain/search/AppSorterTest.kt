package dev.nova.core.domain.search

import dev.nova.core.domain.model.AppCategory
import dev.nova.core.domain.model.AppInfo
import dev.nova.core.domain.model.AppSort
import org.junit.Assert.assertEquals
import org.junit.Test

class AppSorterTest {
    private fun app(label: String, installed: Long = 0) =
        AppInfo(label, label, "C", label, 0L, false, AppCategory.UNDEFINED, installed, 0L)

    @Test fun alphabeticalIgnoresAccentsAndCase() {
        val sorted = AppSorter.sort(listOf(app("Zebra"), app("ágora"), app("Banco"), app("Abc")), AppSort.ALPHABETICAL)
        assertEquals(listOf("Abc", "ágora", "Banco", "Zebra"), sorted.map { it.label })
    }

    @Test fun recentlyInstalledPutsNewestFirst() {
        val sorted = AppSorter.sort(listOf(app("A", 1), app("B", 3), app("C", 2)), AppSort.RECENTLY_INSTALLED)
        assertEquals(listOf("B", "C", "A"), sorted.map { it.label })
    }

    @Test fun emptyListIsFine() = assertEquals(emptyList<AppInfo>(), AppSorter.sort(emptyList(), AppSort.ALPHABETICAL))
}
