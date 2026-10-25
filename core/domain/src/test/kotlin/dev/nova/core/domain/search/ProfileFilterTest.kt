package dev.nova.core.domain.search

import dev.nova.core.domain.model.AppCategory
import dev.nova.core.domain.model.AppInfo
import dev.nova.core.domain.model.UsageProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileFilterTest {
    private fun app(k: String, work: Boolean = false, cat: AppCategory = AppCategory.UNDEFINED) =
        AppInfo(k, k, "C", k, 0L, work, cat, 0L, 0L)

    private val apps = listOf(app("a"), app("w", work = true), app("g", cat = AppCategory.GAME))

    @Test fun personalShowsEverything() = assertEquals(listOf("a", "w", "g"), ProfileFilter.filter(apps, UsageProfile.PERSONAL).map { it.key })

    @Test fun workShowsWorkApps() = assertEquals(listOf("w"), ProfileFilter.filter(apps, UsageProfile.WORK).map { it.key })

    @Test fun gamingShowsGames() = assertEquals(listOf("g"), ProfileFilter.filter(apps, UsageProfile.GAMING).map { it.key })

    @Test fun emptyMatchFallsBackToAll() = assertEquals(2, ProfileFilter.filter(listOf(app("a"), app("b")), UsageProfile.WORK).size)
}
