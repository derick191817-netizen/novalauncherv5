package dev.nova.core.domain.repository

/** Backup/restore de preferências + layouts em JSON (arquivo local; o Drive entra pelo seletor de arquivos). */
interface BackupRepository {
    suspend fun export(): String

    /** @return false se o arquivo for inválido ou de versão incompatível. */
    suspend fun import(json: String): Boolean
}
