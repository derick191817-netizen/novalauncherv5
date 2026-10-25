package dev.nova.core.domain.repository

import dev.nova.core.domain.model.HomeLayout
import dev.nova.core.domain.model.UsageProfile
import kotlinx.coroutines.flow.Flow

/** Persistência do layout da home, um por [UsageProfile]. */
interface HomeLayoutRepository {
    fun observe(profile: UsageProfile): Flow<HomeLayout>

    /** Read-modify-write atômico. */
    suspend fun update(profile: UsageProfile, transform: (HomeLayout) -> HomeLayout)

    /** Todos os layouts em JSON (para backup). */
    suspend fun exportJson(): String

    /** Importa layouts de [json]; widgets são descartados (ids são locais ao aparelho). */
    suspend fun importJson(json: String)
}
