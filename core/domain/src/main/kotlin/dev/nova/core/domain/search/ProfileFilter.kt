package dev.nova.core.domain.search

import dev.nova.core.domain.model.AppCategory
import dev.nova.core.domain.model.AppInfo
import dev.nova.core.domain.model.UsageProfile

/**
 * Filtra os apps da gaveta conforme o perfil de uso. Ideia portada da outra sessão, com uma diferença:
 * PERSONAL mostra tudo (esconder perfis secundários sumiria com clones como o Dual Messenger).
 * WORK e GAMING caem para "todos" se não houver nada que case (a gaveta nunca fica vazia).
 */
object ProfileFilter {
    fun filter(apps: List<AppInfo>, profile: UsageProfile): List<AppInfo> = when (profile) {
        UsageProfile.PERSONAL -> apps
        UsageProfile.WORK -> apps.filter { it.isWorkProfile }.ifEmpty { apps }
        UsageProfile.GAMING -> apps.filter { it.category == AppCategory.GAME }.ifEmpty { apps }
    }
}
