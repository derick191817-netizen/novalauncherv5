package dev.nova.core.domain.usecase

import dev.nova.core.domain.model.AppInfo
import dev.nova.core.domain.model.AppShortcut
import dev.nova.core.domain.model.IconPackInfo
import dev.nova.core.domain.repository.AppRepository
import dev.nova.core.domain.repository.BackupRepository
import dev.nova.core.domain.repository.HiddenAppsRepository
import dev.nova.core.domain.repository.IconPackRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Chaves dos apps ocultos. */
class ObserveHiddenKeysUseCase @Inject constructor(private val repo: HiddenAppsRepository) {
    operator fun invoke(): Flow<Set<String>> = repo.hiddenKeys
}

/** Atalhos (long-press) de um app. */
class GetAppShortcutsUseCase @Inject constructor(private val repo: AppRepository) {
    suspend operator fun invoke(app: AppInfo): List<AppShortcut> = repo.shortcuts(app)
}

/** Abre um atalho. */
class LaunchShortcutUseCase @Inject constructor(private val repo: AppRepository) {
    operator fun invoke(shortcut: AppShortcut): Boolean = repo.launchShortcut(shortcut)
}

/** Gera o JSON de backup. */
class ExportBackupUseCase @Inject constructor(private val repo: BackupRepository) {
    suspend operator fun invoke(): String = repo.export()
}

/** Restaura um backup JSON. */
class ImportBackupUseCase @Inject constructor(private val repo: BackupRepository) {
    suspend operator fun invoke(json: String): Boolean = repo.import(json)
}

/** Lista icon packs instalados. */
class ListIconPacksUseCase @Inject constructor(private val repo: IconPackRepository) {
    suspend operator fun invoke(): List<IconPackInfo> = repo.installedPacks()
}
