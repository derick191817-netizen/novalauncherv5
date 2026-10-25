package dev.nova.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AppInfoExtTest {
    private fun app(update: Long) = AppInfo("p/C#0", "p", "C", "P", 0L, false, AppCategory.UNDEFINED, 0L, update)

    @Test fun keyChangesWhenAppIsUpdated() = assertNotEquals(app(1).iconCacheKey, app(2).iconCacheKey)

    @Test fun keyIsStableForSameApp() = assertEquals(app(7).iconCacheKey, app(7).iconCacheKey)
}
