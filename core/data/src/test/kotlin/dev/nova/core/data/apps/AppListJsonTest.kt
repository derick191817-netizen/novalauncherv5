package dev.nova.core.data.apps

import dev.nova.core.domain.model.AppCategory
import dev.nova.core.domain.model.AppInfo
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Test

class AppListJsonTest {
    private val apps = listOf(
        AppInfo("p/C#0", "p", "C", "Câmera", 0L, false, AppCategory.IMAGE, 100L, 200L),
        AppInfo("q/D#10", "q", "D", "Trabalho", 10L, true, AppCategory.UNDEFINED, 1L, 2L),
    )

    @Test fun roundTrip() = assertEquals(apps, AppListJson.decode(AppListJson.encode(apps)))

    @Test fun unknownCategoryFallsBack() {
        val arr = JSONArray(
            """[{"key":"k","pkg":"p","cls":"c","label":"L","user":0,"work":false,"cat":"NOVA_CATEGORIA","installed":1,"updated":2}]""",
        )
        assertEquals(AppCategory.UNDEFINED, AppListJson.decode(arr).single().category)
    }
}
