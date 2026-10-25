package dev.nova.feature.widgets

import android.appwidget.AppWidgetHostView
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Exibe um widget do host. O tamanho é informado ao provider (widgets responsivos se adaptam). */
@Composable
fun WidgetView(controller: WidgetController, appWidgetId: Int, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier) {
        val w = maxWidth.value.toInt()
        val h = maxHeight.value.toInt()
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx -> controller.createView(ctx, appWidgetId) ?: View(ctx) },
            update = { (it as? AppWidgetHostView)?.updateAppWidgetSize(null, w, h, w, h) },
        )
    }
}

/** Widget simples ou pilha (swipe vertical entre widgets). */
@Composable
fun WidgetStackView(controller: WidgetController, appWidgetIds: List<Int>, modifier: Modifier = Modifier) {
    if (appWidgetIds.size <= 1) {
        appWidgetIds.firstOrNull()?.let { WidgetView(controller, it, modifier) }
        return
    }
    val pager = rememberPagerState { appWidgetIds.size }
    VerticalPager(state = pager, modifier = modifier) { page ->
        WidgetView(controller, appWidgetIds[page], Modifier.fillMaxSize())
    }
}

/** Seletor de widgets instalados. A lista é carregada em Dispatchers.IO. */
@Composable
fun WidgetPickerDialog(
    controller: WidgetController,
    onPick: (WidgetProviderUi) -> Unit,
    onDismiss: () -> Unit,
) {
    val providers by produceState<List<WidgetProviderUi>?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { controller.providers() }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(16.dp)) {
                Text("Adicionar widget", style = MaterialTheme.typography.titleMedium)
                val list = providers
                if (list == null) {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { Text("Carregando…") }
                } else {
                    LazyColumn(Modifier.heightIn(max = 420.dp)) {
                        items(list, key = { it.component.flattenToShortString() }) { p ->
                            Column(Modifier.fillMaxWidth().clickable { onPick(p) }.padding(vertical = 10.dp)) {
                                Text(p.label, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "${p.spanX}×${p.spanY} · ${p.component.packageName}",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
