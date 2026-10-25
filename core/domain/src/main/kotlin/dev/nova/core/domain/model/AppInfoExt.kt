package dev.nova.core.domain.model

/** Chave de cache de ícone: muda quando o app é atualizado (mtime do APK), invalidando o ícone antigo. */
val AppInfo.iconCacheKey: String get() = "$key@$lastUpdateTime"
