package dev.nova.core.domain.usecase

import dev.nova.core.common.AppDispatchers
import dev.nova.core.domain.model.FeatureFlags
import dev.nova.core.domain.model.SearchResult
import dev.nova.core.domain.repository.ContactsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UniversalSearchUseCaseTest {
    private class Dispatchers(d: CoroutineDispatcher) : AppDispatchers {
        override val main = d
        override val default = d
        override val io = d
    }

    private class FakeContacts : ContactsRepository {
        var calls = 0
        override suspend fun search(query: String, limit: Int): List<SearchResult.Contact> {
            calls++
            return listOf(SearchResult.Contact(1, "k", "Maria"))
        }
    }

    @Test fun shortQueryReturnsNothing() = runTest {
        val uc = UniversalSearchUseCase(FakeContacts(), Dispatchers(StandardTestDispatcher(testScheduler)))
        assertTrue(uc("a", FeatureFlags(searchWeb = true)).isEmpty())
    }

    @Test fun allSourcesOffReturnsNothingAndSkipsContacts() = runTest {
        val contacts = FakeContacts()
        val uc = UniversalSearchUseCase(contacts, Dispatchers(StandardTestDispatcher(testScheduler)))
        assertTrue(uc("wifi", FeatureFlags()).isEmpty())
        assertEquals(0, contacts.calls)
    }

    @Test fun settingsMatchIsAccentAndTypoTolerant() = runTest {
        val uc = UniversalSearchUseCase(FakeContacts(), Dispatchers(StandardTestDispatcher(testScheduler)))
        val result = uc("bateria", FeatureFlags(searchSettings = true))
        assertTrue(result.filterIsInstance<SearchResult.SettingsEntry>().any { it.label == "Economia de bateria" })
    }

    @Test fun webAndContactsAreAppendedWhenEnabled() = runTest {
        val uc = UniversalSearchUseCase(FakeContacts(), Dispatchers(StandardTestDispatcher(testScheduler)))
        val result = uc("maria", FeatureFlags(searchContacts = true, searchWeb = true))
        assertTrue(result.any { it is SearchResult.Contact })
        assertEquals(SearchResult.Web("maria"), result.last())
    }
}
