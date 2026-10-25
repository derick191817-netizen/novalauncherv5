package dev.nova.core.domain.model

import androidx.compose.runtime.Immutable

/** Resultado de busca universal que não é um app. */
@Immutable
sealed interface SearchResult {
    /** Contato da agenda. */
    @Immutable
    data class Contact(val id: Long, val lookupKey: String, val name: String) : SearchResult

    /** Atalho para uma tela de Configurações do sistema. */
    @Immutable
    data class SettingsEntry(val action: String, val label: String) : SearchResult

    /** Pesquisa na web. */
    @Immutable
    data class Web(val query: String) : SearchResult
}
