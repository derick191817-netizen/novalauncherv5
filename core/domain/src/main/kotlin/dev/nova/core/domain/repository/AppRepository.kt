package dev.nova.core.domain.repository

import dev.nova.core.domain.model.AppInfo
import dev.nova.core.domain.model.AppShortcut
import kotlinx.coroutines.flow.Flow

/** Fonte dos apps instalados. */
interface AppRepository {
    /**
     * Lista ordenada por nome. Atualiza sozinha em instalação/remoção/atualização.
     * O listener do sistema só existe enquanto houver coletor (lifecycle-aware).
     */
    val apps: Flow<List<AppInfo>>

    /** Abre o app no perfil correto. Retorna false se não foi possível abrir. */
    fun launch(app: AppInfo): Boolean

    /** Atalhos do app (vazio se o launcher não for o padrão ou o perfil estiver bloqueado). */
    suspend fun shortcuts(app: AppInfo): List<AppShortcut>

    /** Abre um atalho. */
    fun launchShortcut(shortcut: AppShortcut): Boolean
}
