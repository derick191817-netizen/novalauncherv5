package dev.nova.core.data.apps

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.nova.core.common.AppDispatchers
import dev.nova.core.domain.model.AppCategory
import dev.nova.core.domain.model.AppInfo
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * Lê os apps via [LauncherApps] (API oficial para launchers, sem QUERY_ALL_PACKAGES) e observa mudanças.
 */
@Singleton
class LauncherAppsDataSource @Inject constructor(
    @ApplicationContext context: Context,
    private val launcherApps: LauncherApps,
    private val userManager: UserManager,
    private val dispatchers: AppDispatchers,
) {
    private val cache = AppListCache(context.cacheDir)

    /** Leitura bloqueante (chamar fora da main). */
    fun load(): List<AppInfo> {
        val me = Process.myUserHandle()
        val result = ArrayList<AppInfo>(256)
        for (user in userManager.userProfiles) {
            val serial = userManager.getSerialNumberForUser(user)
            for (info in launcherApps.getActivityList(null, user)) {
                val component = info.componentName
                val appInfo = info.applicationInfo
                result += AppInfo(
                    key = "${component.flattenToShortString()}#$serial",
                    packageName = component.packageName,
                    className = component.className,
                    label = info.label.toString(),
                    userSerial = serial,
                    isWorkProfile = user != me,
                    category = mapCategory(appInfo.category),
                    firstInstallTime = info.firstInstallTime,
                    // stat() no APK: ~100x mais barato que PackageManager.getPackageInfo por app.
                    lastUpdateTime = File(appInfo.sourceDir).lastModified(),
                )
            }
        }
        result.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
        return result
    }

    /**
     * Emite a lista agora e a cada mudança de pacotes. O callback é registrado quando o fluxo começa a ser
     * coletado e removido em `awaitClose`: nenhum listener fica ativo sem coletor (lifecycle-aware).
     */
    @OptIn(FlowPreview::class)
    fun observe(): Flow<List<AppInfo>> = callbackFlow<Boolean> {
        val callback = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String?, user: UserHandle?) { trySend(false) }
            override fun onPackageAdded(packageName: String?, user: UserHandle?) { trySend(false) }
            override fun onPackageChanged(packageName: String?, user: UserHandle?) { trySend(false) }
            override fun onPackagesAvailable(p: Array<out String>?, user: UserHandle?, replacing: Boolean) {
                trySend(false)
            }
            override fun onPackagesUnavailable(p: Array<out String>?, user: UserHandle?, replacing: Boolean) {
                trySend(false)
            }
            override fun onPackagesSuspended(p: Array<out String>?, user: UserHandle?) { trySend(false) }
            override fun onPackagesUnsuspended(p: Array<out String>?, user: UserHandle?) { trySend(false) }
        }
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        trySend(true) // carga inicial imediata
        awaitClose { launcherApps.unregisterCallback(callback) }
    }
        // Instalações em lote geram rajadas de eventos: agrupa 150ms. A 1ª emissão não espera (cold start).
        .debounce { initial -> if (initial) 0L else DEBOUNCE_MS }
        .conflate()
        .map { load().also(cache::write) }
        // Lista da sessão anterior primeiro: a gaveta aparece na hora, a lista nova substitui em seguida.
        .onStart { cache.read()?.let { emit(it) } }
        .flowOn(dispatchers.io)

    private fun mapCategory(category: Int): AppCategory = when (category) {
        ApplicationInfo.CATEGORY_GAME -> AppCategory.GAME
        ApplicationInfo.CATEGORY_AUDIO -> AppCategory.AUDIO
        ApplicationInfo.CATEGORY_VIDEO -> AppCategory.VIDEO
        ApplicationInfo.CATEGORY_IMAGE -> AppCategory.IMAGE
        ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.SOCIAL
        ApplicationInfo.CATEGORY_NEWS -> AppCategory.NEWS
        ApplicationInfo.CATEGORY_MAPS -> AppCategory.MAPS
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.PRODUCTIVITY
        else -> AppCategory.UNDEFINED
    }

    private companion object {
        const val DEBOUNCE_MS = 150L
    }
}
