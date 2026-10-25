package dev.nova.core.domain.usecase

import dev.nova.core.domain.model.AppInfo
import dev.nova.core.domain.repository.AppRepository
import dev.nova.core.domain.repository.HiddenAppsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/** Apps instalados, sem os ocultos (a menos que [includeHidden], após autenticação). */
class ObserveVisibleAppsUseCase @Inject constructor(
    private val apps: AppRepository,
    private val hidden: HiddenAppsRepository,
) {
    operator fun invoke(includeHidden: Boolean = false): Flow<List<AppInfo>> =
        combine(apps.apps, hidden.hiddenKeys) { all, hiddenKeys ->
            if (includeHidden || hiddenKeys.isEmpty()) all else all.filterNot { it.key in hiddenKeys }
        }.distinctUntilChanged()
}
