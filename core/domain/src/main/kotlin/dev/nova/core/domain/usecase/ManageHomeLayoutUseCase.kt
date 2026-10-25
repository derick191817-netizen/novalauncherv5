package dev.nova.core.domain.usecase

import dev.nova.core.domain.model.AppInfo
import dev.nova.core.domain.model.HomeApp
import dev.nova.core.domain.model.HomeFolder
import dev.nova.core.domain.model.HomeLayout
import dev.nova.core.domain.model.HomeWidget
import dev.nova.core.domain.model.UsageProfile
import dev.nova.core.domain.repository.HomeLayoutRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Todas as edições do layout da home (apps, pastas, dock, widgets). Cada chamada é atômica. */
class ManageHomeLayoutUseCase @Inject constructor(private val repo: HomeLayoutRepository) {

    fun observe(profile: UsageProfile): Flow<HomeLayout> = repo.observe(profile)

    /** Primeiro uso do perfil: preenche o dock com apps padrão (Samsung/Google). Idempotente. */
    suspend fun seed(profile: UsageProfile, apps: List<AppInfo>, dockSize: Int, rows: Int = 0, columns: Int = 0) {
        repo.update(profile) { l ->
            if (l.seeded) {
                l
            } else {
                val dock = defaultDock(apps, dockSize)
                var seeded = l.copy(seeded = true, dock = dock)
                // Com a grade conhecida, a home já nasce com os apps mais comuns (em vez de vazia).
                if (rows > 0 && columns > 0) {
                    for (key in defaultHomeApps(apps, dock)) {
                        val c = seeded.findFreeCell(rows, columns)
                        seeded = seeded.withItem(HomeApp(HomeLayout.newId(), c.page, c.row, c.column, key))
                    }
                }
                seeded
            }
        }
    }

    suspend fun addApp(profile: UsageProfile, appKey: String, rows: Int, columns: Int) =
        repo.update(profile) { l ->
            if (l.containsApp(appKey)) {
                l
            } else {
                val c = l.findFreeCell(rows, columns)
                l.withItem(HomeApp(HomeLayout.newId(), c.page, c.row, c.column, appKey))
            }
        }

    suspend fun remove(profile: UsageProfile, itemId: String) = repo.update(profile) { it.without(itemId) }

    suspend fun createFolder(profile: UsageProfile, itemId: String, name: String = "Pasta") =
        repo.update(profile) { l ->
            l.replace(itemId) { item ->
                if (item is HomeApp) {
                    HomeFolder(item.id, item.page, item.row, item.column, name, listOf(item.appKey))
                } else {
                    item
                }
            }
        }

    suspend fun addToFolder(profile: UsageProfile, folderId: String, appKey: String) =
        repo.update(profile) { l ->
            val folder = l.items.filterIsInstance<HomeFolder>().firstOrNull { it.id == folderId }
            if (folder == null || appKey in folder.appKeys) {
                l
            } else {
                l.copy(items = l.items.filterNot { it is HomeApp && it.appKey == appKey })
                    .replace(folderId) { folder.copy(appKeys = folder.appKeys + appKey) }
            }
        }

    suspend fun removeFromFolder(profile: UsageProfile, folderId: String, appKey: String, rows: Int, columns: Int) =
        repo.update(profile) { l ->
            val folder = l.items.filterIsInstance<HomeFolder>().firstOrNull { it.id == folderId }
            if (folder == null) {
                l
            } else {
                val remaining = folder.appKeys - appKey
                val next = if (remaining.isEmpty()) l.without(folderId) else l.replace(folderId) { folder.copy(appKeys = remaining) }
                val c = next.findFreeCell(rows, columns)
                next.withItem(HomeApp(HomeLayout.newId(), c.page, c.row, c.column, appKey))
            }
        }

    suspend fun addToDock(profile: UsageProfile, appKey: String, dockSize: Int) =
        repo.update(profile) { l -> if (appKey in l.dock || l.dock.size >= dockSize) l else l.copy(dock = l.dock + appKey) }

    suspend fun removeFromDock(profile: UsageProfile, appKey: String) =
        repo.update(profile) { l -> l.copy(dock = l.dock - appKey) }

    /** Coloca um widget; com [stack] ligado, empilha em um widget existente do mesmo tamanho. */
    suspend fun addWidget(
        profile: UsageProfile,
        appWidgetId: Int,
        spanX: Int,
        spanY: Int,
        rows: Int,
        columns: Int,
        stack: Boolean,
    ) = repo.update(profile) { l ->
        val sx = spanX.coerceIn(1, columns)
        val sy = spanY.coerceIn(1, rows)
        val target = if (stack) l.items.filterIsInstance<HomeWidget>().lastOrNull { it.spanX == sx && it.spanY == sy } else null
        if (target != null) {
            l.replace(target.id) { target.copy(appWidgetIds = target.appWidgetIds + appWidgetId) }
        } else {
            val c = l.findFreeCell(rows, columns, sx, sy)
            l.withItem(HomeWidget(HomeLayout.newId(), c.page, c.row, c.column, sx, sy, listOf(appWidgetId)))
        }
    }

    /** Move um item na grade; @return false (sem alterar nada) se o destino for inválido ou estiver ocupado. */
    suspend fun moveItem(profile: UsageProfile, itemId: String, page: Int, row: Int, column: Int, rows: Int, columns: Int): Boolean {
        var moved = false
        repo.update(profile) { l ->
            if (l.canPlace(itemId, page, row, column, rows, columns)) {
                moved = true
                l.moved(itemId, page, row, column)
            } else {
                l
            }
        }
        return moved
    }

    /** Renomeia uma pasta (nome vazio é ignorado; máximo de [MAX_FOLDER_NAME] caracteres). */
    suspend fun renameFolder(profile: UsageProfile, folderId: String, name: String) = repo.update(profile) { l ->
        val clean = name.trim().take(MAX_FOLDER_NAME)
        if (clean.isEmpty()) l else l.replace(folderId) { (it as? HomeFolder)?.copy(name = clean) ?: it }
    }

    /** Recoloca itens fora da grade após mudar linhas/colunas. */
    suspend fun reflow(profile: UsageProfile, rows: Int, columns: Int) =
        repo.update(profile) { it.reflow(rows, columns) }

    /** Remove da fila de restauração um widget já tratado (ou descartado). */
    suspend fun consumePendingWidget(profile: UsageProfile, component: String) =
        repo.update(profile) { l -> l.copy(pendingWidgets = l.pendingWidgets - component) }

    private fun defaultDock(apps: List<AppInfo>, dockSize: Int): List<String> {
        val mine = apps.filterNot { it.isWorkProfile } // nunca a cópia do perfil de trabalho
        val picked = pick(mine, DOCK_ROLES).toMutableList()
        val target = minOf(dockSize, MIN_DOCK)
        for (app in mine) {
            if (picked.size >= target) break
            if (app.key !in picked) picked += app.key
        }
        return picked.take(dockSize)
    }

    private fun defaultHomeApps(apps: List<AppInfo>, dock: List<String>): List<String> =
        pick(apps.filterNot { it.isWorkProfile }, HOME_ROLES).filter { it !in dock }.take(MAX_HOME_SEED)

    // Um app por "função": o primeiro pacote da lista que estiver instalado.
    private fun pick(apps: List<AppInfo>, roles: List<List<String>>): List<String> {
        val byPackage = apps.groupBy { it.packageName }
        return roles.mapNotNull { role -> role.firstNotNullOfOrNull { pkg -> byPackage[pkg]?.firstOrNull()?.key } }
    }

    private companion object {
        const val MIN_DOCK = 5
        const val MAX_HOME_SEED = 12
        const val MAX_FOLDER_NAME = 24

        // Samsung primeiro (Note 20 Ultra), depois Google/AOSP. (Ideia de ordem do dock vinda da outra sessão.)
        val DOCK_ROLES = listOf(
            listOf("com.samsung.android.dialer", "com.google.android.dialer", "com.android.dialer"),
            listOf("com.samsung.android.messaging", "com.google.android.apps.messaging"),
            listOf("com.sec.android.app.sbrowser", "com.android.chrome"),
            listOf("com.sec.android.app.camera", "com.android.camera2", "com.google.android.GoogleCamera"),
            listOf("com.sec.android.gallery3d", "com.google.android.apps.photos"),
            listOf("com.android.settings"),
            listOf("com.samsung.android.app.notes", "com.google.android.keep"),
        )
        val HOME_ROLES = listOf(
            listOf("com.sec.android.gallery3d", "com.google.android.apps.photos"),
            listOf("com.samsung.android.calendar", "com.google.android.calendar"),
            listOf("com.sec.android.app.clockpackage", "com.google.android.deskclock"),
            listOf("com.android.vending"),
            listOf("com.google.android.apps.maps"),
            listOf("com.google.android.youtube"),
            listOf("com.google.android.gm"),
            listOf("com.samsung.android.app.contacts", "com.google.android.contacts"),
            listOf("com.sec.android.app.myfiles", "com.google.android.apps.nbu.files"),
            listOf("com.sec.android.app.popupcalculator", "com.google.android.calculator"),
            listOf("com.samsung.android.app.notes", "com.google.android.keep"),
            listOf("com.android.settings"),
        )
    }
}
