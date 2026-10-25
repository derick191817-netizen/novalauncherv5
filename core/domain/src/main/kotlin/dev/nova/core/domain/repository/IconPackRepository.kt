package dev.nova.core.domain.repository

import dev.nova.core.domain.model.IconPackInfo

/** Descobre icon packs instalados. */
interface IconPackRepository {
    suspend fun installedPacks(): List<IconPackInfo>
}
