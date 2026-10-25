package dev.nova.feature.drawer

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.nova.core.domain.model.AppInfo
import dev.nova.core.domain.model.AppShortcut
import dev.nova.core.domain.model.SearchResult
import dev.nova.core.domain.model.AppSort
import dev.nova.core.domain.search.AppSorter
import dev.nova.core.domain.search.ProfileFilter
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
import dev.nova.core.domain.usecase.SmartFolder
import dev.nova.core.domain.usecase.UniversalSearchUseCase
import dev.nova.core.domain.usecase.UpdateSettingsUseCase
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estado da gaveta de apps. */
@Immutable
data class DrawerUiState(
    val query: String = "",
    val apps: List<AppInfo> = emptyList(),
    val folders: List<SmartFolder> = emptyList(),
    val extra: List<SearchResult> = emptyList(),
    val columns: Int = 5,
    val hiddenAppsEnabled: Boolean = false,
    val hiddenUnlocked: Boolean = false,
    val hiddenKeys: Set<String> = emptySet(),
    val sort: AppSort = AppSort.ALPHABETICAL,
)

/** Busca instantânea (fuzzy) + busca universal + Smart Folders + apps ocultos. */
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class DrawerViewModel @Inject constructor(
    observeSettings: ObserveSettingsUseCase,
    observeApps: ObserveVisibleAppsUseCase,
    observeHiddenKeys: ObserveHiddenKeysUseCase,
    private val searchApps: SearchAppsUseCase,
    private val universalSearch: UniversalSearchUseCase,
    private val groupSmartFolders: GroupSmartFoldersUseCase,
    private val launchApp: LaunchAppUseCase,
    private val manageHome: ManageHomeLayoutUseCase,
    private val setAppHidden: SetAppHiddenUseCase,
    private val getShortcuts: GetAppShortcutsUseCase,
    private val launchShortcut: LaunchShortcutUseCase,
    private val updateSettings: UpdateSettingsUseCase,
) : ViewModel() {

    private class Searched(val query: String, val all: List<AppInfo>, val matches: List<AppInfo>)

    private val settings = observeSettings().shareIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), replay = 1)
    private val query = MutableStateFlow("")
    private val unlocked = MutableStateFlow(false)

    private val searched: Flow<Searched> = unlocked
        .flatMapLatest { observeApps(includeHidden = it) }
        // Perfil de uso: Trabalho mostra só apps do perfil de trabalho, Games só jogos (ver ProfileFilter).
        .combine(settings.map { it.profile }.distinctUntilChanged()) { apps, profile -> ProfileFilter.filter(apps, profile) }
        // Ordem escolhida (A–Z sem acento, ou recém-instalados).
        .combine(settings.map { it.drawerSort }.distinctUntilChanged()) { apps, sort -> AppSorter.sort(apps, sort) }
        .combine(query) { apps, q -> apps to q }
        .mapLatest { (apps, q) -> Searched(q, apps, searchApps(apps, q)) }

    private val extra: Flow<List<SearchResult>> = combine(
        query.debounce(DEBOUNCE_MS),
        settings.map { it.features }.distinctUntilChanged(),
    ) { q, f -> q to f }.mapLatest { (q, f) -> universalSearch(q, f) }

    /** Estado único consumido pela UI. */
    val uiState: StateFlow<DrawerUiState> = combine(
        searched, extra, settings, observeHiddenKeys(), unlocked,
    ) { s, e, st, hidden, open ->
        DrawerUiState(
            query = s.query,
            apps = s.matches,
            folders = if (st.features.smartFolders && s.query.isBlank()) groupSmartFolders(s.all) else emptyList(),
            extra = e,
            columns = st.grid.drawerColumns,
            hiddenAppsEnabled = st.features.hiddenApps,
            hiddenUnlocked = open,
            hiddenKeys = hidden,
            sort = st.drawerSort,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), DrawerUiState())

    fun onQueryChange(value: String) { query.value = value }

    fun setUnlocked(value: Boolean) { unlocked.value = value }

    /** Limpa busca e tranca os apps ocultos (chamado ao fechar a gaveta). */
    fun reset() {
        query.value = ""
        unlocked.value = false
    }

    fun launch(app: AppInfo): Boolean = launchApp(app)

    fun launchShortcut(shortcut: AppShortcut) { launchShortcut.invoke(shortcut) }

    suspend fun shortcuts(app: AppInfo): List<AppShortcut> = getShortcuts(app)

    fun addToHome(app: AppInfo) = withSettings { s ->
        if (!s.lockHomeLayout) manageHome.addApp(s.profile, app.key, s.grid.rows, s.grid.columns)
    }

    fun addToDock(app: AppInfo) = withSettings { s ->
        if (!s.lockHomeLayout) manageHome.addToDock(s.profile, app.key, s.dockSize)
    }

    fun setSort(sort: AppSort) {
        viewModelScope.launch { updateSettings { it.copy(drawerSort = sort) } }
    }

    fun setHidden(app: AppInfo, hidden: Boolean) {
        viewModelScope.launch { setAppHidden(app.key, hidden) }
    }

    private fun withSettings(block: suspend (dev.nova.core.domain.model.LauncherSettings) -> Unit) {
        viewModelScope.launch { block(settings.first()) }
    }

    private companion object {
        const val STOP_MS = 5_000L
        const val DEBOUNCE_MS = 150L
    }
}
