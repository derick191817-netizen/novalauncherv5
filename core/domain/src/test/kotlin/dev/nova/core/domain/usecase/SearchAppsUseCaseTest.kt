package dev.nova.core.domain.usecase

import dev.nova.core.common.AppDispatchers
import dev.nova.core.domain.model.AppCategory
import dev.nova.core.domain.model.AppInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchAppsUseCaseTest {
    private class TestDispatchers(d: CoroutineDispatcher) : AppDispatchers {
        override val main = d
        override val default = d
        override val io = d
    }

    private fun app(label: String) =
        AppInfo(label, "pkg.$label", "C", label, 0L, false, AppCategory.UNDEFINED, 0L, 0L)

    private val apps = listOf("Gmail", "Google Maps", "Câmera", "Calculadora", "Chrome").map(::app)

    @Test fun emptyQueryReturnsAll() = runTest {
        val useCase = SearchAppsUseCase(TestDispatchers(StandardTestDispatcher(testScheduler)))
        assertEquals(apps, useCase(apps, "  "))
    }

    @Test fun accentInsensitive() = runTest {
        val useCase = SearchAppsUseCase(TestDispatchers(StandardTestDispatcher(testScheduler)))
        assertEquals("Câmera", useCase(apps, "camera").first().label)
    }

    @Test fun prefixRanksBeforeInitials() = runTest {
        val useCase = SearchAppsUseCase(TestDispatchers(StandardTestDispatcher(testScheduler)))
        val result = useCase(apps, "gm").map { it.label }
        assertEquals("Gmail", result.first())
        assertTrue("Google Maps" in result)
    }

    @Test fun noMatchReturnsEmpty() = runTest {
        val useCase = SearchAppsUseCase(TestDispatchers(StandardTestDispatcher(testScheduler)))
        assertTrue(useCase(apps, "zzzz").isEmpty())
    }
}
