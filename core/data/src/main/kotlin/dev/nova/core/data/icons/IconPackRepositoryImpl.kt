package dev.nova.core.data.icons

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.nova.core.common.AppDispatchers
import dev.nova.core.domain.model.IconPackInfo
import dev.nova.core.domain.repository.IconPackRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext

/** Descobre packs pelas ações abertas usadas por ADW/Nova (declaradas em `<queries>` no Manifest). */
@Singleton
class IconPackRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: AppDispatchers,
) : IconPackRepository {

    override suspend fun installedPacks(): List<IconPackInfo> = withContext(dispatchers.io) {
        val pm = context.packageManager
        ACTIONS.flatMap { pm.queryIntentActivities(Intent(it), 0) }
            .map { IconPackInfo(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    private companion object {
        val ACTIONS = listOf("org.adw.launcher.THEMES", "com.novalauncher.THEME")
    }
}
