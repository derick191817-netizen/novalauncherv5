package dev.nova.core.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.nova.core.domain.model.AppInfo

/**
 * Ícone + rótulo. Com a S Pen pairando o ícone cresce e o rótulo aparece, mesmo no modo "sem rótulo".
 * Respeita [LocalPerformance] (sem animação no modo Ultra Performance) e descreve o app para o TalkBack.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppTile(
    app: AppInfo,
    iconSize: Dp,
    showLabel: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    labelColor: Color = Color.White,
) {
    var hovered by remember { mutableStateOf(false) }
    val animations = LocalPerformance.current.animations
    val haptic = LocalHapticFeedback.current
    // Estilo calculado uma vez: copy() a cada recomposição geraria lixo para o GC durante o scroll.
    val baseStyle = MaterialTheme.typography.labelSmall
    val labelStyle = remember(baseStyle) { baseStyle.copy(shadow = Shadow(Color.Black, blurRadius = 4f)) }
    val scale by animateFloatAsState(
        targetValue = if (hovered) HOVER_SCALE else 1f,
        animationSpec = if (animations) spring<Float>() else snap<Float>(),
        label = "stylusHover",
    )
    Column(
        modifier = modifier
            .stylusHover { hovered = it }
            .semantics(mergeDescendants = true) { contentDescription = app.label }
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            )
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AppIcon(app, iconSize, Modifier.graphicsLayer { scaleX = scale; scaleY = scale })
        if (showLabel || hovered) {
            Text(
                text = app.label,
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = labelStyle,
            )
        }
    }
}

private const val HOVER_SCALE = 1.15f
