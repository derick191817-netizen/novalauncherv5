package dev.nova.feature.drawer

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import dev.nova.core.domain.model.AppSort
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import dev.nova.core.domain.search.FuzzyMatcher
import kotlinx.coroutines.launch
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nova.core.domain.model.AppCategory
import dev.nova.core.domain.model.AppInfo
import dev.nova.core.domain.model.AppShortcut
import dev.nova.core.domain.model.SearchResult
import dev.nova.core.ui.AppTile
import dev.nova.core.ui.LocalPerformance

/**
 * Gaveta de apps. LazyVerticalGrid só compõe o que está na tela (lazy loading), com `key` estável
 * por app e `contentType` para reaproveitar composições durante o scroll (zero frame drops).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DrawerRoute(
    vm: DrawerViewModel,
    autoFocusSearch: Boolean,
    onClose: () -> Unit,
    onRequestUnlock: (onSuccess: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focus = remember { FocusRequester() }
    val blur = LocalPerformance.current.blur
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    // Índice A–Z só na lista "limpa": com busca ou Smart Folders a posição do app na grade não é a da lista.
    val showAlphabet = state.query.isEmpty() && state.folders.isEmpty() && state.sort == AppSort.ALPHABETICAL &&
        state.apps.size > ALPHABET_MIN_APPS
    val firstIndexByLetter = remember(state.apps) {
        val map = LinkedHashMap<Char, Int>()
        state.apps.forEachIndexed { i, app -> map.putIfAbsent(initialOf(app.label), i) }
        map
    }

    // O texto digitado fica AQUI (TextFieldValue guarda texto + cursor). Antes ele vinha do ViewModel por uma
    // cadeia assíncrona de flows e chegava atrasado: o campo era sobrescrito com o texto antigo e o cursor
    // voltava ao início, então a letra nova entrava antes da anterior.
    var field by remember { mutableStateOf(TextFieldValue("")) }
    BackHandler {
        if (field.text.isNotEmpty()) {
            field = TextFieldValue("")
            vm.onQueryChange("")
        } else {
            vm.reset()
            onClose()
        }
    }
    LaunchedEffect(Unit) { vm.onQueryChange("") } // ViewModel e campo começam sincronizados
    LaunchedEffect(autoFocusSearch) { if (autoFocusSearch) focus.requestFocus() }

    // Um único fundo translúcido (sem camadas extras). Com blur ligado, a Activity desfoca a janela (API 31+)
    // e o fundo fica mais transparente; no modo Ultra Performance usa fundo quase opaco.
    Box(
        modifier.fillMaxSize()
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = if (blur) 0.78f else 0.97f),
                RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            )
            .windowInsetsPadding(WindowInsets.systemBars)
            .imePadding(),
    ) {
        androidx.compose.foundation.layout.Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
            Box(
                Modifier.fillMaxWidth().clickable { vm.reset(); onClose() }.padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) { Text("⌄", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface) }

            OutlinedTextField(
                value = field,
                onValueChange = {
                    field = it
                    vm.onQueryChange(it.text)
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp), // campo em "pílula", como na tela de apps do One UI
                placeholder = { Text("Buscar apps, contatos, configurações…") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    state.apps.firstOrNull()?.let { if (vm.launch(it)) { vm.reset(); onClose() } }
                }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(selected = state.sort == AppSort.ALPHABETICAL, onClick = { vm.setSort(AppSort.ALPHABETICAL) }, label = { Text("A–Z") })
                FilterChip(selected = state.sort == AppSort.RECENTLY_INSTALLED, onClick = { vm.setSort(AppSort.RECENTLY_INSTALLED) }, label = { Text("Novos") })
            }
            if (state.hiddenAppsEnabled) {
                TextButton(onClick = {
                    if (state.hiddenUnlocked) vm.setUnlocked(false) else onRequestUnlock { vm.setUnlocked(true) }
                }) { Text(if (state.hiddenUnlocked) "🔓 Trancar ocultos" else "🔒 Mostrar ocultos") }
            }

            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(state.columns),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (state.extra.isNotEmpty()) {
                    items(state.extra, span = { GridItemSpan(maxLineSpan) }, contentType = { "extra" }) { r ->
                        ExtraRow(r, state.query, context) { vm.reset(); onClose() }
                    }
                }
                if (state.folders.isNotEmpty()) {
                    state.folders.forEach { folder ->
                        item(key = "h:${folder.category}", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                            Text(
                                categoryTitle(folder.category),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                            )
                        }
                        items(folder.apps, key = { "${folder.category}:${it.key}" }, contentType = { "app" }) { app ->
                            DrawerApp(app, state, vm, context, onClose)
                        }
                    }
                } else {
                    items(state.apps, key = { it.key }, contentType = { "app" }) { app ->
                        DrawerApp(app, state, vm, context, onClose)
                    }
                }
            }
        }
        if (showAlphabet) {
            AlphabetBar(
                letters = firstIndexByLetter.keys.toList(),
                onSelect = { letter -> firstIndexByLetter[letter]?.let { scope.launch { gridState.scrollToItem(it) } } },
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(ALPHABET_HEIGHT).width(22.dp),
            )
        }
    }
}

@Composable
private fun DrawerApp(app: AppInfo, state: DrawerUiState, vm: DrawerViewModel, context: Context, onClose: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val shortcuts by produceState(emptyList<AppShortcut>(), menu) { if (menu) value = vm.shortcuts(app) }
    Box(contentAlignment = Alignment.Center) {
        AppTile(
            app = app, iconSize = 52.dp, showLabel = true,
            onClick = { if (vm.launch(app)) { vm.reset(); onClose() } },
            onLongClick = { menu = true },
            labelColor = MaterialTheme.colorScheme.onSurface,
        )
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            shortcuts.forEach { s ->
                DropdownMenuItem(text = { Text(s.label) }, onClick = { menu = false; vm.launchShortcut(s); vm.reset(); onClose() })
            }
            DropdownMenuItem(text = { Text("Adicionar à home") }, onClick = { menu = false; vm.addToHome(app) })
            DropdownMenuItem(text = { Text("Adicionar ao dock") }, onClick = { menu = false; vm.addToDock(app) })
            if (state.hiddenAppsEnabled) {
                val isHidden = app.key in state.hiddenKeys
                DropdownMenuItem(
                    text = { Text(if (isHidden) "Mostrar app" else "Ocultar app") },
                    onClick = { menu = false; vm.setHidden(app, !isHidden) },
                )
            }
            DropdownMenuItem(text = { Text("Info do app") }, onClick = {
                menu = false
                startSafely(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", app.packageName, null)))
            })
            DropdownMenuItem(text = { Text("Desinstalar") }, onClick = {
                menu = false
                startSafely(context, Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.packageName, null)))
            })
        }
    }
}

@Composable
private fun ExtraRow(result: SearchResult, query: String, context: Context, done: () -> Unit) {
    val (title, intent) = when (result) {
        is SearchResult.Contact -> "👤 ${result.name}" to
            Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.getLookupUri(result.id, result.lookupKey))
        is SearchResult.SettingsEntry -> "⚙ ${result.label}" to Intent(result.action)
        is SearchResult.Web -> "🌐 Pesquisar “${result.query}” na web" to
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(query)))
    }
    Row(Modifier.fillMaxWidth().clickable { startSafely(context, intent); done() }.padding(vertical = 12.dp, horizontal = 4.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun startSafely(context: Context, intent: Intent) {
    try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        // sem app para tratar a intent neste aparelho
    } catch (_: SecurityException) {
        // atalho protegido
    }
}

private fun categoryTitle(c: AppCategory): String = when (c) {
    AppCategory.GAME -> "Jogos"
    AppCategory.AUDIO -> "Música e áudio"
    AppCategory.VIDEO -> "Vídeo"
    AppCategory.IMAGE -> "Fotos"
    AppCategory.SOCIAL -> "Social"
    AppCategory.NEWS -> "Notícias"
    AppCategory.MAPS -> "Mapas"
    AppCategory.PRODUCTIVITY -> "Produtividade"
    AppCategory.UNDEFINED -> "Outros"
}

private const val ALPHABET_MIN_APPS = 30
private const val ALPHABET_HEIGHT = 0.7f

private fun initialOf(label: String): Char {
    val c = FuzzyMatcher.normalize(label).firstOrNull() ?: return '#'
    return if (c in 'a'..'z') c.uppercaseChar() else '#'
}

/** Índice A–Z na lateral: toque ou arraste para pular; vibra a cada letra nova. */
@Composable
private fun AlphabetBar(letters: List<Char>, onSelect: (Char) -> Unit, modifier: Modifier = Modifier) {
    if (letters.isEmpty()) return
    val haptic = LocalHapticFeedback.current
    var height by remember { mutableIntStateOf(1) }
    Column(
        modifier = modifier.onSizeChanged { height = it.height.coerceAtLeast(1) }.pointerInput(letters) {
            awaitEachGesture {
                var last = ' '
                fun pick(y: Float) {
                    val letter = letters[(y / height * letters.size).toInt().coerceIn(0, letters.lastIndex)]
                    if (letter != last) {
                        last = letter
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelect(letter)
                    }
                }
                pick(awaitFirstDown().position.y)
                do {
                    val event = awaitPointerEvent()
                    event.changes.firstOrNull()?.let {
                        it.consume()
                        pick(it.position.y)
                    }
                } while (event.changes.any { it.pressed })
            }
        },
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        letters.forEach { Text(it.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
    }
}
