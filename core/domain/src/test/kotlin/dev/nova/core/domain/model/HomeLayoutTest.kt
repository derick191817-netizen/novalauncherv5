package dev.nova.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLayoutTest {
    private fun app(id: String, page: Int, row: Int, col: Int) = HomeApp(id, page, row, col, "k$id")

    @Test fun emptyLayoutStartsAtOrigin() = assertEquals(Cell(0, 0, 0), HomeLayout().findFreeCell(6, 5))

    @Test fun skipsOccupiedCells() {
        val l = HomeLayout(listOf(app("a", 0, 0, 0), app("b", 0, 0, 1)))
        assertEquals(Cell(0, 0, 2), l.findFreeCell(6, 5))
    }

    @Test fun widgetBlocksItsSpan() {
        val w = HomeWidget("w", 0, 0, 0, spanX = 5, spanY = 2, appWidgetIds = listOf(1))
        assertEquals(Cell(0, 2, 0), HomeLayout(listOf(w)).findFreeCell(6, 5))
    }

    @Test fun opensNewPageWhenFull() {
        val full = (0 until 2).flatMap { r -> (0 until 2).map { c -> app("$r$c", 0, r, c) } }
        assertEquals(1, HomeLayout(full).findFreeCell(2, 2).page)
    }

    @Test fun containsAppSeesFolders() {
        val l = HomeLayout(listOf(HomeFolder("f", 0, 0, 0, "Pasta", listOf("x"))))
        assertTrue(l.containsApp("x"))
    }
}
