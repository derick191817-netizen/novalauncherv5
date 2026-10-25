package dev.nova.feature.drawer

import dev.nova.core.domain.model.FeatureFlags
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DrawerViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    private class NoContacts : ContactsRepository {
        override suspend fun search(query: String, limit: Int) = emptyList<SearchResult.Contact>()
    }

    private fun vm(apps: FakeApps, hidden: FakeHidden, settings: FakeSettings = FakeSettings()): DrawerViewModel {
        val dispatchers = TestDispatchers(dispatcher)
        return DrawerViewModel(
            ObserveSettingsUseCase(settings),
            ObserveVisibleAppsUseCase(apps, hidden),
            ObserveHiddenKeysUseCase(hidden),
            SearchAppsUseCase(dispatchers),
            UniversalSearchUseCase(NoContacts(), dispatchers),
            GroupSmartFoldersUseCase(),
            LaunchAppUseCase(apps),
            ManageHomeLayoutUseCase(FakeLayout()),
            SetAppHiddenUseCase(hidden),
            GetAppShortcutsUseCase(apps),
            LaunchShortcutUseCase(apps),
            UpdateSettingsUseCase(settings),
        )
    }

    @Test fun fuzzyQueryFiltersApps() = runTest {
        val vm = vm(FakeApps(listOf(testApp("p.gmail", "Gmail"), testApp("p.maps", "Google Maps"), testApp("p.cam", "Câmera"))), FakeHidden())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        vm.onQueryChange("camera")
        advanceUntilIdle()
        assertEquals(listOf("Câmera"), vm.uiState.value.apps.map { it.label })
    }

    @Test fun hiddenAppsOnlyAfterUnlock() = runTest {
        val hiddenApp = testApp("p.secret", "Secreto")
        val vm = vm(FakeApps(listOf(testApp("p.a", "A"), hiddenApp)), FakeHidden(setOf(hiddenApp.key)))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        assertTrue(vm.uiState.value.apps.none { it.label == "Secreto" })
        vm.setUnlocked(true)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.apps.any { it.label == "Secreto" })
        vm.reset()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.apps.none { it.label == "Secreto" })
    }

    @Test fun webResultAppearsWhenEnabled() = runTest {
        val settings = FakeSettings(LauncherSettings(features = FeatureFlags(searchWeb = true)))
        val vm = vm(FakeApps(listOf(testApp("p.a", "A"))), FakeHidden(), settings)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        vm.onQueryChange("zebra")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.extra.any { it is SearchResult.Web })
    }
}
