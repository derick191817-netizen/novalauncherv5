package dev.nova.core.domain.usecase

import dev.nova.core.domain.model.AppCategory
import dev.nova.core.domain.model.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class GroupSmartFoldersUseCaseTest {
    private fun app(label: String, c: AppCategory) = AppInfo(label, "p.$label", "C", label, 0L, false, c, 0L, 0L)

    @Test fun smallGroupsGoToOthers() {
        val apps = listOf(
            app("A", AppCategory.GAME), app("B", AppCategory.GAME),
            app("C", AppCategory.MAPS), app("D", AppCategory.UNDEFINED),
        )
        val result = GroupSmartFoldersUseCase()(apps)
        assertEquals(listOf(AppCategory.GAME, AppCategory.UNDEFINED), result.map { it.category })
        assertEquals(listOf("C", "D"), result.last().apps.map { it.label })
    }
}
