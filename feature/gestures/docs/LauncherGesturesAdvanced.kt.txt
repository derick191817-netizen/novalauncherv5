package dev.nova.feature.gestures

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import dev.nova.core.domain.model.GestureAction
import dev.nova.core.domain.model.GestureSettings
import kotlin.math.abs

/**
 * Swipe vertical, pinch e swipe de borda. Observa na passada Initial SEM consumir eventos:
 * cliques em ícones, scroll do pager e a S Pen continuam funcionando normalmente.
 * Distância mínima de 72dp e velocidade mínima evitam disparos acidentais.
 */
@Composable
fun Modifier.launcherGestures(settings: GestureSettings, onAction: (GestureAction) -> Unit): Modifier {
    val current by rememberUpdatedState(onAction)
    return pointerInput(settings) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val start = down.position
            val startTime = down.uptimeMillis
            var last = start
            var lastTime = startTime
            var maxPointers = 1
            var firstSpread = 0f
            var lastSpread = 0f
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val pressed = event.changes.filter { it.pressed }
                if (pressed.isNotEmpty()) {
                    last = pressed[0].position
                    lastTime = pressed[0].uptimeMillis
                }
                if (pressed.size >= 2) {
                    maxPointers = maxOf(maxPointers, pressed.size)
                    lastSpread = (pressed[0].position - pressed[1].position).getDistance()
                    if (firstSpread == 0f) firstSpread = lastSpread
                }
            } while (event.changes.any { it.pressed })

            if (maxPointers >= 2) {
                if (firstSpread > 0f && lastSpread / firstSpread < PINCH_IN_RATIO) current(settings.pinch)
                return@awaitEachGesture
            }
            val swipeMin = SWIPE_MIN_DP.dp.toPx()
            val edge = EDGE_DP.dp.toPx()
            val dx = last.x - start.x
            val dy = last.y - start.y
            if (lastTime - startTime > MAX_SWIPE_MS) return@awaitEachGesture
            val horizontal = abs(dx) > swipeMin && abs(dx) > 2 * abs(dy)
            if (settings.edgeSwipeEnabled && horizontal) {
                val fromLeft = start.x < edge && dx > 0
                val fromRight = start.x > size.width - edge && dx < 0
                if (fromLeft || fromRight) current(GestureAction.OPEN_SEARCH)
                return@awaitEachGesture
            }
            if (abs(dy) > swipeMin && abs(dy) > 2 * abs(dx)) {
                current(if (dy < 0) settings.swipeUp else settings.swipeDown)
            }
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
private const val EDGE_DP = 20
private const val MAX_SWIPE_MS = 700L
private const val PINCH_IN_RATIO = 0.75f
