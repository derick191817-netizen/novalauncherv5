package dev.nova.core.domain.usecase

import dev.nova.core.domain.model.LauncherSettings
import dev.nova.core.domain.repository.HiddenAppsRepository
import dev.nova.core.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Observa as preferências. */
class ObserveSettingsUseCase @Inject constructor(private val repo: SettingsRepository) {
    operator fun invoke(): Flow<LauncherSettings> = repo.settings
}

/** Altera as preferências de forma atômica. */
class UpdateSettingsUseCase @Inject constructor(private val repo: SettingsRepository) {
    suspend operator fun invoke(transform: (LauncherSettings) -> LauncherSettings) = repo.update(transform)
}

/** Oculta/exibe um app. */
class SetAppHiddenUseCase @Inject constructor(private val repo: HiddenAppsRepository) {
    suspend operator fun invoke(appKey: String, hidden: Boolean) = repo.setHidden(appKey, hidden)
}
