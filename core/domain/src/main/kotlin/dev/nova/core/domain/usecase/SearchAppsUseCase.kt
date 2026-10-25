package dev.nova.core.domain.usecase

import dev.nova.core.common.AppDispatchers
import dev.nova.core.domain.model.AppInfo
import dev.nova.core.domain.search.FuzzyMatcher
import dev.nova.core.domain.search.SearchEntry
import javax.inject.Inject
import kotlinx.coroutines.withContext

/** Busca fuzzy de apps. Roda em Dispatchers.Default; o índice é reaproveitado enquanto a lista não mudar. */
class SearchAppsUseCase @Inject constructor(
    private val dispatchers: AppDispatchers,
) {
    private class Index(val source: List<AppInfo>, val entries: List<SearchEntry>)

    @Volatile
    private var index: Index? = null

    /** @return apps que casam com [rawQuery], melhores primeiro; lista completa se a busca estiver vazia. */
    suspend operator fun invoke(apps: List<AppInfo>, rawQuery: String, limit: Int = 60): List<AppInfo> {
        val query = FuzzyMatcher.normalize(rawQuery)
        if (query.isEmpty()) return apps
        return withContext(dispatchers.default) {
            val entries = entriesFor(apps)
            val scores = IntArray(apps.size) { FuzzyMatcher.score(query, entries[it]) }
            apps.indices
                .filter { scores[it] > 0 }
                .sortedWith(compareByDescending<Int> { scores[it] }.thenBy { apps[it].label })
                .take(limit)
                .map { apps[it] }
        }
    }

    private fun entriesFor(apps: List<AppInfo>): List<SearchEntry> {
        val cached = index
        if (cached != null && cached.source === apps) return cached.entries
        val entries = apps.map { FuzzyMatcher.entryFor(it.label) }
        index = Index(apps, entries)
        return entries
    }
}
