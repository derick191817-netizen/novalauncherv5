package dev.nova.core.data.layout

import dev.nova.core.domain.model.HomeApp
import dev.nova.core.domain.model.HomeFolder
import dev.nova.core.domain.model.HomeLayout
import dev.nova.core.domain.model.HomeWidget
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLayoutJsonTest {
    private val layout = HomeLayout(
        items = listOf(
            HomeApp("a", 0, 1, 2, "k1"),
            HomeFolder("f", 1, 0, 0, "Jogos", listOf("k2", "k3")),
            HomeWidget("w", 0, 3, 0, 4, 2, listOf(7, 8)),
        ),
        dock = listOf("d1", "d2"),
        seeded = true,
        pendingWidgets = listOf("x/Y"),
    )

    @Test fun roundTripKeepsEverything() {
        val back = HomeLayoutJson.decode(HomeLayoutJson.encode(layout), keepWidgets = true)
        assertEquals(layout, back)
    }

    @Test fun importDropsWidgetsButKeepsPending() {
        val back = HomeLayoutJson.decode(HomeLayoutJson.encode(layout, listOf("p/Q")), keepWidgets = false)
        assertTrue(back.items.none { it is HomeWidget })
        assertEquals(listOf("x/Y", "p/Q"), back.pendingWidgets)
    }

    @Test fun oldBackupWithoutPendingFieldStillLoads() {
        val old = JSONObject("""{"seeded":true,"dock":["d"],"items":[{"id":"a","page":0,"row":0,"col":0,"type":"app","appKey":"k"}]}""")
        val back = HomeLayoutJson.decode(old, keepWidgets = true)
        assertEquals(emptyList<String>(), back.pendingWidgets)
        assertEquals(1, back.items.size)
    }
}
