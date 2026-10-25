package dev.nova.core.domain.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FuzzyMatcherTest {
    private fun score(q: String, label: String) = FuzzyMatcher.score(FuzzyMatcher.normalize(q), FuzzyMatcher.entryFor(label))

    @Test fun normalizeRemovesAccentsAndCase() = assertEquals("camera", FuzzyMatcher.normalize("  Câmera "))

    @Test fun exactBeatsPrefixBeatsContains() {
        assertTrue(score("maps", "Maps") > score("map", "Maps"))
        assertTrue(score("map", "Maps") > score("map", "Google Maps"))
        assertTrue(score("map", "Google Maps") > score("oogle", "Google Maps"))
    }

    @Test fun initialsMatch() = assertTrue(score("gm", "Google Maps") >= 700)

    @Test fun typoToleranceWorks() {
        assertTrue(score("gogle", "Google") > 0)
        assertTrue(score("youtub", "YouTube") > 0)
        assertTrue(score("calculater", "Calculator") > 0)
    }

    @Test fun unrelatedDoesNotMatch() = assertEquals(0, score("zzzz", "Google Maps"))

    @Test fun editDistanceHandlesTransposition() = assertEquals(1, FuzzyMatcher.editDistance("gogole", "google", 2))
}
