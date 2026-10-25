package dev.nova.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import dev.nova.core.ui.TextInputDialog
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nova.core.domain.model.AppInfo
import dev.nova.core.domain.model.GestureAction
import dev.nova.core.ui.AppIcon
import dev.nova.core.ui.AppTile
import dev.nova.core.ui.LocalPerformance
import dev.nova.feature.gestures.GestureBackground
import dev.nova.feature.gestures.launcherGestures
import dev.nova.feature.widgets.WidgetController
import dev.nova.feature.widgets.WidgetPickerDialog
import dev.nova.feature.widgets.WidgetProviderUi
import dev.nova.feature.widgets.WidgetStackView

/** Ponte entre a home e o resto do app (gaveta, widgets, ações de sistema). */
class HomeHost(
    val onGesture: (GestureAction) -> Unit,
    val onAddWidget: (WidgetProviderUi) -> Unit,
)

/** Home: pager de páginas com grade configurável + dock. Só as páginas visíveis são compostas. */
@Composable
fun HomeRoute(vm: HomeViewModel, widgets: WidgetController, host: HomeHost, modifier: Modifier = Modifier) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var showWidgetPicker by remember { mutableStateOf(false) }
    val settings = state.settings
    val scope = rememberCoroutineScope()
    // Backup guarda os widgets pelo provider; aqui o usuário religa um por vez (o bind pode pedir permissão).
    val restoreWidgets: () -> Unit = {
        val component = state.pendingWidgets.firstOrNull()
        if (component != null) {
            scope.launch {
                val provider = withContext(Dispatchers.IO) {
                    widgets.providers().firstOrNull { it.component.flattenToString() == component }
                }
                vm.consumePendingWidget(component)
                if (provider != null) host.onAddWidget(provider)
            }
        }
    }

    BackHandler(enabled = state.editMode || state.openFolder != null) {
        if (state.openFolder != null) vm.closeFolder() else vm.toggleEdit()
    }

    Box(modifier.fillMaxSize().launcherGestures(settings.gestures, host.onGesture)) {
        Column(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = settings.grid.paddingDp.dp),
        ) {
            val pages = state.pageCount + if (state.editMode) 1 else 0
            val pager = rememberPagerState { pages }
            HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
                HomePage(
                    page = page,
                    state = state,
                    vm = vm,
                    widgets = widgets,
                    onGesture = host.onGesture,
                )
            }
            if (pages > 1) PageDots(pages, pager.currentPage)
            Dock(state.dock, state.settings.dockSize, vm)
        }
        if (state.editMode) {
            EditBar(
                pending = state.pendingWidgets.size,
                onRestoreWidgets = restoreWidgets,
                onSettings = { host.onGesture(GestureAction.OPEN_SETTINGS) },
                onAddWidget = { showWidgetPicker = true },
                onDone = vm::toggleEdit,
                modifier = Modifier.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.systemBars),
            )
        }
    }

    if (showWidgetPicker) {
        WidgetPickerDialog(
            controller = widgets,
            onPick = { showWidgetPicker = false; host.onAddWidget(it) },
            onDismiss = { showWidgetPicker = false },
        )
    }
    state.openFolder?.let { folder ->
        FolderDialog(folder, state.allApps, vm, onClose = vm::closeFolder)
    }
}

@Composable
private fun HomePage(
    page: Int,
    state: HomeUiState,
    vm: HomeViewModel,
    widgets: WidgetController,
    onGesture: (GestureAction) -> Unit,
) {
    val grid = state.settings.grid
    val pageItems = remember(state.items, page) { state.items.filter { it.item.page == page } }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Toque duplo (bloquear) e toque longo (editar) em áreas vazias. Fica DENTRO da página: o pager captura
        // os toques, então um fundo atrás dele nunca receberia nada.
        GestureBackground(
            onDoubleTap = { onGesture(state.settings.gestures.doubleTap) },
            onLongPress = vm::toggleEdit,
            modifier = Modifier.fillMaxSize(),
        )
        val cellW = maxWidth / grid.columns
        val cellH = maxHeight / grid.rows
        val iconSize = minOf(cellW * 0.62f, cellH * 0.6f, MAX_ICON)
        for (ui in pageItems) {
            val item = ui.item
            androidx.compose.runtime.key(item.id) {
                Box(
                    Modifier.offset(cellW * item.column, cellH * item.row)
                        .size(cellW * item.spanX, cellH * item.spanY)
                        .cellDrag(state.editMode && state.settings.features.dragAndDrop, cellW, cellH, item.column, item.row) { r, c ->
                            vm.moveItem(item.id, item.page, r, c)
                        },
                ) {
                    when (ui) {
                        is HomeItemUi.App -> AppCell(ui, iconSize, state, vm)
                        is HomeItemUi.Folder -> FolderCell(ui, iconSize, state.editMode, vm)
                        is HomeItemUi.Widget -> WidgetCell(ui, state, vm, widgets)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppCell(ui: HomeItemUi.App, iconSize: Dp, state: HomeUiState, vm: HomeViewModel) {
    var menu by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AppTile(
            app = ui.app, iconSize = iconSize, showLabel = true,
            onClick = { if (!state.editMode) vm.onAppClick(ui.app) },
            onLongClick = { menu = true },
        )
        if (state.editMode) RemoveBadge { vm.remove(ui.item.id) }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text("Criar pasta") }, onClick = { menu = false; vm.createFolder(ui.item.id) })
            DropdownMenuItem(text = { Text("Remover da home") }, onClick = { menu = false; vm.remove(ui.item.id) })
        }
    }
}

@Composable
private fun FolderCell(ui: HomeItemUi.Folder, iconSize: Dp, edit: Boolean, vm: HomeViewModel) {
    var menu by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.clickable { if (!edit) vm.openFolder(ui.item.id) else menu = true }.padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(iconSize).clip(RoundedCornerShape(iconSize / 4)).background(Color.White.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    ui.apps.take(4).chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            row.forEach { AppIcon(it, iconSize * 0.4f) }
                        }
                    }
                }
            }
            Text(ui.item.name, color = Color.White, maxLines = 1, style = MaterialTheme.typography.labelSmall)
        }
        if (edit) RemoveBadge { vm.remove(ui.item.id) }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text("Abrir") }, onClick = { menu = false; vm.openFolder(ui.item.id) })
        }
    }
}

@Composable
private fun WidgetCell(ui: HomeItemUi.Widget, state: HomeUiState, vm: HomeViewModel, widgets: WidgetController) {
    Box(Modifier.fillMaxSize().padding(2.dp)) {
        // Modo Ultra Performance: widgets pesados não são compostos (zero custo de render/atualização).
        if (state.settings.performance.heavyWidgets) {
            WidgetStackView(widgets, ui.item.appWidgetIds, Modifier.fillMaxSize())
        } else {
            Text("Widget desativado (Ultra Performance)", color = Color.White, style = MaterialTheme.typography.labelSmall)
        }
        if (state.editMode) {
            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.35f)).clickable { })
            RemoveBadge {
                ui.item.appWidgetIds.forEach(widgets::delete)
                vm.remove(ui.item.id)
            }
        }
    }
}

@Composable
private fun BoxScope.RemoveBadge(onClick: () -> Unit) {
    Box(
        Modifier.align(Alignment.TopEnd).size(24.dp).clip(CircleShape).background(Color(0xCCB3261E)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text("✕", color = Color.White, style = MaterialTheme.typography.labelSmall) }
}

@Composable
private fun PageDots(count: Int, current: Int) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.Center) {
        repeat(count) { i ->
            Box(
                Modifier.padding(3.dp).size(if (i == current) 8.dp else 6.dp).clip(CircleShape)
                    .background(Color.White.copy(alpha = if (i == current) 0.95f else 0.45f)),
            )
        }
    }
}

@Composable
private fun Dock(dock: List<AppInfo>, dockSize: Int, vm: HomeViewModel) {
    if (dock.isEmpty()) return
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color.Black.copy(alpha = if (LocalPerformance.current.blur) 0.28f else 0.5f),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            dock.take(dockSize).forEach { app ->
                var menu by remember { mutableStateOf(false) }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    AppTile(app, 52.dp, showLabel = false, onClick = { vm.onAppClick(app) }, onLongClick = { menu = true })
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Remover do dock") }, onClick = { menu = false; vm.removeFromDock(app.key) })
                    }
                }
            }
        }
    }
}

@Composable
private fun EditBar(
    pending: Int,
    onRestoreWidgets: () -> Unit,
    onSettings: () -> Unit,
    onAddWidget: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f), modifier = modifier.padding(8.dp)) {
        Row(Modifier.padding(horizontal = 8.dp)) {
            TextButton(onClick = onAddWidget) { Text("+ Widget") }
            if (pending > 0) TextButton(onClick = onRestoreWidgets) { Text("Restaurar widgets ($pending)") }
            TextButton(onClick = onSettings) { Text("Ajustes") }
            TextButton(onClick = onDone) { Text("Concluir") }
        }
    }
}

@Composable
private fun FolderDialog(folder: HomeItemUi.Folder, allApps: List<AppInfo>, vm: HomeViewModel, onClose: () -> Unit) {
    var picking by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onClose) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(16.dp)) {
                Text(folder.item.name, style = MaterialTheme.typography.titleMedium)
                LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.heightIn(max = 360.dp)) {
                    items(folder.apps, key = { it.key }) { app ->
                        AppTile(
                            app, 48.dp, showLabel = true,
                            onClick = { vm.onAppClick(app); onClose() },
                            onLongClick = { vm.removeFromFolder(folder.item.id, app.key) },
                            labelColor = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                Text("Toque longo remove da pasta.", style = MaterialTheme.typography.labelSmall)
                Row {
                    TextButton(onClick = { renaming = true }) { Text("Renomear") }
                    TextButton(onClick = { picking = true }) { Text("Adicionar app") }
                    TextButton(onClick = onClose) { Text("Fechar") }
                }
            }
        }
    }
    if (renaming) {
        TextInputDialog(
            title = "Renomear pasta",
            initial = folder.item.name,
            onConfirm = { vm.renameFolder(folder.item.id, it) },
            onDismiss = { renaming = false },
        )
    }
    if (picking) {
        Dialog(onDismissRequest = { picking = false }) {
            Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface) {
                LazyColumn(Modifier.heightIn(max = 480.dp).padding(16.dp)) {
                    items(allApps.filter { it.key !in folder.item.appKeys }, key = { it.key }) { app ->
                        Row(Modifier.fillMaxWidth().clickable { vm.addToFolder(folder.item.id, app.key); picking = false }.padding(8.dp)) {
                            AppIcon(app, 32.dp)
                            Text(app.label, Modifier.padding(start = 12.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Arrastar e soltar (modo de edição + flag `dragAndDrop`): o item acompanha o dedo e, ao soltar, vai para a
 * célula mais próxima. Destino ocupado/inválido => volta ao lugar (validado em ManageHomeLayoutUseCase.moveItem).
 * Só move dentro da mesma página.
 */
@Composable
private fun Modifier.cellDrag(
    enabled: Boolean,
    cellW: Dp,
    cellH: Dp,
    column: Int,
    row: Int,
    onDrop: (row: Int, column: Int) -> Unit,
): Modifier {
    var drag by remember(column, row) { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val cw = with(density) { cellW.toPx() }
    val ch = with(density) { cellH.toPx() }
    val drop by rememberUpdatedState(onDrop)
    if (!enabled) return this
    return this
        .zIndex(if (drag == Offset.Zero) 0f else 1f)
        .graphicsLayer { translationX = drag.x; translationY = drag.y }
        .pointerInput(column, row, cw, ch) {
            detectDragGestures(
                onDragEnd = {
                    val targetColumn = column + (drag.x / cw).roundToInt()
                    val targetRow = row + (drag.y / ch).roundToInt()
                    drag = Offset.Zero
                    drop(targetRow, targetColumn)
                },
                onDragCancel = { drag = Offset.Zero },
            ) { change, amount ->
                change.consume()
                drag += amount
            }
        }
}

private val MAX_ICON = 64.dp
