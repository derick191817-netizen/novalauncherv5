package dev.nova.feature.gestures

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import dev.nova.core.domain.model.GestureAction
import dev.nova.core.domain.model.GestureSettings

/**
 * Gestos da home em versão simples e estável: deslizar para cima/baixo (distância mínima de 72dp).
 * A versão completa, com pinça e gesto de borda, está guardada em docs/LauncherGesturesAdvanced.kt.txt
 * (fora do build) e volta quando o build estiver verde. Os ajustes de pinça/borda ficam salvos mas ainda sem efeito.
 */
@Composable
fun Modifier.launcherGestures(settings: GestureSettings, onAction: (GestureAction) -> Unit): Modifier {
    val current by rememberUpdatedState(onAction)
    return pointerInput(settings) {
        val threshold = SWIPE_MIN_DP.dp.toPx()
        var total = 0f
        detectVerticalDragGestures(
            onDragStart = { total = 0f },
            onDragEnd = {
                when {
                    total <= -threshold -> current(settings.swipeUp)
                    total >= threshold -> current(settings.swipeDown)
                }
                total = 0f
            },
            onDragCancel = { total = 0f },
        ) { change, dragAmount ->
            change.consume()
            total += dragAmount
        }
    }
}

/** Camada de fundo: toque duplo (bloquear tela) e toque longo (modo de edição) em áreas vazias. */
@Composable
fun GestureBackground(onDoubleTap: () -> Unit, onLongPress: () -> Unit, modifier: Modifier = Modifier) {
    val doubleTap by rememberUpdatedState(onDoubleTap)
    val longPress by rememberUpdatedState(onLongPress)
    Box(
        modifier.pointerInput(Unit) {
            detectTapGestures(onDoubleTap = { doubleTap() }, onLongPress = { longPress() })
        },
    )
}

private const val SWIPE_MIN_DP = 72
