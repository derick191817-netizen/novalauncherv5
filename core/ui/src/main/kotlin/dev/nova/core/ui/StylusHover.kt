package dev.nova.core.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Detecta a S Pen pairando sobre o elemento (hover). Observa na passada Initial e NÃO consome eventos:
 * os botões da S Pen e as Air Actions nativas (tratadas pelo sistema) continuam funcionando.
 */
fun Modifier.stylusHover(onHover: (Boolean) -> Unit): Modifier = pointerInput(onHover) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull() ?: continue
            if (change.type != PointerType.Stylus) continue
            when (event.type) {
                PointerEventType.Enter -> onHover(true)
                PointerEventType.Exit -> onHover(false)
                else -> Unit
            }
        }
    }
}
