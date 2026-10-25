package dev.nova.feature.home

import dev.nova.core.domain.model.LauncherSettings
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
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelLockTest {
    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun lockedLayoutRejectsEditsAndEditMode() = runTest {
        val apps = FakeApps(listOf(testApp("a")))
        val manage = ManageHomeLayoutUseCase(FakeLayout())
        val settings = FakeSettings(LauncherSettings(lockHomeLayout = true))
        val vm = HomeViewModel(ObserveSettingsUseCase(settings), ObserveVisibleAppsUseCase(apps, FakeHidden()), manage, LaunchAppUseCase(apps))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        manage.addApp(UsageProfile.PERSONAL, "a/C#0", 6, 5)
        advanceUntilIdle()
        val id = vm.uiState.value.items.single().item.id
        vm.remove(id)
        vm.toggleEdit()
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.items.size) // continua lá
        assertFalse(vm.uiState.value.editMode)
    }

    @Test fun unlockedLayoutStillEdits() = runTest {
        val apps = FakeApps(listOf(testApp("a")))
        val manage = ManageHomeLayoutUseCase(FakeLayout())
        val vm = HomeViewModel(ObserveSettingsUseCase(FakeSettings()), ObserveVisibleAppsUseCase(apps, FakeHidden()), manage, LaunchAppUseCase(apps))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        manage.addApp(UsageProfile.PERSONAL, "a/C#0", 6, 5)
        advanceUntilIdle()
        vm.remove(vm.uiState.value.items.single().item.id)
        advanceUntilIdle()
        assertEquals(0, vm.uiState.value.items.size)
    }
}
