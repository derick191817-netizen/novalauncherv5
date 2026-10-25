package dev.nova.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLayoutEditTest {
    private fun app(id: String, row: Int, col: Int, page: Int = 0) = HomeApp(id, page, row, col, "k$id")

    @Test fun canPlaceOnFreeCell() = assertTrue(HomeLayout(listOf(app("a", 0, 0))).canPlace("a", 0, 2, 3, 6, 5))

    @Test fun cannotPlaceOnOccupiedCell() {
        val l = HomeLayout(listOf(app("a", 0, 0), app("b", 1, 1)))
        assertFalse(l.canPlace("a", 0, 1, 1, 6, 5))
    }

    @Test fun cannotPlaceOutsideGrid() {
        val l = HomeLayout(listOf(app("a", 0, 0)))
        assertFalse(l.canPlace("a", 0, 6, 0, 6, 5))
        assertFalse(l.canPlace("a", 0, 0, -1, 6, 5))
    }

    @Test fun widgetMustFitItsSpan() {
        val w = HomeWidget("w", 0, 0, 0, 2, 2, listOf(1))
        assertFalse(HomeLayout(listOf(w)).canPlace("w", 0, 0, 4, 6, 5)) // 4 + 2 > 5 colunas
        assertTrue(HomeLayout(listOf(w)).canPlace("w", 0, 0, 1, 6, 5)) // sobrepor a si mesmo é permitido
    }

    @Test fun movedChangesOnlyPosition() {
        val moved = HomeLayout(listOf(app("a", 0, 0))).moved("a", 1, 2, 3).items.single() as HomeApp
        assertEquals(Triple(1, 2, 3), Triple(moved.page, moved.row, moved.column))
        assertEquals("ka", moved.appKey)
    }

    @Test fun reflowKeepsLayoutWhenEverythingFits() {
        val l = HomeLayout(listOf(app("a", 0, 0)))
        assertSame(l, l.reflow(6, 5))
    }

    @Test fun reflowPullsItemsBackInsideSmallerGrid() {
        val l = HomeLayout(listOf(app("a", 0, 0), app("b", 5, 4)))
        val r = l.reflow(3, 3)
        assertEquals(2, r.items.size)
        assertTrue(r.items.all { it.row < 3 && it.column < 3 })
        assertEquals(0, r.items.map { it.id }.toSet().size - 2) // ids preservados, sem perder item
    }

    @Test fun reflowShrinksOversizedWidget() {
        val w = HomeWidget("w", 0, 0, 0, 5, 4, listOf(1))
        val r = (HomeLayout(listOf(w)).reflow(3, 3).items.single() as HomeWidget)
        assertEquals(3, r.spanX)
        assertEquals(3, r.spanY)
    }
}
