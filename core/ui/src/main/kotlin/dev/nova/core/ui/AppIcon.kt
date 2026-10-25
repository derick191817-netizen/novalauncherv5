package dev.nova.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import dev.nova.core.domain.model.AppInfo

/**
 * Ícone de app. Primeiro tenta a memória (sem I/O => sem "pisca"); se faltar, carrega fora da main thread.
 * Bitmaps HARDWARE são desenhados direto pelo RenderThread.
 */
@Composable
fun AppIcon(app: AppInfo, size: Dp, modifier: Modifier = Modifier) {
    val repo = LocalIconRepository.current
    val style by repo.styleKey.collectAsState()
    val px = with(LocalDensity.current) { size.roundToPx() }
    var bitmap by remember(app.key, app.lastUpdateTime, px, style) { mutableStateOf(repo.peek(app, px)) }
    LaunchedEffect(app.key, app.lastUpdateTime, px, style) {
        if (bitmap == null && style != null) bitmap = repo.load(app, px)
    }
    val image = remember(bitmap) { bitmap?.asImageBitmap() }
    if (image != null) {
        Image(bitmap = image, contentDescription = null, modifier = modifier.size(size))
    } else {
        Spacer(modifier.size(size))
    }
}
