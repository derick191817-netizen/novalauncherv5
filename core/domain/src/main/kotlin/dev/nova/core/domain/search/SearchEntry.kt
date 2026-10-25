package dev.nova.core.domain.search

/** Rótulo pré-normalizado, calculado uma vez por lista de apps (a busca não aloca por tecla). */
class SearchEntry(
    val normalized: String,
    val words: List<String>,
    val initials: String,
)
