package dev.nova.core.domain.usecase

import dev.nova.core.domain.model.AppCategory
import dev.nova.core.domain.model.AppInfo
import dev.nova.core.domain.model.HomeApp
import dev.nova.core.domain.model.HomeFolder
import dev.nova.core.domain.model.HomeLayout
import dev.nova.core.domain.model.UsageProfile
import dev.nova.core.domain.repository.HomeLayoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ManageHomeLayoutUseCaseTest {
    private class FakeRepo : HomeLayoutRepository {
        val state = MutableStateFlow(mapOf<UsageProfile, HomeLayout>())
        override fun observe(profile: UsageProfile): Flow<HomeLayout> = state.map { it[profile] ?: HomeLayout() }
        override suspend fun update(profile: UsageProfile, transform: (HomeLayout) -> HomeLayout) {
            state.value = state.value + (profile to transform(state.value[profile] ?: HomeLayout()))
        }
        override suspend fun exportJson() = "{}"
        override suspend fun importJson(json: String) = Unit
        fun layout() = state.value[UsageProfile.PERSONAL] ?: HomeLayout()
    }

    private val p = UsageProfile.PERSONAL
    private fun app(pkg: String) = AppInfo("$pkg/C#0", pkg, "C", pkg, 0L, false, AppCategory.UNDEFINED, 0L, 0L)

    @Test fun seedPrefersSamsungAppsAndIsIdempotent() = runTest {
        val repo = FakeRepo(); val uc = ManageHomeLayoutUseCase(repo)
        val apps = listOf(app("com.samsung.android.dialer"), app("com.android.chrome"), app("zzz"), app("com.android.settings"), app("aaa"))
        uc.seed(p, apps, 5)
        val dock = repo.layout().dock
        assertEquals("com.samsung.android.dialer/C#0", dock.first())
        assertEquals(5, dock.size)
        uc.seed(p, emptyList(), 5)
        assertEquals(dock, repo.layout().dock)
    }

    @Test fun folderLifecycle() = runTest {
        val repo = FakeRepo(); val uc = ManageHomeLayoutUseCase(repo)
        uc.addApp(p, "a", 6, 5); uc.addApp(p, "b", 6, 5)
        val first = repo.layout().items.first().id
        uc.createFolder(p, first)
        val folder = repo.layout().items.filterIsInstance<HomeFolder>().single()
        uc.addToFolder(p, folder.id, "b")
        assertEquals(listOf("a", "b"), repo.layout().items.filterIsInstance<HomeFolder>().single().appKeys)
        assertTrue(repo.layout().items.none { it is HomeApp && it.appKey == "b" })
        uc.removeFromFolder(p, folder.id, "a", 6, 5)
        assertTrue(repo.layout().items.any { it is HomeApp && it.appKey == "a" })
    }

    @Test fun dockRespectsLimit() = runTest {
        val repo = FakeRepo(); val uc = ManageHomeLayoutUseCase(repo)
        listOf("1", "2", "3").forEach { uc.addToDock(p, it, 2) }
        assertEquals(listOf("1", "2"), repo.layout().dock)
    }
}
