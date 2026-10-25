package dev.nova.core.domain.usecase

import androidx.compose.runtime.Immutable
import dev.nova.core.domain.model.AppCategory
import dev.nova.core.domain.model.AppInfo
import javax.inject.Inject

/** Grupo automático de apps por categoria. */
@Immutable
data class SmartFolder(val category: AppCategory, val apps: List<AppInfo>)

/** Agrupa apps por categoria; categorias com menos de 2 apps vão para "outros" (UNDEFINED, sempre por último). */
class GroupSmartFoldersUseCase @Inject constructor() {
    operator fun invoke(apps: List<AppInfo>): List<SmartFolder> {
        val grouped = apps.groupBy { it.category }
        val others = ArrayList<AppInfo>(grouped[AppCategory.UNDEFINED].orEmpty())
        val folders = ArrayList<SmartFolder>()
        for (category in AppCategory.entries) {
            if (category == AppCategory.UNDEFINED) continue
            val list = grouped[category].orEmpty()
            if (list.size >= MIN_GROUP) folders += SmartFolder(category, list) else others += list
        }
        if (others.isNotEmpty()) folders += SmartFolder(AppCategory.UNDEFINED, others.sortedBy { it.label.lowercase() })
        return folders
    }

    private companion object {
        const val MIN_GROUP = 2
    }
}
