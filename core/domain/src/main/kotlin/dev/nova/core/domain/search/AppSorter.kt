package dev.nova.core.domain.search

import dev.nova.core.domain.model.AppInfo
import dev.nova.core.domain.model.AppSort

/**
 * Ordena a gaveta. A ordem alfabética ignora acentos e maiúsculas ("Ágora" fica entre os "A", não depois do "Z"
 * como na ordem padrão de strings). O rótulo é normalizado uma vez por app, não a cada comparação.
 */
object AppSorter {
    fun sort(apps: List<AppInfo>, sort: AppSort): List<AppInfo> = when (sort) {
        AppSort.ALPHABETICAL ->
            apps.map { FuzzyMatcher.normalize(it.label) to it }.sortedBy { it.first }.map { it.second }
        AppSort.RECENTLY_INSTALLED -> apps.sortedByDescending { it.firstInstallTime }
    }
}
