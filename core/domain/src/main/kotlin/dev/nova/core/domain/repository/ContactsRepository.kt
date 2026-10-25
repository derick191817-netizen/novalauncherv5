package dev.nova.core.domain.repository

import dev.nova.core.domain.model.SearchResult

/** Busca de contatos (exige READ_CONTACTS; sem permissão devolve vazio). */
interface ContactsRepository {
    suspend fun search(query: String, limit: Int): List<SearchResult.Contact>
}
