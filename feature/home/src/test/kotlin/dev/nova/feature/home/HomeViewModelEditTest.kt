package dev.nova.feature.home

import dev.nova.core.domain.model.HomeApp
import dev.nova.core.domain.model.HomeFolder
import dev.nova.core.domain.model.UsageProfile
import dev.nova.core.domain.usecase.LaunchAppUseCase
import dev.nova.core.domain.usecase.ManageHomeLayoutUseCase
import dev.nova.core.domain.usecase.ObserveSettingsUseCase
import dev.nova.core.domain.usecase.ObserveVisibleAppsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelEditTest {
    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    private class Env {
        val apps = FakeApps(listOf(testApp("a"), testApp("b")))
        val layoutRepo = FakeLayout()
        val settings = FakeSettings()
        val manage = ManageHomeLayoutUseCase(layoutRepo)
        val vm = HomeViewModel(ObserveSettingsUseCase(settings), ObserveVisibleAppsUseCase(apps, FakeHidden()), manage, LaunchAppUseCase(apps))
    }

    @Test fun moveItemChangesPositionAndIgnoresOccupiedCell() = runTest {
        val env = Env()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { env.vm.uiState.collect {} }
        advanceUntilIdle()
        val rows = env.settings.state.value.grid.rows
        val columns = env.settings.state.value.grid.columns
        env.manage.addApp(UsageProfile.PERSONAL, "a/C#0", rows, columns)
        env.manage.addApp(UsageProfile.PERSONAL, "b/C#0", rows, columns)
        advanceUntilIdle()
        val items = env.vm.uiState.value.items.map { it.item as HomeApp }
        val a = items.first { it.appKey == "a/C#0" }
        val b = items.first { it.appKey == "b/C#0" }
        env.vm.moveItem(a.id, 0, b.row, b.column) // ocupada: ignorado
        env.vm.moveItem(b.id, 0, 4, 3) // livre
        advanceUntilIdle()
        val after = env.vm.uiState.value.items.map { it.item as HomeApp }
        assertEquals(a.row to a.column, after.first { it.appKey == "a/C#0" }.let { it.row to it.column })
        assertEquals(4 to 3, after.first { it.appKey == "b/C#0" }.let { it.row to it.column })
    }

    @Test fun renameFolderUpdatesName() = runTest {
        val env = Env()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { env.vm.uiState.collect {} }
        advanceUntilIdle()
        env.manage.addApp(UsageProfile.PERSONAL, "a/C#0", 6, 5)
        advanceUntilIdle()
        env.vm.createFolder(env.vm.uiState.value.items.first { it is HomeItemUi.App }.item.id)
        advanceUntilIdle()
        val folderId = env.vm.uiState.value.items.filterIsInstance<HomeItemUi.Folder>().single().item.id
        env.vm.renameFolder(folderId, "Trabalho")
        advanceUntilIdle()
        assertEquals("Trabalho", (env.vm.uiState.value.items.filterIsInstance<HomeItemUi.Folder>().single().item as HomeFolder).name)
    }

    @Test fun shrinkingTheGridPullsItemsBackInside() = runTest {
        val env = Env()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { env.vm.uiState.collect {} }
        advanceUntilIdle()
        env.layoutRepo.update(UsageProfile.PERSONAL) { it.withItem(HomeApp("far", 0, 5, 4, "a/C#0")) }
        advanceUntilIdle()
        env.settings.update { it.copy(grid = it.grid.copy(rows = 3, columns = 3)) }
        advanceUntilIdle()
        assertTrue(env.vm.uiState.value.items.all { it.item.row < 3 && it.item.column < 3 })
    }

    @Test fun pendingWidgetsAreExposedAndConsumed() = runTest {
        val env = Env()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { env.vm.uiState.collect {} }
        advanceUntilIdle()
        env.layoutRepo.update(UsageProfile.PERSONAL) { it.copy(pendingWidgets = listOf("p/W")) }
        advanceUntilIdle()
        assertEquals(listOf("p/W"), env.vm.uiState.value.pendingWidgets)
        env.vm.consumePendingWidget("p/W")
        advanceUntilIdle()
        assertTrue(env.vm.uiState.value.pendingWidgets.isEmpty())
    }
}
