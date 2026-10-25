package dev.nova.feature.home

import androidx.compose.runtime.Immutable
import dev.nova.core.domain.model.AppInfo
import dev.nova.core.domain.model.HomeApp
import dev.nova.core.domain.model.HomeFolder
import dev.nova.core.domain.model.HomeItem
import dev.nova.core.domain.model.HomeWidget
import dev.nova.core.domain.model.LauncherSettings

/** Item da home já resolvido para a UI (com os [AppInfo] reais). */
@Immutable
sealed interface HomeItemUi {
    val item: HomeItem

    /** App solto. */
    @Immutable
    data class App(override val item: HomeApp, val app: AppInfo) : HomeItemUi

    /** Pasta. */
    @Immutable
    data class Folder(override val item: HomeFolder, val apps: List<AppInfo>) : HomeItemUi

    /** Widget ou pilha de widgets. */
    @Immutable
    data class Widget(override val item: HomeWidget) : HomeItemUi
}

/** Estado completo da home. */
@Immutable
data class HomeUiState(
    val items: List<HomeItemUi> = emptyList(),
    val dock: List<AppInfo> = emptyList(),
    val allApps: List<AppInfo> = emptyList(),
    val pageCount: Int = 1,
    val settings: LauncherSettings = LauncherSettings(),
    val editMode: Boolean = false,
    val openFolder: HomeItemUi.Folder? = null,
    val pendingWidgets: List<String> = emptyList(),
)
