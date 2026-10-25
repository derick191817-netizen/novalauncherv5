package dev.nova.core.domain.repository

import kotlinx.coroutines.flow.Flow

/** Lista de apps ocultos (armazenada criptografada). A autenticação é feita pela UI (biometria/senha). */
interface HiddenAppsRepository {
    val hiddenKeys: Flow<Set<String>>

    suspend fun setHidden(appKey: String, hidden: Boolean)
}
