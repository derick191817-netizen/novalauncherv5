package dev.nova.core.domain.repository

import dev.nova.core.domain.model.LauncherSettings
import kotlinx.coroutines.flow.Flow

/** Preferências persistidas. */
interface SettingsRepository {
    val settings: Flow<LauncherSettings>

    /** Aplica [transform] de forma atômica (read-modify-write) e persiste. */
    suspend fun update(transform: (LauncherSettings) -> LauncherSettings)
}
