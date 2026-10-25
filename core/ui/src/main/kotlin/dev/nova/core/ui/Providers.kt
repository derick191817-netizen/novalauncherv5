package dev.nova.core.ui

import androidx.compose.runtime.staticCompositionLocalOf
import dev.nova.core.domain.model.PerformanceMode
import dev.nova.core.domain.repository.IconRepository

/** Repositório de ícones disponível para toda a árvore Compose. */
val LocalIconRepository = staticCompositionLocalOf<IconRepository> {
    error("LocalIconRepository não fornecido: envolva a UI em CompositionLocalProvider")
}

/** Política de desempenho atual (animações, blur, widgets pesados). */
val LocalPerformance = staticCompositionLocalOf { PerformanceMode.NORMAL }
