package dev.nova.core.domain.usecase

import dev.nova.core.domain.model.AppInfo
import dev.nova.core.domain.repository.AppRepository
import javax.inject.Inject

/** Abre um app. */
class LaunchAppUseCase @Inject constructor(private val apps: AppRepository) {
    operator fun invoke(app: AppInfo): Boolean = apps.launch(app)
}
