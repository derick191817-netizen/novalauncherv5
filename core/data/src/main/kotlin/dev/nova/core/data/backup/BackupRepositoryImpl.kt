package dev.nova.core.data.backup

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.nova.core.domain.repository.BackupRepository
import dev.nova.core.domain.repository.HomeLayoutRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import org.json.JSONException
import org.json.JSONObject

/** Backup em um único JSON: `{version, settings{...}, layouts{perfil: layout}}`. */
@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val store: DataStore<Preferences>,
    private val layouts: HomeLayoutRepository,
) : BackupRepository {

    override suspend fun export(): String {
        val settings = JSONObject()
        for ((key, value) in store.data.first().asMap()) {
            when (value) {
                is Boolean, is Int, is String -> settings.put(key.name, value)
                else -> Unit
            }
        }
        return JSONObject()
            .put("version", VERSION)
            .put("settings", settings)
            .put("layouts", JSONObject(layouts.exportJson()))
            .toString(2)
    }

    override suspend fun import(json: String): Boolean {
        val root = try { JSONObject(json) } catch (_: JSONException) { return false }
        if (root.optInt("version", -1) != VERSION) return false
        val settings = root.optJSONObject("settings") ?: JSONObject()
        store.edit { prefs ->
            for (name in settings.keys()) {
                when (val v = settings.get(name)) {
                    is Boolean -> prefs[booleanPreferencesKey(name)] = v
                    is Int -> prefs[intPreferencesKey(name)] = v
                    is String -> prefs[stringPreferencesKey(name)] = v
                    else -> Unit
                }
            }
        }
        layouts.importJson((root.optJSONObject("layouts") ?: JSONObject()).toString())
        return true // valores fora do intervalo são corrigidos por LauncherSettings.sanitized() na leitura
    }

    private companion object {
        const val VERSION = 1
    }
}
