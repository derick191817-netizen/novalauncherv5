package dev.nova.core.data.icons

import android.app.ActivityManager
import android.content.Context
import android.content.pm.LauncherApps
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.UserManager
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.nova.core.common.AppDispatchers
import dev.nova.core.common.ApplicationScope
import dev.nova.core.domain.model.AppInfo
import dev.nova.core.domain.model.iconCacheKey
import dev.nova.core.domain.repository.IconRepository
import dev.nova.core.domain.repository.SettingsRepository
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Pipeline de ícones: memória (LRU, HARDWARE) → disco (WebP lossless) → renderização.
 *
 * Otimizações:
 * - Orçamento de memória proporcional ao aparelho (menor em low-RAM).
 * - Requisições duplicadas do mesmo ícone compartilham um único trabalho ([inFlight]).
 * - Paralelismo limitado a 3 threads: renderizar ícones não compete com a thread de UI/RenderThread.
 * - A chave inclui tamanho, mtime do APK e icon pack: atualizar o app ou trocar o pack invalida sozinho.
 */
@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class IconRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    launcherApps: LauncherApps,
    userManager: UserManager,
    settings: SettingsRepository,
    @ApplicationScope private val scope: CoroutineScope,
    dispatchers: AppDispatchers,
) : IconRepository {

    private val memory = MemoryIconCache(memoryBudgetBytes(context))
    private val disk = DiskIconCache(File(context.cacheDir, "icons"))
    private val renderer = IconRenderer(context, launcherApps, userManager, IconPackLoader(context))
    private val iconDispatcher = dispatchers.io.limitedParallelism(PARALLELISM)
    private val inFlight = ConcurrentHashMap<String, Deferred<Bitmap?>>()

    init {
        // Modo Economia (freezeOffscreenIcons): metade do cache de ícones em memória.
        scope.launch {
            settings.settings.map { it.performance.freezeOffscreenIcons }.distinctUntilChanged()
                .collect { memory.scale(if (it) HALF else 1f) }
        }
    }

    override val styleKey: StateFlow<String?> = settings.settings
        .map { IconStyle(it.iconPackPackage, it.features.themedIcons, it.iconShape).key }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, null)

    override fun peek(app: AppInfo, sizePx: Int): Bitmap? {
        val pack = styleKey.value ?: return null
        return memory.get(cacheKey(app, sizePx, pack))
    }

    override suspend fun load(app: AppInfo, sizePx: Int): Bitmap? {
        val pack = styleKey.filterNotNull().first()
        val key = cacheKey(app, sizePx, pack)
        memory.get(key)?.let { return it }
        val job = inFlight.computeIfAbsent(key) {
            // Escopo do app: se quem pediu cancelar (scroll rápido), o ícone ainda é concluído e cacheado.
            scope.async(iconDispatcher, start = CoroutineStart.LAZY) {
                try {
                    loadFromDiskOrRender(key, app, sizePx, pack)
                } finally {
                    inFlight.remove(key)
                }
            }
        }
        return job.await()
    }

    override fun onTrimMemory(level: Int) = memory.trim(level)

    private fun loadFromDiskOrRender(key: String, app: AppInfo, sizePx: Int, pack: String): Bitmap? {
        disk.read(key)?.let {
            memory.put(key, it)
            return it
        }
        val style = IconStyle.parse(pack)
        val rendered = renderer.render(app, sizePx, style.pack, style.themed, isNight(), style.shape)
            ?: return null
        disk.write(key, rendered)
        val hardware = rendered.copy(Bitmap.Config.HARDWARE, false)
        val result = if (hardware != null) {
            rendered.recycle()
            hardware
        } else {
            rendered
        }
        memory.put(key, result)
        return result
    }

    private fun cacheKey(app: AppInfo, sizePx: Int, pack: String): String =
        "${app.iconCacheKey}@$sizePx@$pack@${if (isNight()) "n" else "d"}"

    private fun isNight(): Boolean =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    private companion object {
        const val HALF = 0.5f
        const val PARALLELISM = 3
        const val MB = 1024 * 1024

        fun memoryBudgetBytes(context: Context): Int {
            val am = context.getSystemService(ActivityManager::class.java)
            val budgetMb = when {
                am == null -> 8
                am.isLowRamDevice -> 6
                else -> (am.memoryClass / 10).coerceIn(8, 24)
            }
            return budgetMb * MB
        }
    }
}
