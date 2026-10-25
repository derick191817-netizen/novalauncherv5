package dev.nova.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.nova.core.domain.model.AppInfo
import dev.nova.core.domain.model.HomeApp
import dev.nova.core.domain.model.HomeFolder
import dev.nova.core.domain.model.HomeWidget
import dev.nova.core.domain.model.LauncherSettings
import dev.nova.core.domain.usecase.LaunchAppUseCase
import dev.nova.core.domain.usecase.ManageHomeLayoutUseCase
import dev.nova.core.domain.usecase.ObserveSettingsUseCase
import dev.nova.core.domain.usecase.ObserveVisibleAppsUseCase
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estado e ações da home. Toda escrita no layout passa por [ManageHomeLayoutUseCase]. */
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel @Inject constructor(
    observeSettings: ObserveSettingsUseCase,
    private val observeApps: ObserveVisibleAppsUseCase,
    private val layout: ManageHomeLayoutUseCase,
    private val launchApp: LaunchAppUseCase,
) : ViewModel() {

    private val settings = observeSettings().shareIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), replay = 1)
    private val editMode = MutableStateFlow(false)
    private val openFolderId = MutableStateFlow<String?>(null)

    private val layoutFlow = settings.map { it.profile }.distinctUntilChanged().flatMapLatest { layout.observe(it) }

    /** Estado único consumido pela UI. */
    val uiState: StateFlow<HomeUiState> = combine(
        settings, observeApps(), layoutFlow, editMode, openFolderId,
    ) { s, apps, l, edit, folderId ->
        val byKey = apps.associateBy { it.key }
        val items = l.items.mapNotNull { item ->
            when (item) {
                is HomeApp -> byKey[item.appKey]?.let { HomeItemUi.App(item, it) }
                is HomeFolder -> HomeItemUi.Folder(item, item.appKeys.mapNotNull(byKey::get))
                is HomeWidget -> HomeItemUi.Widget(item)
            }
        }
        HomeUiState(
            items = items,
            dock = l.dock.mapNotNull(byKey::get).take(s.dockSize),
            allApps = apps,
            pageCount = l.pageCount,
            settings = s,
            editMode = edit,
            openFolder = items.filterIsInstance<HomeItemUi.Folder>().firstOrNull { it.item.id == folderId },
            pendingWidgets = l.pendingWidgets,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), HomeUiState())

    init {
        // Primeiro uso de cada perfil: dock padrão (idempotente).
        viewModelScope.launch {
            settings.map { it.profile }.distinctUntilChanged().collect { profile ->
                val apps = observeApps().first { it.isNotEmpty() }
                val s = settings.first()
                layout.seed(profile, apps, s.dockSize, s.grid.rows, s.grid.columns)
            }
        }
        // Mudou linhas/colunas: itens que ficaram fora da grade voltam para dentro (senão "somem").
        viewModelScope.launch {
            settings.map { Triple(it.profile, it.grid.rows, it.grid.columns) }.distinctUntilChanged()
                .collect { (profile, rows, columns) -> layout.reflow(profile, rows, columns) }
        }
    }

    fun onAppClick(app: AppInfo) {
        launchApp(app)
    }

    /** Com o layout travado não entra em modo de edição (mas sempre dá para sair dele). */
    fun toggleEdit() {
        if (uiState.value.settings.lockHomeLayout && !editMode.value) return
        editMode.value = !editMode.value
    }

    fun closeTransient() {
        editMode.value = false
        openFolderId.value = null
    }

    fun openFolder(id: String) { openFolderId.value = id }

    fun closeFolder() { openFolderId.value = null }

    fun remove(itemId: String) = edit { s -> layout.remove(s.profile, itemId) }

    fun createFolder(itemId: String) = edit { s -> layout.createFolder(s.profile, itemId) }

    fun addToFolder(folderId: String, appKey: String) = edit { s -> layout.addToFolder(s.profile, folderId, appKey) }

    fun removeFromFolder(folderId: String, appKey: String) = edit { s ->
        layout.removeFromFolder(s.profile, folderId, appKey, s.grid.rows, s.grid.columns)
    }

    fun removeFromDock(appKey: String) = edit { s -> layout.removeFromDock(s.profile, appKey) }

    fun addWidget(appWidgetId: Int, spanX: Int, spanY: Int) = edit { s ->
        layout.addWidget(s.profile, appWidgetId, spanX, spanY, s.grid.rows, s.grid.columns, s.features.widgetStack)
    }

    /** Move um item para outra célula da mesma página (arrastar e soltar); destino inválido é ignorado. */
    fun moveItem(itemId: String, page: Int, row: Int, column: Int) = edit { s ->
        layout.moveItem(s.profile, itemId, page, row, column, s.grid.rows, s.grid.columns)
    }

    fun renameFolder(folderId: String, name: String) = edit { s -> layout.renameFolder(s.profile, folderId, name) }

    /** Tira da fila de restauração um widget de backup já tratado. */
    fun consumePendingWidget(component: String) = edit { s -> layout.consumePendingWidget(s.profile, component) }

    // Toda alteração do layout passa aqui: com "Bloquear layout" ligado nada é movido, removido ou adicionado.
    private fun edit(block: suspend (LauncherSettings) -> Unit) {
        viewModelScope.launch {
            val s = settings.first()
            if (!s.lockHomeLayout) block(s)
        }
    }

    private companion object {
        const val STOP_MS = 5_000L
    }
}
