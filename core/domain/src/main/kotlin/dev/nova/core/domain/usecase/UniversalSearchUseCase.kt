package dev.nova.core.domain.usecase

import dev.nova.core.common.AppDispatchers
import dev.nova.core.domain.model.FeatureFlags
import dev.nova.core.domain.model.SearchResult
import dev.nova.core.domain.repository.ContactsRepository
import dev.nova.core.domain.search.FuzzyMatcher
import dev.nova.core.domain.search.SettingsCatalog
import javax.inject.Inject
import kotlinx.coroutines.withContext

/** Busca universal além dos apps: configurações, contatos e web (cada fonte é opcional). */
class UniversalSearchUseCase @Inject constructor(
    private val contacts: ContactsRepository,
    private val dispatchers: AppDispatchers,
) {
    suspend operator fun invoke(rawQuery: String, features: FeatureFlags): List<SearchResult> {
        val query = rawQuery.trim()
        if (query.length < MIN_QUERY) return emptyList()
        return withContext(dispatchers.default) {
            buildList {
                if (features.searchSettings) {
                    val q = FuzzyMatcher.normalize(query)
                    SettingsCatalog.items.filter { FuzzyMatcher.score(q, it.entry) > 0 }.take(MAX_SETTINGS)
                        .forEach { add(SearchResult.SettingsEntry(it.action, it.label)) }
                }
                if (features.searchContacts) addAll(contacts.search(query, MAX_CONTACTS))
                if (features.searchWeb) add(SearchResult.Web(query))
            }
        }
    }

    private companion object {
        const val MIN_QUERY = 2
        const val MAX_SETTINGS = 4
        const val MAX_CONTACTS = 5
    }
}
