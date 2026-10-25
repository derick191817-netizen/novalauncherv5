package dev.nova.core.domain.model

import androidx.compose.runtime.Immutable
import java.util.UUID

/** Posição em uma página da home. */
@Immutable
data class Cell(val page: Int, val row: Int, val column: Int)

/** Item posicionado na grade da home. */
@Immutable
sealed interface HomeItem {
    val id: String
    val page: Int
    val row: Int
    val column: Int
    val spanX: Int get() = 1
    val spanY: Int get() = 1
}

/** Atalho de app na home. */
@Immutable
data class HomeApp(
    override val id: String,
    override val page: Int,
    override val row: Int,
    override val column: Int,
    val appKey: String,
) : HomeItem

/** Pasta na home. */
@Immutable
data class HomeFolder(
    override val id: String,
    override val page: Int,
    override val row: Int,
    override val column: Int,
    val name: String,
    val appKeys: List<String>,
) : HomeItem

/** Widget na home; mais de um id = pilha (widget stack). */
@Immutable
data class HomeWidget(
    override val id: String,
    override val page: Int,
    override val row: Int,
    override val column: Int,
    override val spanX: Int,
    override val spanY: Int,
    val appWidgetIds: List<Int>,
) : HomeItem

/** Layout completo de um perfil: páginas da home + dock. */
@Immutable
data class HomeLayout(
    val items: List<HomeItem> = emptyList(),
    val dock: List<String> = emptyList(),
    val seeded: Boolean = false,
    /** Providers de widget a restaurar (vindos de um backup); o usuário religa um a um. */
    val pendingWidgets: List<String> = emptyList(),
) {
    /** Número de páginas ocupadas (mínimo 1). */
    val pageCount: Int get() = (items.maxOfOrNull { it.page } ?: 0) + 1

    /** Primeira posição livre para um bloco [spanX] x [spanY]; abre uma página nova se preciso. */
    fun findFreeCell(rows: Int, columns: Int, spanX: Int = 1, spanY: Int = 1): Cell {
        val sx = spanX.coerceIn(1, columns)
        val sy = spanY.coerceIn(1, rows)
        for (page in 0..pageCount) {
            val occupied = Array(rows) { BooleanArray(columns) }
            for (item in items) {
                if (item.page != page) continue
                for (r in item.row until minOf(rows, item.row + item.spanY)) {
                    for (c in item.column until minOf(columns, item.column + item.spanX)) occupied[r][c] = true
                }
            }
            for (r in 0..rows - sy) {
                for (c in 0..columns - sx) {
                    if (isFree(occupied, r, c, sx, sy)) return Cell(page, r, c)
                }
            }
        }
        return Cell(pageCount, 0, 0)
    }

    private fun isFree(occ: Array<BooleanArray>, r: Int, c: Int, sx: Int, sy: Int): Boolean {
        for (rr in r until r + sy) for (cc in c until c + sx) if (occ[rr][cc]) return false
        return true
    }

    /** true se o app já está solto na home ou dentro de uma pasta. */
    fun containsApp(appKey: String): Boolean = items.any {
        (it is HomeApp && it.appKey == appKey) || (it is HomeFolder && appKey in it.appKeys)
    }

    /** true se [itemId] cabe em ([page], [row], [column]) dentro da grade e sem sobrepor outro item. */
    fun canPlace(itemId: String, page: Int, row: Int, column: Int, rows: Int, columns: Int): Boolean {
        val item = items.firstOrNull { it.id == itemId } ?: return false
        if (row < 0 || column < 0 || row + item.spanY > rows || column + item.spanX > columns) return false
        return items.none { other ->
            other.id != itemId && other.page == page &&
                row < other.row + other.spanY && other.row < row + item.spanY &&
                column < other.column + other.spanX && other.column < column + item.spanX
        }
    }

    /** Cópia com [itemId] movido; não valida (use [canPlace]). */
    fun moved(itemId: String, page: Int, row: Int, column: Int): HomeLayout =
        replace(itemId) { relocate(it, page, row, column, it.spanX, it.spanY) }

    /**
     * Recoloca itens que ficaram fora da grade (ex.: usuário diminuiu linhas/colunas) no primeiro espaço livre.
     * Widgets maiores que a nova grade são reduzidos. Sem itens fora da grade devolve `this`.
     */
    fun reflow(rows: Int, columns: Int): HomeLayout {
        val (inside, outside) = items.partition { it.row + it.spanY <= rows && it.column + it.spanX <= columns }
        if (outside.isEmpty()) return this
        var result = copy(items = inside)
        for (item in outside) {
            val sx = item.spanX.coerceAtMost(columns)
            val sy = item.spanY.coerceAtMost(rows)
            val c = result.findFreeCell(rows, columns, sx, sy)
            result = result.withItem(relocate(item, c.page, c.row, c.column, sx, sy))
        }
        return result
    }

    private fun relocate(item: HomeItem, page: Int, row: Int, column: Int, sx: Int, sy: Int): HomeItem = when (item) {
        is HomeApp -> item.copy(page = page, row = row, column = column)
        is HomeFolder -> item.copy(page = page, row = row, column = column)
        is HomeWidget -> item.copy(page = page, row = row, column = column, spanX = sx, spanY = sy)
    }

    fun withItem(item: HomeItem): HomeLayout = copy(items = items + item)

    fun without(id: String): HomeLayout = copy(items = items.filterNot { it.id == id })

    /** Substitui (ou remove, se [transform] devolver null) o item [id]. */
    fun replace(id: String, transform: (HomeItem) -> HomeItem?): HomeLayout =
        copy(items = items.mapNotNull { if (it.id == id) transform(it) else it })

    companion object {
        /** Novo id de item. */
        fun newId(): String = UUID.randomUUID().toString()
    }
}
