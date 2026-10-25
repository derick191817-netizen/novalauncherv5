package dev.nova.core.data.apps

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.pm.LauncherApps
import android.os.UserManager
import dev.nova.core.common.AppDispatchers
import dev.nova.core.common.ApplicationScope
import dev.nova.core.domain.model.AppInfo
import dev.nova.core.domain.model.AppShortcut
import dev.nova.core.domain.repository.AppRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext

/** Implementação de [AppRepository] sobre [LauncherApps]. */
@Singleton
class AppRepositoryImpl @Inject constructor(
    source: LauncherAppsDataSource,
    @ApplicationScope scope: CoroutineScope,
    private val dispatchers: AppDispatchers,
    private val launcherApps: LauncherApps,
    private val userManager: UserManager,
) : AppRepository {

    // WhileSubscribed(5s): sem coletor (launcher em background) o listener é removido => zero trabalho ocioso.
    override val apps: Flow<List<AppInfo>> = source.observe()
        .distinctUntilChanged()
        .shareIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), replay = 1)

    override fun launch(app: AppInfo): Boolean {
        val user = userManager.getUserForSerialNumber(app.userSerial) ?: return false
        return try {
            launcherApps.startMainActivity(ComponentName(app.packageName, app.className), user, null, null)
            true
        } catch (_: SecurityException) {
            false
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    override suspend fun shortcuts(app: AppInfo): List<AppShortcut> = withContext(dispatchers.io) {
        val user = userManager.getUserForSerialNumber(app.userSerial) ?: return@withContext emptyList()
        try {
            val query = LauncherApps.ShortcutQuery()
                .setPackage(app.packageName)
                .setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
                )
            launcherApps.getShortcuts(query, user).orEmpty()
                .filter { it.isEnabled }
                .take(MAX_SHORTCUTS)
                .map { AppShortcut(it.id, app.packageName, (it.shortLabel ?: it.id).toString(), app.userSerial) }
        } catch (_: SecurityException) {
            emptyList() // só o launcher padrão pode listar atalhos
        } catch (_: IllegalStateException) {
            emptyList() // perfil bloqueado
        }
    }

    override fun launchShortcut(shortcut: AppShortcut): Boolean {
        val user = userManager.getUserForSerialNumber(shortcut.userSerial) ?: return false
        return try {
            launcherApps.startShortcut(shortcut.packageName, shortcut.id, null, null, user)
            true
        } catch (_: SecurityException) {
            false
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val MAX_SHORTCUTS = 4
    }
}
