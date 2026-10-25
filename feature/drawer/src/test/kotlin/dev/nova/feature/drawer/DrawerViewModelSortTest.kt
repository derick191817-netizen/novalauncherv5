package dev.nova.feature.drawer

import dev.nova.core.domain.model.AppCategory
import dev.nova.core.domain.model.AppInfo
import dev.nova.core.domain.model.AppSort
import dev.nova.core.domain.model.LauncherSettings
import dev.nova.core.domain.model.SearchResult
import dev.nova.core.domain.repository.ContactsRepository
import dev.nova.core.domain.usecase.GetAppShortcutsUseCase
import dev.nova.core.domain.usecase.GroupSmartFoldersUseCase
import dev.nova.core.domain.usecase.LaunchAppUseCase
import dev.nova.core.domain.usecase.LaunchShortcutUseCase
import dev.nova.core.domain.usecase.ManageHomeLayoutUseCase
import dev.nova.core.domain.usecase.ObserveHiddenKeysUseCase
import dev.nova.core.domain.usecase.ObserveSettingsUseCase
import dev.nova.core.domain.usecase.ObserveVisibleAppsUseCase
import dev.nova.core.domain.usecase.SearchAppsUseCase
import dev.nova.core.domain.usecase.SetAppHiddenUseCase
import dev.nova.core.domain.usecase.UniversalSearchUseCase
import dev.nova.core.domain.usecase.UpdateSettingsUseCase
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DrawerViewModelSortTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    private class NoContacts : ContactsRepository {
        override suspend fun search(query: String, limit: Int) = emptyList<SearchResult.Contact>()
    }

    private fun app(label: String, installed: Long) =
        AppInfo(label, "p.$label", "C", label, 0L, false, AppCategory.UNDEFINED, installed, 0L)

    private fun vm(apps: FakeApps, settings: FakeSettings): DrawerViewModel {
        val hidden = FakeHidden()
        val dispatchers = TestDispatchers(dispatcher)
        return DrawerViewModel(
            ObserveSettingsUseCase(settings), ObserveVisibleAppsUseCase(apps, hidden), ObserveHiddenKeysUseCase(hidden),
            SearchAppsUseCase(dispatchers), UniversalSearchUseCase(NoContacts(), dispatchers), GroupSmartFoldersUseCase(),
            LaunchAppUseCase(apps), ManageHomeLayoutUseCase(FakeLayout()), SetAppHiddenUseCase(hidden),
            GetAppShortcutsUseCase(apps), LaunchShortcutUseCase(apps), UpdateSettingsUseCase(settings),
        )
    }

    private val apps = FakeApps(listOf(app("Zebra", 5), app("Ágora", 1), app("Abc", 9)))

    @Test fun alphabeticalOrderIgnoresAccents() = runTest {
        val vm = vm(apps, FakeSettings())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        assertEquals(listOf("Abc", "Ágora", "Zebra"), vm.uiState.value.apps.map { it.label })
    }

    @Test fun changingSortReordersTheList() = runTest {
        val vm = vm(apps, FakeSettings(LauncherSettings()))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.setSort(AppSort.RECENTLY_INSTALLED)
        advanceUntilIdle()
        assertEquals(listOf("Abc", "Zebra", "Ágora"), vm.uiState.value.apps.map { it.label })
        assertEquals(AppSort.RECENTLY_INSTALLED, vm.uiState.value.sort)
    }
}
