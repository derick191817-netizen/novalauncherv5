package dev.nova.core.domain.repository

import android.graphics.Bitmap
import dev.nova.core.domain.model.AppInfo
import kotlinx.coroutines.flow.StateFlow

/**
 * Ícones com cache em memória (LRU) + disco.
 * Usa [Bitmap] por pragmatismo: é o tipo que o Compose/RenderThread consome sem cópia.
 */
interface IconRepository {
    /** Identifica o "estilo" atual (icon pack); null enquanto carrega. Muda => a UI deve recarregar. */
    val styleKey: StateFlow<String?>

    /** Consulta síncrona só na memória (sem I/O): use para evitar "pisca" no primeiro frame. */
    fun peek(app: AppInfo, sizePx: Int): Bitmap?

    /** Memória → disco → renderização. Chamadas simultâneas para o mesmo ícone são unificadas. */
    suspend fun load(app: AppInfo, sizePx: Int): Bitmap?

    /** Repasse de `onTrimMemory` do Application. */
    fun onTrimMemory(level: Int)
}
