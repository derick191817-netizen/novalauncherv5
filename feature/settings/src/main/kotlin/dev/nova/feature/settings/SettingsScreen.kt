package dev.nova.feature.settings

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nova.core.domain.model.GestureAction
import dev.nova.core.domain.model.IconShape
import dev.nova.core.domain.model.LockMethod
import dev.nova.core.domain.model.PerformanceMode
import dev.nova.core.domain.model.ThemeMode
import dev.nova.core.domain.model.UsageProfile
import dev.nova.core.ui.NovaTheme
import kotlin.math.roundToInt

/** Ações que dependem de Activity/sistema, fornecidas pelo módulo :app. */
class SettingsActions(
    val requestDeviceAdmin: () -> Unit,
    val openAccessibility: () -> Unit,
    val pickWallpaper: () -> Unit,
    val pickLiveWallpaper: () -> Unit,
    val openDefaultHome: () -> Unit,
)

private enum class Explain { CONTACTS, DEVICE_ADMIN, ACCESSIBILITY }

/** Tela de configurações completa (grade, dock, tema, desempenho, gestos, módulos, backup). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsRoute(actions: SettingsActions, vm: SettingsViewModel = hiltViewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val s = state.settings
    val context = LocalContext.current
    var explain by remember { mutableStateOf<Explain?>(null) }
    var showUnsaved by remember { mutableStateOf(false) }
    val closeScreen: () -> Unit = { context.findActivity()?.finish() }
    val requestClose: () -> Unit = { if (state.hasChanges) { showUnsaved = true } else { closeScreen() } }
    // Voltar com alterações pendentes pergunta em vez de perder tudo em silêncio; sem alterações, volta normal.
    BackHandler(enabled = state.hasChanges) { showUnsaved = true }

    LaunchedEffect(message) {
        message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show(); vm.messageShown() }
    }
    val contactsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.update { it.copy(features = it.features.copy(searchContacts = true)) }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(vm::export)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(vm::import) }

    NovaTheme(s.theme, s.performance) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize()) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).windowInsetsPadding(WindowInsets.systemBars)
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = if (state.hasChanges) 96.dp else 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Configurações", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = requestClose) { Text("Fechar") }
                }
                Text("Mexa nas opções e toque em Salvar para aplicar. Descartar volta ao que estava antes.", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = actions.openDefaultHome, modifier = Modifier.fillMaxWidth()) {
                    Text("Definir como launcher padrão")
                }

                Section("Grade e dock")
                SliderRow("Linhas", s.grid.rows, 3..10) { v -> vm.update { it.copy(grid = it.grid.copy(rows = v)) } }
                SliderRow("Colunas", s.grid.columns, 3..8) { v -> vm.update { it.copy(grid = it.grid.copy(columns = v)) } }
                SliderRow("Margem (dp)", s.grid.paddingDp, 0..32) { v -> vm.update { it.copy(grid = it.grid.copy(paddingDp = v)) } }
                SliderRow("Colunas da gaveta", s.grid.drawerColumns, 3..7) { v ->
                    vm.update { it.copy(grid = it.grid.copy(drawerColumns = v)) }
                }
                SliderRow("Apps no dock", s.dockSize, 5..7) { v -> vm.update { it.copy(dockSize = v) } }
                SwitchRow("Bloquear layout da tela inicial", "Impede mover, remover ou adicionar itens por engano.", s.lockHomeLayout) { v -> vm.update { it.copy(lockHomeLayout = v) } }

                Section("Aparência")
                Choice("Tema", ThemeMode.entries, s.theme, { themeName(it) }) { v -> vm.update { it.copy(theme = v) } }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = actions.pickWallpaper) { Text("Wallpaper") }
                    OutlinedButton(onClick = actions.pickLiveWallpaper) { Text("Wallpaper animado") }
                }
                Choice("Formato dos ícones", IconShape.entries, s.iconShape, { shapeName(it) }) { v -> vm.update { it.copy(iconShape = v) } }
                if (state.iconPacks.isNotEmpty()) {
                    Text("Pacote de ícones", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = s.iconPackPackage.isEmpty(), onClick = { vm.update { it.copy(iconPackPackage = "") } }, label = { Text("Padrão") })
                        state.iconPacks.forEach { p ->
                            FilterChip(selected = s.iconPackPackage == p.packageName, onClick = { vm.update { it.copy(iconPackPackage = p.packageName) } }, label = { Text(p.label) })
                        }
                    }
                }

                Section("Desempenho")
                Choice("Modo", PerformanceMode.entries, s.performance, { perfName(it) }) { v -> vm.update { it.copy(performance = v) } }
                Text(perfDescription(s.performance), style = MaterialTheme.typography.bodySmall)

                Section("Perfil de uso")
                Choice("Perfil (cada um tem sua home)", UsageProfile.entries, s.profile, { profileName(it) }) { v -> vm.update { it.copy(profile = v) } }

                Section("Gestos")
                Choice("Deslizar para cima", GestureAction.entries, s.gestures.swipeUp, { actionName(it) }) { v -> vm.update { it.copy(gestures = it.gestures.copy(swipeUp = v)) } }
                Choice("Deslizar para baixo", GestureAction.entries, s.gestures.swipeDown, { actionName(it) }) { v -> vm.update { it.copy(gestures = it.gestures.copy(swipeDown = v)) } }
                Choice("Toque duplo", GestureAction.entries, s.gestures.doubleTap, { actionName(it) }) { v -> vm.update { it.copy(gestures = it.gestures.copy(doubleTap = v)) } }
                Choice("Pinça", GestureAction.entries, s.gestures.pinch, { actionName(it) }) { v -> vm.update { it.copy(gestures = it.gestures.copy(pinch = v)) } }
                SwitchRow("Gestos de borda", "Deslize a partir da borda esquerda/direita para abrir a busca.", s.gestures.edgeSwipeEnabled) { v ->
                    vm.update { it.copy(gestures = it.gestures.copy(edgeSwipeEnabled = v)) }
                }
                Choice("Bloquear com toque duplo", LockMethod.entries, s.features.lockMethod, { lockName(it) }) { v ->
                    when (v) {
                        LockMethod.NONE -> vm.update { it.copy(features = it.features.copy(lockMethod = v)) }
                        LockMethod.DEVICE_ADMIN -> explain = Explain.DEVICE_ADMIN
                        LockMethod.ACCESSIBILITY -> explain = Explain.ACCESSIBILITY
                    }
                }

                Section("Módulos")
                SwitchRow("Smart Folders", "Agrupa a gaveta por categoria automaticamente.", s.features.smartFolders) { v -> vm.update { it.copy(features = it.features.copy(smartFolders = v)) } }
                SwitchRow("Apps ocultos", "Esconde apps da gaveta; para ver, exige biometria ou senha do aparelho.", s.features.hiddenApps) { v -> vm.update { it.copy(features = it.features.copy(hiddenApps = v)) } }
                SwitchRow("Pilha de widgets", "Novos widgets do mesmo tamanho empilham (deslize na vertical).", s.features.widgetStack) { v -> vm.update { it.copy(features = it.features.copy(widgetStack = v)) } }
                SwitchRow("Arrastar e soltar (experimental)", "No modo de edição, arraste itens da home para outra célula da mesma página.", s.features.dragAndDrop) { v -> vm.update { it.copy(features = it.features.copy(dragAndDrop = v)) } }
                SwitchRow("Ícones temáticos", "Android 13+: ícones com a cor do wallpaper (só apps com ícone monocromático).", s.features.themedIcons) { v -> vm.update { it.copy(features = it.features.copy(themedIcons = v)) } }
                SwitchRow("Buscar em configurações", "Mostra atalhos para telas do sistema na busca.", s.features.searchSettings) { v -> vm.update { it.copy(features = it.features.copy(searchSettings = v)) } }
                SwitchRow("Buscar na web", "Oferece pesquisar o texto no navegador.", s.features.searchWeb) { v -> vm.update { it.copy(features = it.features.copy(searchWeb = v)) } }
                SwitchRow("Buscar contatos", "Pede a permissão de contatos; os dados ficam só no aparelho.", s.features.searchContacts) { v ->
                    if (!v) {
                        vm.update { it.copy(features = it.features.copy(searchContacts = false)) }
                    } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                        vm.update { it.copy(features = it.features.copy(searchContacts = true)) }
                    } else {
                        explain = Explain.CONTACTS
                    }
                }

                Section("Backup")
                Text("Escolha “Google Drive” no seletor de arquivos para guardar na nuvem.", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { exportLauncher.launch("nova-launcher-backup.json") }) { Text("Exportar") }
                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }) { Text("Importar") }
                }

                Section("Segurança")
                Text("Se algo ficar estranho, volte tudo ao padrão. Nada vale até você tocar em Salvar.", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = vm::resetDraftToDefaults, modifier = Modifier.fillMaxWidth()) { Text("Restaurar padrões") }
            }
            if (state.hasChanges) {
                SaveBar(onSave = { vm.save() }, onDiscard = vm::discard, modifier = Modifier.align(Alignment.BottomCenter))
            }
            }
        }

        if (showUnsaved) {
            UnsavedChangesDialog(
                onSave = { showUnsaved = false; vm.save(onDone = closeScreen) },
                onDiscard = { showUnsaved = false; vm.discard(); closeScreen() },
                onKeepEditing = { showUnsaved = false },
            )
        }

        explain?.let { which ->
            val (title, body) = explanation(which)
            AlertDialog(
                onDismissRequest = { explain = null },
                title = { Text(title) },
                text = { Text(body) },
                confirmButton = {
                    TextButton(onClick = {
                        explain = null
                        when (which) {
                            Explain.CONTACTS -> contactsPermission.launch(Manifest.permission.READ_CONTACTS)
                            Explain.DEVICE_ADMIN -> {
                                vm.update { it.copy(features = it.features.copy(lockMethod = LockMethod.DEVICE_ADMIN)) }
                                actions.requestDeviceAdmin()
                            }
                            Explain.ACCESSIBILITY -> {
                                vm.update { it.copy(features = it.features.copy(lockMethod = LockMethod.ACCESSIBILITY)) }
                                actions.openAccessibility()
                            }
                        }
                    }) { Text("Continuar") }
                },
                dismissButton = { TextButton(onClick = { explain = null }) { Text("Cancelar") } },
            )
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 16.dp))
}

@Composable
private fun SliderRow(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Column {
        Text("$label: $value", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = (range.last - range.first - 1).coerceAtLeast(0),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> Choice(label: String, options: List<T>, selected: T, name: (T) -> String, onSelect: (T) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { o -> FilterChip(selected = o == selected, onClick = { onSelect(o) }, label = { Text(name(o)) }) }
        }
    }
}

@Composable
private fun SwitchRow(label: String, description: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

private fun explanation(e: Explain): Pair<String, String> = when (e) {
    Explain.CONTACTS -> "Permissão de contatos" to
        "Usada somente para mostrar contatos na busca. Nada é enviado para fora do aparelho."
    Explain.DEVICE_ADMIN -> "Administrador do dispositivo" to
        "Necessário apenas para bloquear a tela com toque duplo. O launcher não acessa nem altera outras políticas " +
            "(compatível com Samsung Knox). Obs.: após bloquear assim, o Android pode exigir PIN/senha em vez de biometria."
    Explain.ACCESSIBILITY -> "Serviço de acessibilidade" to
        "Usado apenas para a ação “bloquear tela” (e abrir notificações, se necessário). O serviço não lê o conteúdo da tela. " +
            "Você ativa manualmente na próxima tela."
}

private fun themeName(m: ThemeMode) = when (m) {
    ThemeMode.LIGHT -> "Claro"
    ThemeMode.DARK -> "Escuro"
    ThemeMode.AUTO -> "Automático"
    ThemeMode.AMOLED_BLACK -> "AMOLED preto"
}

private fun perfName(m: PerformanceMode) = when (m) {
    PerformanceMode.NORMAL -> "Normal"
    PerformanceMode.ULTRA_PERFORMANCE -> "Ultra Performance"
    PerformanceMode.BATTERY_SAVER -> "Economia"
}

private fun perfDescription(m: PerformanceMode) = when (m) {
    PerformanceMode.NORMAL -> "Animações, blur e widgets ativos; taxa de atualização máxima (120Hz)."
    PerformanceMode.ULTRA_PERFORMANCE -> "Sem animações, blur nem widgets pesados."
    PerformanceMode.BATTERY_SAVER -> "Limita a 60Hz e sem blur."
}

private fun profileName(p: UsageProfile) = when (p) {
    UsageProfile.PERSONAL -> "Pessoal"
    UsageProfile.WORK -> "Trabalho"
    UsageProfile.GAMING -> "Games"
}

private fun lockName(l: LockMethod) = when (l) {
    LockMethod.NONE -> "Desligado"
    LockMethod.DEVICE_ADMIN -> "Administrador"
    LockMethod.ACCESSIBILITY -> "Acessibilidade"
}

private fun actionName(a: GestureAction) = when (a) {
    GestureAction.NONE -> "Nada"
    GestureAction.OPEN_DRAWER -> "Gaveta"
    GestureAction.OPEN_NOTIFICATIONS -> "Notificações"
    GestureAction.LOCK_SCREEN -> "Bloquear"
    GestureAction.OPEN_SEARCH -> "Busca"
    GestureAction.OPEN_SETTINGS -> "Ajustes"
    GestureAction.EDIT_LAYOUT -> "Editar home"
}

private fun shapeName(shape: IconShape) = when (shape) {
    IconShape.SYSTEM -> "Do sistema"
    IconShape.CIRCLE -> "Círculo"
    IconShape.SQUIRCLE -> "Squircle"
    IconShape.ROUNDED_SQUARE -> "Arredondado"
    IconShape.TEARDROP -> "Gota"
}

/** Barra fixa Salvar / Descartar: aparece só quando há alterações. Nada vale antes de "Salvar". */
@Composable
private fun SaveBar(onSave: () -> Unit, onDiscard: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(12.dp),
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
    ) {
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Alterações não salvas", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = onDiscard) { Text("Descartar") }
            Button(onClick = onSave) { Text("Salvar") }
        }
    }
}

/** Aparece ao fechar/voltar com alterações pendentes: o usuário sempre tem uma saída. */
@Composable
private fun UnsavedChangesDialog(onSave: () -> Unit, onDiscard: () -> Unit, onKeepEditing: () -> Unit) {
    AlertDialog(
        onDismissRequest = onKeepEditing,
        title = { Text("Salvar alterações?") },
        text = { Text("Salvar aplica no launcher. Descartar volta ao que estava antes.") },
        confirmButton = { TextButton(onClick = onSave) { Text("Salvar") } },
        dismissButton = {
            Row {
                TextButton(onClick = onDiscard) { Text("Descartar") }
                TextButton(onClick = onKeepEditing) { Text("Continuar editando") }
            }
        },
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
