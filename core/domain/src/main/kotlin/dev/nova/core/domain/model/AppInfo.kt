package dev.nova.core.domain.model

import androidx.compose.runtime.Immutable

/**
 * App lançável (uma atividade LAUNCHER de um perfil).
 *
 * @property key identificador estável e único: `pacote/classe#serialDoPerfil`. Use como key em LazyGrid.
 * @property userSerial serial do perfil de usuário (principal, Work, clone).
 * @property isWorkProfile true se não pertence ao perfil principal (recebe selo no ícone).
 * @property lastUpdateTime mtime do APK; invalida o cache de ícones quando o app é atualizado.
 */
@Immutable
data class AppInfo(
    val key: String,
    val packageName: String,
    val className: String,
    val label: String,
    val userSerial: Long,
    val isWorkProfile: Boolean,
    val category: AppCategory,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
)
