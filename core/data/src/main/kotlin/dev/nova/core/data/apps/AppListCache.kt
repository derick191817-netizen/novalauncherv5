package dev.nova.core.data.apps

import dev.nova.core.common.toEnumOrDefault
import dev.nova.core.domain.model.AppCategory
import dev.nova.core.domain.model.AppInfo
import java.io.File
import java.io.IOException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** (De)serialização da lista de apps em JSON. */
internal object AppListJson {
    fun encode(apps: List<AppInfo>): JSONArray = JSONArray().also { arr ->
        apps.forEach {
            arr.put(
                JSONObject()
                    .put("key", it.key).put("pkg", it.packageName).put("cls", it.className).put("label", it.label)
                    .put("user", it.userSerial).put("work", it.isWorkProfile).put("cat", it.category.name)
                    .put("installed", it.firstInstallTime).put("updated", it.lastUpdateTime),
            )
        }
    }

    fun decode(arr: JSONArray): List<AppInfo> = List(arr.length()) { i ->
        val o = arr.getJSONObject(i)
        AppInfo(
            key = o.getString("key"),
            packageName = o.getString("pkg"),
            className = o.getString("cls"),
            label = o.getString("label"),
            userSerial = o.getLong("user"),
            isWorkProfile = o.getBoolean("work"),
            category = o.optString("cat").toEnumOrDefault(AppCategory.UNDEFINED),
            firstInstallTime = o.optLong("installed"),
            lastUpdateTime = o.optLong("updated"),
        )
    }
}

/**
 * Última lista de apps em disco. Na abertura a gaveta mostra a lista da sessão anterior na hora (alguns ms)
 * enquanto o PackageManager monta a lista nova (stale-while-revalidate). Cache corrompido é só ignorado.
 */
internal class AppListCache(dir: File) {
    private val file = File(dir, "apps.json")

    fun read(): List<AppInfo>? = try {
        if (file.exists()) AppListJson.decode(JSONArray(file.readText())) else null
    } catch (_: JSONException) {
        null
    } catch (_: IOException) {
        null
    }

    fun write(apps: List<AppInfo>) {
        try {
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(AppListJson.encode(apps).toString())
            if (!tmp.renameTo(file)) tmp.delete()
        } catch (_: IOException) {
            // cache é só otimização
        }
    }
}
