package dev.nova.core.data.layout

import dev.nova.core.domain.model.HomeApp
import dev.nova.core.domain.model.HomeFolder
import dev.nova.core.domain.model.HomeItem
import dev.nova.core.domain.model.HomeLayout
import dev.nova.core.domain.model.HomeWidget
import org.json.JSONArray
import org.json.JSONObject

/** (De)serialização do layout em JSON (org.json do Android: zero dependência extra). */
internal object HomeLayoutJson {
    fun encode(layout: HomeLayout, extraPending: List<String> = emptyList()): JSONObject = JSONObject().apply {
        put("seeded", layout.seeded)
        put("dock", JSONArray(layout.dock))
        put("pendingWidgets", JSONArray((layout.pendingWidgets + extraPending).distinct()))
        put("items", JSONArray().also { arr -> layout.items.forEach { arr.put(encodeItem(it)) } })
    }

    private fun encodeItem(item: HomeItem): JSONObject = JSONObject().apply {
        put("id", item.id)
        put("page", item.page)
        put("row", item.row)
        put("col", item.column)
        when (item) {
            is HomeApp -> { put("type", "app"); put("appKey", item.appKey) }
            is HomeFolder -> { put("type", "folder"); put("name", item.name); put("apps", JSONArray(item.appKeys)) }
            is HomeWidget -> {
                put("type", "widget")
                put("sx", item.spanX)
                put("sy", item.spanY)
                put("ids", JSONArray(item.appWidgetIds))
            }
        }
    }

    fun decode(obj: JSONObject, keepWidgets: Boolean): HomeLayout {
        val items = ArrayList<HomeItem>()
        val arr = obj.optJSONArray("items") ?: JSONArray()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val id = o.optString("id").ifEmpty { HomeLayout.newId() }
            val page = o.optInt("page")
            val row = o.optInt("row")
            val col = o.optInt("col")
            when (o.optString("type")) {
                "app" -> items += HomeApp(id, page, row, col, o.optString("appKey"))
                "folder" -> items += HomeFolder(id, page, row, col, o.optString("name", "Pasta"), strings(o.optJSONArray("apps")))
                "widget" -> if (keepWidgets) {
                    items += HomeWidget(id, page, row, col, o.optInt("sx", 1), o.optInt("sy", 1), ints(o.optJSONArray("ids")))
                }
            }
        }
        return HomeLayout(
            items,
            strings(obj.optJSONArray("dock")),
            obj.optBoolean("seeded"),
            strings(obj.optJSONArray("pendingWidgets")), // ausente em backups antigos => vazio
        )
    }

    private fun strings(a: JSONArray?): List<String> = if (a == null) emptyList() else List(a.length()) { a.getString(it) }

    private fun ints(a: JSONArray?): List<Int> = if (a == null) emptyList() else List(a.length()) { a.getInt(it) }
}
