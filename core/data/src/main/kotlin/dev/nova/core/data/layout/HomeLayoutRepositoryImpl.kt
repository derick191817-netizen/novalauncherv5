package dev.nova.core.data.layout

import android.appwidget.AppWidgetManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.nova.core.common.AppDispatchers
import dev.nova.core.domain.model.HomeLayout
import dev.nova.core.domain.model.HomeWidget
import dev.nova.core.domain.model.UsageProfile
import dev.nova.core.domain.repository.HomeLayoutRepository
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

/** Layout em `filesDir/layouts/<perfil>.json`, escrita atômica (tmp + rename), leitura única e preguiçosa. */
@Singleton
class HomeLayoutRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val dispatchers: AppDispatchers,
) : HomeLayoutRepository {

    private val dir = File(context.filesDir, "layouts")
    private val widgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)
    private val state = MutableStateFlow<Map<UsageProfile, HomeLayout>?>(null)
    private val mutex = Mutex()

    override fun observe(profile: UsageProfile): Flow<HomeLayout> =
        state.onStart { mutex.withLock { ensureLoaded() } }
            .filterNotNull()
            .map { it[profile] ?: HomeLayout() }
            .distinctUntilChanged()

    override suspend fun update(profile: UsageProfile, transform: (HomeLayout) -> HomeLayout) {
        mutex.withLock {
            ensureLoaded()
            val current = state.value.orEmpty()
            val previous = current[profile] ?: HomeLayout()
            val next = transform(previous)
            if (next == previous) return@withLock // sem mudança: não reescreve o arquivo
            state.value = current + (profile to next)
            withContext(dispatchers.io) { write(profile, next) }
        }
    }

    override suspend fun exportJson(): String = mutex.withLock {
        ensureLoaded()
        val root = JSONObject()
        state.value.orEmpty().forEach { (profile, layout) -> root.put(profile.name, HomeLayoutJson.encode(layout, widgetProviders(layout))) }
        root.toString()
    }

    override suspend fun importJson(json: String) {
        val root = try { JSONObject(json) } catch (_: JSONException) { return }
        for (profile in UsageProfile.entries) {
            val obj = root.optJSONObject(profile.name) ?: continue
            val layout = HomeLayoutJson.decode(obj, keepWidgets = false).copy(seeded = true)
            update(profile) { layout }
        }
    }

    private suspend fun ensureLoaded() {
        if (state.value != null) return
        val loaded = withContext(dispatchers.io) {
            UsageProfile.entries.associateWith { profile ->
                val file = fileFor(profile)
                try {
                    if (file.exists()) HomeLayoutJson.decode(JSONObject(file.readText()), keepWidgets = true) else HomeLayout()
                } catch (_: JSONException) {
                    HomeLayout()
                } catch (_: IOException) {
                    HomeLayout()
                }
            }
        }
        state.value = loaded
    }

    private fun write(profile: UsageProfile, layout: HomeLayout) {
        try {
            dir.mkdirs()
            val target = fileFor(profile)
            val tmp = File(dir, target.name + ".tmp")
            tmp.writeText(HomeLayoutJson.encode(layout).toString())
            if (!tmp.renameTo(target)) tmp.delete()
        } catch (_: IOException) {
            // Falha de disco: o estado em memória continua válido nesta sessão.
        }
    }

    // Ids de widget são locais ao aparelho; no backup guardamos o provider para o usuário religar depois.
    private fun widgetProviders(layout: HomeLayout): List<String> =
        layout.items.filterIsInstance<HomeWidget>().flatMap { it.appWidgetIds }
            .mapNotNull { widgetManager.getAppWidgetInfo(it)?.provider?.flattenToString() }

    private fun fileFor(profile: UsageProfile) = File(dir, profile.name.lowercase() + ".json")
}
