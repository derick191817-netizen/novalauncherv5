package dev.nova.launcher

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nova.core.domain.model.GestureAction
import dev.nova.core.domain.model.LauncherSettings
import dev.nova.core.domain.model.PerformanceMode
import dev.nova.core.domain.repository.IconRepository
import dev.nova.core.ui.LocalIconRepository
import dev.nova.core.ui.NovaTheme
import dev.nova.feature.drawer.DrawerRoute
import dev.nova.feature.drawer.DrawerViewModel
import dev.nova.feature.home.HomeHost
import dev.nova.feature.home.HomeRoute
import dev.nova.feature.home.HomeViewModel
import dev.nova.feature.widgets.WidgetController
import dev.nova.feature.widgets.WidgetProviderUi

/** Raiz da UI do launcher: home + gaveta sobreposta. Toda a navegação é um único booleano (sem NavHost: cold start menor). */
@Composable
fun LauncherRoot(
    homeVm: HomeViewModel,
    drawerVm: DrawerViewModel,
    widgets: WidgetController,
    icons: IconRepository,
    homeSignal: Int,
    onSystemAction: (GestureAction, LauncherSettings) -> Unit,
    onAddWidget: (WidgetProviderUi) -> Unit,
    onUnlockHidden: (onSuccess: () -> Unit) -> Unit,
    onOpenSettings: () -> Unit,
    onWindowEffects: (PerformanceMode, Boolean) -> Unit,
) {
    val home by homeVm.uiState.collectAsStateWithLifecycle()
    val settings = home.settings
    var drawerOpen by rememberSaveable { mutableStateOf(false) }
    var focusSearch by rememberSaveable { mutableStateOf(false) }

    // Registrado primeiro => menor prioridade: o botão voltar nunca fecha o launcher.
    BackHandler(enabled = true) { }

    // O host de widgets só escuta atualizações enquanto a tela está visível.
    LifecycleStartEffect(widgets) {
        widgets.startListening()
        onStopOrDispose { widgets.stopListening() }
    }
    // Botão Home pressionado com o launcher já aberto: volta para a home.
    LaunchedEffect(homeSignal) {
        if (homeSignal > 0) {
            drawerOpen = false
            drawerVm.reset()
            homeVm.closeTransient()
        }
    }
    LaunchedEffect(settings.performance, drawerOpen) { onWindowEffects(settings.performance, drawerOpen) }

    val handle: (GestureAction) -> Unit = { action ->
        when (action) {
            GestureAction.OPEN_DRAWER -> { focusSearch = false; drawerOpen = true }
            GestureAction.OPEN_SEARCH -> { focusSearch = true; drawerOpen = true }
            GestureAction.OPEN_SETTINGS -> onOpenSettings()
            GestureAction.EDIT_LAYOUT -> homeVm.toggleEdit()
            GestureAction.LOCK_SCREEN, GestureAction.OPEN_NOTIFICATIONS -> onSystemAction(action, settings)
            GestureAction.NONE -> Unit
        }
    }

    CompositionLocalProvider(LocalIconRepository provides icons) {
        NovaTheme(settings.theme, settings.performance) {
            Box(Modifier.fillMaxSize()) {
                HomeRoute(homeVm, widgets, HomeHost(handle, onAddWidget))
                val animated = settings.performance.animations
                AnimatedVisibility(
                    visible = drawerOpen,
                    enter = if (animated) slideInVertically { it / 3 } + fadeIn() else EnterTransition.None,
                    exit = if (animated) slideOutVertically { it / 3 } + fadeOut() else ExitTransition.None,
                ) {
                    // Absorve toques: sem isso, gestos da home (toque duplo, swipe) disparariam por baixo da gaveta.
                    Box(Modifier.fillMaxSize().pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }) {
                        DrawerRoute(
                            vm = drawerVm,
                            autoFocusSearch = focusSearch,
                            onClose = { drawerOpen = false },
                            onRequestUnlock = onUnlockHidden,
                        )
                    }
                }
            }
        }
    }
}
