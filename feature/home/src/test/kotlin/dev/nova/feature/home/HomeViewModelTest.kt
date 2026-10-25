package dev.nova.feature.home

import dev.nova.core.domain.model.HomeWidget
import dev.nova.core.domain.model.UsageProfile
import dev.nova.core.domain.usecase.LaunchAppUseCase
import dev.nova.core.domain.usecase.ManageHomeLayoutUseCase
import dev.nova.core.domain.usecase.ObserveHiddenKeysUseCase
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
class HomeViewModelTest {
    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    private class Env(apps: FakeApps, val layoutRepo: FakeLayout = FakeLayout(), val settings: FakeSettings = FakeSettings()) {
        val manage = ManageHomeLayoutUseCase(layoutRepo)
        val vm = HomeViewModel(ObserveSettingsUseCase(settings), ObserveVisibleAppsUseCase(apps, FakeHidden()), manage, LaunchAppUseCase(apps))
    }

    @Test fun seedsDefaultDock() = runTest {
        val env = Env(FakeApps(listOf(testApp("com.samsung.android.dialer"), testApp("com.android.settings"), testApp("zzz"))))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { env.vm.uiState.collect {} }
        advanceUntilIdle()
        assertEquals("com.samsung.android.dialer", env.vm.uiState.value.dock.first().packageName)
    }

    @Test fun createFolderAndAddWidget() = runTest {
        val apps = FakeApps(listOf(testApp("a"), testApp("b")))
        val env = Env(apps)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { env.vm.uiState.collect {} }
        advanceUntilIdle()
        env.manage.addApp(UsageProfile.PERSONAL, "a/C#0", 6, 5)
        advanceUntilIdle()
        val appItem = env.vm.uiState.value.items.filterIsInstance<HomeItemUi.App>().single()
        env.vm.createFolder(appItem.item.id)
        env.vm.addWidget(7, 2, 1)
        advanceUntilIdle()
        val items = env.vm.uiState.value.items
        assertTrue(items.any { it is HomeItemUi.Folder })
        assertTrue(items.any { it is HomeItemUi.Widget && (it.item as HomeWidget).appWidgetIds == listOf(7) })
    }

    @Test fun clickLaunchesApp() = runTest {
        val apps = FakeApps(listOf(testApp("a")))
        val env = Env(apps)
        env.vm.onAppClick(apps.apps.value.first())
        assertEquals(listOf("a/C#0"), apps.launched)
    }
}
