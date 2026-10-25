package dev.nova.feature.home

import dev.nova.core.common.AppDispatchers
import dev.nova.core.domain.model.AppCategory
import dev.nova.core.domain.model.AppInfo
import dev.nova.core.domain.model.AppShortcut
import dev.nova.core.domain.model.HomeLayout
import dev.nova.core.domain.model.LauncherSettings
import dev.nova.core.domain.model.UsageProfile
import dev.nova.core.domain.repository.AppRepository
import dev.nova.core.domain.repository.HiddenAppsRepository
import dev.nova.core.domain.repository.HomeLayoutRepository
import dev.nova.core.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal fun testApp(pkg: String, label: String = pkg) =
    AppInfo("$pkg/C#0", pkg, "C", label, 0L, false, AppCategory.UNDEFINED, 0L, 0L)

internal class FakeApps(apps: List<AppInfo>) : AppRepository {
    override val apps = MutableStateFlow(apps)
    val launched = mutableListOf<String>()
    override fun launch(app: AppInfo): Boolean { launched += app.key; return true }
    override suspend fun shortcuts(app: AppInfo): List<AppShortcut> = emptyList()
    override fun launchShortcut(shortcut: AppShortcut) = false
}

internal class FakeHidden(initial: Set<String> = emptySet()) : HiddenAppsRepository {
    val keys = MutableStateFlow(initial)
    override val hiddenKeys: Flow<Set<String>> = keys
    override suspend fun setHidden(appKey: String, hidden: Boolean) { keys.value = if (hidden) keys.value + appKey else keys.value - appKey }
}

internal class FakeSettings(initial: LauncherSettings = LauncherSettings()) : SettingsRepository {
    val state = MutableStateFlow(initial)
    override val settings: Flow<LauncherSettings> = state
    override suspend fun update(transform: (LauncherSettings) -> LauncherSettings) { state.value = transform(state.value) }
}

internal class FakeLayout : HomeLayoutRepository {
    val state = MutableStateFlow(mapOf<UsageProfile, HomeLayout>())
    override fun observe(profile: UsageProfile): Flow<HomeLayout> = state.map { it[profile] ?: HomeLayout() }
    override suspend fun update(profile: UsageProfile, transform: (HomeLayout) -> HomeLayout) {
        state.value = state.value + (profile to transform(state.value[profile] ?: HomeLayout()))
    }
    override suspend fun exportJson() = "{}"
    override suspend fun importJson(json: String) = Unit
}

internal class TestDispatchers(d: CoroutineDispatcher) : AppDispatchers {
    override val main = d
    override val default = d
    override val io = d
}
