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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManageHomeLayoutEditTest {
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
    private fun app(pkg: String, work: Boolean = false) =
        AppInfo("$pkg/C#${if (work) 1 else 0}", pkg, "C", pkg, if (work) 1L else 0L, work, AppCategory.UNDEFINED, 0L, 0L)

    @Test fun moveItemRejectsOccupiedAndAcceptsFree() = runTest {
        val repo = FakeRepo(); val uc = ManageHomeLayoutUseCase(repo)
        uc.addApp(p, "a", 6, 5); uc.addApp(p, "b", 6, 5)
        val a = repo.layout().items.first() as HomeApp
        val b = repo.layout().items.last() as HomeApp
        assertFalse(uc.moveItem(p, a.id, 0, b.row, b.column, 6, 5))
        assertTrue(uc.moveItem(p, a.id, 0, 4, 4, 6, 5))
        assertEquals(4, (repo.layout().items.first { it.id == a.id }).row)
    }

    @Test fun renameFolderTrimsAndIgnoresBlank() = runTest {
        val repo = FakeRepo(); val uc = ManageHomeLayoutUseCase(repo)
        uc.addApp(p, "a", 6, 5)
        uc.createFolder(p, repo.layout().items.single().id)
        val id = repo.layout().items.single().id
        uc.renameFolder(p, id, "  Jogos  ")
        assertEquals("Jogos", (repo.layout().items.single() as HomeFolder).name)
        uc.renameFolder(p, id, "   ")
        assertEquals("Jogos", (repo.layout().items.single() as HomeFolder).name)
    }

    @Test fun reflowBringsItemsInside() = runTest {
        val repo = FakeRepo(); val uc = ManageHomeLayoutUseCase(repo)
        repo.update(p) { it.withItem(HomeApp("x", 0, 5, 4, "kx")) }
        uc.reflow(p, 3, 3)
        assertTrue(repo.layout().items.all { it.row < 3 && it.column < 3 })
    }

    @Test fun pendingWidgetIsConsumed() = runTest {
        val repo = FakeRepo(); val uc = ManageHomeLayoutUseCase(repo)
        repo.update(p) { it.copy(pendingWidgets = listOf("a/B", "c/D")) }
        uc.consumePendingWidget(p, "a/B")
        assertEquals(listOf("c/D"), repo.layout().pendingWidgets)
    }

    @Test fun seedFillsHomeAndSkipsWorkCopies() = runTest {
        val repo = FakeRepo(); val uc = ManageHomeLayoutUseCase(repo)
        val apps = listOf(
            app("com.samsung.android.dialer"), app("com.android.chrome", work = true), app("com.android.chrome"),
            app("f1"), app("f2"), app("f3"), // preenchem o dock (5 vagas)
            app("com.google.android.youtube"), app("com.android.vending"),
        )
        uc.seed(p, apps, 5, rows = 6, columns = 5)
        val l = repo.layout()
        assertTrue(l.dock.none { it.endsWith("#1") }) // nada do perfil de trabalho no dock
        val onHome = l.items.filterIsInstance<HomeApp>().map { it.appKey }
        assertTrue("com.google.android.youtube/C#0" in onHome)
        assertTrue(onHome.none { it in l.dock }) // sem duplicar dock e home
    }
}
