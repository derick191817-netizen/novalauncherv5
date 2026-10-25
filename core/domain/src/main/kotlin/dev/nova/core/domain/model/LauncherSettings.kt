package dev.nova.core.domain.model

import androidx.compose.runtime.Immutable

/** Tema do launcher. */
enum class ThemeMode { LIGHT, DARK, AUTO, AMOLED_BLACK }

/**
 * Modo de desempenho. As propriedades são a política que a UI deve respeitar.
 *
 * @property maxRefreshHz limite de taxa de atualização (null = sem limite, 120Hz no Note 20 Ultra).
 */
// freezeOffscreenIcons: no modo Economia o cache de ícones em memória cai pela metade.
enum class PerformanceMode(
    val animations: Boolean,
    val blur: Boolean,
    val heavyWidgets: Boolean,
    val freezeOffscreenIcons: Boolean,
    val maxRefreshHz: Int?,
) {
    NORMAL(animations = true, blur = true, heavyWidgets = true, freezeOffscreenIcons = false, maxRefreshHz = null),
    ULTRA_PERFORMANCE(false, false, false, freezeOffscreenIcons = false, maxRefreshHz = null),
    BATTERY_SAVER(true, false, true, freezeOffscreenIcons = true, maxRefreshHz = 60),
}

/** Perfil de uso (troca rápida de layout/apps). */
enum class UsageProfile { PERSONAL, WORK, GAMING }

/** Ações atribuíveis a gestos. */
enum class GestureAction { NONE, OPEN_DRAWER, OPEN_NOTIFICATIONS, LOCK_SCREEN, OPEN_SEARCH, OPEN_SETTINGS, EDIT_LAYOUT }

/** Como o double-tap bloqueia a tela. */
enum class LockMethod { NONE, DEVICE_ADMIN, ACCESSIBILITY }

/** Grade da home. */
@Immutable
data class GridSettings(
    val rows: Int = 6,
    val columns: Int = 5,
    val paddingDp: Int = 12,
    val drawerColumns: Int = 5,
)

/** Gestos configuráveis. */
@Immutable
data class GestureSettings(
    val swipeUp: GestureAction = GestureAction.OPEN_DRAWER,
    val swipeDown: GestureAction = GestureAction.OPEN_NOTIFICATIONS,
    val doubleTap: GestureAction = GestureAction.LOCK_SCREEN,
    val pinch: GestureAction = GestureAction.EDIT_LAYOUT,
    val edgeSwipeEnabled: Boolean = false,
)

/** Módulos opcionais (ativar/desativar). */
@Immutable
data class FeatureFlags(
    val smartFolders: Boolean = false,
    val hiddenApps: Boolean = false,
    val widgetStack: Boolean = false,
    val lockMethod: LockMethod = LockMethod.NONE,
    val searchContacts: Boolean = false,
    val searchSettings: Boolean = false,
    val searchWeb: Boolean = false,
    /** Arrastar e soltar na home (experimental). */
    val dragAndDrop: Boolean = false,
    /** Ícones temáticos Material You (Android 13+, usa a camada monocromática do app). */
    val themedIcons: Boolean = false,
)

/** Todas as preferências do launcher (fonte única da verdade, persistida em DataStore). */
@Immutable
data class LauncherSettings(
    val grid: GridSettings = GridSettings(),
    val dockSize: Int = 5,
    val theme: ThemeMode = ThemeMode.AUTO,
    val performance: PerformanceMode = PerformanceMode.NORMAL,
    val profile: UsageProfile = UsageProfile.PERSONAL,
    val gestures: GestureSettings = GestureSettings(),
    val features: FeatureFlags = FeatureFlags(),
    val iconPackPackage: String = "",
    val iconShape: IconShape = IconShape.SYSTEM,
    val drawerSort: AppSort = AppSort.ALPHABETICAL,
    /** Trava a tela inicial: sem mover, remover ou adicionar itens por engano (como no One UI). */
    val lockHomeLayout: Boolean = false,
) {
    /** Retorna uma cópia com todos os valores dentro dos limites suportados. */
    fun sanitized(): LauncherSettings = copy(
        grid = grid.copy(
            rows = grid.rows.coerceIn(3, 10),
            columns = grid.columns.coerceIn(3, 8),
            paddingDp = grid.paddingDp.coerceIn(0, 32),
            drawerColumns = grid.drawerColumns.coerceIn(3, 7),
        ),
        dockSize = dockSize.coerceIn(5, 7),
    )
}
